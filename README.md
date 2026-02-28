# FLV Player for Google Drive

📺 播放 Google Drive 上 FLV (及其他格式) 视频的 Android 流媒体播放器

## 功能

- 🔐 Google 账号登录（仅需 Google Drive 只读权限）
- 📁 浏览 Google Drive 文件夹
- 🔍 搜索网盘内文件
- ▶️ 流式播放网盘视频（无需下载，支持 FLV, MP4, HLS 等）
- ⏩ 倍速播放（0.5x ~ 3x）
- 📝 自动记忆播放进度
- 📜 播放历史记录
- 🎯 特别针对含有 `[主播名][标题][日期]` 格式文件名的解析优化
- 🌓 跟随系统深色/浅色主题

## 技术栈

- Kotlin + Jetpack Compose
- ExoPlayer (Media3) — 视频流式播放
- Google Drive API v3
- Room Database — 本地历史记录存储
- Hilt — 依赖注入
- GitHub Actions — 自动化 CI/CD 构建

## 下载安装

每次更新会自动在 [Actions](../../actions) 页面生成并存放最新的 APK。
点击最近一次成功的 **Build APK** 工作流，滑动到页面底部的 **Artifacts**，下载 `flv-player-debug.zip` 并解压得到 APK 文件进行安装。

## 开发与构建

```bash
./gradlew assembleDebug
```

编译输出目录: `app/build/outputs/apk/debug/app-debug.apk`

## 配置 Google OAuth

由于涉及读取 Google Drive，需要开发者提供 Google Cloud 的 OAuth 配置信息。如果你是拉取源码自己编译，请在 [Google Cloud Console](https://console.cloud.google.com/) 进行如下配置：

1. 创建一个新项目
2. 启用 **Google Drive API**
3. 配置 OAuth 同意屏幕
4. 创建 **Android OAuth Client ID**（需填入你签名的 SHA-1 和包名 `com.tyoii.flvplayer`）

## License

MIT
