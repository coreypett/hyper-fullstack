package org.coreypett.fullstack.market.trade.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TradesSubscriptionDto(
    val type: String,
    val coin: String,
)
