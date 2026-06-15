package org.coreypett.fullstack.marketdetails.presentation

import org.coreypett.fullstack.market.candle.model.CandleInterval

enum class MarketDetailsPanel(val title: String) {
    OrderBook("Order Book"),
    RecentTrades("Trades"),
}

object MarketDetailsDisplay {
    val panels: List<MarketDetailsPanel> = listOf(
        MarketDetailsPanel.OrderBook,
        MarketDetailsPanel.RecentTrades,
    )

    val chartIntervals: List<CandleInterval> = listOf(
        CandleInterval.OneMinute,
        CandleInterval.FiveMinutes,
        CandleInterval.OneHour,
        CandleInterval.FourHours,
        CandleInterval.OneDay,
    )
}
