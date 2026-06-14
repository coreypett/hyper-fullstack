import Pow
@preconcurrency import SharedLogic
import SwiftUI

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

struct MarketSummarySection: View {
    let selectedMarket: MarketSymbol
    let state: MarketSummaryState

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            VStack(alignment: .leading, spacing: 3) {
                MarketLeadRow(
                    selectedMarket: selectedMarket
                )

                if state.isMidPriceLoading {
                    SkeletonBlock(width: 210, height: 42, cornerRadius: 6)
                        .transition(.movingParts.blur.combined(with: .opacity))
                } else {
                    Text(state.midPriceText)
                        .font(.system(size: 36, weight: .semibold, design: .rounded))
                        .foregroundStyle(MR.colors.shared.text_primary.swiftUIColor)
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
            .frame(minHeight: 84, alignment: .center)
            .animation(.easeInOut(duration: 0.22), value: state.isMidPriceLoading)
            .animation(.easeInOut(duration: 0.22), value: state.isChangeLoading)

            VStack(alignment: .trailing, spacing: 4) {
                ForEach(state.stats, id: \.self) { stat in
                    MarketStatRow(stat: stat)
                }
            }
            .frame(width: 154, alignment: .trailing)
            .frame(minHeight: 84, alignment: .center)
        }
        .padding(.vertical, 2)
    }
}

private struct MarketLeadRow: View {
    let selectedMarket: MarketSymbol

    var body: some View {
        Text("\(selectedMarket.displayName)-USD")
            .font(.system(size: 13, weight: .medium, design: .rounded))
            .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)
            .lineLimit(1)
            .minimumScaleFactor(0.82)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct MarketStatRow: View {
    let stat: MarketStat

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(stat.label)
                .font(.system(size: 10))
                .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)
                .lineLimit(1)

            Spacer(minLength: 8)

            if stat.isLoading {
                SkeletonBlock(width: 58, height: 13, cornerRadius: 3)
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else {
                Text(stat.value)
                    .font(.system(size: 12, weight: .medium, design: .monospaced))
                    .foregroundStyle(MR.colors.shared.text_primary.swiftUIColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                    .transition(.movingParts.blur.combined(with: .opacity))
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .animation(.easeInOut(duration: 0.22), value: stat.isLoading)
    }
}

private func changeColor(for direction: MarketChangeDirection) -> Color {
    switch direction {
    case .up:
        return MR.colors.shared.bid.swiftUIColor
    case .down:
        return MR.colors.shared.ask.swiftUIColor
    case .flat:
        return MR.colors.shared.text_secondary.swiftUIColor
    }
}
