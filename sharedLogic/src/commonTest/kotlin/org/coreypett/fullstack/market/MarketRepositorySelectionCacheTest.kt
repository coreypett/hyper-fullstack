package org.coreypett.fullstack.market

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.coreypett.fullstack.market.candle.model.CandleBar
import org.coreypett.fullstack.market.candle.model.CandleChartUiState
import org.coreypett.fullstack.market.candle.model.CandleHistoryRange
import org.coreypett.fullstack.market.candle.model.CandleInterval
import org.coreypett.fullstack.market.candle.model.CandleSelection
import org.coreypett.fullstack.market.candle.repository.CandleRepository
import org.coreypett.fullstack.market.candle.service.CandleService
import org.coreypett.fullstack.market.model.MarketSymbol
import org.coreypett.fullstack.market.price.model.LivePrice
import org.coreypett.fullstack.market.price.model.LivePriceUiState
import org.coreypett.fullstack.market.price.repository.LivePriceRepository
import org.coreypett.fullstack.market.price.service.LivePriceService
import org.coreypett.fullstack.market.summary.model.MarketSummary
import org.coreypett.fullstack.market.summary.model.MarketSummaryUiState
import org.coreypett.fullstack.market.summary.repository.MarketSummaryRepository
import org.coreypett.fullstack.market.summary.service.MarketSummaryService
import org.coreypett.fullstack.market.trade.model.RecentTradesEntry
import org.coreypett.fullstack.market.trade.model.RecentTradesSelection
import org.coreypett.fullstack.market.trade.model.RecentTradesSide
import org.coreypett.fullstack.market.trade.model.RecentTradesUiState
import org.coreypett.fullstack.market.trade.repository.RecentTradesRepository
import org.coreypett.fullstack.market.trade.service.RecentTradesService
import org.coreypett.fullstack.network.RealtimeFeedEvent
import org.coreypett.fullstack.orderbook.model.OrderBookLevel
import org.coreypett.fullstack.orderbook.model.OrderBookSelection
import org.coreypett.fullstack.orderbook.model.OrderBookSide
import org.coreypett.fullstack.orderbook.model.OrderBookSnapshot
import org.coreypett.fullstack.orderbook.model.OrderBookUiState
import org.coreypett.fullstack.orderbook.model.PricePrecision
import org.coreypett.fullstack.orderbook.repository.OrderBookRepository
import org.coreypett.fullstack.orderbook.service.OrderBookService

class MarketRepositorySelectionCacheTest {
    @Test
    fun livePricesReuseCachedMarketOnReselect() = runBlocking {
        val market = MutableStateFlow(MarketSymbol.BTC)
        val states = mutableListOf<LivePriceUiState>()
        val repository = LivePriceRepository.Impl(FakeLivePriceService())
        val job = collectInto(states) { repository.states(market) }

        waitUntil { states.any { it is LivePriceUiState.Live && it.price.market == MarketSymbol.BTC } }
        market.value = MarketSymbol.ETH
        waitUntil { states.any { it is LivePriceUiState.Live && it.price.market == MarketSymbol.ETH } }

        val beforeReselect = states.size
        market.value = MarketSymbol.BTC
        waitUntil { states.size > beforeReselect }

        val reselectedState = states[beforeReselect] as LivePriceUiState.Live
        assertEquals(MarketSymbol.BTC, reselectedState.price.market)

        job.cancelAndJoin()
    }

    @Test
    fun marketSummariesReuseCachedMarketOnReselect() = runBlocking {
        val market = MutableStateFlow(MarketSymbol.BTC)
        val states = mutableListOf<MarketSummaryUiState>()
        val repository = MarketSummaryRepository.Impl(FakeMarketSummaryService())
        val job = collectInto(states) { repository.states(market) }

        waitUntil { states.any { it is MarketSummaryUiState.Live && it.summary.market == MarketSymbol.BTC } }
        market.value = MarketSymbol.ETH
        waitUntil { states.any { it is MarketSummaryUiState.Live && it.summary.market == MarketSymbol.ETH } }

        val beforeReselect = states.size
        market.value = MarketSymbol.BTC
        waitUntil { states.size > beforeReselect }

        val reselectedState = states[beforeReselect] as MarketSummaryUiState.Live
        assertEquals(MarketSymbol.BTC, reselectedState.summary.market)

        job.cancelAndJoin()
    }

    @Test
    fun candleChartsReuseCachedSelectionOnReselect() = runBlocking {
        val selection = MutableStateFlow(CandleSelection(market = MarketSymbol.BTC))
        val range = MutableStateFlow(CandleHistoryRange(startTimeMillis = 0, endTimeMillis = 1))
        val states = mutableListOf<CandleChartUiState>()
        val repository = CandleRepository.Impl(FakeCandleService())
        val job = collectInto(states) { repository.states(selection, range) }

        waitUntil { states.any { it is CandleChartUiState.Live && it.bars.firstOrNull()?.market == MarketSymbol.BTC } }
        selection.value = CandleSelection(market = MarketSymbol.ETH)
        waitUntil { states.any { it is CandleChartUiState.Live && it.bars.firstOrNull()?.market == MarketSymbol.ETH } }

        val beforeReselect = states.size
        selection.value = CandleSelection(market = MarketSymbol.BTC)
        waitUntil { states.size > beforeReselect }

        val reselectedState = states[beforeReselect] as CandleChartUiState.Live
        assertEquals(MarketSymbol.BTC, reselectedState.bars.single().market)

        job.cancelAndJoin()
    }

    @Test
    fun recentTradesReuseCachedSelectionOnReselect() = runBlocking {
        val selection = MutableStateFlow(RecentTradesSelection(market = MarketSymbol.BTC))
        val states = mutableListOf<RecentTradesUiState>()
        val repository = RecentTradesRepository.Impl(FakeRecentTradesService())
        val job = collectInto(states) { repository.states(selection) }

        waitUntil { states.any { it is RecentTradesUiState.Live && it.recentTrades.firstOrNull()?.market == MarketSymbol.BTC } }
        selection.value = RecentTradesSelection(market = MarketSymbol.ETH)
        waitUntil { states.any { it is RecentTradesUiState.Live && it.recentTrades.firstOrNull()?.market == MarketSymbol.ETH } }

        val beforeReselect = states.size
        selection.value = RecentTradesSelection(market = MarketSymbol.BTC)
        waitUntil { states.size > beforeReselect }

        val reselectedState = states[beforeReselect] as RecentTradesUiState.Live
        assertEquals(MarketSymbol.BTC, reselectedState.recentTrades.single().market)

        job.cancelAndJoin()
    }

    @Test
    fun orderBooksReuseCachedSelectionOnReselect() = runBlocking {
        val selection = MutableStateFlow(OrderBookSelection(market = MarketSymbol.BTC))
        val states = mutableListOf<OrderBookUiState>()
        val repository = OrderBookRepository.Impl(FakeOrderBookService())
        val job = collectInto(states) { repository.states(selection) }

        waitUntil { states.any { it is OrderBookUiState.Live && it.snapshot.market == MarketSymbol.BTC } }
        selection.value = OrderBookSelection(market = MarketSymbol.ETH)
        waitUntil { states.any { it is OrderBookUiState.Live && it.snapshot.market == MarketSymbol.ETH } }

        val beforeReselect = states.size
        selection.value = OrderBookSelection(market = MarketSymbol.BTC)
        waitUntil { states.size > beforeReselect }

        val reselectedState = states[beforeReselect] as OrderBookUiState.Live
        assertEquals(MarketSymbol.BTC, reselectedState.snapshot.market)

        job.cancelAndJoin()
    }
}

private fun <State> CoroutineScope.collectInto(
    states: MutableList<State>,
    flow: () -> Flow<State>,
) = launch {
    flow().collect { state ->
        states += state
    }
}

private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(1_000) {
        while (!condition()) {
            delay(10)
        }
    }
}

private class FakeLivePriceService : LivePriceService {
    override fun priceEvents(market: MarketSymbol): Flow<RealtimeFeedEvent<LivePrice>> = flow {
        emit(RealtimeFeedEvent.Live(LivePrice(market = market, price = market.testPrice)))
        awaitCancellation()
    }
}

private class FakeMarketSummaryService : MarketSummaryService {
    override suspend fun summary(market: MarketSymbol): MarketSummary =
        MarketSummary(
            market = market,
            midPrice = market.testPrice,
            previousDayPrice = market.testPrice - 1.0,
            volumeUsd24h = 1_000_000.0,
            high24h = market.testPrice + 10.0,
            low24h = market.testPrice - 10.0,
            openInterest = 500_000.0,
        )
}

private class FakeCandleService : CandleService {
    override fun candleEvents(selection: CandleSelection): Flow<RealtimeFeedEvent<CandleBar>> = flow {
        awaitCancellation()
    }

    override suspend fun history(
        selection: CandleSelection,
        range: CandleHistoryRange,
    ): List<CandleBar> = listOf(
        CandleBar(
            market = selection.market,
            interval = selection.interval,
            openTimeMillis = 0,
            closeTimeMillis = 1,
            open = selection.market.testPrice - 1.0,
            close = selection.market.testPrice,
            high = selection.market.testPrice + 1.0,
            low = selection.market.testPrice - 2.0,
            volume = 10.0,
            tradeCount = 1,
        ),
    )
}

private class FakeRecentTradesService : RecentTradesService {
    override fun recentTradesEvents(
        selection: RecentTradesSelection,
    ): Flow<RealtimeFeedEvent<List<RecentTradesEntry>>> = flow {
        emit(RealtimeFeedEvent.Live(listOf(trade(selection.market))))
        awaitCancellation()
    }

    private fun trade(market: MarketSymbol): RecentTradesEntry =
        RecentTradesEntry(
            market = market,
            side = RecentTradesSide.Buy,
            price = market.testPrice,
            size = 1.0,
            transactionHash = "0x${market.wireName.lowercase()}",
            timeMillis = market.testTime,
            tradeId = market.ordinal.toLong(),
            buyer = null,
            seller = null,
        )
}

private class FakeOrderBookService : OrderBookService {
    override fun snapshotEvents(selection: OrderBookSelection): Flow<RealtimeFeedEvent<OrderBookSnapshot>> = flow {
        emit(RealtimeFeedEvent.Live(snapshot(selection)))
        awaitCancellation()
    }

    private fun snapshot(selection: OrderBookSelection): OrderBookSnapshot =
        OrderBookSnapshot(
            market = selection.market,
            precision = selection.precision,
            timeMillis = selection.market.testTime,
            bids = listOf(level(OrderBookSide.Bid, selection.market.testPrice - 1.0)),
            asks = listOf(level(OrderBookSide.Ask, selection.market.testPrice + 1.0)),
        )

    private fun level(side: OrderBookSide, price: Double): OrderBookLevel =
        OrderBookLevel(
            side = side,
            price = price,
            size = 1.0,
            orderCount = 1,
            depthFraction = 1.0f,
        )
}

private val MarketSymbol.testPrice: Double
    get() = when (this) {
        MarketSymbol.BTC -> 70_000.0
        MarketSymbol.ETH -> 4_000.0
        MarketSymbol.SOL -> 200.0
    }

private val MarketSymbol.testTime: Long
    get() = 1_000L + ordinal
