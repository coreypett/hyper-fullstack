package org.coreypett.fullstack.market.candle.model

import org.coreypett.fullstack.market.MarketNumberFormatter
import org.coreypett.fullstack.market.model.MarketSymbol

data class CandleBar(
    val market: MarketSymbol,
    val interval: CandleInterval,
    val openTimeMillis: Long,
    val closeTimeMillis: Long,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Double,
    val tradeCount: Int,
) {
    val openText: String = MarketNumberFormatter.price(open)
    val closeText: String = MarketNumberFormatter.price(close)
    val highText: String = MarketNumberFormatter.price(high)
    val lowText: String = MarketNumberFormatter.price(low)
}
