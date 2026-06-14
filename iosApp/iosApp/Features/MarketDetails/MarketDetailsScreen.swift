@preconcurrency import LightweightCharts
@preconcurrency import SharedLogic
import SwiftUI
import UIKit

@MainActor
final class MarketDetailsViewModel: ObservableObject {
    @Published private(set) var selection: OrderBookSelection
    @Published private(set) var chartInterval: CandleInterval
    @Published private(set) var summary: MarketSummaryState
    @Published private(set) var chartBars: [MarketChartBar]
    @Published private(set) var orderBook: OrderBookState

    private let orderBookFeature: OrderBookFeature?
    private let candleChartFeature: CandleChartFeature?
    private let marketSummaryFeature: MarketSummaryFeature?
    private var latestOrderBookViewState: OrderBookViewState?
    private var latestMarketSummaryViewState: MarketSummaryViewState?

    init(
        orderBookFeature: OrderBookFeature? = SharedDependencyGraph.shared.orderBookFeature(),
        candleChartFeature: CandleChartFeature? = SharedDependencyGraph.shared.candleChartFeature(),
        marketSummaryFeature: MarketSummaryFeature? = SharedDependencyGraph.shared.marketSummaryFeature()
    ) {
        let initialSelection = orderBookFeature?.selection ?? OrderBookSelection(market: .btc, precision: .five)
        let initialCandleSelection = candleChartFeature?.selection ?? CandleSelection(market: initialSelection.market, interval: .oneHour)
        let initialOrderBook = orderBookFeature?.currentState
        let initialCandles = candleChartFeature?.currentState.bars ?? []
        let initialMarketSummary = marketSummaryFeature?.currentState
        self.orderBookFeature = orderBookFeature
        self.candleChartFeature = candleChartFeature
        self.marketSummaryFeature = marketSummaryFeature
        self.latestOrderBookViewState = initialOrderBook
        self.latestMarketSummaryViewState = initialMarketSummary
        self.selection = initialSelection
        self.chartInterval = initialCandleSelection.interval
        self.summary = marketSummaryFeature == nil && orderBookFeature == nil
            ? .preview
            : MarketSummaryState(marketSummary: initialMarketSummary, orderBook: initialOrderBook)
        self.chartBars = candleChartFeature == nil ? MarketChartBar.preview : initialCandles.map(MarketChartBar.init)
        self.orderBook = initialOrderBook.map(OrderBookState.init) ?? .preview
        marketSummaryFeature?.selectMarket(market: initialSelection.market)

        orderBookFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                let selection = self.orderBookFeature?.selection ?? self.selection
                self.selection = selection
                self.latestOrderBookViewState = state
                self.summary = MarketSummaryState(
                    marketSummary: self.latestMarketSummaryViewState,
                    orderBook: state
                )
                self.orderBook = OrderBookState(shared: state)
            }
        }

        candleChartFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                self?.chartBars = state.bars.map(MarketChartBar.init)
                if let selection = self?.candleChartFeature?.selection {
                    self?.chartInterval = selection.interval
                }
            }
        }

        marketSummaryFeature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                self.latestMarketSummaryViewState = state
                self.summary = MarketSummaryState(
                    marketSummary: state,
                    orderBook: self.latestOrderBookViewState
                )
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
    }

    func selectMarket(_ market: MarketSymbol) {
        orderBookFeature?.selectMarket(market: market)
        candleChartFeature?.selectMarket(market: market)
        marketSummaryFeature?.selectMarket(market: market)
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
        MarketDetailsViewModel(orderBookFeature: nil, candleChartFeature: nil, marketSummaryFeature: nil)
    }
}

struct MarketSummaryState {
    let midPriceText: String
    let priceChangeText: String
    let priceChangePercentText: String
    let changeDirection: MarketChangeDirection
    let stats: [MarketStat]

    static let preview = MarketSummaryState(
        midPriceText: "69,125.75",
        priceChangeText: "+1,228.50",
        priceChangePercentText: "+1.81%",
        changeDirection: .up,
        stats: [
            MarketStat(label: "24h Vol", value: "$4.82B"),
            MarketStat(label: "24h High", value: "70,184.00"),
            MarketStat(label: "24h Low", value: "67,914.50"),
        ]
    )

    init(marketSummary: MarketSummaryViewState?, orderBook: OrderBookViewState?) {
        self.midPriceText = marketSummary?.midPriceText ?? orderBook?.midPriceText ?? "--"
        self.priceChangeText = marketSummary?.priceChangeText ?? "--"
        self.priceChangePercentText = marketSummary?.priceChangePercentText ?? "--"
        self.changeDirection = MarketSummaryState.changeDirection(from: marketSummary?.changeDirection)
        self.stats = [
            MarketStat(label: "24h Vol", value: marketSummary?.volume24hText ?? "--"),
            MarketStat(label: "24h High", value: marketSummary?.high24hText ?? "--"),
            MarketStat(label: "24h Low", value: marketSummary?.low24hText ?? "--"),
        ]
    }

    init(
        midPriceText: String,
        priceChangeText: String,
        priceChangePercentText: String,
        changeDirection: MarketChangeDirection,
        stats: [MarketStat]
    ) {
        self.midPriceText = midPriceText
        self.priceChangeText = priceChangeText
        self.priceChangePercentText = priceChangePercentText
        self.changeDirection = changeDirection
        self.stats = stats
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

    var hasRows: Bool {
        !asks.isEmpty || !bids.isEmpty
    }

    static let preview = OrderBookState(
        status: .live,
        statusLabel: "Live",
        centerMessage: nil,
        spreadText: "0.50",
        asks: [
            OrderBookRow(side: .ask, priceText: "69,128.00", sizeText: "0.75", orderCountText: "1", depthFraction: 0.38, change: .none),
            OrderBookRow(side: .ask, priceText: "69,127.50", sizeText: "1.00", orderCountText: "2", depthFraction: 0.5, change: .down),
            OrderBookRow(side: .ask, priceText: "69,126.00", sizeText: "2.00", orderCountText: "4", depthFraction: 1.0, change: .none),
        ],
        bids: [
            OrderBookRow(side: .bid, priceText: "69,125.50", sizeText: "1.25", orderCountText: "3", depthFraction: 1.0, change: .none),
            OrderBookRow(side: .bid, priceText: "69,124.00", sizeText: "0.50", orderCountText: "1", depthFraction: 0.4, change: .up),
            OrderBookRow(side: .bid, priceText: "69,123.50", sizeText: "0.25", orderCountText: "2", depthFraction: 0.2, change: .none),
        ]
    )

    init(shared: OrderBookViewState) {
        self.status = shared.status
        self.statusLabel = shared.statusLabel
        self.centerMessage = shared.centerMessage
        self.spreadText = shared.spreadText
        self.asks = shared.asks.map(OrderBookRow.init)
        self.bids = shared.bids.map(OrderBookRow.init)
    }

    init(
        status: OrderBookStatus,
        statusLabel: String,
        centerMessage: String?,
        spreadText: String?,
        asks: [OrderBookRow],
        bids: [OrderBookRow]
    ) {
        self.status = status
        self.statusLabel = statusLabel
        self.centerMessage = centerMessage
        self.spreadText = spreadText
        self.asks = asks
        self.bids = bids
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

struct MarketDetailsScreen: View {
    @StateObject private var viewModel: MarketDetailsViewModel

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
                    onSelectInterval: viewModel.selectChartInterval
                )

                OrderBookSection(
                    selection: viewModel.selection,
                    orderBook: viewModel.orderBook,
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
        SegmentedSelector(
            values: [.btc, .eth, .sol],
            selected: selectedMarket,
            label: \.displayName,
            onSelect: onSelect
        )
    }
}

private struct MarketSummarySection: View {
    let state: MarketSummaryState

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Text(state.midPriceText)
                    .font(.system(size: 36, weight: .semibold, design: .rounded))
                    .foregroundStyle(AppColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)

                HStack(spacing: 8) {
                    Text(state.priceChangeText)
                    Text(state.priceChangePercentText)
                }
                .font(.system(size: 14, weight: .semibold, design: .monospaced))
                .foregroundStyle(changeColor(for: state.changeDirection))
                .lineLimit(1)
                .minimumScaleFactor(0.82)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .trailing, spacing: 8) {
                ForEach(state.stats, id: \.self) { stat in
                    MarketStatRow(stat: stat)
                }
            }
            .frame(width: 112, alignment: .trailing)
        }
        .padding(.vertical, 2)
    }
}

private struct MarketStatRow: View {
    let stat: MarketStat

    var body: some View {
        VStack(alignment: .trailing, spacing: 2) {
            Text(stat.label)
                .font(.system(size: 11))
                .foregroundStyle(AppColors.textTertiary)
                .lineLimit(1)

            Text(stat.value)
                .font(.system(size: 13, weight: .medium, design: .monospaced))
                .foregroundStyle(AppColors.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
    }
}

private struct MarketChartSection: View {
    let market: MarketSymbol
    let selectedInterval: CandleInterval
    let bars: [MarketChartBar]
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

                MarketCandlestickChart(bars: bars)
            }
                .frame(height: 260)
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

private struct OrderBookSection: View {
    let selection: OrderBookSelection
    let orderBook: OrderBookState
    let onSelectPrecision: (PricePrecision) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center) {
                Text("Order Book")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(AppColors.textPrimary)
                    .lineLimit(1)

                Spacer(minLength: 12)

                PrecisionSelector(
                    selected: selection.precision,
                    onSelect: onSelectPrecision
                )
                .frame(width: 164)
            }

            if orderBook.hasRows {
                OrderBookList(orderBook: orderBook)
            } else {
                CenterMessage(text: orderBook.centerMessage ?? "Connecting")
                    .frame(height: 220)
            }
        }
    }
}

private struct PrecisionSelector: View {
    let selected: PricePrecision
    let onSelect: (PricePrecision) -> Void

    var body: some View {
        SegmentedSelector(
            values: [.two, .three, .four, .five],
            selected: selected,
            label: { "\($0.nSigFigs)" },
            onSelect: onSelect
        )
    }
}

private struct SegmentedSelector<Value: Hashable>: View {
    let values: [Value]
    let selected: Value
    let label: (Value) -> String
    let onSelect: (Value) -> Void

    var body: some View {
        HStack(spacing: 3) {
            ForEach(values, id: \.self) { value in
                Button {
                    onSelect(value)
                } label: {
                    Text(label(value))
                        .font(.system(size: 13, weight: selected == value ? .semibold : .regular))
                        .foregroundStyle(selected == value ? AppColors.textPrimary : AppColors.textSecondary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 34)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .background(selected == value ? AppColors.selection : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(AppColors.panel)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(AppColors.border, lineWidth: 1)
        )
    }
}

private struct OrderBookList: View {
    let orderBook: OrderBookState

    var body: some View {
        LazyVStack(spacing: 0) {
            HeaderRow()

            ForEach(orderBook.asks, id: \.self) { level in
                LevelRow(level: level)
            }

            SpreadRow(spreadText: orderBook.spreadText ?? "--")

            ForEach(orderBook.bids, id: \.self) { level in
                LevelRow(level: level)
            }
        }
    }
}

private struct HeaderRow: View {
    var body: some View {
        HStack {
            HeaderCell(text: "Price", alignment: .leading)
            HeaderCell(text: "Size", alignment: .trailing)
            HeaderCell(text: "Orders", alignment: .trailing)
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
    }
}

private struct LevelRow: View {
    let level: OrderBookRow

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: level.side == .bid ? .leading : .trailing) {
                flashColor(for: level.change)

                sideColor(for: level.side)
                    .opacity(0.14)
                    .frame(width: geometry.size.width * CGFloat(level.depthFraction))

                HStack {
                    Text(level.priceText)
                        .foregroundStyle(sideColor(for: level.side))
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Text(level.sizeText)
                        .foregroundStyle(AppColors.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .trailing)

                    Text(level.orderCountText)
                        .foregroundStyle(AppColors.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .font(.system(size: 13, design: .monospaced))
                .lineLimit(1)
                .padding(.horizontal, 4)
            }
        }
        .frame(height: 30)
    }
}

private struct SpreadRow: View {
    let spreadText: String

    var body: some View {
        HStack {
            Text("Spread")
                .font(.system(size: 12))
                .foregroundStyle(AppColors.textTertiary)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(spreadText)
                .font(.system(size: 15, weight: .semibold, design: .monospaced))
                .foregroundStyle(AppColors.accent)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
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

private let chartIntervals: [CandleInterval] = [
    .oneMinute,
    .fiveMinutes,
    .oneHour,
    .fourHours,
    .oneDay,
]

private func sideColor(for side: OrderBookSide) -> Color {
    side == .bid ? AppColors.bid : AppColors.ask
}

private func flashColor(for change: LevelChange) -> Color {
    if change == .up {
        return AppColors.upFlash
    }
    if change == .down {
        return AppColors.downFlash
    }
    return .clear
}

private extension MarketChartBar {
    var candlestickData: CandlestickData {
        CandlestickData(time: time, open: open, high: high, low: low, close: close)
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
        upColor: AppColors.bid.chartColor,
        downColor: AppColors.ask.chartColor,
        borderVisible: false,
        wickUpColor: AppColors.bid.chartColor,
        wickDownColor: AppColors.ask.chartColor
    )
}

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
    static let upFlash = MR.colors.shared.up_flash.asSwiftUIColor()
    static let downFlash = MR.colors.shared.down_flash.asSwiftUIColor()
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
