package org.coreypett.fullstack.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    ): Flow<String>

    class Impl(
        private val webSocketClient: HttpClient = platformWebSocketClient(),
        private val json: Json = HyperliquidJson,
    ) : HyperliquidWebSocketClient {
        override fun <Subscription : Any> subscribe(
            subscription: Subscription,
            serializer: KSerializer<Subscription>,
        ): Flow<String> = flow {
            webSocketClient.webSocket(urlString = HyperliquidWebSocketUrl) {
                val request = HyperliquidWebSocketRequestDto(
                    method = SubscribeMethod,
                    subscription = json.encodeToJsonElement(serializer, subscription),
                )
                send(Frame.Text(json.encodeToString(HyperliquidWebSocketRequestDto.serializer(), request)))

                for (frame in incoming) {
                    val textFrame = frame as? Frame.Text ?: continue
                    emit(textFrame.readText())
                }
            }
        }
    }
}

@Serializable
private data class HyperliquidWebSocketRequestDto(
    val method: String,
    val subscription: JsonElement,
)

private const val SubscribeMethod = "subscribe"
private const val HyperliquidWebSocketUrl = "wss://api.hyperliquid.xyz/ws"
