package org.coreypett.fullstack.orderbook.model

import org.coreypett.fullstack.market.MarketNumberFormatter
import org.coreypett.fullstack.market.model.MarketSymbol

data class OrderBookSnapshot(
    val market: MarketSymbol,
    val precision: PricePrecision,
    val timeMillis: Long,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
) {
    val bestBid: OrderBookLevel? = bids.firstOrNull()
    val bestAsk: OrderBookLevel? = asks.firstOrNull()
    val midPrice: Double? = bestAsk?.let { ask -> bestBid?.let { bid -> (ask.price + bid.price) / 2 } }
    val spread: Double? = bestAsk?.let { ask -> bestBid?.let { bid -> ask.price - bid.price } }
    val spreadPercent: Double? = midPrice
        ?.takeIf { it != 0.0 }
        ?.let { mid -> spread?.let { (it / mid) * 100.0 } }
    val spreadText: String = spread?.let(MarketNumberFormatter::price) ?: "--"
    val spreadPercentText: String = spreadPercent?.let(::formatSpreadPercent) ?: "--"
}

private fun formatSpreadPercent(value: Double): String {
    val scaled = kotlin.math.round(value * 1000.0).toLong()
    val sign = if (scaled < 0) "-" else ""
    val absolute = kotlin.math.abs(scaled)
    val whole = absolute / 1000
    val fraction = absolute % 1000
    return "$sign$whole.${fraction.toString().padStart(3, '0')}%"
}
