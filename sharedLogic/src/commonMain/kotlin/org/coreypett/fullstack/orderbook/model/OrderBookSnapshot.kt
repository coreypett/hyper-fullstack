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
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    val whole = rounded.toLong()
    val fraction = kotlin.math.round((rounded - whole) * 100.0).toLong()
    return "$whole.${fraction.toString().padStart(2, '0')}%"
}
