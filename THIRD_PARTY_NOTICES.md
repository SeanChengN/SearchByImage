# Third-party notices

The application uses the following primary dependencies. Exact versions are pinned in `gradle/libs.versions.toml`.

| Component | Version | License / project |
| --- | ---: | --- |
| AndroidX Core, Activity, Lifecycle, Browser, WorkManager, DataStore, Room | pinned | [Apache License 2.0](https://source.android.com/docs/setup/about/licenses) |
| Jetpack Compose and Navigation 3 | Compose BOM 2026.08.00 / 1.1.6 | [Apache License 2.0](https://source.android.com/docs/setup/about/licenses) |
| OkHttp | 5.5.0 | [Apache License 2.0](https://github.com/square/okhttp/blob/master/LICENSE.txt) |
| Coil Compose / OkHttp | 3.5.0 | [Apache License 2.0](https://github.com/coil-kt/coil/blob/main/LICENSE.txt) |
| Android Image Cropper | 4.7.0 | [Apache License 2.0](https://github.com/CanHub/Android-Image-Cropper/blob/main/LICENSE.txt) |
| Kotlin coroutines | 1.11.0 | [Apache License 2.0](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt) |
| JSON-java (tests only) | 20260814 | [JSON License](https://github.com/stleary/JSON-java/blob/master/LICENSE) |
| Google Lens, Baidu and Sogou vector marks | local assets | Vector geometry and brand colors sourced from [Simple Icons at `8ece2c1`](https://github.com/simple-icons/simple-icons/tree/8ece2c134419494a02b49a118e93a53da575a86f), whose repository is under [CC0-1.0](https://github.com/simple-icons/simple-icons/blob/8ece2c134419494a02b49a118e93a53da575a86f/LICENSE.md); see its [brand-asset disclaimer](https://github.com/simple-icons/simple-icons/blob/8ece2c134419494a02b49a118e93a53da575a86f/DISCLAIMER.md) |
| trace.moe mark | local asset | Vector geometry and colors converted from the official [trace.moe favicon](https://trace.moe/favicon.svg) |
| Yandex mark | local asset | Vector geometry supplied from the Yandex wordmark SVG and converted to an Android vector; the first letter uses Yandex red and the remaining wordmark uses black |
| TinEye mark | local asset | User-supplied `tineye-logo.png`, resized once for the engine selector and bundled as PNG data in the APK |
| SauceNAO, Lenso.ai and Ascii2D marks | local assets | Official site favicons downloaded from each engine's HTTPS origin, converted once to 32 px PNG data, and bundled in the APK; no runtime third-party request is made to display them |

Service names, logos, and trademarks belong to their respective owners. The bundled marks identify the user-selected destination service only; inclusion in the engine catalog does not imply endorsement or affiliation. Engines without a suitably sourced small-format mark use the application's generic search icon.

The repository was derived from the archived [RikkaW/SearchByImage](https://github.com/RikkaW/SearchByImage) project. The checked-out upstream tree did not contain a top-level license file, so this project preserves attribution without inventing a license grant.
