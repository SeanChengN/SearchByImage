# 搜索引擎发布前检查表

检查日期：2026-08-23。官网与公开文档状态不等同于真实上传通过；发布前必须使用不含个人信息的中性测试图完成“实测”列。

| 引擎 | 接入方式 | 当前实现 | 发布前实测 |
| --- | --- | --- | --- |
| Google Lens | 外部应用 | 优先显式调用 Google 应用暴露的 Lens 图片分享 Activity，再回退到 Chromium 使用的两个图片契约和手动选图；每次临时授权单张 URI | API 37 Google Play 模拟器和真机 |
| trace.moe | 自动 API | `POST https://api.trace.moe/search?anilistInfo`，原生结果及匹配画面缩略图 | 无凭据中性动画截图 |
| SauceNAO | 自动 API / BYOK | 官方 `search.php` JSON API，原生结果解析 | 本地 API Key，覆盖 401/429 |
| SauceNAO 网页版 | 网页辅助 | 打开官网，不要求 API Key；用户重新选择处理后的图片 | 移动页面文件选择与结果页 |
| Lenso.ai | 自动 API / BYOK/收费 | `POST https://api.lenso.ai/search`，Bearer Token，原生结果解析 | 开发者订阅 Token，覆盖 401/402/429 |
| TinEye | 自动 API / BYOK/收费 | `POST https://api.tineye.com/rest/search/`，`x-api-key`，1 MB 上传副本，原生结果解析 | 商业 API Key，覆盖 401/402/429 |
| Yandex Images | 网页辅助 | 打开官网 | 移动页面图片选择 |
| 百度 / 搜狗 | 网页辅助 | 打开各自 HTTPS 桌面式识图页 | 中国大陆网络、手机浏览器桌面版布局与图片选择 |
| IQDB / 3D IQDB / Ascii2D | 网页辅助 | 打开对应图片搜索官网 | 移动页面图片选择与结果跳转 |

通过标准：

1. 自动引擎必须真实接受图片并返回与夹具结构一致的结果；重定向只能保持 HTTPS 且经过域名验证。
2. 网页辅助必须在 Android Custom Tab 中提供可用文件选择，并在打开前准确说明移动页面或桌面式兼容页面的操作路径。
3. 401、402、403、429、超时、取消和解析失败均必须给出可行动的中文提示，不记录凭据或响应原文。
4. 验证码、永久失效、危险跳转或无法稳定进入图片上传结果的引擎从内置目录移除，不以“官网能打开”代替验收；Bing 与 360 已按此标准移除。
5. Google Lens 必须直接读取授权 URI 并进入图片结果；如果设备不支持直接交接，必须明确提示重新选图，不能静默打开空白 Lens 首页。
6. 原生结果中的缩略图只能加载经过 HTTPS 安全校验的 URL，跨协议或未经验证的重定向按加载失败处理。
