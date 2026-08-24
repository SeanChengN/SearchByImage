# SearchByImage Agent Guide

## Project Scope

- This is a single-module Kotlin, Jetpack Compose, and Material 3 Android application.
- Keep the release application ID and namespace `io.github.seancheng.searchbyimage`; Debug uses the `.debug` suffix.
- Distribution is signed APK files attached to GitHub Releases only. Do not add Google Play publishing or AAB output.
- Toolchain, signing variables, and local build commands are documented in [README.md](README.md).

## Non-negotiable Boundaries

- Check Git status before editing and preserve user changes. Never permanently delete files; move replaced content under `.recycle-bin/`.
- Do not add storage permissions, cleartext traffic, a general WebView, Firebase, Billing, analytics, ads, or embedded credentials.
- Send one metadata-stripped image only to the one engine explicitly selected by the user.
- Keep custom engines HTTPS-only and retain private-network, credential-in-URL, port, and redirect protections.
- Never create or move tags, publish a Release, or change external signing state without explicit user authorization.
- Keep `.github/workflows/android.yml` release-only; do not add push, pull-request, schedule, or workflow-dispatch triggers.

## Task Routing

- UI and navigation: Compose screens under `ui/`, `SearchByImageApp`, and `AppViewModel`.
- Built-in engine behavior: `domain/EngineCatalog`, `data/network/SearchCoordinator`, and the relevant adapter.
- Image preparation and URI handling: `data/image/ImageRepository`, crop UI, manifest, and FileProvider paths.
- Credentials and settings: `data/security`, `data/settings`, and the credentials/settings screens.
- Custom-engine persistence and validation: `data/db` and `CustomEngineAdapter`.
- Release automation: `.github/workflows/android.yml`; live service acceptance: [docs/LIVE_ENGINE_CHECKLIST.md](docs/LIVE_ENGINE_CHECKLIST.md).
- Privacy and dependency disclosures: [PRIVACY.md](PRIVACY.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Tool Entry Points

- For unfamiliar modules, cross-file call chains, blast-radius analysis, or architecture questions, use CodeGraph `codegraph_explore` first, then read only the necessary files.
- Run `codegraph sync` when the graph is stale and `codegraph index` after large package or directory moves.
- Use RTK only for noisy summary-oriented read-only output, such as Gradle test summaries or Git status overviews.
- Use native `rg`, Git, Gradle, Android SDK, and PowerShell commands for exact searches, diffs, logs, configuration, tests, and every mutation.

## Delivery Checks

- Normal code changes: `testDebugUnitTest lintDebug assembleDebug`.
- UI, image, provider, URI, or device behavior: run the relevant `connectedDebugAndroidTest` coverage on an appropriate emulator or device.
- Adapter tests must use fixtures and MockWebServer; do not make CI depend on third-party live services.
- Validate real engines only with neutral images and the live-engine checklist.
- Release-affecting changes also require Release lint, the R8 Release APK build, and signature verification when local signing variables are configured.
