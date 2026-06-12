package org.coreypett.fullstack.orderbook.model

enum class MarketSymbol(val wireName: String, val displayName: String) {
    BTC("BTC", "BTC"),
    ETH("ETH", "ETH"),
}

enum class PricePrecision(val nSigFigs: Int, val label: String) {
    Two(2, "2 sig figs"),
    Three(3, "3 sig figs"),
    Four(4, "4 sig figs"),
    Five(5, "5 sig figs"),
}

enum class OrderBookSide {
    Bid,
    Ask,
}

enum class LevelChange {
    None,
    Up,
    Down,
}

data class OrderBookSelection(
    val market: MarketSymbol = MarketSymbol.BTC,
    val precision: PricePrecision = PricePrecision.Five,
)

data class OrderBookLevel(
    val side: OrderBookSide,
    val price: Double,
    val size: Double,
    val orderCount: Int,
    val depthFraction: Float,
    val change: LevelChange = LevelChange.None,
) {
    val priceText: String = formatPrice(price)
    val sizeText: String = formatSize(size)
}

data class OrderBookSnapshot(
    val market: MarketSymbol,
    val precision: PricePrecision,
    val timeMillis: Long,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
) {
    val bestBid: OrderBookLevel? = bids.firstOrNull()
    val bestAsk: OrderBookLevel? = asks.firstOrNull()
    val spread: Double? = bestAsk?.let { ask -> bestBid?.let { bid -> ask.price - bid.price } }
    val spreadText: String = spread?.let(::formatPrice) ?: "--"
}

sealed interface OrderBookUiState {
    data object Connecting : OrderBookUiState
    data class Live(val snapshot: OrderBookSnapshot) : OrderBookUiState
    data class Failed(val message: String) : OrderBookUiState
}

private fun formatPrice(value: Double): String = when {
    value >= 10_000.0 -> value.toStringWithDecimals(0)
    value >= 1_000.0 -> value.toStringWithDecimals(1)
    else -> value.toStringWithDecimals(2)
}

private fun formatSize(value: Double): String = when {
    value >= 1_000.0 -> "${(value / 1_000.0).toStringWithDecimals(2)}K"
    value >= 1.0 -> value.toStringWithDecimals(4)
    else -> value.toStringWithDecimals(6)
}

private fun Double.toStringWithDecimals(decimals: Int): String {
    val multiplier = pow10(decimals)
    val rounded = kotlin.math.round(this * multiplier) / multiplier
    val raw = rounded.toString()
    val dotIndex = raw.indexOf('.')
    return when {
        decimals == 0 -> raw.substringBefore('.')
        dotIndex == -1 -> raw + "." + "0".repeat(decimals)
        raw.length - dotIndex - 1 >= decimals -> raw.take(dotIndex + decimals + 1)
        else -> raw + "0".repeat(decimals - (raw.length - dotIndex - 1))
    }
}

private fun pow10(decimals: Int): Double {
    var value = 1.0
    repeat(decimals) {
        value *= 10.0
    }
    return value
}
