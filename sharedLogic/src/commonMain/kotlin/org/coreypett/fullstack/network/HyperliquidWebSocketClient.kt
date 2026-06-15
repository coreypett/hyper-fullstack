package org.coreypett.fullstack.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Hyperliquid WebSocket subscription transport.
 *
 * Docs: https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/websocket/subscriptions
 */
internal interface HyperliquidWebSocketClient {
    companion object {
        val Shared: HyperliquidWebSocketClient by lazy { Impl() }
    }

    fun <Subscription : Any> subscribe(
        subscription: Subscription,
        serializer: KSerializer<Subscription>,
    ): Flow<String> = subscribeEvents(
        subscription = subscription,
        serializer = serializer,
    ).mapNotNull { event ->
        (event as? HyperliquidWebSocketEvent.Text)?.text
    }

    fun <Subscription : Any> subscribeEvents(
        subscription: Subscription,
        serializer: KSerializer<Subscription>,
    ): Flow<HyperliquidWebSocketEvent>

    class Impl(
        private val webSocketClient: HttpClient = platformWebSocketClient(),
        private val json: Json = HyperliquidJson,
        private val retryPolicy: HyperliquidWebSocketRetryPolicy = HyperliquidWebSocketRetryPolicy(),
    ) : HyperliquidWebSocketClient {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val mutex = Mutex()
        private val events = MutableSharedFlow<HyperliquidWebSocketEvent>(
            extraBufferCapacity = EventBufferCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        private val subscriptionCounts = mutableMapOf<String, Int>()
        private var connectionJob: Job? = null
        private var activeSession: DefaultClientWebSocketSession? = null

        override fun <Subscription : Any> subscribeEvents(
            subscription: Subscription,
            serializer: KSerializer<Subscription>,
        ): Flow<HyperliquidWebSocketEvent> = callbackFlow {
            val requestTexts = HyperliquidWebSocketRequestTexts(
                subscribeText = encodeSubscribeRequest(
                    subscription = subscription,
                    serializer = serializer,
                    json = json,
                ),
                unsubscribeText = encodeUnsubscribeRequest(
                    subscription = subscription,
                    serializer = serializer,
                    json = json,
                ),
            )
            val collector = launch {
                events.collect { event ->
                    trySend(event)
                }
            }

            try {
                register(requestTexts.subscribeText)
            } catch (error: Throwable) {
                collector.cancel()
                close(error)
            }

            awaitClose {
                collector.cancel()
                scope.launch { unregister(requestTexts) }
            }
        }.buffer(Channel.CONFLATED)

        private suspend fun register(requestText: String) {
            mutex.lock()
            try {
                val activeCount = subscriptionCounts[requestText] ?: 0
                subscriptionCounts[requestText] = activeCount + 1
                ensureConnectionLocked()

                if (activeCount == 0) {
                    activeSession?.sendWebSocketRequest(requestText)
                }
            } finally {
                mutex.unlock()
            }
        }

        private suspend fun unregister(requestTexts: HyperliquidWebSocketRequestTexts) {
            var jobToCancel: Job? = null

            mutex.lock()
            try {
                val requestText = requestTexts.subscribeText
                val activeCount = subscriptionCounts[requestText] ?: return
                if (activeCount <= 1) {
                    subscriptionCounts.remove(requestText)
                } else {
                    subscriptionCounts[requestText] = activeCount - 1
                }

                if (subscriptionCounts.isEmpty()) {
                    activeSession = null
                    jobToCancel = connectionJob
                    connectionJob = null
                } else {
                    if (activeCount <= 1) {
                        activeSession?.sendWebSocketRequest(requestTexts.unsubscribeText)
                    }
                }
            } finally {
                mutex.unlock()
            }

            jobToCancel?.cancel()
        }

        private fun ensureConnectionLocked() {
            if (connectionJob?.isActive == true) return
            connectionJob = scope.launch { connectLoop() }
        }

        private suspend fun connectLoop() {
            var attempt = 0L

            while (currentCoroutineContext().isActive) {
                var openedSession: DefaultClientWebSocketSession? = null
                try {
                    webSocketClient.webSocket(urlString = HyperliquidWebSocketUrl) {
                        attempt = 0L
                        openedSession = this

                        mutex.lock()
                        try {
                            activeSession = this@webSocket
                            subscriptionCounts.keys.forEach { requestText ->
                                send(Frame.Text(requestText))
                            }
                        } finally {
                            mutex.unlock()
                        }

                        for (frame in incoming) {
                            val textFrame = frame as? Frame.Text ?: continue
                            events.tryEmit(HyperliquidWebSocketEvent.Text(textFrame.readText()))
                        }
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    attempt += 1
                    val delayMillis = retryPolicy.delayMillis(attempt, error)
                    events.tryEmit(
                        HyperliquidWebSocketEvent.Reconnecting(
                            attempt = attempt,
                            delayMillis = delayMillis,
                            reason = error.message,
                        ),
                    )
                    delay(delayMillis)
                    continue
                } finally {
                    val session = openedSession
                    if (session != null) {
                        mutex.withLock {
                            if (activeSession === session) {
                                activeSession = null
                            }
                        }
                    }
                }

                attempt += 1
                val delayMillis = retryPolicy.delayMillis(attempt, null)
                events.tryEmit(
                    HyperliquidWebSocketEvent.Reconnecting(
                        attempt = attempt,
                        delayMillis = delayMillis,
                        reason = "WebSocket closed",
                    ),
                )
                delay(delayMillis)
            }
        }

        private suspend fun DefaultClientWebSocketSession.sendWebSocketRequest(requestText: String) {
            try {
                send(Frame.Text(requestText))
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
            }
        }
    }
}

private data class HyperliquidWebSocketRequestTexts(
    val subscribeText: String,
    val unsubscribeText: String,
)

internal sealed interface HyperliquidWebSocketEvent {
    data class Text(val text: String) : HyperliquidWebSocketEvent
    data class Reconnecting(
        val attempt: Long,
        val delayMillis: Long,
        val reason: String?,
    ) : HyperliquidWebSocketEvent
}

internal data class HyperliquidWebSocketRetryPolicy(
    val initialDelayMillis: Long = 500L,
    val maxDelayMillis: Long = 10_000L,
    val multiplier: Int = 2,
    val maxReconnectAttempts: Long = Long.MAX_VALUE,
) {
    init {
        require(initialDelayMillis > 0L)
        require(maxDelayMillis >= initialDelayMillis)
        require(multiplier >= 1)
        require(maxReconnectAttempts > 0L)
    }

    fun delayMillis(attempt: Long, cause: Throwable?): Long {
        if (attempt > maxReconnectAttempts) {
            throw cause ?: IllegalStateException("WebSocket closed after $maxReconnectAttempts reconnect attempts")
        }

        var delayMillis = initialDelayMillis
        repeat((attempt - 1).coerceAtMost(62).toInt()) {
            delayMillis = (delayMillis * multiplier).coerceAtMost(maxDelayMillis)
        }
        return delayMillis
    }
}

internal fun <Subscription : Any> encodeSubscribeRequest(
    subscription: Subscription,
    serializer: KSerializer<Subscription>,
    json: Json = HyperliquidJson,
): String = encodeRequest(
    method = SubscribeMethod,
    subscription = subscription,
    serializer = serializer,
    json = json,
)

internal fun <Subscription : Any> encodeUnsubscribeRequest(
    subscription: Subscription,
    serializer: KSerializer<Subscription>,
    json: Json = HyperliquidJson,
): String = encodeRequest(
    method = UnsubscribeMethod,
    subscription = subscription,
    serializer = serializer,
    json = json,
)

private fun <Subscription : Any> encodeRequest(
    method: String,
    subscription: Subscription,
    serializer: KSerializer<Subscription>,
    json: Json,
): String {
    val request = HyperliquidWebSocketRequestDto(
        method = method,
        subscription = json.encodeToJsonElement(serializer, subscription),
    )
    return json.encodeToString(HyperliquidWebSocketRequestDto.serializer(), request)
}

@Serializable
private data class HyperliquidWebSocketRequestDto(
    val method: String,
    val subscription: JsonElement,
)

private const val SubscribeMethod = "subscribe"
private const val UnsubscribeMethod = "unsubscribe"
private const val HyperliquidWebSocketUrl = "wss://api.hyperliquid.xyz/ws"
private const val EventBufferCapacity = 128
