package org.coreypett.fullstack.network

import kotlinx.serialization.json.Json

internal val HyperliquidJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
