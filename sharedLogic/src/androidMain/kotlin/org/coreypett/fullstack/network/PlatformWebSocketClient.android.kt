package org.coreypett.fullstack.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import java.util.concurrent.TimeUnit

internal actual fun platformWebSocketClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectTimeout(WebSocketConnectTimeoutMillis, TimeUnit.MILLISECONDS)
            writeTimeout(WebSocketWriteTimeoutMillis, TimeUnit.MILLISECONDS)
            readTimeout(0, TimeUnit.MILLISECONDS)
            pingInterval(WebSocketPingIntervalMillis, TimeUnit.MILLISECONDS)
            retryOnConnectionFailure(true)
        }
    }
    install(WebSockets)
}

private const val WebSocketConnectTimeoutMillis = 5_000L
private const val WebSocketWriteTimeoutMillis = 5_000L
