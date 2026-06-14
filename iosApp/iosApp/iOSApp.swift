import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        SharedDependencyGraph.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
        }
    }
}
