@preconcurrency import SharedLogic
import SwiftUI
import UIKit

@MainActor
final class OrderBookViewModel: ObservableObject {
    @Published private(set) var selection: OrderBookSelection
    @Published private(set) var renderState: OrderBookRenderState

    private let feature: OrderBookFeature?

    init(feature: OrderBookFeature? = SharedDependencyGraph.shared.orderBookFeature()) {
        self.feature = feature
        self.selection = feature?.selection ?? OrderBookSelection(market: .btc, precision: .five)
        self.renderState = feature.map { OrderBookRenderState(shared: $0.currentState) } ?? .connecting

        feature?.observe { [weak self] state in
            MainActor.assumeIsolated {
                self?.renderState = OrderBookRenderState(shared: state)
                if let selection = self?.feature?.selection {
                    self?.selection = selection
                }
            }
        }
    }

    deinit {
        feature?.clearObserver()
        feature?.close()
    }

    func selectMarket(_ market: MarketSymbol) {
        feature?.selectMarket(market: market)
        selection = selection.doCopy(market: market, precision: selection.precision)
    }

    func selectPrecision(_ precision: PricePrecision) {
        feature?.selectPrecision(precision: precision)
        selection = selection.doCopy(market: selection.market, precision: precision)
    }

    static var preview: OrderBookViewModel {
        let viewModel = OrderBookViewModel(feature: nil)
        viewModel.renderState = OrderBookRenderState(
            status: .live,
            statusLabel: "Live",
            centerMessage: nil,
            spreadText: "0.50",
            asks: [
                OrderBookRenderRow(side: .ask, priceText: "69,128.00", sizeText: "0.75", orderCountText: "1", depthFraction: 0.38, change: .none),
                OrderBookRenderRow(side: .ask, priceText: "69,127.50", sizeText: "1.00", orderCountText: "2", depthFraction: 0.5, change: .down),
                OrderBookRenderRow(side: .ask, priceText: "69,126.00", sizeText: "2.00", orderCountText: "4", depthFraction: 1.0, change: .none),
            ],
            bids: [
                OrderBookRenderRow(side: .bid, priceText: "69,125.50", sizeText: "1.25", orderCountText: "3", depthFraction: 1.0, change: .none),
                OrderBookRenderRow(side: .bid, priceText: "69,124.00", sizeText: "0.50", orderCountText: "1", depthFraction: 0.4, change: .up),
                OrderBookRenderRow(side: .bid, priceText: "69,123.50", sizeText: "0.25", orderCountText: "2", depthFraction: 0.2, change: .none),
            ]
        )
        return viewModel
    }
}

struct OrderBookRenderState {
    let status: OrderBookStatus
    let statusLabel: String
    let centerMessage: String?
    let spreadText: String?
    let asks: [OrderBookRenderRow]
    let bids: [OrderBookRenderRow]

    var hasRows: Bool {
        !asks.isEmpty || !bids.isEmpty
    }

    static let connecting = OrderBookRenderState(
        status: .connecting,
        statusLabel: "Connecting",
        centerMessage: "Connecting",
        spreadText: nil,
        asks: [],
        bids: []
    )

    init(shared: OrderBookViewState) {
        self.status = shared.status
        self.statusLabel = shared.statusLabel
        self.centerMessage = shared.centerMessage
        self.spreadText = shared.spreadText
        self.asks = shared.asks.map(OrderBookRenderRow.init)
        self.bids = shared.bids.map(OrderBookRenderRow.init)
    }

    init(
        status: OrderBookStatus,
        statusLabel: String,
        centerMessage: String?,
        spreadText: String?,
        asks: [OrderBookRenderRow],
        bids: [OrderBookRenderRow]
    ) {
        self.status = status
        self.statusLabel = statusLabel
        self.centerMessage = centerMessage
        self.spreadText = spreadText
        self.asks = asks
        self.bids = bids
    }
}

struct OrderBookRenderRow: Hashable {
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

struct OrderBookScreen: View {
    @StateObject private var viewModel: OrderBookViewModel

    init(viewModel: OrderBookViewModel = OrderBookViewModel()) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, 16)
                .padding(.top, 12)

            VStack(spacing: 10) {
                SegmentedSelector(
                    values: [.btc, .eth, .sol],
                    selected: viewModel.selection.market,
                    label: \.displayName,
                    onSelect: viewModel.selectMarket
                )

                SegmentedSelector(
                    values: [.two, .three, .four, .five],
                    selected: viewModel.selection.precision,
                    label: { "\($0.nSigFigs)" },
                    onSelect: viewModel.selectPrecision
                )
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)

            Group {
                if viewModel.renderState.hasRows {
                    OrderBookList(renderState: viewModel.renderState)
                } else {
                    CenterMessage(text: viewModel.renderState.centerMessage ?? "Connecting")
                }
            }
            .padding(.top, 14)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppColors.background.ignoresSafeArea())
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(viewModel.selection.market.displayName)-USD")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(AppColors.textPrimary)
                    .lineLimit(1)

                Text(viewModel.renderState.statusLabel)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(statusColor(for: viewModel.renderState.status))
                    .lineLimit(1)
            }

            Spacer(minLength: 12)

            Text("Hyperliquid")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(AppColors.accent)
                .lineLimit(1)
        }
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
    let renderState: OrderBookRenderState

    var body: some View {
        VStack(spacing: 0) {
            HeaderRow()
                .padding(.horizontal, 20)

            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(renderState.asks, id: \.self) { level in
                        LevelRow(level: level)
                    }

                    SpreadRow(spreadText: renderState.spreadText ?? "--")

                    ForEach(renderState.bids, id: \.self) { level in
                        LevelRow(level: level)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
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
    let level: OrderBookRenderRow

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

private enum AppColors {
    static let background = MR.colors.shared.app_background.asSwiftUIColor()
    static let panel = MR.colors.shared.app_panel.asSwiftUIColor()
    static let selection = MR.colors.shared.app_selection.asSwiftUIColor()
    static let border = MR.colors.shared.app_border.asSwiftUIColor()
    static let textPrimary = MR.colors.shared.text_primary.asSwiftUIColor()
    static let textSecondary = MR.colors.shared.text_secondary.asSwiftUIColor()
    static let textTertiary = MR.colors.shared.text_tertiary.asSwiftUIColor()
    static let accent = MR.colors.shared.brand_orange.asSwiftUIColor()
    static let bid = MR.colors.shared.bid.asSwiftUIColor()
    static let ask = MR.colors.shared.ask.asSwiftUIColor()
    static let upFlash = MR.colors.shared.up_flash.asSwiftUIColor()
    static let downFlash = MR.colors.shared.down_flash.asSwiftUIColor()
}

private extension ResourcesColorResource {
    func asSwiftUIColor() -> Color {
        guard let color = UIColor(named: name, in: bundle, compatibleWith: nil) else {
            preconditionFailure("Missing moko color resource: \(name)")
        }
        return Color(uiColor: color)
    }
}

struct OrderBookScreen_Previews: PreviewProvider {
    static var previews: some View {
        OrderBookScreen(viewModel: .preview)
    }
}
