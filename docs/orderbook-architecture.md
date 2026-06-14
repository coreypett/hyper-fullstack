# Order Book Architecture

This project keeps the assignment-critical trading logic in KMP while leaving the final app surfaces native to each platform.

## Module Boundaries

`sharedLogic`

- Owns the Hyperliquid websocket connection.
- Builds the `l2Book` subscription with `coin` and `nSigFigs`.
- Builds the `trades` subscription with `coin` for recent market executions.
- Parses raw websocket frames into stable `OrderBookSnapshot` values.
- Parses raw websocket `WsTrade[]` frames into stable `Trade` values.
- Normalizes asks, bids, spread, depth fractions, and row-level size-change hints.
- Applies shared websocket reconnect/backoff handling and preserves last-known data as stale UI state.
- Multiplexes default realtime feed subscriptions onto one shared websocket connection.
- Exports a narrow `OrderBookRepository.states(selection)` flow for platform UI.
- Exports a narrow `TradeRepository.states(selection)` flow for platform UI.

`iosApp`

- Remains the native SwiftUI target for the required iOS order book widget.
- Uses Tuist as the source of truth for generated Xcode project files.
- Keeps iOS-specific Fastlane and `match` configuration under `iosApp/fastlane`.
- Should consume `SharedLogic` through a small Swift-facing adapter or view model.
- Should implement iOS-native scrolling, animation, haptics, and selection controls.

`androidApp`

- Hosts the Android-specific Compose order book bonus app.
- Consumes `sharedLogic` state without duplicating websocket or parsing behavior.
- Uses Android-native packaging, permissions, and lifecycle.

## Dependencies

- Ktor client core and websockets for the shared websocket feed.
- Ktor OkHttp engine for Android.
- Ktor Darwin engine for iOS.
- kotlinx.coroutines for shared flow state.
- kotlinx.serialization-json for flexible websocket JSON parsing without a serialization compiler plugin.

## Next Build Slice

1. Add deeper Android market-details parity for candles, summary, and recent trades.
2. Tune shared market-data throttling if `l2Book`, candles, summary, and trades are active on more app surfaces.
3. Configure `iosApp/.env`, then run `cd iosApp && bundle exec fastlane ios sync_development_signing`.
