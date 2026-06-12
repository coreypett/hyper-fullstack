fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## iOS

### ios sync_development_signing

```sh
[bundle exec] fastlane ios sync_development_signing
```

Install development signing assets managed by fastlane match

### ios sync_appstore_signing

```sh
[bundle exec] fastlane ios sync_appstore_signing
```

Install App Store/TestFlight signing assets managed by fastlane match

### ios build_simulator

```sh
[bundle exec] fastlane ios build_simulator
```

Generate the Tuist project and build an unsigned simulator app

### ios build_testflight

```sh
[bundle exec] fastlane ios build_testflight
```

Build a TestFlight-ready archive using fastlane match App Store signing

### ios upload_testflight

```sh
[bundle exec] fastlane ios upload_testflight
```

Upload the latest TestFlight archive to App Store Connect

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
