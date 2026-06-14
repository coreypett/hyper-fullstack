package org.coreypett.fullstack.market.trade.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.coreypett.fullstack.network.HyperliquidDoubleSerializer

@Serializable
internal data class WsTradeDto(
    val coin: String,
    val side: String,
    @SerialName("px")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val price: Double,
    @SerialName("sz")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val size: Double,
    val hash: String,
    val time: Long,
    val tid: Long,
    val users: List<String> = emptyList(),
)
