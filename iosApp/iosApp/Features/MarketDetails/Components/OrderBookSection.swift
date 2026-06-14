import Pow
@preconcurrency import SharedLogic
import SwiftUI

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

struct OrderBookSection: View {
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
            .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)
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
            LevelText(level?.sizeText, color: MR.colors.shared.text_primary.swiftUIColor, alignment: .leading)
            LevelText(level?.priceText, color: MR.colors.shared.bid.swiftUIColor, alignment: .trailing)
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
            LevelText(level?.priceText, color: MR.colors.shared.ask.swiftUIColor, alignment: .leading)
            LevelText(level?.sizeText, color: MR.colors.shared.text_primary.swiftUIColor, alignment: .trailing)
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
                .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)

            Text(spreadText)
                .font(.system(size: 15, weight: .semibold, design: .monospaced))
                .foregroundStyle(MR.colors.shared.brand_orange.swiftUIColor)
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .frame(height: 38)
        .padding(.horizontal, 4)
        .background(MR.colors.shared.app_panel.swiftUIColor)
    }
}

private let orderBookPriceGap: CGFloat = 8

private func sideColor(for side: OrderBookSide) -> Color {
    side == .bid ? MR.colors.shared.bid.swiftUIColor : MR.colors.shared.ask.swiftUIColor
}
