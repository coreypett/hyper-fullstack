package org.coreypett.fullstack.market.price.model

import org.coreypett.fullstack.market.MarketNumberFormatter
import org.coreypett.fullstack.market.model.MarketSymbol

data class LivePrice(
    val market: MarketSymbol,
    val price: Double,
) {
    val priceText: String = MarketNumberFormatter.price(price)
}
