@preconcurrency import SharedLogic
import SwiftUI

struct OrderBookState {
    let status: OrderBookStatus
    let statusLabel: String
    let centerMessage: String?
    let spreadText: String?
    let spreadPercentText: String?
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
        spreadPercentText: "0.001%",
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
        spreadPercentText: nil,
        asks: [],
        bids: [],
        isLoading: true
    )

    init(shared: OrderBookViewState) {
        self.status = shared.status
        self.statusLabel = shared.statusLabel
        self.centerMessage = shared.centerMessage
        self.spreadText = shared.spreadText
        self.spreadPercentText = shared.spreadPercentText
        self.asks = shared.asks.map(OrderBookRow.init)
        self.bids = shared.bids.map(OrderBookRow.init)
        self.isLoading = shared.status == .connecting && !shared.hasRows
    }

    init(
        status: OrderBookStatus,
        statusLabel: String,
        centerMessage: String?,
        spreadText: String?,
        spreadPercentText: String?,
        asks: [OrderBookRow],
        bids: [OrderBookRow],
        isLoading: Bool
    ) {
        self.status = status
        self.statusLabel = statusLabel
        self.centerMessage = centerMessage
        self.spreadText = spreadText
        self.spreadPercentText = spreadPercentText
        self.asks = asks
        self.bids = bids
        self.isLoading = isLoading
    }
}

struct OrderBookRow: Hashable {
    let rowKey: String
    let side: OrderBookSide
    let priceText: String
    let sizeText: String
    let orderCountText: String
    let depthFraction: Double
    let sizeChangeFraction: Double
    let change: LevelChange

    init(shared: OrderBookRowDisplay) {
        self.rowKey = shared.rowKey
        self.side = shared.side
        self.priceText = shared.priceText
        self.sizeText = shared.sizeText
        self.orderCountText = shared.orderCountText
        self.depthFraction = Double(shared.depthFraction)
        self.sizeChangeFraction = Double(shared.sizeChangeFraction)
        self.change = shared.change
    }

    init(
        rowKey: String? = nil,
        side: OrderBookSide,
        priceText: String,
        sizeText: String,
        orderCountText: String,
        depthFraction: Double,
        sizeChangeFraction: Double = 0,
        change: LevelChange
    ) {
        self.rowKey = rowKey ?? "\(side)-\(priceText)"
        self.side = side
        self.priceText = priceText
        self.sizeText = sizeText
        self.orderCountText = orderCountText
        self.depthFraction = depthFraction
        self.sizeChangeFraction = sizeChangeFraction
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
        HStack(spacing: orderBookTextCenterGap) {
            HStack(spacing: 0) {
                SkeletonBlock(width: sizeWidth(for: seed), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                SkeletonBlock(width: priceWidth(for: seed), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, orderBookOuterPadding)
            .padding(.trailing, orderBookCenterPadding)

            HStack(spacing: 0) {
                SkeletonBlock(width: priceWidth(for: seed + 2), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                SkeletonBlock(width: sizeWidth(for: seed + 1), height: 13, cornerRadius: 3)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, orderBookCenterPadding)
            .padding(.trailing, orderBookOuterPadding)
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

            SpreadRow(
                spreadText: orderBook.spreadText ?? "--",
                spreadPercentText: orderBook.spreadPercentText ?? "--"
            )

            ForEach(Array(orderBook.pairedRows.enumerated()), id: \.offset) { _, pair in
                PairedLevelRow(pair: pair)
            }
        }
    }
}

private struct HeaderRow: View {
    var body: some View {
        HStack(spacing: orderBookTextCenterGap) {
            HStack(spacing: 0) {
                HeaderCell(text: "Size", alignment: .leading)
                HeaderCell(text: "Price (Bid)", alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, orderBookOuterPadding)
            .padding(.trailing, orderBookCenterPadding)

            HStack(spacing: 0) {
                HeaderCell(text: "Price (Ask)", alignment: .leading)
                HeaderCell(text: "Size", alignment: .trailing)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, orderBookCenterPadding)
            .padding(.trailing, orderBookOuterPadding)
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

            HStack(spacing: orderBookTextCenterGap) {
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
        HStack(spacing: orderBookDepthCenterGap) {
            HStack(spacing: 0) {
                PriceDepth(level: pair.bid, side: .bid)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, orderBookOuterPadding)
            .padding(.trailing, orderBookCenterPadding)

            HStack(spacing: 0) {
                PriceDepth(level: pair.ask, side: .ask)
            }
            .frame(maxWidth: .infinity)
            .padding(.leading, orderBookCenterPadding)
            .padding(.trailing, orderBookOuterPadding)
        }
    }
}

private struct PriceDepth: View {
    let level: OrderBookRow?
    let side: OrderBookSide

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: side == .bid ? .trailing : .leading) {
                sideColor(for: side)
                    .opacity(level == nil ? 0 : 0.11)
                    .frame(width: geometry.size.width * CGFloat(depthFraction))
                    .animation(.easeInOut(duration: 0.18), value: depthFraction)
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

    private var depthFraction: Double {
        level.map { clamped($0.depthFraction) } ?? 0
    }
}

private struct BidColumns: View {
    let level: OrderBookRow?

    var body: some View {
        HStack(spacing: 0) {
            SizeText(level: level, side: .bid, alignment: .leading)
            LevelText(level?.priceText, color: MR.colors.shared.bid.swiftUIColor, alignment: .trailing)
        }
        .frame(maxWidth: .infinity)
        .padding(.leading, orderBookOuterPadding)
        .padding(.trailing, orderBookCenterPadding)
    }
}

private struct AskColumns: View {
    let level: OrderBookRow?

    var body: some View {
        HStack(spacing: 0) {
            LevelText(level?.priceText, color: MR.colors.shared.ask.swiftUIColor, alignment: .leading)
            SizeText(level: level, side: .ask, alignment: .trailing)
        }
        .frame(maxWidth: .infinity)
        .padding(.leading, orderBookCenterPadding)
        .padding(.trailing, orderBookOuterPadding)
    }
}

private struct SizeText: View {
    let level: OrderBookRow?
    let side: OrderBookSide
    let alignment: Alignment

    @State private var flashOpacity = 0.0

    var body: some View {
        ZStack(alignment: alignment) {
            Text(level?.sizeText ?? "")
                .foregroundStyle(MR.colors.shared.text_primary.swiftUIColor)

            Text(level?.sizeText ?? "")
                .foregroundStyle(sideColor(for: side).opacity(flashOpacity))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: alignment)
        .lineLimit(1)
        .minimumScaleFactor(0.72)
        .onChange(of: flashTrigger) { _, _ in
            runFlash()
        }
    }

    private var flashTrigger: String? {
        guard let level,
              level.change != .none,
              level.sizeChangeFraction >= orderBookFlashMinChangeFraction
        else {
            return nil
        }
        return "\(level.rowKey):\(level.sizeText):\(level.change)"
    }

    private var flashStartOpacity: Double {
        switch level?.change {
        case .up:
            return orderBookFlashUpOpacity
        case .down:
            return orderBookFlashDownOpacity
        default:
            return 0
        }
    }

    private func runFlash() {
        let startOpacity = flashStartOpacity
        guard startOpacity > 0 else {
            flashOpacity = 0
            return
        }
        flashOpacity = startOpacity
        withAnimation(.easeOut(duration: 0.24)) {
            flashOpacity = 0
        }
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
    let spreadPercentText: String

    var body: some View {
        HStack(spacing: orderBookSpreadItemGap) {
            Text("Spread")
                .font(.system(size: 12))
                .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)

            Text(spreadText)
                .font(.system(size: 15, weight: .semibold, design: .monospaced))
                .foregroundStyle(MR.colors.shared.brand_orange.swiftUIColor)

            Text(spreadPercentText)
                .font(.system(size: 12, weight: .medium, design: .monospaced))
                .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)
        }
        .lineLimit(1)
        .minimumScaleFactor(0.72)
        .frame(maxWidth: .infinity)
        .frame(height: 38)
        .padding(.horizontal, orderBookOuterPadding)
        .background(MR.colors.shared.app_panel.swiftUIColor)
    }
}

private let orderBookTextCenterGap: CGFloat = 8
private let orderBookDepthCenterGap: CGFloat = 0
private let orderBookCenterPadding: CGFloat = 0
private let orderBookOuterPadding: CGFloat = 4
private let orderBookSpreadItemGap: CGFloat = 12
private let orderBookFlashMinChangeFraction = 0.08
private let orderBookFlashUpOpacity = 0.88
private let orderBookFlashDownOpacity = 0.78

private func sideColor(for side: OrderBookSide) -> Color {
    side == .bid ? MR.colors.shared.bid.swiftUIColor : MR.colors.shared.ask.swiftUIColor
}
