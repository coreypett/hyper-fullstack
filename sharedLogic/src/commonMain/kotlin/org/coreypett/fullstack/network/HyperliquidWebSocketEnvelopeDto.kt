package org.coreypett.fullstack.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class HyperliquidWebSocketEnvelopeDto(
    val channel: String,
    val data: JsonElement? = null,
)
