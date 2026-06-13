// swift-tools-version: 6.0
import PackageDescription

#if TUIST
    import ProjectDescription

    let packageSettings = PackageSettings(
        productTypes: [
            "LightweightCharts": .framework,
        ]
    )
#endif

let package = Package(
    name: "HyperFullstackDependencies",
    dependencies: [
        .package(url: "https://github.com/tradingview/LightweightChartsIOS.git", exact: "5.2.0"),
    ]
)
