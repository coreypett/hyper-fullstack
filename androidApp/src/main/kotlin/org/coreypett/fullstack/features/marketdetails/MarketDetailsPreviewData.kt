package org.coreypett.fullstack.features.marketdetails

import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.presentation.CandleChartState
import org.coreypett.fullstack.market.candle.presentation.CandleChartStatus
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.summary.model.MarketSummaryChangeDirection
import org.coreypett.fullstack.market.summary.model.MarketSummaryStatus
import org.coreypett.fullstack.market.summary.model.MarketSummaryViewState
import org.coreypett.fullstack.market.trade.model.RecentTradesSide
import org.coreypett.fullstack.market.trade.presentation.RecentTradesRowDisplay
import org.coreypett.fullstack.market.trade.presentation.RecentTradesStatus
import org.coreypett.fullstack.market.trade.presentation.RecentTradesViewState
import org.coreypett.fullstack.orderbook.model.LevelChange
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.presentation.OrderBookRowDisplay
import org.coreypett.fullstack.orderbook.presentation.OrderBookStatus
import org.coreypett.fullstack.orderbook.presentation.OrderBookViewState

internal val MarketDetailsUiState.Companion.Preview: MarketDetailsUiState
    get() = MarketDetailsUiState(
        selectedMarket = MarketSymbol.BTC,
        selectedInterval = CandleInterval.OneHour,
        selectedPrecision = PricePrecision.Four,
        selectedPanel = MarketDetailsPanel.OrderBook,
        summaryState = MarketSummaryViewState(
            status = MarketSummaryStatus.Live,
            message = null,
            midPriceText = "$104,232.10",
            priceChangeText = "+$812.40",
            priceChangePercentText = "+0.79%",
            changeDirection = MarketSummaryChangeDirection.Up,
            volume24hText = "$4.21B",
            high24hText = "$105,018.80",
            low24hText = "$101,640.50",
            openInterestText = "128.42K",
        ),
        candleState = CandleChartState(
            status = CandleChartStatus.Live,
            message = null,
            bars = previewBars(),
        ),
        orderBookState = previewOrderBookState(),
        recentTradesState = previewRecentTradesState(),
    )

private fun previewOrderBookState(): OrderBookViewState {
    val asks = List(8) { index ->
        OrderBookRowDisplay(
            rowKey = "Ask:${104_260 + index * 7}.00",
            side = OrderBookSide.Ask,
            priceText = "$${104_260 + index * 7}.00",
            sizeText = "${(1.24 + index * 0.18).format(2)}",
            orderCountText = "${7 + index}",
            depthFraction = 0.18f + index * 0.08f,
            sizeChangeFraction = if (index == 2) 0.16f else 0f,
            change = if (index == 2) LevelChange.Down else LevelChange.None,
        )
    }
    val bids = List(8) { index ->
        OrderBookRowDisplay(
            rowKey = "Bid:${104_224 - index * 6}.00",
            side = OrderBookSide.Bid,
            priceText = "$${104_224 - index * 6}.00",
            sizeText = "${(1.92 + index * 0.21).format(2)}",
            orderCountText = "${5 + index}",
            depthFraction = 0.22f + index * 0.07f,
            sizeChangeFraction = if (index == 1) 0.18f else 0f,
            change = if (index == 1) LevelChange.Up else LevelChange.None,
        )
    }
    return OrderBookViewState(
        status = OrderBookStatus.Live,
        statusLabel = "Live",
        centerMessage = null,
        midPriceText = "$104,232.10",
        spreadText = "$36.00",
        spreadPercentText = "0.035%",
        asks = asks,
        bids = bids,
    )
}

private fun previewRecentTradesState(): RecentTradesViewState = RecentTradesViewState(
    status = RecentTradesStatus.Live,
    statusLabel = "Live",
    centerMessage = null,
    recentTrades = List(10) { index ->
        RecentTradesRowDisplay(
            side = if (index % 2 == 0) RecentTradesSide.Buy else RecentTradesSide.Sell,
            sideText = if (index % 2 == 0) "Buy" else "Sell",
            priceText = "$${104_210 + index * 11}.50",
            sizeText = "${(0.32 + index * 0.13).format(2)}",
            transactionHash = "0xpreview${index}",
            timeMillis = 1_784_160_000_000L + index * 6_000L,
            tradeId = index.toLong(),
        )
    },
)

private fun previewBars(): List<CandleBar> {
    val start = 1_784_073_600_000L
    return List(48) { index ->
        val open = 103_600.0 + index * 18.0 + if (index % 5 == 0) 110.0 else -30.0
        val close = open + if (index % 3 == 0) 140.0 else -70.0
        CandleBar(
            market = MarketSymbol.BTC,
            interval = CandleInterval.OneHour,
            openTimeMillis = start + index * 3_600_000L,
            closeTimeMillis = start + (index + 1) * 3_600_000L,
            open = open,
            high = maxOf(open, close) + 120.0,
            low = minOf(open, close) - 90.0,
            close = close,
            volume = 1_000_000.0 + index * 48_000.0,
            tradeCount = 800 + index * 12,
        )
    }
}

private fun Double.format(decimals: Int): String {
    val scale = List(decimals) { 10.0 }.fold(1.0) { acc, next -> acc * next }
    val rounded = kotlin.math.round(this * scale) / scale
    val whole = rounded.toLong()
    val fraction = kotlin.math.round((rounded - whole) * scale).toLong()
    return "$whole.${fraction.toString().padStart(decimals, '0')}"
}
