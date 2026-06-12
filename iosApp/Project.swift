import ProjectDescription

let project = Project(
    name: "hyper-fullstack",
    organizationName: "Corey Pett",
    settings: .settings(
        base: [
            "ALWAYS_SEARCH_USER_PATHS": "NO",
            "CLANG_ANALYZER_NONNULL": "YES",
            "CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION": "YES_AGGRESSIVE",
            "CLANG_CXX_LANGUAGE_STANDARD": "gnu++20",
            "CLANG_ENABLE_MODULES": "YES",
            "CLANG_ENABLE_OBJC_ARC": "YES",
            "CLANG_ENABLE_OBJC_WEAK": "YES",
            "COPY_PHASE_STRIP": "NO",
            "ENABLE_STRICT_OBJC_MSGSEND": "YES",
            "ENABLE_USER_SCRIPT_SANDBOXING": "NO",
            "GCC_C_LANGUAGE_STANDARD": "gnu17",
            "GCC_NO_COMMON_BLOCKS": "YES",
            "IPHONEOS_DEPLOYMENT_TARGET": "18.2",
            "LOCALIZATION_PREFERS_STRING_CATALOGS": "YES",
            "MTL_FAST_MATH": "YES",
            "SDKROOT": "iphoneos",
        ],
        configurations: [
            .debug(name: "Debug", xcconfig: "Configuration/Config.xcconfig"),
            .release(name: "Release", xcconfig: "Configuration/Config.xcconfig"),
        ]
    ),
    targets: [
        .target(
            name: "iosApp",
            destinations: [.iPhone, .iPad],
            product: .app,
            bundleId: "$(PRODUCT_BUNDLE_IDENTIFIER)",
            deploymentTargets: .iOS("18.2"),
            infoPlist: .file(path: "iosApp/Info.plist"),
            sources: [
                "iosApp/**/*.swift",
            ],
            resources: [
                "iosApp/Assets.xcassets",
                "iosApp/Preview Content/**",
            ],
            scripts: [
                .pre(
                    script: """
                    if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                      echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES"
                      exit 0
                    fi

                    cd "$SRCROOT/.."
                    ./gradlew :sharedLogic:embedAndSignAppleFrameworkForXcode --console=plain
                    """,
                    name: "Compile Kotlin Framework",
                    basedOnDependencyAnalysis: false
                ),
            ],
            settings: .settings(
                base: [
                    "ARCHS": "arm64",
                    "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
                    "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME": "AccentColor",
                    "CODE_SIGN_STYLE": "Manual",
                    "DEVELOPMENT_ASSET_PATHS": "\"iosApp/Preview Content\"",
                    "DEVELOPMENT_TEAM": "$(DEVELOPMENT_TEAM)",
                    "ENABLE_PREVIEWS": "YES",
                    "FRAMEWORK_SEARCH_PATHS": [
                        "$(inherited)",
                        "$(SRCROOT)/../sharedLogic/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
                    ],
                    "GENERATE_INFOPLIST_FILE": "YES",
                    "INFOPLIST_FILE": "iosApp/Info.plist",
                    "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
                    "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents": "YES",
                    "INFOPLIST_KEY_UILaunchScreen_Generation": "YES",
                    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone": "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                    "LD_RUNPATH_SEARCH_PATHS": [
                        "$(inherited)",
                        "@executable_path/Frameworks",
                    ],
                    "OTHER_LDFLAGS": [
                        "$(inherited)",
                        "-framework",
                        "SharedLogic",
                    ],
                    "PRODUCT_NAME": "hyper-fullstack",
                    "SWIFT_EMIT_LOC_STRINGS": "YES",
                    "SWIFT_VERSION": "5.0",
                    "TARGETED_DEVICE_FAMILY": "1,2",
                ],
                configurations: [
                    .debug(
                        name: "Debug",
                        settings: [
                            "CODE_SIGN_IDENTITY": "Apple Development",
                            "PROVISIONING_PROFILE_SPECIFIER": "match Development org.coreypett.fullstack.orderbook",
                        ],
                        xcconfig: "Configuration/Config.xcconfig"
                    ),
                    .release(
                        name: "Release",
                        settings: [
                            "CODE_SIGN_IDENTITY": "Apple Distribution",
                            "PROVISIONING_PROFILE_SPECIFIER": "match AppStore org.coreypett.fullstack.orderbook",
                        ],
                        xcconfig: "Configuration/Config.xcconfig"
                    ),
                ]
            )
        ),
    ],
    schemes: [
        .scheme(
            name: "iosApp",
            shared: true,
            buildAction: .buildAction(targets: ["iosApp"]),
            runAction: .runAction(configuration: "Debug"),
            archiveAction: .archiveAction(configuration: "Release")
        ),
    ]
)
