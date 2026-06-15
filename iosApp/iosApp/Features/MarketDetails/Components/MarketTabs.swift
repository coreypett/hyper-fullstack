import Pow
@preconcurrency import SharedLogic
import SwiftUI
import UIKit

struct MarketTabs: View {
    let selectedMarket: MarketSymbol
    let onSelect: (MarketSymbol) -> Void

    @State private var shineTrigger = 0

    var body: some View {
        HStack(spacing: 10) {
            ForEach([MarketSymbol.btc, .eth, .sol], id: \.self) { market in
                MarketTabButton(
                    market: market,
                    isSelected: selectedMarket == market,
                    shineTrigger: shineTrigger,
                    onSelect: onSelect
                )
                .frame(maxWidth: .infinity)
            }
        }
        .onChange(of: selectedMarket) { _, _ in
            shineTrigger += 1
        }
    }
}

private struct MarketTabButton: View {
    let market: MarketSymbol
    let isSelected: Bool
    let shineTrigger: Int
    let onSelect: (MarketSymbol) -> Void

    var body: some View {
        Button {
            AppHaptics.selectionChanged()
            onSelect(market)
        } label: {
            HStack(spacing: 8) {
                MarketTokenIcon(market: market, size: 22)

                Text(market.displayName)
                    .font(.system(size: 13, weight: isSelected ? .semibold : .medium))
                    .foregroundStyle(isSelected ? MR.colors.shared.text_primary.swiftUIColor : MR.colors.shared.text_secondary.swiftUIColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 38)
            .padding(.horizontal, 10)
            .background(isSelected ? MR.colors.shared.app_selection.swiftUIColor : MR.colors.shared.app_panel.swiftUIColor)
            .clipShape(Capsule(style: .continuous))
            .overlay(
                Capsule(style: .continuous)
                    .stroke(isSelected ? MR.colors.shared.text_secondary.swiftUIColor.opacity(0.42) : MR.colors.shared.app_border.swiftUIColor, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .changeEffect(.shine(duration: 0.45), value: shineTrigger, isEnabled: isSelected)
    }
}

struct MarketTokenIcon: View {
    let market: MarketSymbol
    var size: CGFloat = 22

    var body: some View {
        if let image {
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
        }
    }

    private var image: UIImage? {
        switch market {
        case .btc:
            return MR.images.shared.bitcoin_token.toUIImage()
        case .eth:
            return MR.images.shared.ethereum_token.toUIImage()
        case .sol:
            return MR.images.shared.solana_token.toUIImage()
        }
    }
}
