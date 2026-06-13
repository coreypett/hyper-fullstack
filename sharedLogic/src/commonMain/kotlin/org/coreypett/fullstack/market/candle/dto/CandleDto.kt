package org.coreypett.fullstack.market.candle.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.coreypett.fullstack.network.HyperliquidDoubleSerializer

@Serializable
internal data class CandleDto(
    @SerialName("t")
    val openTimeMillis: Long,
    @SerialName("T")
    val closeTimeMillis: Long,
    @SerialName("s")
    val coin: String,
    @SerialName("i")
    val interval: String,
    @SerialName("o")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val open: Double,
    @SerialName("c")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val close: Double,
    @SerialName("h")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val high: Double,
    @SerialName("l")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val low: Double,
    @SerialName("v")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val volume: Double,
    @SerialName("n")
    val tradeCount: Int,
)
