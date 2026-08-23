# Search By Image

面向 Android 10–17 的以图搜图应用。项目从 2016 年版 Android 工程重写为 Kotlin、Jetpack Compose 与 Material 3，应用 ID 为：

```text
io.github.seancheng.searchbyimage
```

新版本与旧版包名不同，可以同时安装，但不会读取或迁移旧版数据。

## 功能

- 从系统分享菜单或照片选择器接收单张图片。
- 在上传前限制尺寸、修正 EXIF 方向，并重新编码以移除位置和设备元数据。
- 原生 API 结果：trace.moe；配置个人凭据后支持 SauceNAO、Lenso.ai 与 TinEye。
- Google Lens 使用系统 URI 授权交给 Google 应用。
- Bing、Yandex、百度、搜狗、360、IQDB、Ascii2D 使用网页辅助模式。
- 第三方网页只在 Custom Tabs 中打开；应用不包含通用 WebView 或网页脚本注入。
- 自定义搜索引擎仅允许 HTTPS multipart POST，并拒绝回环、私网和不安全跳转。
- API 凭据由 Android Keystore 的 AES-GCM 密钥加密，不备份、不记录。

## 工具链

- Android Studio 2026.1
- Android Gradle Plugin 9.3.1
- Gradle 9.5.0
- `compileSdk` / `targetSdk`: 37
- `minSdk`: 29
- Java/Kotlin 字节码目标：17

AGP 9.3 默认使用 Build Tools 36.0.0。Android Studio 自带的 JBR 25 可以运行 Gradle 9.5，无需额外安装 JDK 17。

本机还应在 SDK Manager 中安装：

- Android SDK Command-line Tools (latest)
- API 29、33、37 的 x86_64 系统镜像
- API 37 Google Play 镜像，用于 Google/Lens 交接测试

## 构建

PowerShell：

```powershell
$env:JAVA_HOME = 'D:\Program Files\Android\Android Studio\jbr'
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
./gradlew.bat connectedDebugAndroidTest
./gradlew.bat lintRelease assembleRelease
```

部分澎湃 OS 真机会阻止 instrumentation 从后台拉起测试 Activity，表现为测试停在 `6/8` 且应用仍在桌面。保持手机解锁，并在另一个终端执行一次与测试规则一致的 Intent 后即可继续：

```powershell
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10008000 -n io.github.seancheng.searchbyimage.debug/io.github.seancheng.searchbyimage.MainActivity
```

这是厂商系统的真机测试前台限制，不影响普通安装和启动；GitHub Actions 的 Android 模拟器不需要该步骤。

`local.properties` 只保存本机 SDK 路径，不应提交。依赖版本集中在 `gradle/libs.versions.toml`，不使用动态版本。

项目仅通过 GitHub Release 分发 APK，不发布到 Google Play。Actions 只在 GitHub Release 发布时触发，先运行单元测试、Release Lint 和 API 29/33/37 仪器测试，再上传已签名 APK；不生成或发布 AAB。仓库需要配置以下 Actions Secrets：

- `ANDROID_SIGNING_KEYSTORE_BASE64`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

签名文件只在 Runner 临时目录中解码。对应本地环境变量为 `SEARCHBYIMAGE_KEYSTORE_PATH`、`SEARCHBYIMAGE_STORE_PASSWORD`、`SEARCHBYIMAGE_KEY_ALIAS` 和 `SEARCHBYIMAGE_KEY_PASSWORD`；四项都未设置时可生成供本机检查的未签名 Release APK，部分设置会直接让构建失败。

## 数据边界

每次搜索只会把一张图片发送给用户明确选择的一个服务。没有自有后端、Firebase、分析、广告或 Play Billing。详细说明见 [PRIVACY.md](PRIVACY.md)。

## 上游与第三方

此仓库源自已归档的 [RikkaW/SearchByImage](https://github.com/RikkaW/SearchByImage)。原始检出中没有顶层许可证文件，因此本仓库不自行推断或补写上游许可证；上游归属继续保留。

搜索引擎候选参考 [dessant/search-by-image wiki](https://github.com/dessant/search-by-image/wiki/Search-engines)，没有复制该项目的 GPL JavaScript。依赖版本及许可证见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
