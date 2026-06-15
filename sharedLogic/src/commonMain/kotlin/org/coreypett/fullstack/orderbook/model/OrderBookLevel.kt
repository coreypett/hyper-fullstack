package org.coreypett.fullstack.orderbook.model

import org.coreypett.fullstack.market.MarketNumberFormatter

data class OrderBookLevel(
    val side: OrderBookSide,
    val price: Double,
    val size: Double,
    val orderCount: Int,
    val depthFraction: Float,
    val sizeChangeFraction: Float = 0f,
    val change: LevelChange = LevelChange.None,
) {
    val priceText: String = MarketNumberFormatter.price(price)
    val sizeText: String = MarketNumberFormatter.size(size)
}
