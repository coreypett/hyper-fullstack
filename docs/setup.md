# Setup

This project uses Kotlin Multiplatform for shared order book logic, native Android UI, and native SwiftUI for iOS.

## Prerequisites

- Xcode installed and selected with `xcode-select`
- Android Studio or Android command line tools
- Homebrew
- Ruby through `asdf` using the version in `.tool-versions`

## Install Tools

Install the Homebrew dependencies from the repo root:

```bash
brew bundle
```

The `Brewfile` installs:

- Tuist for generating the iOS Xcode project
- SwiftLint and SwiftFormat for Swift code quality
- xcode-kotlin for debugging Kotlin from Xcode
- xcodes for managing local Xcode versions

### Homebrew Trust

Homebrew may refuse formulae from third-party taps on a new machine. If `brew bundle` fails with an untrusted tap error, trust the required taps:

```bash
brew trust tuist/tuist
brew trust xcodesorg/made
brew bundle
```

Formula-scoped trust also works if Homebrew prints an exact command, for example:

```bash
brew trust --formula tuist/tuist/tuist@4.200.1
brew trust --formula xcodesorg/made/xcodes
```

Prefer the tap-level commands for this repo setup because Tuist formula versions can change.

## Ruby and Fastlane

Install the pinned Ruby gems from the repo root:

```bash
bundle install
bundle exec fastlane lanes
```

Fastlane is pinned in `Gemfile.lock`; use `bundle exec fastlane ...` so the pinned version is used.

## Generate iOS Project

Generate the Tuist-managed Xcode project:

```bash
cd iosApp
tuist generate
```

Open `iosApp/hyper-fullstack.xcodeproj` or `iosApp/hyper-fullstack.xcworkspace` after generation.

## iOS Signing

Signing is managed with `fastlane match`.

Copy the environment template:

```bash
cp fastlane/.env.example fastlane/.env
```

Fill in:

- `FASTLANE_USER`
- `FASTLANE_TEAM_ID`
- `MATCH_GIT_URL`
- `MATCH_PASSWORD`

For a new match repository, set `MATCH_READONLY=false` once and run:

```bash
bundle exec fastlane ios sync_development_signing
bundle exec fastlane ios sync_appstore_signing
```

After match creates the certificates and profiles, switch `MATCH_READONLY` back to `true`.

## Validation

Run a simulator build without signing:

```bash
bundle exec fastlane ios build_simulator
```

Run Android checks:

```bash
./gradlew :androidApp:assembleDebug
./gradlew :sharedLogic:testAndroidHostTest
```
