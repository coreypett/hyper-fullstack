package org.coreypett.fullstack.features.marketdetails

import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.presentation.CandleChartFeature
import org.coreypett.fullstack.market.candle.presentation.CandleChartState
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.summary.model.MarketSummaryViewState
import org.coreypett.fullstack.market.summary.presentation.MarketSummaryFeature
import org.coreypett.fullstack.market.trade.presentation.RecentTradesFeature
import org.coreypett.fullstack.market.trade.presentation.RecentTradesViewState
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.presentation.OrderBookFeature
import org.coreypett.fullstack.orderbook.presentation.OrderBookViewState

data class MarketDetailsFeatures(
    val orderBookFeature: OrderBookFeature,
    val candleChartFeature: CandleChartFeature,
    val marketSummaryFeature: MarketSummaryFeature,
    val recentTradesFeature: RecentTradesFeature,
)

data class MarketDetailsUiState(
    val selectedMarket: MarketSymbol,
    val selectedInterval: CandleInterval,
    val selectedPrecision: PricePrecision,
    val selectedPanel: MarketDetailsPanel,
    val summaryState: MarketSummaryViewState,
    val candleState: CandleChartState,
    val orderBookState: OrderBookViewState,
    val recentTradesState: RecentTradesViewState,
) {
    companion object
}

data class MarketDetailsCallbacks(
    val onMarketSelected: (MarketSymbol) -> Unit,
    val onIntervalSelected: (CandleInterval) -> Unit,
    val onPrecisionSelected: (PricePrecision) -> Unit,
    val onPanelSelected: (MarketDetailsPanel) -> Unit,
) {
    companion object {
        val Empty = MarketDetailsCallbacks(
            onMarketSelected = {},
            onIntervalSelected = {},
            onPrecisionSelected = {},
            onPanelSelected = {},
        )
    }
}

enum class MarketDetailsPanel(val title: String) {
    OrderBook("Order Book"),
    RecentTrades("Trades"),
}

val MarketDetailsChartIntervals: List<CandleInterval> = listOf(
    CandleInterval.OneMinute,
    CandleInterval.FiveMinutes,
    CandleInterval.OneHour,
    CandleInterval.FourHours,
    CandleInterval.OneDay,
)
