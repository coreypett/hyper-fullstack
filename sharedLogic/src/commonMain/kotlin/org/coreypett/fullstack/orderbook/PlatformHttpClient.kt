package org.coreypett.fullstack.orderbook

import io.ktor.client.HttpClient

internal expect fun platformHttpClient(): HttpClient
