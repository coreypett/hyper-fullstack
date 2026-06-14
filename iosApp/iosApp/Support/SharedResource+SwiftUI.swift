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
