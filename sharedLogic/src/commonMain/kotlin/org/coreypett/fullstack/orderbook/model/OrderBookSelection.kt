package org.coreypett.fullstack.orderbook.model

data class OrderBookSelection(
    val market: MarketSymbol = MarketSymbol.BTC,
    val precision: PricePrecision = PricePrecision.Five,
)
