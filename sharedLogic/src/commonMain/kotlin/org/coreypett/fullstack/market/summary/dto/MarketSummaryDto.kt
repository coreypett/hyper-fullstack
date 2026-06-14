package org.coreypett.fullstack.market.summary.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.coreypett.fullstack.network.HyperliquidDoubleSerializer

@Serializable
internal data class MarketSummaryRequestDto(
    val type: String,
)

@Serializable
internal data class MarketMetaDto(
    val universe: List<MarketMetaAssetDto>,
)

@Serializable
internal data class MarketMetaAssetDto(
    val name: String,
)

@Serializable
internal data class MarketAssetContextDto(
    @SerialName("dayNtlVlm")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val dayNotionalVolume: Double? = null,
    @SerialName("midPx")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val midPrice: Double? = null,
    @SerialName("markPx")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val markPrice: Double? = null,
    @SerialName("prevDayPx")
    @Serializable(with = HyperliquidDoubleSerializer::class)
    val previousDayPrice: Double? = null,
)
