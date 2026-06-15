import Pow
@preconcurrency import SharedLogic
import SwiftUI

struct RecentTradesState {
    let status: RecentTradesStatus
    let statusLabel: String
    let centerMessage: String?
    let rows: [RecentTradesRow]
    let isLoading: Bool

    var hasRows: Bool {
        !rows.isEmpty
    }

    static let preview = RecentTradesState(
        status: .live,
        statusLabel: "Live",
        centerMessage: nil,
        rows: [
            RecentTradesRow(side: .buy, sideText: "Buy", priceText: "$69,125.50", sizeText: "0.42", transactionHash: "0xabc123def4567890", timeMillis: 1_710_000_000_000, tradeId: 42),
            RecentTradesRow(side: .sell, sideText: "Sell", priceText: "$69,124", sizeText: "0.25", transactionHash: "0xdef456abc1237890", timeMillis: 1_710_000_000_100, tradeId: 43),
            RecentTradesRow(side: .buy, sideText: "Buy", priceText: "$69,126", sizeText: "0.18", transactionHash: "0x7890abc123def456", timeMillis: 1_710_000_000_200, tradeId: 44),
        ],
        isLoading: false
    )

    static let loading = RecentTradesState(
        status: .connecting,
        statusLabel: "Connecting",
        centerMessage: nil,
        rows: [],
        isLoading: true
    )

    init(shared: RecentTradesViewState) {
        self.status = shared.status
        self.statusLabel = shared.statusLabel
        self.centerMessage = shared.centerMessage
        self.rows = shared.recentTrades.map(RecentTradesRow.init)
        self.isLoading = shared.status == .connecting && !shared.hasTrades
    }

    init(
        status: RecentTradesStatus,
        statusLabel: String,
        centerMessage: String?,
        rows: [RecentTradesRow],
        isLoading: Bool
    ) {
        self.status = status
        self.statusLabel = statusLabel
        self.centerMessage = centerMessage
        self.rows = rows
        self.isLoading = isLoading
    }
}

struct RecentTradesRow: Hashable, Identifiable {
    let side: RecentTradesSide
    let sideText: String
    let priceText: String
    let sizeText: String
    let transactionHash: String
    let timeMillis: Int64
    let tradeId: Int64

    var id: String {
        "\(tradeId)-\(transactionHash)"
    }

    var timeText: String {
        Self.timeFormatter.string(from: Date(timeIntervalSince1970: Double(timeMillis) / 1_000))
    }

    init(shared: RecentTradesRowDisplay) {
        self.side = shared.side
        self.sideText = shared.sideText
        self.priceText = shared.priceText
        self.sizeText = shared.sizeText
        self.transactionHash = shared.transactionHash
        self.timeMillis = shared.timeMillis
        self.tradeId = shared.tradeId
    }

    init(
        side: RecentTradesSide,
        sideText: String,
        priceText: String,
        sizeText: String,
        transactionHash: String,
        timeMillis: Int64,
        tradeId: Int64
    ) {
        self.side = side
        self.sideText = sideText
        self.priceText = priceText
        self.sizeText = sizeText
        self.transactionHash = transactionHash
        self.timeMillis = timeMillis
        self.tradeId = tradeId
    }

    private static let timeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()
}

struct RecentTradesSection: View {
    let market: MarketSymbol
    let recentTrades: RecentTradesState

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if recentTrades.isLoading {
                RecentTradesSkeleton(market: market)
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else if recentTrades.hasRows {
                RecentTradesList(market: market, rows: recentTrades.rows)
                    .transition(.movingParts.blur.combined(with: .opacity))
            } else {
                CenterMessage(text: recentTrades.centerMessage ?? "Connecting")
                    .frame(height: 180)
                    .transition(.movingParts.blur.combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.22), value: recentTrades.isLoading)
    }
}

private struct RecentTradesSkeleton: View {
    let market: MarketSymbol

    var body: some View {
        VStack(spacing: 0) {
            RecentTradesHeaderRow(market: market)

            ForEach(0..<7, id: \.self) { index in
                RecentTradesSkeletonRow(seed: index)
            }
        }
    }
}

private struct RecentTradesSkeletonRow: View {
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

private struct RecentTradesList: View {
    let market: MarketSymbol
    let rows: [RecentTradesRow]
    @Environment(\.openURL) private var openURL

    var body: some View {
        LazyVStack(spacing: 0) {
            RecentTradesHeaderRow(market: market)

            ForEach(Array(rows.prefix(18).enumerated()), id: \.element) { _, row in
                RecentTradesListRow(row: row) {
                    openURL(row.hyperliquidExplorerURL)
                }
            }
        }
    }
}

private struct RecentTradesHeaderRow: View {
    let market: MarketSymbol

    var body: some View {
        HStack(spacing: 10) {
            RecentTradesHeaderCell(text: "Price(USD)", alignment: .leading)
            Text("Size (\(market.displayName))")
                .font(.system(size: 11))
                .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)
                .frame(width: recentTradesSizeColumnWidth, alignment: .leading)
                .frame(maxWidth: .infinity, alignment: .center)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
            RecentTradesHeaderCell(text: "Time", alignment: .trailing)
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 6)
    }
}

private struct RecentTradesHeaderCell: View {
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

private struct RecentTradesListRow: View {
    let row: RecentTradesRow
    let onSelectHash: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Text(row.priceText)
                .foregroundStyle(recentTradesColor(for: row.side))
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(row.sizeText)
                .foregroundStyle(MR.colors.shared.text_primary.swiftUIColor)
                .frame(width: recentTradesSizeColumnWidth, alignment: .leading)
                .frame(maxWidth: .infinity, alignment: .center)

            HStack(spacing: 6) {
                Text(row.timeText)
                    .foregroundStyle(MR.colors.shared.text_secondary.swiftUIColor)

                TransactionHashButton(action: onSelectHash)
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .font(.system(size: 13, design: .monospaced))
        .lineLimit(1)
        .minimumScaleFactor(0.72)
        .frame(height: 30)
        .padding(.horizontal, 4)
    }
}

private struct TransactionHashButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.up.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(MR.colors.shared.text_secondary.swiftUIColor)
                .frame(width: 22, height: 22)
                .background(MR.colors.shared.app_panel.swiftUIColor.opacity(0.82))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Open transaction in Hyperliquid explorer")
    }
}

private func recentTradesColor(for side: RecentTradesSide) -> Color {
    switch side {
    case .buy:
        return MR.colors.shared.bid.swiftUIColor
    case .sell:
        return MR.colors.shared.ask.swiftUIColor
    case .unknown:
        return MR.colors.shared.text_secondary.swiftUIColor
    }
}

private extension RecentTradesRow {
    var hyperliquidExplorerURL: URL {
        URL(string: "https://app.hyperliquid.xyz/explorer/tx/\(transactionHash)")!
    }
}

private let recentTradesSizeColumnWidth: CGFloat = 64
