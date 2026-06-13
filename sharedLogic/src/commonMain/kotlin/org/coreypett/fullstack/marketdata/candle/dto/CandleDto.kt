package org.coreypett.fullstack.marketdata.candle.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val open: Double,
    @SerialName("c")
    val close: Double,
    @SerialName("h")
    val high: Double,
    @SerialName("l")
    val low: Double,
    @SerialName("v")
    val volume: Double,
    @SerialName("n")
    val tradeCount: Int,
)
