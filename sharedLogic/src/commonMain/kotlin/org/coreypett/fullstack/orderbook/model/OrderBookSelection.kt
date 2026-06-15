package org.coreypett.fullstack.orderbook.model

import org.coreypett.fullstack.market.model.MarketSymbol

data class OrderBookSelection(
    val market: MarketSymbol = MarketSymbol.BTC,
    val precision: PricePrecision = PricePrecision.Two,
)
