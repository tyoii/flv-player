# FLV Player for Google Drive

📺 播放 Google Drive 上 bililive-go 直播录像的 Android 播放器

## 功能

- 🔐 Google 账号登录（仅需只读权限）
- 📁 浏览 Google Drive 文件夹
- 🔍 搜索 FLV 文件
- ▶️ FLV 视频流式播放（无需下载）
- ⏩ 倍速播放（0.5x ~ 3x）
- 📝 自动记忆播放进度
- 📜 播放历史记录
- 🎯 自动解析 bililive-go 文件名（主播名/标题/日期）
- 🌓 跟随系统深色/浅色主题

## 技术栈

- Kotlin + Jetpack Compose
- ExoPlayer (Media3) — FLV 流式播放
- Google Drive API v3
- Room Database — 播放历史
- Hilt — 依赖注入
- GitHub Actions — 自动编译 APK

## 安装

从 [Actions](../../actions) 页面下载最新的 `flv-player-debug.apk`。

## 构建

```bash
./gradlew assembleDebug
```

APK 输出: `app/build/outputs/apk/debug/app-debug.apk`

## 配置 Google OAuth

首次使用需要在 [Google Cloud Console](https://console.cloud.google.com/) 配置：

1. 创建项目
2. 启用 Google Drive API
3. 配置 OAuth 同意屏幕
4. 创建 Android OAuth Client ID（需要 SHA-1 签名）

## License

MIT
