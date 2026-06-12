package org.coreypett.fullstack.network

import io.ktor.client.HttpClient

internal expect fun platformWebSocketClient(): HttpClient
