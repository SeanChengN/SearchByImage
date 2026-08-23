# 搜索引擎发布前检查表

检查日期：2026-08-23。官网与公开文档状态不等同于真实上传通过；发布前必须使用不含个人信息的中性测试图完成“实测”列。

| 引擎 | 接入方式 | 当前实现 | 发布前实测 |
| --- | --- | --- | --- |
| Google Lens | 外部应用 | `ACTION_SEND image/*` + 临时 URI 授权；不可用时官网辅助 | API 37 Google Play 模拟器和真机 |
| trace.moe | 自动 API | `POST https://api.trace.moe/search?anilistInfo`，原生结果解析 | 无凭据中性动画截图 |
| SauceNAO | 自动 API / BYOK | 官方 `search.php` JSON API，原生结果解析 | 本地 API Key，覆盖 401/429 |
| Lenso.ai | 自动 API / BYOK/收费 | `POST https://api.lenso.ai/search`，Bearer Token，原生结果解析 | 开发者订阅 Token，覆盖 401/402/429 |
| TinEye | 自动 API / BYOK/收费 | `POST https://api.tineye.com/rest/search/`，`x-api-key`，1 MB 上传副本，原生结果解析 | 商业 API Key，覆盖 401/402/429 |
| Bing Visual Search | 网页辅助 | 打开消费级移动页面，不使用已退役 Bing Search API | 移动页面图片选择 |
| Yandex Images | 网页辅助 | 打开官网 | 移动页面图片选择 |
| 百度 / 搜狗 / 360 | 网页辅助 | 打开各自 HTTPS 官网 | 中国大陆网络与移动页面图片选择 |
| IQDB / Ascii2D | 网页辅助 | 打开动漫图片搜索官网 | 移动页面图片选择与结果跳转 |

通过标准：

1. 自动引擎必须真实接受图片并返回与夹具结构一致的结果；重定向只能保持 HTTPS 且经过域名验证。
2. 网页辅助必须在 Android Custom Tab 的移动页面中提供可用文件选择，并在打开前明确提示再次选择图片。
3. 401、402、403、429、超时、取消和解析失败均必须给出可行动的中文提示，不记录凭据或响应原文。
4. 验证码、永久失效、危险跳转或不再提供图片上传的引擎从内置目录移除，不以“官网能打开”代替验收。
