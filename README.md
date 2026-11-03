# Moe Memos

[![justforfunnoreally.dev badge](https://img.shields.io/badge/justforfunnoreally-dev-9ff)](https://justforfunnoreally.dev)

<img alt="Moe Memos" src="https://memos.moe/memos.png" width="160" height="160" />

**Moe Memos** is an app to help you capture thoughts and ideas.

You can use Moe Memos with either a self-hosted [✍️memos](https://github.com/usememos/memos) server or locally on your device (no server required).

**Note: Current Moe Memos version supports Memos 0.21.0, Memos 0.27.0 to 0.31.0, and Memos 26.10 to 26.11. Memos update may introduce breaking API changes. If you are using an unsupported version, it is recommended to use [Mortis](https://github.com/mudkipme/mortis) to convert the newer Memos API to the Memos 0.21.0 API and re-login in Moe Memos.**

## Installation

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/me.mudkip.moememos/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=me.mudkip.moememos)

Or download and install the APK package from the [Releases section](https://github.com/mudkipme/MoeMemosAndroid/releases/latest).

## Features

- Write memos like tweeting to yourself
- Use Moe Memos locally on your device (with backup and restore) or sync with your own ✍️memos server
- Offline-first experience with automatic sync when you are back online
- Material You design with dynamic themes and themed icon
- Rich memo content: Markdown editor and renderer, images, non-image attachments, and to-do items
- Organize and find memos with tags, pinning, and search
- Home screen widget and share sheet integration (text, images, and webpages)
- View your memo activity with a progress graph
- Full privacy protection, no data collection

Moe Memos is a third-party client for [✍️memos](https://github.com/usememos/memos) and both projects aren't affiliated with each other.

## 本仓库的定制改动

本仓库基于 [mudkipme/MoeMemosAndroid](https://github.com/mudkipme/MoeMemosAndroid)，是个人使用的修改版，不向上游提交。相对上游的改动：

### 界面
- Material 3 / Pixel 风格：圆角卡片、tonal 表面、统一字号层级与 16/24dp 圆角
- 顶栏局部磨砂（Haze backdrop blur），首页与抽屉内各页面统一；页面底保持不透明，避免转场透出旧内容
- 抽屉条目圆角、抽屉使用实色 tonal 背景，开合动画不再透明闪烁

### 交互与导航
- 统一抽屉条目的点击顺序（先导航、后关闭抽屉），修复 Settings/Resources 转场先停顿再跳的问题
- 抽屉入口的返回栈规范化：Resources、Settings、Explore、Archived 返回均回到首页

### 性能
- 首屏只读取 80 条缓存，其余分页在后台补齐（冷启动缓存读取约 1.7s → 0.4s）
- memo 与 resource 的 N+1 Room 查询改为一次关系查询
- 同步操作移到 `Dispatchers.IO`，避免与首屏滚动争抢主线程
- 列表预览改为轻量文本渲染，详情页仍使用完整 Markdown
- 启动时请求设备最高刷新率（120Hz）

### 构建
- 本地调试包使用标准 Android debug 签名；如需指定签名文件，放在不入库的 `local.properties` 中
- 支持 Memos 26.10–26.11（该兼容性来自上游）

## Development

Moe Memos tends to keep minimal and optimized for best native experience. The Android version is developed with Kotlin and Jetpack Compose.

Any contributions are greatly appreciated.

## License

The Android version of Moe Memos is under [GPLv3](LICENSE).
