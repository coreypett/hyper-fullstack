package org.coreypett.fullstack.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal interface HyperliquidWebSocketClient {
    fun subscribe(subscription: JsonObject): Flow<String>

    class Impl(
        private val webSocketClient: HttpClient = platformWebSocketClient(),
    ) : HyperliquidWebSocketClient {
        override fun subscribe(subscription: JsonObject): Flow<String> = flow {
            webSocketClient.webSocket(urlString = HyperliquidWebSocketUrl) {
                send(Frame.Text(subscribeMessage(subscription)))

                for (frame in incoming) {
                    val textFrame = frame as? Frame.Text ?: continue
                    emit(textFrame.readText())
                }
            }
        }

        private fun subscribeMessage(subscription: JsonObject): String =
            buildJsonObject {
                put("method", "subscribe")
                put("subscription", subscription)
            }.toString()
    }
}

private const val HyperliquidWebSocketUrl = "wss://api.hyperliquid.xyz/ws"
