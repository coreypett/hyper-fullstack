package org.coreypett.fullstack.market.summary.model

import org.coreypett.fullstack.market.MarketNumberFormatter
import org.coreypett.fullstack.market.model.MarketSymbol

data class MarketSummary(
    val market: MarketSymbol,
    val midPrice: Double,
    val previousDayPrice: Double?,
    val volumeUsd24h: Double?,
    val high24h: Double?,
    val low24h: Double?,
) {
    val priceChange: Double? = previousDayPrice?.let { midPrice - it }
    val priceChangePercent: Double? = previousDayPrice
        ?.takeIf { it != 0.0 }
        ?.let { previous -> ((midPrice - previous) / previous) * 100.0 }
}

sealed interface MarketSummaryUiState {
    data object Connecting : MarketSummaryUiState
    data class Live(val summary: MarketSummary) : MarketSummaryUiState
    data class Stale(val summary: MarketSummary, val message: String) : MarketSummaryUiState
    data class Failed(val message: String) : MarketSummaryUiState
}

data class MarketSummaryViewState(
    val status: MarketSummaryStatus,
    val message: String?,
    val midPriceText: String?,
    val priceChangeText: String?,
    val priceChangePercentText: String?,
    val changeDirection: MarketSummaryChangeDirection,
    val volume24hText: String?,
    val high24hText: String?,
    val low24hText: String?,
) {
    companion object {
        val Connecting = MarketSummaryViewState(
            status = MarketSummaryStatus.Connecting,
            message = "Loading market",
            midPriceText = null,
            priceChangeText = null,
            priceChangePercentText = null,
            changeDirection = MarketSummaryChangeDirection.Flat,
            volume24hText = null,
            high24hText = null,
            low24hText = null,
        )

        fun from(uiState: MarketSummaryUiState): MarketSummaryViewState = when (uiState) {
            MarketSummaryUiState.Connecting -> Connecting
            is MarketSummaryUiState.Failed -> Connecting.copy(
                status = MarketSummaryStatus.Failed,
                message = uiState.message,
            )
            is MarketSummaryUiState.Live -> uiState.summary.toViewState(MarketSummaryStatus.Live, null)
            is MarketSummaryUiState.Stale -> uiState.summary.toViewState(
                status = MarketSummaryStatus.Stale,
                message = uiState.message,
            )
        }
    }
}

enum class MarketSummaryStatus {
    Connecting,
    Live,
    Stale,
    Failed,
}

enum class MarketSummaryChangeDirection {
    Up,
    Down,
    Flat,
}

private fun MarketSummary.toViewState(
    status: MarketSummaryStatus,
    message: String?,
): MarketSummaryViewState {
    val change = priceChange
    return MarketSummaryViewState(
        status = status,
        message = message,
        midPriceText = MarketNumberFormatter.price(midPrice),
        priceChangeText = change?.let(::formatSignedPrice),
        priceChangePercentText = priceChangePercent?.let(::formatSignedPercent),
        changeDirection = when {
            change == null -> MarketSummaryChangeDirection.Flat
            change > 0.0 -> MarketSummaryChangeDirection.Up
            change < 0.0 -> MarketSummaryChangeDirection.Down
            else -> MarketSummaryChangeDirection.Flat
        },
        volume24hText = volumeUsd24h?.let(::formatCompactUsd),
        high24hText = high24h?.let(MarketNumberFormatter::price),
        low24hText = low24h?.let(MarketNumberFormatter::price),
    )
}

private fun formatSignedPrice(value: Double): String {
    val sign = when {
        value > 0.0 -> "+"
        value < 0.0 -> "-"
        else -> ""
    }
    return "$sign${MarketNumberFormatter.price(kotlin.math.abs(value))}"
}

private fun formatSignedPercent(value: Double): String {
    val sign = when {
        value > 0.0 -> "+"
        value < 0.0 -> "-"
        else -> ""
    }
    return "$sign${kotlin.math.abs(value).formatFixed(2)}%"
}

private fun formatCompactUsd(value: Double): String {
    val absolute = kotlin.math.abs(value)
    val formatted = when {
        absolute >= 1_000_000_000.0 -> "${(absolute / 1_000_000_000.0).formatFixed(2)}B"
        absolute >= 1_000_000.0 -> "${(absolute / 1_000_000.0).formatFixed(2)}M"
        absolute >= 1_000.0 -> "${(absolute / 1_000.0).formatFixed(2)}K"
        else -> absolute.formatFixed(2)
    }
    return if (value < 0.0) "-$$formatted" else "$$formatted"
}

private fun Double.formatFixed(decimals: Int): String {
    val scale = pow10(decimals)
    val rounded = kotlin.math.round(this * scale) / scale
    val whole = rounded.toLong()
    val fraction = kotlin.math.round((rounded - whole) * scale).toLong()
    return "$whole.${fraction.toString().padStart(decimals, '0')}"
}

private fun pow10(decimals: Int): Double {
    var result = 1.0
    repeat(decimals) {
        result *= 10.0
    }
    return result
}
