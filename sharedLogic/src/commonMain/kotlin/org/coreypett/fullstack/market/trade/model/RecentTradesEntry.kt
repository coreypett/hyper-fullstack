package org.coreypett.fullstack.market.trade.model

import org.coreypett.fullstack.market.MarketNumberFormatter
import org.coreypett.fullstack.market.model.MarketSymbol

data class RecentTradesEntry(
    val market: MarketSymbol,
    val side: RecentTradesSide,
    val price: Double,
    val size: Double,
    val transactionHash: String,
    val timeMillis: Long,
    val tradeId: Long,
    val buyer: String?,
    val seller: String?,
) {
    val priceText: String = MarketNumberFormatter.price(price)
    val sizeText: String = MarketNumberFormatter.size(size)
}
