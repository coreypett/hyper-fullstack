@preconcurrency import SharedLogic
import SwiftUI

@MainActor
final class MarketDetailsViewModel: ObservableObject {
    @Published private(set) var selection: OrderBookSelection
    @Published private(set) var chartInterval: CandleInterval
    @Published private(set) var summary: MarketSummaryState
    @Published private(set) var chartBars: [MarketChartBar]
    @Published private(set) var isChartLoading: Bool
    @Published private(set) var orderBook: OrderBookState
    @Published private(set) var recentTrades: RecentTradesState

    private let orderBookFeature: OrderBookFeature?
    private let candleChartFeature: CandleChartFeature?
    private let marketSummaryFeature: MarketSummaryFeature?
    private let recentTradesFeature: RecentTradesFeature?
    private var latestOrderBookViewState: OrderBookViewState?
    private var latestMarketSummaryViewState: MarketSummaryViewState?
    private var latestCandlePriceText: String?

    init(
        orderBookFeature: OrderBookFeature? = SharedDependencyGraph.shared.orderBookFeature(),
        candleChartFeature: CandleChartFeature? = SharedDependencyGraph.shared.candleChartFeature(),
        marketSummaryFeature: MarketSummaryFeature? = SharedDependencyGraph.shared.marketSummaryFeature(),
        recentTradesFeature: RecentTradesFeature? = SharedDependencyGraph.shared.recentTradesFeature()
    ) {
        let initialSelection = orderBookFeature?.selection ?? OrderBookSelection(market: .btc, precision: .two)
        let initialCandleSelection = candleChartFeature?.selection ?? CandleSelection(market: initialSelection.market, interval: .oneHour)
        let initialOrderBook = orderBookFeature?.currentState
        let initialCandles = candleChartFeature?.currentState.bars ?? []
        let initialMarketSummary = marketSummaryFeature?.currentState
        let initialTrades = recentTradesFeature?.currentState
        self.orderBookFeature = orderBookFeature
        self.candleChartFeature = candleChartFeature
        self.marketSummaryFeature = marketSummaryFeature
        self.recentTradesFeature = recentTradesFeature
        self.latestOrderBookViewState = initialOrderBook
        self.latestMarketSummaryViewState = initialMarketSummary
        self.latestCandlePriceText = initialCandles.last?.closeText
        self.selection = initialSelection
        self.chartInterval = initialCandleSelection.interval
        self.summary = marketSummaryFeature == nil && orderBookFeature == nil
            ? .preview
            : MarketSummaryState(
                marketSummary: initialMarketSummary,
                orderBook: initialOrderBook,
                livePriceText: latestCandlePriceText
            )
        self.chartBars = candleChartFeature == nil ? MarketChartBar.preview : initialCandles.map(MarketChartBar.init)
        self.isChartLoading = candleChartFeature != nil && initialCandles.isEmpty
        self.orderBook = orderBookFeature == nil ? .preview : initialOrderBook.map(OrderBookState.init) ?? .loading
        self.recentTrades = recentTradesFeature == nil ? .preview : initialTrades.map(RecentTradesState.init) ?? .loading
        marketSummaryFeature?.selectMarket(market: initialSelection.market)
        recentTradesFeature?.selectMarket(market: initialSelection.market)

        orderBookFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                let selection = self.orderBookFeature?.selection ?? self.selection
                self.selection = selection
                self.latestOrderBookViewState = state
                self.summary = MarketSummaryState(
                    marketSummary: self.latestMarketSummaryViewState,
                    orderBook: state,
                    livePriceText: self.latestCandlePriceText
                )
                self.orderBook = OrderBookState(shared: state)
            }
        }

        candleChartFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                self.chartBars = state.bars.map(MarketChartBar.init)
                self.isChartLoading = state.bars.isEmpty
                self.latestCandlePriceText = state.bars.last?.closeText
                self.summary = MarketSummaryState(
                    marketSummary: self.latestMarketSummaryViewState,
                    orderBook: self.latestOrderBookViewState,
                    livePriceText: self.latestCandlePriceText
                )
                if let selection = self.candleChartFeature?.selection {
                    self.chartInterval = selection.interval
                }
            }
        }

        marketSummaryFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                self.latestMarketSummaryViewState = state
                self.summary = MarketSummaryState(
                    marketSummary: state,
                    orderBook: self.latestOrderBookViewState,
                    livePriceText: self.latestCandlePriceText
                )
            }
        }

        recentTradesFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                self.recentTrades = RecentTradesState(shared: state)
            }
        }
    }

    deinit {
        orderBookFeature?.clearObserver()
        orderBookFeature?.close()
        candleChartFeature?.clearObserver()
        candleChartFeature?.close()
        marketSummaryFeature?.clearObserver()
        marketSummaryFeature?.close()
        recentTradesFeature?.clearObserver()
        recentTradesFeature?.close()
    }

    func selectMarket(_ market: MarketSymbol) {
        orderBookFeature?.selectMarket(market: market)
        candleChartFeature?.selectMarket(market: market)
        marketSummaryFeature?.selectMarket(market: market)
        recentTradesFeature?.selectMarket(market: market)
        latestOrderBookViewState = nil
        latestMarketSummaryViewState = nil
        latestCandlePriceText = nil
        summary = marketSummaryFeature == nil && orderBookFeature == nil
            ? .preview
            : MarketSummaryState(
                marketSummary: nil,
                orderBook: nil,
                livePriceText: nil
            )
        chartBars = candleChartFeature == nil ? MarketChartBar.preview : []
        isChartLoading = candleChartFeature != nil
        orderBook = orderBookFeature == nil ? .preview : .loading
        recentTrades = recentTradesFeature == nil ? .preview : .loading
        selection = selection.doCopy(market: market, precision: selection.precision)
    }

    func selectPrecision(_ precision: PricePrecision) {
        orderBookFeature?.selectPrecision(precision: precision)
        selection = selection.doCopy(market: selection.market, precision: precision)
    }

    func selectChartInterval(_ interval: CandleInterval) {
        candleChartFeature?.selectInterval(interval: interval)
        chartInterval = interval
    }

    static var preview: MarketDetailsViewModel {
        MarketDetailsViewModel(orderBookFeature: nil, candleChartFeature: nil, marketSummaryFeature: nil, recentTradesFeature: nil)
    }
}
