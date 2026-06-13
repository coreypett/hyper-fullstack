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
    val spread: Double? = bestAsk?.let { ask -> bestBid?.let { bid -> ask.price - bid.price } }
    val spreadText: String = spread?.let(MarketNumberFormatter::price) ?: "--"
}
