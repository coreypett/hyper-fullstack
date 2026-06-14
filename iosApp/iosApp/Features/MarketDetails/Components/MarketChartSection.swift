@preconcurrency import LightweightCharts
import Pow
@preconcurrency import SharedLogic
import SwiftUI
import UIKit

struct MarketChartBar {
    let time: Time
    let open: Double
    let high: Double
    let low: Double
    let close: Double
    let volume: Double

    init(shared: CandleBar) {
        self.time = .utc(timestamp: Double(shared.openTimeMillis) / 1_000)
        self.open = shared.open
        self.high = shared.high
        self.low = shared.low
        self.close = shared.close
        self.volume = shared.volume
    }

    init(
        time: Time,
        open: Double,
        high: Double,
        low: Double,
        close: Double,
        volume: Double
    ) {
        self.time = time
        self.open = open
        self.high = high
        self.low = low
        self.close = close
        self.volume = volume
    }

    static let preview: [MarketChartBar] = [
        MarketChartBar(time: .string("2026-06-01"), open: 68120, high: 68980, low: 67840, close: 68720, volume: 48920),
        MarketChartBar(time: .string("2026-06-02"), open: 68720, high: 69410, low: 68240, close: 69080, volume: 53110),
        MarketChartBar(time: .string("2026-06-03"), open: 69080, high: 69860, low: 68620, close: 69650, volume: 61640),
        MarketChartBar(time: .string("2026-06-04"), open: 69650, high: 70180, low: 69140, close: 69320, volume: 45875),
        MarketChartBar(time: .string("2026-06-05"), open: 69320, high: 69940, low: 68780, close: 69790, volume: 50280),
        MarketChartBar(time: .string("2026-06-06"), open: 69790, high: 70420, low: 69440, close: 70130, volume: 72860),
        MarketChartBar(time: .string("2026-06-07"), open: 70130, high: 70680, low: 69690, close: 69910, volume: 69410),
        MarketChartBar(time: .string("2026-06-08"), open: 69910, high: 70220, low: 69120, close: 69480, volume: 56300),
        MarketChartBar(time: .string("2026-06-09"), open: 69480, high: 70010, low: 68980, close: 69870, volume: 47725),
        MarketChartBar(time: .string("2026-06-10"), open: 69870, high: 70720, low: 69610, close: 70420, volume: 81240),
        MarketChartBar(time: .string("2026-06-11"), open: 70420, high: 70980, low: 69920, close: 70240, volume: 63880),
        MarketChartBar(time: .string("2026-06-12"), open: 70240, high: 70620, low: 69380, close: 69690, volume: 58540),
        MarketChartBar(time: .string("2026-06-13"), open: 69690, high: 70130, low: 69160, close: 69980, volume: 42935),
        MarketChartBar(time: .string("2026-06-14"), open: 69980, high: 70340, low: 68720, close: 69125.75, volume: 76520),
    ]
}

struct MarketChartSection: View {
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
                .foregroundStyle(MR.colors.shared.text_primary.swiftUIColor)
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
        selectedInterval == interval ? MR.colors.shared.control_text_selected.swiftUIColor : MR.colors.shared.control_text_dimmed.swiftUIColor
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
        let volumeSeries = chart.addHistogramSeries(options: volumeSeriesOptions)
        volumeSeries.priceScale().applyOptions(options: volumePriceScaleOptions)
        context.coordinator.volumeSeries = volumeSeries
        series.setData(data: bars.map(\.candlestickData))
        volumeSeries.setData(data: bars.map(\.volumeData))
        chart.timeScale().scrollToRealTime()

        return chart
    }

    func updateUIView(_ chart: LightweightCharts, context: Context) {
        chart.isOpaque = false
        chart.backgroundColor = .clear
        chart.clearWebViewBackground()
        context.coordinator.series?.setData(data: bars.map(\.candlestickData))
        context.coordinator.volumeSeries?.setData(data: bars.map(\.volumeData))
        chart.timeScale().scrollToRealTime()
    }

    final class Coordinator {
        var series: CandlestickSeries?
        var volumeSeries: HistogramSeries?
    }

    private var chartOptions: ChartOptions {
        marketChartOptions()
    }

    private var seriesOptions: CandlestickSeriesOptions {
        marketChartSeriesOptions()
    }

    private var volumeSeriesOptions: HistogramSeriesOptions {
        marketChartVolumeSeriesOptions()
    }
}

private extension MarketChartBar {
    var candlestickData: CandlestickData {
        CandlestickData(time: time, open: open, high: high, low: low, close: close)
    }

    var volumeData: HistogramData {
        HistogramData(color: volumeColor, time: time, value: volume)
    }

    private var volumeColor: ChartColor {
        close >= open ? MR.colors.shared.bid.swiftUIColor.opacity(0.26).chartColor : MR.colors.shared.ask.swiftUIColor.opacity(0.26).chartColor
    }
}

private func marketChartOptions() -> ChartOptions {
    ChartOptions(
        autoSize: true,
        layout: LayoutOptions(
            background: .solid(color: Color.clear.chartColor),
            textColor: MR.colors.shared.text_secondary.swiftUIColor.chartColor,
            attributionLogo: false
        ),
        rightPriceScale: VisiblePriceScaleOptions(
            scaleMargins: PriceScaleMargins(top: 0.08, bottom: 0.28),
            borderVisible: false
        ),
        timeScale: TimeScaleOptions(borderVisible: false),
        crosshair: CrosshairOptions(mode: .normal),
        grid: GridOptions(
            verticalLines: GridLineOptions(color: MR.colors.shared.app_border.swiftUIColor.opacity(0.36).chartColor),
            horizontalLines: GridLineOptions(color: MR.colors.shared.app_border.swiftUIColor.opacity(0.36).chartColor)
        )
    )
}

private func marketChartVolumeSeriesOptions() -> HistogramSeriesOptions {
    HistogramSeriesOptions(
        lastValueVisible: false,
        priceScaleId: "volume",
        priceLineVisible: false,
        priceFormat: .builtIn(BuiltInPriceFormat(type: .volume, precision: nil, minMove: nil)),
        color: MR.colors.shared.text_tertiary.swiftUIColor.opacity(0.22).chartColor
    )
}

private var volumePriceScaleOptions: PriceScaleOptions {
    PriceScaleOptions(
        scaleMargins: PriceScaleMargins(top: 0.78, bottom: 0),
        borderVisible: false,
        visible: false
    )
}

private func marketChartSeriesOptions() -> CandlestickSeriesOptions {
    CandlestickSeriesOptions(
        lastValueVisible: true,
        priceLineVisible: true,
        priceFormat: marketChartPriceFormat(),
        upColor: MR.colors.shared.bid.swiftUIColor.chartColor,
        downColor: MR.colors.shared.ask.swiftUIColor.chartColor,
        borderVisible: false,
        wickUpColor: MR.colors.shared.bid.swiftUIColor.chartColor,
        wickDownColor: MR.colors.shared.ask.swiftUIColor.chartColor
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
