package org.coreypett.fullstack.market.candle.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CandleSubscriptionDto(
    val type: String,
    val coin: String,
    val interval: String,
)
