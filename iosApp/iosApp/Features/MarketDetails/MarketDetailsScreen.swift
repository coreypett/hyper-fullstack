@preconcurrency import SharedLogic
import SwiftUI

enum MarketDataPanel: CaseIterable {
    case orderBook
    case recentTrades

    var title: String {
        switch self {
        case .orderBook:
            return "Order Book"
        case .recentTrades:
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

                MarketSummarySection(
                    selectedMarket: viewModel.selection.market,
                    state: viewModel.summary
                )
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
                    recentTrades: viewModel.recentTrades,
                    onSelectPanel: { selectedMarketDataPanel = $0 },
                    onSelectPrecision: viewModel.selectPrecision
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .scrollIndicators(.hidden)
        .background(MR.colors.shared.app_background.swiftUIColor.ignoresSafeArea())
    }
}

struct MarketDetailsScreen_Previews: PreviewProvider {
    static var previews: some View {
        MarketDetailsScreen(viewModel: .preview)
    }
}
