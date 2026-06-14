import Pow
@preconcurrency import SharedLogic
import SwiftUI

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

struct TradesSection: View {
    let market: MarketSymbol
    let trades: TradeState

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if trades.isLoading {
                TradesSkeleton(market: market)
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else if trades.hasRows {
                TradesList(market: market, rows: trades.rows)
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

private struct TradesSkeleton: View {
    let market: MarketSymbol

    var body: some View {
        VStack(spacing: 0) {
            TradeHeaderRow(market: market)

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
                .frame(maxWidth: .infinity, alignment: .center)

            SkeletonBlock(width: 58, height: 13, cornerRadius: 3)
                .frame(maxWidth: .infinity, alignment: .trailing)
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

private struct TradesList: View {
    let market: MarketSymbol
    let rows: [TradeRow]

    var body: some View {
        LazyVStack(spacing: 0) {
            TradeHeaderRow(market: market)

            ForEach(Array(rows.prefix(18).enumerated()), id: \.element) { _, row in
                TradeListRow(row: row)
            }
        }
    }
}

private struct TradeHeaderRow: View {
    let market: MarketSymbol

    var body: some View {
        HStack(spacing: 10) {
            TradeHeaderCell(text: "Price(USD)", alignment: .leading)
            TradeHeaderCell(text: "Quantity(\(market.displayName))", alignment: .center)
            TradeHeaderCell(text: "Time", alignment: .trailing)
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
            .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)
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
                .foregroundStyle(MR.colors.shared.text_primary.swiftUIColor)
                .frame(maxWidth: .infinity, alignment: .center)

            Text(row.timeText)
                .foregroundStyle(MR.colors.shared.text_secondary.swiftUIColor)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .font(.system(size: 13, design: .monospaced))
        .lineLimit(1)
        .minimumScaleFactor(0.72)
        .frame(height: 30)
        .padding(.horizontal, 4)
    }
}

private func tradeColor(for side: TradeSide) -> Color {
    switch side {
    case .buy:
        return MR.colors.shared.bid.swiftUIColor
    case .sell:
        return MR.colors.shared.ask.swiftUIColor
    case .unknown:
        return MR.colors.shared.text_secondary.swiftUIColor
    }
}
