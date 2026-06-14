@preconcurrency import LightweightCharts
import Pow
@preconcurrency import SharedLogic
import SwiftUI
import UIKit

@MainActor
final class MarketDetailsViewModel: ObservableObject {
    @Published private(set) var selection: OrderBookSelection
    @Published private(set) var chartInterval: CandleInterval
    @Published private(set) var summary: MarketSummaryState
    @Published private(set) var chartBars: [MarketChartBar]
    @Published private(set) var isChartLoading: Bool
    @Published private(set) var orderBook: OrderBookState
    @Published private(set) var trades: TradeState

    private let orderBookFeature: OrderBookFeature?
    private let candleChartFeature: CandleChartFeature?
    private let marketSummaryFeature: MarketSummaryFeature?
    private let tradeFeature: TradeFeature?
    private var latestOrderBookViewState: OrderBookViewState?
    private var latestMarketSummaryViewState: MarketSummaryViewState?
    private var latestCandlePriceText: String?

    init(
        orderBookFeature: OrderBookFeature? = SharedDependencyGraph.shared.orderBookFeature(),
        candleChartFeature: CandleChartFeature? = SharedDependencyGraph.shared.candleChartFeature(),
        marketSummaryFeature: MarketSummaryFeature? = SharedDependencyGraph.shared.marketSummaryFeature(),
        tradeFeature: TradeFeature? = SharedDependencyGraph.shared.tradeFeature()
    ) {
        let initialSelection = orderBookFeature?.selection ?? OrderBookSelection(market: .btc, precision: .five)
        let initialCandleSelection = candleChartFeature?.selection ?? CandleSelection(market: initialSelection.market, interval: .oneHour)
        let initialOrderBook = orderBookFeature?.currentState
        let initialCandles = candleChartFeature?.currentState.bars ?? []
        let initialMarketSummary = marketSummaryFeature?.currentState
        let initialTrades = tradeFeature?.currentState
        self.orderBookFeature = orderBookFeature
        self.candleChartFeature = candleChartFeature
        self.marketSummaryFeature = marketSummaryFeature
        self.tradeFeature = tradeFeature
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
        self.trades = tradeFeature == nil ? .preview : initialTrades.map(TradeState.init) ?? .loading
        marketSummaryFeature?.selectMarket(market: initialSelection.market)
        tradeFeature?.selectMarket(market: initialSelection.market)

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

        tradeFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                self.trades = TradeState(shared: state)
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
        tradeFeature?.clearObserver()
        tradeFeature?.close()
    }

    func selectMarket(_ market: MarketSymbol) {
        orderBookFeature?.selectMarket(market: market)
        candleChartFeature?.selectMarket(market: market)
        marketSummaryFeature?.selectMarket(market: market)
        tradeFeature?.selectMarket(market: market)
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
        trades = tradeFeature == nil ? .preview : .loading
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
        MarketDetailsViewModel(orderBookFeature: nil, candleChartFeature: nil, marketSummaryFeature: nil, tradeFeature: nil)
    }
}

struct MarketSummaryState {
    let midPriceText: String
    let priceChangeText: String
    let priceChangePercentText: String
    let changeDirection: MarketChangeDirection
    let stats: [MarketStat]
    let isMidPriceLoading: Bool
    let isChangeLoading: Bool

    static let preview = MarketSummaryState(
        midPriceText: "$69,125.75",
        priceChangeText: "+$1,228.50",
        priceChangePercentText: "+1.81%",
        changeDirection: .up,
        stats: [
            MarketStat(label: "24h Vol", value: "$4.82B", isLoading: false),
            MarketStat(label: "24h High", value: "$70,184", isLoading: false),
            MarketStat(label: "24h Low", value: "$67,914.50", isLoading: false),
            MarketStat(label: "Open Int.", value: "28.42K", isLoading: false),
        ]
    )

    init(
        marketSummary: MarketSummaryViewState?,
        orderBook: OrderBookViewState?,
        livePriceText: String? = nil
    ) {
        let midPriceText = livePriceText ?? marketSummary?.midPriceText ?? orderBook?.midPriceText
        let priceChangeText = marketSummary?.priceChangeText
        let priceChangePercentText = marketSummary?.priceChangePercentText
        self.midPriceText = midPriceText ?? ""
        self.priceChangeText = priceChangeText ?? ""
        self.priceChangePercentText = priceChangePercentText ?? ""
        self.changeDirection = MarketSummaryState.changeDirection(from: marketSummary?.changeDirection)
        self.isMidPriceLoading = midPriceText == nil
        self.isChangeLoading = priceChangeText == nil || priceChangePercentText == nil
        let volume24hText = marketSummary?.volume24hText
        let high24hText = marketSummary?.high24hText
        let low24hText = marketSummary?.low24hText
        var stats = [
            MarketStat(label: "24h Vol", value: volume24hText ?? "", isLoading: volume24hText == nil),
            MarketStat(label: "24h High", value: high24hText ?? "", isLoading: high24hText == nil),
            MarketStat(label: "24h Low", value: low24hText ?? "", isLoading: low24hText == nil),
        ]
        if let openInterestText = marketSummary?.openInterestText {
            stats.append(MarketStat(label: "Open Int.", value: openInterestText, isLoading: false))
        }
        self.stats = stats
    }

    init(
        midPriceText: String,
        priceChangeText: String,
        priceChangePercentText: String,
        changeDirection: MarketChangeDirection,
        stats: [MarketStat],
        isMidPriceLoading: Bool = false,
        isChangeLoading: Bool = false
    ) {
        self.midPriceText = midPriceText
        self.priceChangeText = priceChangeText
        self.priceChangePercentText = priceChangePercentText
        self.changeDirection = changeDirection
        self.stats = stats
        self.isMidPriceLoading = isMidPriceLoading
        self.isChangeLoading = isChangeLoading
    }

    private static func changeDirection(from shared: MarketSummaryChangeDirection?) -> MarketChangeDirection {
        switch shared {
        case .up:
            return .up
        case .down:
            return .down
        case .flat, .none:
            return .flat
        }
    }
}

struct MarketStat: Hashable {
    let label: String
    let value: String
    let isLoading: Bool
}

enum MarketChangeDirection {
    case up
    case down
    case flat
}

struct MarketChartBar {
    let time: Time
    let open: Double
    let high: Double
    let low: Double
    let close: Double

    init(shared: CandleBar) {
        self.time = .utc(timestamp: Double(shared.openTimeMillis) / 1_000)
        self.open = shared.open
        self.high = shared.high
        self.low = shared.low
        self.close = shared.close
    }

    init(
        time: Time,
        open: Double,
        high: Double,
        low: Double,
        close: Double
    ) {
        self.time = time
        self.open = open
        self.high = high
        self.low = low
        self.close = close
    }

    static let preview: [MarketChartBar] = [
        MarketChartBar(time: .string("2026-06-01"), open: 68120, high: 68980, low: 67840, close: 68720),
        MarketChartBar(time: .string("2026-06-02"), open: 68720, high: 69410, low: 68240, close: 69080),
        MarketChartBar(time: .string("2026-06-03"), open: 69080, high: 69860, low: 68620, close: 69650),
        MarketChartBar(time: .string("2026-06-04"), open: 69650, high: 70180, low: 69140, close: 69320),
        MarketChartBar(time: .string("2026-06-05"), open: 69320, high: 69940, low: 68780, close: 69790),
        MarketChartBar(time: .string("2026-06-06"), open: 69790, high: 70420, low: 69440, close: 70130),
        MarketChartBar(time: .string("2026-06-07"), open: 70130, high: 70680, low: 69690, close: 69910),
        MarketChartBar(time: .string("2026-06-08"), open: 69910, high: 70220, low: 69120, close: 69480),
        MarketChartBar(time: .string("2026-06-09"), open: 69480, high: 70010, low: 68980, close: 69870),
        MarketChartBar(time: .string("2026-06-10"), open: 69870, high: 70720, low: 69610, close: 70420),
        MarketChartBar(time: .string("2026-06-11"), open: 70420, high: 70980, low: 69920, close: 70240),
        MarketChartBar(time: .string("2026-06-12"), open: 70240, high: 70620, low: 69380, close: 69690),
        MarketChartBar(time: .string("2026-06-13"), open: 69690, high: 70130, low: 69160, close: 69980),
        MarketChartBar(time: .string("2026-06-14"), open: 69980, high: 70340, low: 68720, close: 69125.75),
    ]
}

struct OrderBookState {
    let status: OrderBookStatus
    let statusLabel: String
    let centerMessage: String?
    let spreadText: String?
    let asks: [OrderBookRow]
    let bids: [OrderBookRow]
    let isLoading: Bool

    var hasRows: Bool {
        !asks.isEmpty || !bids.isEmpty
    }

    var pairedRows: [OrderBookLevelPair] {
        let visibleAsks = Array(asks.reversed())
        let rowCount = max(bids.count, visibleAsks.count)
        return (0..<rowCount).map { index in
            OrderBookLevelPair(
                bid: bids.indices.contains(index) ? bids[index] : nil,
                ask: visibleAsks.indices.contains(index) ? visibleAsks[index] : nil
            )
        }
    }

    static let preview = OrderBookState(
        status: .live,
        statusLabel: "Live",
        centerMessage: nil,
        spreadText: "$0.50",
        asks: [
            OrderBookRow(side: .ask, priceText: "$69,128", sizeText: "0.75", orderCountText: "1", depthFraction: 0.38, change: .none),
            OrderBookRow(side: .ask, priceText: "$69,127.50", sizeText: "1.00", orderCountText: "2", depthFraction: 0.5, change: .down),
            OrderBookRow(side: .ask, priceText: "$69,126", sizeText: "2.00", orderCountText: "4", depthFraction: 1.0, change: .none),
        ],
        bids: [
            OrderBookRow(side: .bid, priceText: "$69,125.50", sizeText: "1.25", orderCountText: "3", depthFraction: 1.0, change: .none),
            OrderBookRow(side: .bid, priceText: "$69,124", sizeText: "0.50", orderCountText: "1", depthFraction: 0.4, change: .up),
            OrderBookRow(side: .bid, priceText: "$69,123.50", sizeText: "0.25", orderCountText: "2", depthFraction: 0.2, change: .none),
        ],
        isLoading: false
    )

    static let loading = OrderBookState(
        status: .connecting,
        statusLabel: "Connecting",
        centerMessage: nil,
        spreadText: nil,
        asks: [],
        bids: [],
        isLoading: true
    )

    init(shared: OrderBookViewState) {
        self.status = shared.status
        self.statusLabel = shared.statusLabel
        self.centerMessage = shared.centerMessage
        self.spreadText = shared.spreadText
        self.asks = shared.asks.map(OrderBookRow.init)
        self.bids = shared.bids.map(OrderBookRow.init)
        self.isLoading = shared.status == .connecting && !shared.hasRows
    }

    init(
        status: OrderBookStatus,
        statusLabel: String,
        centerMessage: String?,
        spreadText: String?,
        asks: [OrderBookRow],
        bids: [OrderBookRow],
        isLoading: Bool
    ) {
        self.status = status
        self.statusLabel = statusLabel
        self.centerMessage = centerMessage
        self.spreadText = spreadText
        self.asks = asks
        self.bids = bids
        self.isLoading = isLoading
    }
}

struct OrderBookRow: Hashable {
    let side: OrderBookSide
    let priceText: String
    let sizeText: String
    let orderCountText: String
    let depthFraction: Double
    let change: LevelChange

    init(shared: OrderBookRowDisplay) {
        self.side = shared.side
        self.priceText = shared.priceText
        self.sizeText = shared.sizeText
        self.orderCountText = shared.orderCountText
        self.depthFraction = Double(shared.depthFraction)
        self.change = shared.change
    }

    init(
        side: OrderBookSide,
        priceText: String,
        sizeText: String,
        orderCountText: String,
        depthFraction: Double,
        change: LevelChange
    ) {
        self.side = side
        self.priceText = priceText
        self.sizeText = sizeText
        self.orderCountText = orderCountText
        self.depthFraction = depthFraction
        self.change = change
    }
}

struct OrderBookLevelPair {
    let bid: OrderBookRow?
    let ask: OrderBookRow?
}

struct TradeState {
    let status: TradeStatus
    let statusLabel: String
    let centerMessage: String?
    let rows: [TradeRow]
    let isLoading: Bool

    var hasRows: Bool {
        !rows.isEmpty
    }

    static let preview = TradeState(
        status: .live,
        statusLabel: "Live",
        centerMessage: nil,
        rows: [
            TradeRow(side: .buy, sideText: "Buy", priceText: "$69,125.50", sizeText: "0.42", timeMillis: 1_710_000_000_000, tradeId: 42),
            TradeRow(side: .sell, sideText: "Sell", priceText: "$69,124", sizeText: "0.25", timeMillis: 1_710_000_000_100, tradeId: 43),
            TradeRow(side: .buy, sideText: "Buy", priceText: "$69,126", sizeText: "0.18", timeMillis: 1_710_000_000_200, tradeId: 44),
        ],
        isLoading: false
    )

    static let loading = TradeState(
        status: .connecting,
        statusLabel: "Connecting",
        centerMessage: nil,
        rows: [],
        isLoading: true
    )

    init(shared: TradeViewState) {
        self.status = shared.status
        self.statusLabel = shared.statusLabel
        self.centerMessage = shared.centerMessage
        self.rows = shared.trades.map(TradeRow.init)
        self.isLoading = shared.status == .connecting && !shared.hasTrades
    }

    init(
        status: TradeStatus,
        statusLabel: String,
        centerMessage: String?,
        rows: [TradeRow],
        isLoading: Bool
    ) {
        self.status = status
        self.statusLabel = statusLabel
        self.centerMessage = centerMessage
        self.rows = rows
        self.isLoading = isLoading
    }
}

struct TradeRow: Hashable {
    let side: TradeSide
    let sideText: String
    let priceText: String
    let sizeText: String
    let timeMillis: Int64
    let tradeId: Int64

    var timeText: String {
        Self.timeFormatter.string(from: Date(timeIntervalSince1970: Double(timeMillis) / 1_000))
    }

    init(shared: TradeRowDisplay) {
        self.side = shared.side
        self.sideText = shared.sideText
        self.priceText = shared.priceText
        self.sizeText = shared.sizeText
        self.timeMillis = shared.timeMillis
        self.tradeId = shared.tradeId
    }

    init(
        side: TradeSide,
        sideText: String,
        priceText: String,
        sizeText: String,
        timeMillis: Int64,
        tradeId: Int64
    ) {
        self.side = side
        self.sideText = sideText
        self.priceText = priceText
        self.sizeText = sizeText
        self.timeMillis = timeMillis
        self.tradeId = tradeId
    }

    private static let timeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()
}

private enum MarketDataPanel: CaseIterable {
    case orderBook
    case trades

    var title: String {
        switch self {
        case .orderBook:
            return "Order Book"
        case .trades:
            return "Trades"
        }
    }
}

struct MarketDetailsScreen: View {
    @StateObject private var viewModel: MarketDetailsViewModel
    @State private var selectedMarketDataPanel: MarketDataPanel = .orderBook

    init(viewModel: MarketDetailsViewModel = MarketDetailsViewModel()) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 18, pinnedViews: []) {
                MarketTabs(
                    selectedMarket: viewModel.selection.market,
                    onSelect: viewModel.selectMarket
                )
                .padding(.horizontal, 16)
                .padding(.top, 12)

                MarketSummarySection(state: viewModel.summary)
                    .padding(.horizontal, 16)

                MarketChartSection(
                    market: viewModel.selection.market,
                    selectedInterval: viewModel.chartInterval,
                    bars: viewModel.chartBars,
                    isLoading: viewModel.isChartLoading,
                    onSelectInterval: viewModel.selectChartInterval
                )

                MarketDataSection(
                    selectedPanel: selectedMarketDataPanel,
                    selection: viewModel.selection,
                    orderBook: viewModel.orderBook,
                    trades: viewModel.trades,
                    onSelectPanel: { selectedMarketDataPanel = $0 },
                    onSelectPrecision: viewModel.selectPrecision
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .scrollIndicators(.hidden)
        .background(AppColors.background.ignoresSafeArea())
    }
}

private struct MarketTabs: View {
    let selectedMarket: MarketSymbol
    let onSelect: (MarketSymbol) -> Void

    var body: some View {
        HStack(spacing: 10) {
            ForEach([MarketSymbol.btc, .eth, .sol], id: \.self) { market in
                MarketTabButton(
                    market: market,
                    isSelected: selectedMarket == market,
                    onSelect: onSelect
                )
                .frame(maxWidth: .infinity)
            }
        }
    }
}

private struct MarketTabButton: View {
    let market: MarketSymbol
    let isSelected: Bool
    let onSelect: (MarketSymbol) -> Void

    var body: some View {
        Button {
            onSelect(market)
        } label: {
            HStack(spacing: 8) {
                if let image = market.tokenImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 22, height: 22)
                }

                Text(market.displayName)
                    .font(.system(size: 13, weight: isSelected ? .semibold : .medium))
                    .foregroundStyle(isSelected ? AppColors.textPrimary : AppColors.textSecondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 38)
            .padding(.horizontal, 10)
            .background(isSelected ? AppColors.selection : AppColors.panel)
            .clipShape(Capsule(style: .continuous))
            .overlay(
                Capsule(style: .continuous)
                    .stroke(isSelected ? AppColors.textSecondary.opacity(0.42) : AppColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .changeEffect(.shine(duration: 0.45), value: isSelected, isEnabled: isSelected)
    }
}

private struct MarketSummarySection: View {
    let state: MarketSummaryState

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                if state.isMidPriceLoading {
                    SkeletonBlock(width: 210, height: 42, cornerRadius: 6)
                        .transition(.movingParts.blur.combined(with: .opacity))
                } else {
                    Text(state.midPriceText)
                        .font(.system(size: 36, weight: .semibold, design: .rounded))
                        .foregroundStyle(AppColors.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.52)
                        .allowsTightening(true)
                        .contentTransition(.numericText())
                        .animation(.easeInOut(duration: 0.18), value: state.midPriceText)
                        .changeEffect(.shine(duration: 0.7), value: state.midPriceText)
                        .transition(.movingParts.blur.combined(with: .opacity))
                }

                if state.isChangeLoading {
                    SkeletonBlock(width: 132, height: 18, cornerRadius: 4)
                        .transition(.movingParts.blur.combined(with: .opacity))
                } else {
                    HStack(spacing: 8) {
                        Text(state.priceChangeText)
                        Text(state.priceChangePercentText)
                    }
                    .font(.system(size: 14, weight: .semibold, design: .monospaced))
                    .foregroundStyle(changeColor(for: state.changeDirection))
                    .lineLimit(1)
                    .minimumScaleFactor(0.82)
                    .transition(.movingParts.blur.combined(with: .opacity))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(minHeight: 76, alignment: .center)
            .animation(.easeInOut(duration: 0.22), value: state.isMidPriceLoading)
            .animation(.easeInOut(duration: 0.22), value: state.isChangeLoading)

            VStack(alignment: .trailing, spacing: 4) {
                ForEach(state.stats, id: \.self) { stat in
                    MarketStatRow(stat: stat)
                }
            }
            .frame(width: 154, alignment: .trailing)
            .frame(minHeight: 76, alignment: .center)
        }
        .padding(.vertical, 2)
    }
}

private struct MarketStatRow: View {
    let stat: MarketStat

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(stat.label)
                .font(.system(size: 10))
                .foregroundStyle(AppColors.textTertiary)
                .lineLimit(1)

            Spacer(minLength: 8)

            if stat.isLoading {
                SkeletonBlock(width: 58, height: 13, cornerRadius: 3)
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else {
                Text(stat.value)
                    .font(.system(size: 12, weight: .medium, design: .monospaced))
                    .foregroundStyle(AppColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                    .transition(.movingParts.blur.combined(with: .opacity))
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .animation(.easeInOut(duration: 0.22), value: stat.isLoading)
    }
}

private struct MarketChartSection: View {
    let market: MarketSymbol
    let selectedInterval: CandleInterval
    let bars: [MarketChartBar]
    let isLoading: Bool
    let onSelectInterval: (CandleInterval) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            IntervalSelector(
                selectedInterval: selectedInterval,
                onSelect: onSelectInterval
            )
            .padding(.horizontal, 16)

            ZStack {
                ChartWatermark(market: market)

                if isLoading {
                    ChartSkeleton()
                        .transition(.movingParts.blur.combined(with: .opacity))
                } else {
                    MarketCandlestickChart(bars: bars)
                        .transition(.movingParts.blur.combined(with: .opacity))
                }
            }
                .frame(height: 260)
                .animation(.easeInOut(duration: 0.22), value: isLoading)
        }
    }
}

private struct ChartWatermark: View {
    let market: MarketSymbol

    var body: some View {
        VStack {
            if let image = MR.images.shared.fullstack_logo.toUIImage() {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .opacity(0.18)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .allowsHitTesting(false)
    }
}

private struct IntervalSelector: View {
    let selectedInterval: CandleInterval
    let onSelect: (CandleInterval) -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text("Int.")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(AppColors.textTertiary)
                .lineLimit(1)

            HStack(spacing: 12) {
                ForEach(chartIntervals, id: \.self) { interval in
                    Button {
                        onSelect(interval)
                    } label: {
                        Text(intervalLabel(for: interval))
                            .font(.system(size: 12, weight: selectedInterval == interval ? .semibold : .medium))
                            .foregroundStyle(intervalColor(for: interval))
                            .lineLimit(1)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func intervalColor(for interval: CandleInterval) -> Color {
        selectedInterval == interval ? AppColors.controlTextSelected : AppColors.controlTextDimmed
    }
}

private struct MarketCandlestickChart: UIViewRepresentable {
    let bars: [MarketChartBar]

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> LightweightCharts {
        let chart = LightweightCharts(options: chartOptions)
        chart.isOpaque = false
        chart.backgroundColor = .clear
        chart.clearWebViewBackground()

        let series = chart.addCandlestickSeries(options: seriesOptions)
        context.coordinator.series = series
        series.setData(data: bars.map(\.candlestickData))
        chart.timeScale().scrollToRealTime()

        return chart
    }

    func updateUIView(_ chart: LightweightCharts, context: Context) {
        chart.isOpaque = false
        chart.backgroundColor = .clear
        chart.clearWebViewBackground()
        context.coordinator.series?.setData(data: bars.map(\.candlestickData))
        chart.timeScale().scrollToRealTime()
    }

    final class Coordinator {
        var series: CandlestickSeries?
    }

    private var chartOptions: ChartOptions {
        marketChartOptions()
    }

    private var seriesOptions: CandlestickSeriesOptions {
        marketChartSeriesOptions()
    }
}

private struct MarketDataSection: View {
    let selectedPanel: MarketDataPanel
    let selection: OrderBookSelection
    let orderBook: OrderBookState
    let trades: TradeState
    let onSelectPanel: (MarketDataPanel) -> Void
    let onSelectPrecision: (PricePrecision) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 12) {
                MarketDataPanelSelector(
                    selectedPanel: selectedPanel,
                    onSelect: onSelectPanel
                )

                Spacer(minLength: 12)

                if selectedPanel == .orderBook {
                    GroupingMenu(
                        selected: selection.precision,
                        onSelect: onSelectPrecision
                    )
                    .transition(.movingParts.blur.combined(with: .opacity))
                }
            }

            Group {
                switch selectedPanel {
                case .orderBook:
                    OrderBookSection(orderBook: orderBook)
                case .trades:
                    TradesSection(trades: trades)
                }
            }
            .animation(.easeInOut(duration: 0.18), value: selectedPanel)
        }
    }
}

private struct MarketDataPanelSelector: View {
    let selectedPanel: MarketDataPanel
    let onSelect: (MarketDataPanel) -> Void

    var body: some View {
        HStack(spacing: 3) {
            ForEach(MarketDataPanel.allCases, id: \.self) { panel in
                Button {
                    onSelect(panel)
                } label: {
                    Text(panel.title)
                        .font(.system(size: 13, weight: selectedPanel == panel ? .semibold : .medium))
                        .foregroundStyle(selectedPanel == panel ? AppColors.textPrimary : AppColors.textSecondary)
                        .frame(minWidth: 86)
                        .frame(height: 34)
                        .padding(.horizontal, 6)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .background(selectedPanel == panel ? AppColors.selection : Color.clear)
                        .clipShape(Capsule(style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(AppColors.panel.opacity(0.62))
        .clipShape(Capsule(style: .continuous))
    }
}

private struct OrderBookSection: View {
    let orderBook: OrderBookState

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if orderBook.isLoading {
                OrderBookSkeleton()
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else if orderBook.hasRows {
                OrderBookList(orderBook: orderBook)
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else {
                CenterMessage(text: orderBook.centerMessage ?? "Connecting")
                    .frame(height: 220)
                    .transition(.movingParts.blur.combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.22), value: orderBook.isLoading)
    }
}

private struct TradesSection: View {
    let trades: TradeState

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if trades.isLoading {
                TradesSkeleton()
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else if trades.hasRows {
                TradesList(rows: trades.rows)
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else {
                CenterMessage(text: trades.centerMessage ?? "Connecting")
                    .frame(height: 180)
                    .transition(.movingParts.blur.combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.22), value: trades.isLoading)
    }
}

private struct TradesList: View {
    let rows: [TradeRow]

    var body: some View {
        LazyVStack(spacing: 0) {
            TradeHeaderRow()

            ForEach(Array(rows.prefix(18).enumerated()), id: \.element) { _, row in
                TradeListRow(row: row)
            }
        }
    }
}

private struct TradeHeaderRow: View {
    var body: some View {
        HStack(spacing: 10) {
            TradeHeaderCell(text: "Price (USD)", alignment: .leading)
            TradeHeaderCell(text: "Size", alignment: .trailing)
            TradeHeaderCell(text: "Time", alignment: .trailing, width: 72)
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 6)
    }
}

private struct TradeHeaderCell: View {
    let text: String
    let alignment: Alignment
    var width: CGFloat?

    var body: some View {
        Text(text)
            .font(.system(size: 11))
            .foregroundStyle(AppColors.textTertiary)
            .frame(maxWidth: width == nil ? .infinity : nil, alignment: alignment)
            .frame(width: width, alignment: alignment)
            .lineLimit(1)
            .minimumScaleFactor(0.75)
    }
}

private struct TradeListRow: View {
    let row: TradeRow

    var body: some View {
        HStack(spacing: 10) {
            Text(row.priceText)
                .foregroundStyle(tradeColor(for: row.side))
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(row.sizeText)
                .foregroundStyle(AppColors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .trailing)

            Text(row.timeText)
                .foregroundStyle(AppColors.textSecondary)
                .frame(width: 72, alignment: .trailing)
        }
        .font(.system(size: 13, design: .monospaced))
        .lineLimit(1)
        .minimumScaleFactor(0.72)
        .frame(height: 30)
        .padding(.horizontal, 4)
    }
}

private struct GroupingMenu: View {
    let selected: PricePrecision
    let onSelect: (PricePrecision) -> Void

    var body: some View {
        Menu {
            ForEach([PricePrecision.two, .three, .four, .five], id: \.self) { precision in
                Button {
                    onSelect(precision)
                } label: {
                    HStack {
                        Text(groupingLabel(for: precision))
                        if selected == precision {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: 6) {
                Text("Grouping")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(AppColors.textTertiary)

                Text("\(selected.nSigFigs)")
                    .font(.system(size: 13, weight: .semibold, design: .monospaced))
                    .foregroundStyle(AppColors.textPrimary)

                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(AppColors.textSecondary)
            }
            .frame(height: 34)
            .padding(.horizontal, 10)
            .background(AppColors.panel.opacity(0.62))
            .clipShape(Capsule(style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct OrderBookList: View {
    let orderBook: OrderBookState

    var body: some View {
        LazyVStack(spacing: 0) {
            HeaderRow()

            SpreadRow(spreadText: orderBook.spreadText ?? "--")

            ForEach(Array(orderBook.pairedRows.enumerated()), id: \.offset) { _, pair in
                PairedLevelRow(pair: pair)
            }
        }
    }
}

private struct HeaderRow: View {
    var body: some View {
        HStack(spacing: orderBookPriceGap) {
            HStack(spacing: 0) {
                HeaderCell(text: "Size", alignment: .leading)
                HeaderCell(text: "Price (Bid)", alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, 4)
            .padding(.trailing, 6)

            HStack(spacing: 0) {
                HeaderCell(text: "Price (Ask)", alignment: .leading)
                HeaderCell(text: "Size", alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, 6)
            .padding(.trailing, 4)
        }
        .padding(.vertical, 6)
    }
}

private struct HeaderCell: View {
    let text: String
    let alignment: Alignment

    var body: some View {
        Text(text)
            .font(.system(size: 11))
            .foregroundStyle(AppColors.textTertiary)
            .frame(maxWidth: .infinity, alignment: alignment)
            .lineLimit(1)
            .minimumScaleFactor(0.75)
    }
}

private struct PairedLevelRow: View {
    let pair: OrderBookLevelPair

    var body: some View {
        ZStack {
            DepthColumns(pair: pair)

            HStack(spacing: orderBookPriceGap) {
                BidColumns(level: pair.bid)
                AskColumns(level: pair.ask)
            }
            .font(.system(size: 13, design: .monospaced))
            .lineLimit(1)
        }
        .frame(height: 30)
    }
}

private struct DepthColumns: View {
    let pair: OrderBookLevelPair

    var body: some View {
        HStack(spacing: orderBookPriceGap) {
            HStack(spacing: 0) {
                Color.clear
                    .frame(maxWidth: .infinity)

                PriceDepth(level: pair.bid, side: .bid)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, 4)
            .padding(.trailing, 6)

            HStack(spacing: 0) {
                PriceDepth(level: pair.ask, side: .ask)

                Color.clear
                    .frame(maxWidth: .infinity)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, 6)
            .padding(.trailing, 4)
        }
    }
}

private struct PriceDepth: View {
    let level: OrderBookRow?
    let side: OrderBookSide

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: side == .bid ? .trailing : .leading) {
                if let level {
                    sideColor(for: side)
                        .opacity(0.11)
                        .frame(width: geometry.size.width * CGFloat(clamped(level.depthFraction)))
                }
            }
            .frame(
                width: geometry.size.width,
                height: geometry.size.height,
                alignment: side == .bid ? .trailing : .leading
            )
        }
        .frame(maxWidth: .infinity)
        .frame(height: 30)
    }

    private func clamped(_ value: Double) -> Double {
        min(max(value, 0), 1)
    }
}

private struct BidColumns: View {
    let level: OrderBookRow?

    var body: some View {
        HStack(spacing: 0) {
            LevelText(level?.sizeText, color: AppColors.textPrimary, alignment: .leading)
            LevelText(level?.priceText, color: AppColors.bid, alignment: .trailing)
        }
        .frame(maxWidth: .infinity)
        .padding(.leading, 4)
        .padding(.trailing, 6)
    }
}

private struct AskColumns: View {
    let level: OrderBookRow?

    var body: some View {
        HStack(spacing: 0) {
            LevelText(level?.priceText, color: AppColors.ask, alignment: .leading)
            LevelText(level?.sizeText, color: AppColors.textPrimary, alignment: .trailing)
        }
        .frame(maxWidth: .infinity)
        .padding(.leading, 6)
        .padding(.trailing, 4)
    }
}

private struct LevelText: View {
    let text: String?
    let color: Color
    let alignment: Alignment

    init(_ text: String?, color: Color, alignment: Alignment) {
        self.text = text
        self.color = color
        self.alignment = alignment
    }

    var body: some View {
        Text(text ?? "")
            .foregroundStyle(color)
            .frame(maxWidth: .infinity, alignment: alignment)
            .lineLimit(1)
            .minimumScaleFactor(0.72)
    }
}

private struct SpreadRow: View {
    let spreadText: String

    var body: some View {
        HStack(spacing: 8) {
            Text("Spread")
                .font(.system(size: 12))
                .foregroundStyle(AppColors.textTertiary)

            Text(spreadText)
                .font(.system(size: 15, weight: .semibold, design: .monospaced))
                .foregroundStyle(AppColors.accent)
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .frame(height: 38)
        .padding(.horizontal, 4)
        .background(AppColors.panel)
    }
}

private struct CenterMessage: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 14))
            .foregroundStyle(AppColors.textSecondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ChartSkeleton: View {
    private let bars: [CGFloat] = [0.42, 0.66, 0.54, 0.76, 0.48, 0.58, 0.82, 0.62, 0.44, 0.7, 0.52, 0.64]

    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let height = geometry.size.height
            let spacing: CGFloat = 8
            let barWidth = max((width - CGFloat(bars.count - 1) * spacing - 32) / CGFloat(bars.count), 8)

            HStack(alignment: .bottom, spacing: spacing) {
                ForEach(Array(bars.enumerated()), id: \.offset) { index, fraction in
                    VStack(spacing: 4) {
                        SkeletonBlock(width: 2, height: height * 0.22, cornerRadius: 1)

                        SkeletonBlock(
                            width: barWidth,
                            height: max(height * fraction * 0.42, 28),
                            cornerRadius: 3
                        )
                        .opacity(index.isMultiple(of: 2) ? 0.86 : 0.64)
                    }
                    .frame(maxHeight: .infinity, alignment: .bottom)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
            .padding(.horizontal, 16)
            .padding(.vertical, 18)
        }
    }
}

private struct OrderBookSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            HeaderRow()
            SkeletonBlock(width: 116, height: 16, cornerRadius: 4)
                .frame(maxWidth: .infinity, minHeight: 38, alignment: .center)

            ForEach(0..<8, id: \.self) { index in
                SkeletonOrderBookRow(seed: index)
            }
        }
    }
}

private struct SkeletonOrderBookRow: View {
    let seed: Int

    var body: some View {
        HStack(spacing: orderBookPriceGap) {
            HStack(spacing: 0) {
                SkeletonBlock(width: sizeWidth(for: seed), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                SkeletonBlock(width: priceWidth(for: seed), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, 4)
            .padding(.trailing, 6)

            HStack(spacing: 0) {
                SkeletonBlock(width: priceWidth(for: seed + 2), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                SkeletonBlock(width: sizeWidth(for: seed + 1), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, 6)
            .padding(.trailing, 4)
        }
        .frame(height: 30)
    }

    private func priceWidth(for seed: Int) -> CGFloat {
        [72, 84, 64, 78][seed % 4]
    }

    private func sizeWidth(for seed: Int) -> CGFloat {
        [34, 46, 38, 52][seed % 4]
    }
}

private struct TradesSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            TradeHeaderRow()

            ForEach(0..<7, id: \.self) { index in
                SkeletonTradeRow(seed: index)
            }
        }
    }
}

private struct SkeletonTradeRow: View {
    let seed: Int

    var body: some View {
        HStack(spacing: 10) {
            SkeletonBlock(width: priceWidth(for: seed), height: 13, cornerRadius: 3)
                .frame(maxWidth: .infinity, alignment: .leading)

            SkeletonBlock(width: sizeWidth(for: seed), height: 13, cornerRadius: 3)
                .frame(maxWidth: .infinity, alignment: .trailing)

            SkeletonBlock(width: 58, height: 13, cornerRadius: 3)
                .frame(width: 72, alignment: .trailing)
        }
        .frame(height: 30)
        .padding(.horizontal, 4)
    }

    private func priceWidth(for seed: Int) -> CGFloat {
        [72, 84, 64, 78][seed % 4]
    }

    private func sizeWidth(for seed: Int) -> CGFloat {
        [34, 46, 38, 52][seed % 4]
    }
}

private struct SkeletonBlock: View {
    let width: CGFloat
    let height: CGFloat
    let cornerRadius: CGFloat

    @State private var isAnimating = false

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(AppColors.selection)
            .overlay {
                GeometryReader { geometry in
                    LinearGradient(
                        colors: [
                            Color.clear,
                            AppColors.textPrimary.opacity(0.08),
                            Color.clear,
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: geometry.size.width * 1.6)
                    .offset(x: isAnimating ? geometry.size.width : -geometry.size.width * 1.6)
                }
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            }
            .frame(width: width, height: height)
            .onAppear {
                withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                    isAnimating = true
                }
            }
    }
}

private func statusColor(for status: OrderBookStatus) -> Color {
    if status == .failed {
        return AppColors.ask
    }
    if status == .live {
        return AppColors.bid
    }
    if status == .stale {
        return AppColors.accent
    }
    return AppColors.textSecondary
}

private func statusColor(for status: TradeStatus) -> Color {
    if status == .failed {
        return AppColors.ask
    }
    if status == .live {
        return AppColors.bid
    }
    if status == .stale {
        return AppColors.accent
    }
    return AppColors.textSecondary
}

private func changeColor(for direction: MarketChangeDirection) -> Color {
    switch direction {
    case .up:
        return AppColors.bid
    case .down:
        return AppColors.ask
    case .flat:
        return AppColors.textSecondary
    }
}

private func intervalLabel(for interval: CandleInterval) -> String {
    interval == .oneDay ? "1D" : interval.displayName
}

private func groupingLabel(for precision: PricePrecision) -> String {
    "\(precision.nSigFigs) significant figures"
}

private let chartIntervals: [CandleInterval] = [
    .oneMinute,
    .fiveMinutes,
    .oneHour,
    .fourHours,
    .oneDay,
]

private let orderBookPriceGap: CGFloat = 8

private func sideColor(for side: OrderBookSide) -> Color {
    side == .bid ? AppColors.bid : AppColors.ask
}

private func tradeColor(for side: TradeSide) -> Color {
    switch side {
    case .buy:
        return AppColors.bid
    case .sell:
        return AppColors.ask
    case .unknown:
        return AppColors.textSecondary
    }
}

private extension MarketChartBar {
    var candlestickData: CandlestickData {
        CandlestickData(time: time, open: open, high: high, low: low, close: close)
    }
}

private extension MarketSymbol {
    var tokenImage: UIImage? {
        switch self {
        case .btc:
            return MR.images.shared.bitcoin_token.toUIImage()
        case .eth:
            return MR.images.shared.ethereum_token.toUIImage()
        case .sol:
            return MR.images.shared.solana_token.toUIImage()
        }
    }
}

private func marketChartOptions() -> ChartOptions {
    ChartOptions(
        autoSize: true,
        layout: LayoutOptions(
            background: .solid(color: Color.clear.chartColor),
            textColor: AppColors.textSecondary.chartColor,
            attributionLogo: false
        ),
        rightPriceScale: VisiblePriceScaleOptions(borderVisible: false),
        timeScale: TimeScaleOptions(borderVisible: false),
        crosshair: CrosshairOptions(mode: .normal),
        grid: GridOptions(
            verticalLines: GridLineOptions(color: AppColors.border.opacity(0.36).chartColor),
            horizontalLines: GridLineOptions(color: AppColors.border.opacity(0.36).chartColor)
        )
    )
}

private func marketChartSeriesOptions() -> CandlestickSeriesOptions {
    CandlestickSeriesOptions(
        lastValueVisible: true,
        priceLineVisible: true,
        priceFormat: marketChartPriceFormat(),
        upColor: AppColors.bid.chartColor,
        downColor: AppColors.ask.chartColor,
        borderVisible: false,
        wickUpColor: AppColors.bid.chartColor,
        wickDownColor: AppColors.ask.chartColor
    )
}

private func marketChartPriceFormat() -> PriceFormat {
    .custom(
        CustomPriceFormat(
            minMove: 0.01,
            formatterJavaScript: dollarPriceFormatterJavaScript,
            tickmarksFormatterJavaScript: dollarTickmarksFormatterJavaScript
        )
    )
}

private let dollarPriceFormatterJavaScript = #"""
function(price) {
    var sign = price < 0 ? '-$' : '$';
    var rounded = Math.round(Math.abs(price) * 100);
    var whole = String(Math.floor(rounded / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    var cents = rounded % 100;
    if (cents === 0) {
        return sign + whole;
    }
    return sign + whole + '.' + (cents < 10 ? '0' + cents : String(cents));
}
"""#

private let dollarTickmarksFormatterJavaScript = #"""
function(prices) {
    return prices.map(function(price) {
        var sign = price < 0 ? '-$' : '$';
        var rounded = Math.round(Math.abs(price) * 100);
        var whole = String(Math.floor(rounded / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        var cents = rounded % 100;
        if (cents === 0) {
            return sign + whole;
        }
        return sign + whole + '.' + (cents < 10 ? '0' + cents : String(cents));
    });
}
"""#

private enum AppColors {
    static let background = MR.colors.shared.app_background.asSwiftUIColor()
    static let panel = MR.colors.shared.app_panel.asSwiftUIColor()
    static let selection = MR.colors.shared.app_selection.asSwiftUIColor()
    static let border = MR.colors.shared.app_border.asSwiftUIColor()
    static let textPrimary = MR.colors.shared.text_primary.asSwiftUIColor()
    static let textSecondary = MR.colors.shared.text_secondary.asSwiftUIColor()
    static let textTertiary = MR.colors.shared.text_tertiary.asSwiftUIColor()
    static let controlTextSelected = MR.colors.shared.control_text_selected.asSwiftUIColor()
    static let controlTextDimmed = MR.colors.shared.control_text_dimmed.asSwiftUIColor()
    static let accent = MR.colors.shared.brand_orange.asSwiftUIColor()
    static let bid = MR.colors.shared.bid.asSwiftUIColor()
    static let ask = MR.colors.shared.ask.asSwiftUIColor()
}

private extension Color {
    var chartColor: ChartColor {
        ChartColor(UIColor(self))
    }
}

private extension ResourcesColorResource {
    func asSwiftUIColor() -> Color {
        guard let color = UIColor(named: name, in: bundle, compatibleWith: nil) else {
            preconditionFailure("Missing moko color resource: \(name)")
        }
        return Color(uiColor: color)
    }
}

struct MarketDetailsScreen_Previews: PreviewProvider {
    static var previews: some View {
        MarketDetailsScreen(viewModel: .preview)
    }
}
