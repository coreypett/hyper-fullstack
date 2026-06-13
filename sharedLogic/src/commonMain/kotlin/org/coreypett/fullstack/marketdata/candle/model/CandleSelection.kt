package org.coreypett.fullstack.marketdata.candle.model

import org.coreypett.fullstack.market.model.MarketSymbol

data class CandleSelection(
    val market: MarketSymbol = MarketSymbol.BTC,
    val interval: CandleInterval = CandleInterval.OneHour,
)
