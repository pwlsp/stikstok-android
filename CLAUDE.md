# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**TikStok** — a stock market sandbox app for Android. Users trade popular US stocks and cryptocurrencies using virtual money, track candlestick price charts, and manage multiple independent investment profiles under one account.

Comparable app on Play Store: "Day Trading Simulator & Games" by Kovets.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.example.tikstok.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Kotlin + Jetpack Compose, Material3 |
| Auth & Remote DB | Firebase Auth + Firestore |
| Local DB | Room (offline cache) |
| Market Data | Yahoo Finance API (OHLC, min 1-minute interval) |
| Min SDK | API 33 (Android 13) · compileSdk 37 (required by androidx.core 1.19.0) |

Dependencies are managed via version catalog at `gradle/libs.versions.toml`. Navigation Compose and material-icons-extended are wired up. Firebase, Room, and Retrofit/Ktor still need to be added before feature work begins.

## Architecture

MVVM with a repository layer, single-module for now. Implemented so far: the navigation skeleton (bottom nav + 3 placeholder screens) and the localization layer. The `data/`, `model/`, ViewModels, `auth/`, and `trade/` packages are still to be created.

```
app/src/main/java/com/example/tikstok/
├── MainActivity.kt    # Sets the theme and hosts TikStokApp()
├── ui/
│   ├── TikStokApp.kt  # Root Scaffold: shared top bar + NavigationBar + NavHost
│   ├── components/    # Shared composables (PlaceholderContent, ...)
│   ├── invest/        # Asset chart screen (+ ViewModel — TODO)
│   ├── trade/         # Buy/sell screen (+ ViewModel — TODO)
│   ├── portfolio/     # Portfolio & holdings screen (+ ViewModel — TODO)
│   ├── account/       # Profile list, settings + language switcher
│   └── theme/         # Color, Type, Theme
├── navigation/
│   └── TikStokDestination.kt  # enum of bottom-nav tabs (route, label, icon)
├── locale/            # AppLanguage enum + LocaleHelper (per-app locale)
├── data/              # TODO: local (Room), remote (firebase, yahoo), repository
└── model/             # TODO: Asset, Profile, Transaction
```

Tabs are switched via `NavHostController.navigateToTab` in `TikStokApp.kt` (single back-stack entry per tab with `saveState`/`restoreState`). Add new top-level tabs by extending the `TikStokDestination` enum; add detail screens (trade, transaction history) as extra `composable` routes in the same `NavHost`.

## Key Domain Concepts

- **User** — a Firebase account (email/password, Google, or GitHub auth).
- **Profile** — an isolated virtual portfolio under a user. One user can have multiple profiles, each with its own cash balance, holdings, and transaction history. Profiles are created with a configurable starting balance ($500 / $1,000 / $5,000 / $10,000).
- **Active profile** — selected via a chip in the top-right corner, visible on all main screens.
- **Asset** — a stock (e.g. AAPL, AMZN) or cryptocurrency (e.g. BTC, DOGE, ETH) from the US market.
- **Transaction** — a buy or sell event: asset, quantity, price, timestamp, profile.

## Screen Navigation

Bottom navigation bar with three tabs:

1. **Invest** — asset picker (stocks/crypto), candlestick chart with timeframe selector (30m / 1h / 4h / 1d / 1w / 1mo / 1y / 5y), current price + % change, user's holding in this asset, Buy / Sell buttons.
2. **Portfolio** — cash balance (with top-up option), total portfolio value + P/L, holdings list (sortable by value), transaction history per asset on tap.
3. **Account** — profile list (each showing cash / portfolio / all-time P/L), create new profile, settings (nickname, email, password, sign out, delete account).

Trade flow: tapping Buy or Sell opens a screen/sheet with a dollar amount input, a quick-select slider ($10/$25/$50/$100/MAX), asset-unit equivalent, transaction summary (price, potential profit, amount received), and a "Swipe to buy/sell" confirmation gesture.

## Data & Caching Strategy

- **Firebase Firestore** is the source of truth for user profiles and all transaction history.
- **Room** caches market price data and portfolio snapshots locally so the app works without a round-trip on every view change.
- **Yahoo Finance API** provides OHLC price data; minimum granularity is 1 minute.
- Write-through: transactions are written to Firestore first, then reflected in Room.

## Localization

Primary language is **English** (`values/strings.xml`). Polish is a secondary language (`values-pl/strings.xml`). All user-visible strings must go through the string resource system — never hardcode UI text. Keep English and Polish files in sync with every new string.

Runtime language switching uses the **per-app locale** API (`LocaleManager`, available natively from API 33). `locale/AppLanguage.kt` enumerates supported languages; `locale/LocaleHelper.kt` reads/sets the current one. Setting a locale recreates the activity, so the UI refreshes automatically. Supported locales are also declared in `res/xml/locales_config.xml` (referenced from the manifest via `android:localeConfig`) so the app appears in the system per-app-language settings. To add a language: add the enum entry, the `values-<tag>` folder, and a `<locale>` line in `locales_config.xml`.

## UI & Code Style

Prefer standard Jetpack Compose components (`Column`, `Row`, `LazyColumn`, `Card`, `Button`, `TextField`, etc.) and built-in Material3 elements over custom or third-party alternatives. Reach for a custom implementation only when the standard component genuinely cannot cover the use case. Keep the code simple and readable.

**Charts:** Use [Vico](https://patrykandpatrick.com/vico/) for price charts. Fall back to custom `Canvas` drawing only if Vico can't achieve the required result.

## Commit Package Name

The current application ID is `com.example.tikstok`. Rename to a proper reverse-domain before publishing.
