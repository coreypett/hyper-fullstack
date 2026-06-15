import ProjectDescription

let appName = "hyper-fullstack"
let projectName = "iosApp"
let organizationName = "Corey Pett"
let targetName = "iosApp"
let bundleIdentifier = "org.coreypett.fullstack"
let deploymentTarget = "26.0"
let marketingVersion = "1.0"
let buildNumber = "2"

let infoPlistPath: Path = "iosApp/Resources/Info.plist"
let sharedFrameworkName = "SharedLogic"
let sharedFrameworkSearchPath = "$(SRCROOT)/../sharedLogic/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)"

func setting(_ value: String) -> SettingValue {
    .string(value)
}

func setting(_ values: [String]) -> SettingValue {
    .array(values)
}

let project = Project(
    name: projectName,
    organizationName: organizationName,
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
            "IPHONEOS_DEPLOYMENT_TARGET": setting(deploymentTarget),
            "LOCALIZATION_PREFERS_STRING_CATALOGS": "YES",
            "MTL_FAST_MATH": "YES",
            "SDKROOT": "iphoneos",
        ],
        configurations: [
            .debug(name: "Debug"),
            .release(name: "Release"),
        ]
    ),
    targets: [
        .target(
            name: targetName,
            destinations: [.iPhone, .iPad],
            product: .app,
            bundleId: bundleIdentifier,
            deploymentTargets: .iOS(deploymentTarget),
            infoPlist: .file(path: infoPlistPath),
            sources: [
                "iosApp/**/*.swift",
            ],
            resources: [
                "iosApp/Resources/Assets.xcassets",
                "iosApp/Resources/**/*.storyboard",
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
                    ./gradlew :sharedLogic:copyFrameworkResourcesToApp \
                      -Pmoko.resources.BUILT_PRODUCTS_DIR="$BUILT_PRODUCTS_DIR" \
                      -Pmoko.resources.CONTENTS_FOLDER_PATH="$CONTENTS_FOLDER_PATH" \
                      -Pmoko.resources.CONFIGURATION="$CONFIGURATION" \
                      -Pmoko.resources.PLATFORM_NAME="$PLATFORM_NAME" \
                      -Pmoko.resources.ARCHS="$ARCHS" \
                      --console=plain
                    """,
                    name: "Compile Kotlin Framework",
                    basedOnDependencyAnalysis: false
                ),
            ],
            dependencies: [
                .external(name: "LightweightCharts"),
                .external(name: "Pow"),
            ],
            settings: .settings(
                base: [
                    "ARCHS": "arm64",
                    "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
                    "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME": "AccentColor",
                    "CODE_SIGN_STYLE": "Manual",
                    "CURRENT_PROJECT_VERSION": setting(buildNumber),
                    "DEVELOPMENT_TEAM": "$(DEVELOPMENT_TEAM)",
                    "DEVELOPMENT_ASSET_PATHS": "",
                    "ENABLE_PREVIEWS": "YES",
                    "FRAMEWORK_SEARCH_PATHS": setting([
                        "$(inherited)",
                        sharedFrameworkSearchPath,
                    ]),
                    "GENERATE_INFOPLIST_FILE": "YES",
                    "INFOPLIST_FILE": "iosApp/Resources/Info.plist",
                    "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
                    "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents": "YES",
                    "INFOPLIST_KEY_UILaunchStoryboardName": "LaunchScreen",
                    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone": "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                    "INFOPLIST_KEY_UIUserInterfaceStyle": "Dark",
                    "LD_RUNPATH_SEARCH_PATHS": setting([
                        "$(inherited)",
                        "@executable_path/Frameworks",
                    ]),
                    "MARKETING_VERSION": setting(marketingVersion),
                    "OTHER_LDFLAGS": setting([
                        "$(inherited)",
                        "-framework",
                        sharedFrameworkName,
                    ]),
                    "PRODUCT_BUNDLE_IDENTIFIER": setting(bundleIdentifier),
                    "PRODUCT_NAME": setting(appName),
                    "SWIFT_EMIT_LOC_STRINGS": "YES",
                    "SWIFT_VERSION": "6.0",
                    "TARGETED_DEVICE_FAMILY": "1,2",
                ],
                configurations: [
                    .debug(
                        name: "Debug",
                        settings: [
                            "CODE_SIGN_IDENTITY": "Apple Development",
                            "PROVISIONING_PROFILE_SPECIFIER": "match Development org.coreypett.fullstack",
                        ],
                    ),
                    .release(
                        name: "Release",
                        settings: [
                            "CODE_SIGN_IDENTITY": "Apple Distribution",
                            "PROVISIONING_PROFILE_SPECIFIER": "match AppStore org.coreypett.fullstack",
                        ]
                    ),
                ]
            )
        ),
    ],
    schemes: [
        .scheme(
            name: targetName,
            shared: true,
            buildAction: .buildAction(targets: [.target(targetName)]),
            runAction: .runAction(configuration: "Debug"),
            archiveAction: .archiveAction(configuration: "Release")
        ),
    ]
)
