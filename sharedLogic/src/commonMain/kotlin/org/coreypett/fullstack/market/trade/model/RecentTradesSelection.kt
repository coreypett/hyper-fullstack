package org.coreypett.fullstack.market.trade.model

import org.coreypett.fullstack.market.model.MarketSymbol

data class RecentTradesSelection(
    val market: MarketSymbol = MarketSymbol.BTC,
)
