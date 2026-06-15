@preconcurrency import LightweightCharts
import SharedLogic
import SwiftUI
import UIKit

extension ResourcesColorResource {
    var swiftUIColor: Color {
        guard let color = UIColor(named: name, in: bundle, compatibleWith: nil) else {
            preconditionFailure("Missing moko color resource: \(name)")
        }
        return Color(uiColor: color)
    }
}

extension Color {
    var chartColor: ChartColor {
        ChartColor(UIColor(self))
    }
}

@MainActor
enum AppHaptics {
    private static let impactGenerator = UIImpactFeedbackGenerator(style: .light)
    private static let selectionGenerator = UISelectionFeedbackGenerator()

    static func buttonTap() {
        impactGenerator.impactOccurred(intensity: 0.75)
        impactGenerator.prepare()
    }

    static func selectionChanged() {
        selectionGenerator.selectionChanged()
        selectionGenerator.prepare()
    }
}
