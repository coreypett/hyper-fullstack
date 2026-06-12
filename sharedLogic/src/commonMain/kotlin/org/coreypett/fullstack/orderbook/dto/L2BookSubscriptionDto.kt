package org.coreypett.fullstack.orderbook.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class L2BookSubscriptionDto(
    val type: String,
    val coin: String,
    val nSigFigs: Int,
)
