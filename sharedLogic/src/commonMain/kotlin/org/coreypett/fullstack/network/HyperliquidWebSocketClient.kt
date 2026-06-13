package org.coreypett.fullstack.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
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
        override fun <Subscription : Any> subscribeEvents(
            subscription: Subscription,
            serializer: KSerializer<Subscription>,
        ): Flow<HyperliquidWebSocketEvent> = flow {
            val requestText = encodeSubscribeRequest(
                subscription = subscription,
                serializer = serializer,
                json = json,
            )
            var attempt = 0L

            while (currentCoroutineContext().isActive) {
                try {
                    webSocketClient.webSocket(urlString = HyperliquidWebSocketUrl) {
                        attempt = 0L
                        send(Frame.Text(requestText))

                        for (frame in incoming) {
                            val textFrame = frame as? Frame.Text ?: continue
                            emit(HyperliquidWebSocketEvent.Text(textFrame.readText()))
                        }
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    attempt += 1
                    val delayMillis = retryPolicy.delayMillis(attempt, error)
                    emit(
                        HyperliquidWebSocketEvent.Reconnecting(
                            attempt = attempt,
                            delayMillis = delayMillis,
                            reason = error.message,
                        ),
                    )
                    delay(delayMillis)
                    continue
                }

                attempt += 1
                val delayMillis = retryPolicy.delayMillis(attempt, null)
                emit(
                    HyperliquidWebSocketEvent.Reconnecting(
                        attempt = attempt,
                        delayMillis = delayMillis,
                        reason = "WebSocket closed",
                    ),
                )
                delay(delayMillis)
            }
        }
    }
}

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
): String {
    val request = HyperliquidWebSocketRequestDto(
        method = SubscribeMethod,
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
private const val HyperliquidWebSocketUrl = "wss://api.hyperliquid.xyz/ws"
