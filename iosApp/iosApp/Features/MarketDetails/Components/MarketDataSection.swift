import Pow
@preconcurrency import SharedLogic
import SwiftUI

struct MarketDataSection: View {
    let selectedPanel: MarketDataPanel
    let selection: OrderBookSelection
    let orderBook: OrderBookState
    let recentTrades: RecentTradesState
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
                case .recentTrades:
                    RecentTradesSection(market: selection.market, recentTrades: recentTrades)
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
                        .foregroundStyle(selectedPanel == panel ? MR.colors.shared.text_primary.swiftUIColor : MR.colors.shared.text_secondary.swiftUIColor)
                        .frame(minWidth: 86)
                        .frame(height: 34)
                        .padding(.horizontal, 6)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .background(selectedPanel == panel ? MR.colors.shared.app_selection.swiftUIColor : Color.clear)
                        .clipShape(Capsule(style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(MR.colors.shared.app_panel.swiftUIColor.opacity(0.62))
        .clipShape(Capsule(style: .continuous))
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
                    .foregroundStyle(MR.colors.shared.text_tertiary.swiftUIColor)

                Text("\(selected.nSigFigs)")
                    .font(.system(size: 13, weight: .semibold, design: .monospaced))
                    .foregroundStyle(MR.colors.shared.text_primary.swiftUIColor)

                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(MR.colors.shared.text_secondary.swiftUIColor)
            }
            .frame(height: 34)
            .padding(.horizontal, 10)
            .background(MR.colors.shared.app_panel.swiftUIColor.opacity(0.62))
            .clipShape(Capsule(style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private func groupingLabel(for precision: PricePrecision) -> String {
    "\(precision.nSigFigs)"
}
