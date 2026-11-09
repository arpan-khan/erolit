# EroLit

An unofficial, ad-free Android reader for [Literotica](https://www.literotica.com), built with Kotlin and Jetpack Compose.

[![Latest release](https://img.shields.io/github/v/release/arpan-khan/erolit?label=latest%20release)](https://github.com/arpan-khan/erolit/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> **18+ only.** EroLit displays adult fiction supplied by an external service. It is unofficial and is not affiliated with, endorsed by, or supported by Literotica. The app bundles no stories, images, or author content. Use it at your own risk and follow the upstream service's terms.

**[Download the latest APK](https://github.com/arpan-khan/erolit/releases/latest)** · **[All releases](https://github.com/arpan-khan/erolit/releases)** · **[Source code](https://github.com/arpan-khan/erolit)**

## Features

- **Browse** New, Top, and Series feeds.
  - Top lists: Top rated, Most read, and Most favorited, for 7 days, 30 days, 12 months, or all time.
  - Series feeds: updated, discussed, favorited, recently started, and longest running.
- **Categories** — story categories with story counts and category browsing.
- **Search** — stories, poetry, and members, with sorting and local search history.
- **Reader** — server-side page navigation, page/progress indicator, resume where you left off, adjustable font, size, spacing, justification, and theme.
- **Local saved list** — keep stories on the device for later. No account required; pages you have already opened are cached for offline reading.
- **Series and authors** — ordered series table of contents, author profiles, and author works.
- **My Lists** — sign in to view, create, and delete your account lists; add stories from the reader and remove them from a list.
- **First-run age gate** — confirms you are 18 or older before the app opens.
- **Polite networking** — one request in flight, at most one request per second (two seconds in slow mode), with backoff for transient errors. No WebView, no CAPTCHA bypass, no bulk downloading.
- **Private by default** — no ads, analytics, telemetry, or crash reporting. Session secrets are encrypted with the Android Keystore, and Android backups are disabled.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.0.21, Coroutines, Flow |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Networking | OkHttp 4.12, kotlinx.serialization |
| Local storage | Room 2.6 (KSP), DataStore Preferences |
| Images | Coil |
| Secrets | Android Keystore (AES-GCM) |
| Architecture | Single activity, MVVM, repository pattern, manual DI |

Minimum Android version: **Android 8.0 (API 26)**. Target and compile SDK: **35**.

## Getting started

### Requirements

- Android Studio (Ladybug or newer recommended)
- Android SDK 35
- JDK 17

### Build and run

```bash
git clone https://github.com/arpan-khan/erolit.git
cd erolit
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`.

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can also open the project in Android Studio and run it on a device or emulator running Android 8.0 or newer.

### Tests

```bash
./gradlew test
```

### Release builds

Create `keystore.properties` from the example and fill in your own signing details:

```bash
cp keystore.properties.example keystore.properties
```

Then build:

```bash
./gradlew assembleRelease
```

`keystore.properties`, `*.jks`, and other signing files are ignored by Git. Never commit them.

## Project structure

```text
app/src/main/java/dev/erolit/reader/
├── core/       networking, authentication, database, settings
├── data/       API DTOs, mappers, repositories
├── domain/     domain models
└── ui/         Compose screens, components, and ViewModels
```

## How it works

EroLit talks directly to the upstream service's JSON endpoints. It does not embed a browser or scrape rendered pages. Requests are serialized and rate-limited, and story text is rendered as native Compose text.

Reading progress, cached pages, search history, and the local saved list are stored locally in Room. There is no cross-device sync. Login uses the upstream session flow; the long-lived session ID is encrypted at rest, and short-lived bearer tokens are refreshed automatically.

The upstream API is **unofficial and undocumented**, so it can change or disappear without notice. When that happens, some screens may stop working until the app is updated.

## Limitations

- Read-only first: there is no account creation, commenting, rating, following, or favorites management.
- Reading progress and the local saved list are local to one device.
- The app is not distributed through Google Play; install APKs from the [Releases page](https://github.com/arpan-khan/erolit/releases).
- Content availability and account access are controlled by the upstream service.

## AI-assisted development

This project was built with substantial AI assistance. AI coding tools helped with upstream API research, architecture, Kotlin and Compose implementation, tests, and documentation. The generated code was reviewed and tested during development; as with any software, bugs may remain.

AI is **not** used inside the app at runtime: EroLit has no AI features and sends nothing to an AI service.

## Privacy and legal

- EroLit is an unofficial client. It is not connected to Literotica.
- No stories or author content are included in this repository or the APK.
- Content remains the copyright of its respective authors and the upstream service.
- You are responsible for complying with the upstream service's terms and the laws that apply where you live.
- The software is provided **as is**, without warranty of any kind. See [LICENSE](LICENSE).
- To report a security issue, see [SECURITY.md](SECURITY.md).

## Contributing

Issues and pull requests are welcome. Please do not include credentials, session tokens, scraped content, or copyrighted story text in issues, pull requests, or test fixtures.

## License

The source code is available under the [MIT License](LICENSE). The license applies only to this project's code, not to content fetched from the upstream service.
