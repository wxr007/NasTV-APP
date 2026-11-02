# NasTV

NasTV 是一个基于 Android 平台的视频聚合客户端，支持多种视频源，提供手机和 TV 双端界面。

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17 |
| Android SDK | compileSdk 34 / targetSdk 34 / minSdk 28 |
| Kotlin | 2.0.0 |
| Gradle | 8.11.1 |
| Android Gradle Plugin (AGP) | 8.5.0 |

## 构建工具

| 工具 | 版本 | 说明 |
|------|------|------|
| Kotlin Compose Plugin | 2.0.0 | Compose 编译器插件 |
| KSP | 2.0.0-1.0.22 | Kotlin 符号处理器（用于 Hilt、Moshi 代码生成） |
| Compose BOM | 2024.05.00 | Compose 物料清单（统一版本管理） |
| ProGuard | Android 默认 | 启用于 release 构建（`proguard-rules.pro`） |

## 项目模块结构

```
client-android/
├── app/          # 主应用模块
├── build.gradle.kts    # 项目级构建配置
├── settings.gradle.kts # 项目设置
└── gradle/             # Gradle Wrapper
```

## 第三方依赖

### AndroidX

| 依赖 | 版本 | 用途 |
|------|------|------|
| `core-ktx` | 1.13.1 | AndroidX 核心扩展 |
| `lifecycle-runtime-ktx` | 2.8.2 | Lifecycle 运行时 |
| `activity-compose` | 1.9.0 | Activity Compose 集成 |
| `navigation-compose` | 2.7.7 | 页面导航 |
| `lifecycle-viewmodel-compose` | 2.8.2 | ViewModel + Compose |
| `lifecycle-runtime-compose` | 2.8.2 | Lifecycle + Compose 运行时 |
| `datastore-preferences` | 1.1.1 | 键值对本地存储 |

### Compose

| 依赖 | 版本（BOM 管理） | 用途 |
|------|-----------------|------|
| `ui` | 2024.05.00 | UI 基础 |
| `ui-graphics` | 2024.05.00 | 图形绘制 |
| `ui-tooling-preview` | 2024.05.00 | 预览支持 |
| `material3` | 2024.05.00 | Material3 组件 |
| `material-icons-extended` | 2024.05.00 | 扩展图标库 |

### 依赖注入

| 依赖 | 版本 | 用途 |
|------|------|------|
| Hilt Android | 2.51.1 | 依赖注入框架 |
| Hilt Compiler (KSP) | 2.51.1 | Hilt 代码生成 |
| Hilt Navigation Compose | 1.2.0 | Hilt + Navigation 集成 |

### 网络

| 依赖 | 版本 | 用途 |
|------|------|------|
| Retrofit | 2.11.0 | HTTP 客户端 |
| Converter Moshi | 2.11.0 | JSON 序列化 |
| OkHttp | 4.12.0 | HTTP 底层引擎 |
| Logging Interceptor | 4.12.0 | 网络日志拦截器 |
| Moshi | 1.15.1 | JSON 解析 |
| Moshi Kotlin Codegen (KSP) | 1.15.1 | Moshi 代码生成 |

### 媒体播放

| 依赖 | 版本 | 用途 |
|------|------|------|
| Media3 ExoPlayer | 1.3.1 | 视频播放核心 |
| Media3 ExoPlayer HLS | 1.3.1 | HLS 流播放 |
| Media3 UI | 1.3.1 | 播放器 UI 组件 |

### 图片加载

| 依赖 | 版本 | 用途 |
|------|------|------|
| Coil Compose | 2.6.0 | Compose 图片加载 |
| AVIF Coder | 1.8.0 | AVIF 格式图片解码 |

### 协程

| 依赖 | 版本 | 用途 |
|------|------|------|
| kotlinx-coroutines-android | 1.8.1 | Android 协程调度 |
| kotlinx-coroutines-play-services | 1.8.1 | Google Play Services 协程集成 |

## ABI 支持

应用打包时包含以下 CPU 架构：

- `armeabi-v7a`
- `arm64-v8a`
- `x86_64`

## CI 构建（GitHub Actions）

`.github/workflows/build-apk.yml` 负责在云端编译 APK：

| 触发条件 | 行为 |
|----------|------|
| push 到 `master` / `main`、PR、手动运行 | 并行编译 release 与 debug，产物上传为 Actions Artifacts |
| 推送 `v*` 标签 | 额外创建 GitHub Release，附上 release 版全部 APK |

产物按 ABI 分包，命名形如 `app-arm64-v8a-release.apk`、`app-armeabi-v7a-release.apk`、`app-x86_64-release.apk`，在 Actions 运行页面底部的 Artifacts 区域下载。

## 签名

release 构建的签名由 `client-android/keystore.properties` 决定（该文件与 `keystore/` 目录均已加入 `.gitignore`，**不要提交**）：

```properties
storeFile=keystore/release.jks
storePassword=******
keyAlias=nastv
keyPassword=******
```

- 文件存在且四个字段齐全 → 使用正式 release 密钥签名
- 文件缺失 → 自动回退为 debug 签名（本地随手编译也能出包）

CI 端通过 Secrets 注入密钥，需在仓库 Settings → Secrets and variables → Actions 中配置：

| Secret | 说明 |
|--------|------|
| `ANDROID_KEYSTORE_BASE64` | `release.jks` 的 base64 内容（`base64 -w0 release.jks`） |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 |
| `ANDROID_KEY_PASSWORD` | 密钥密码（可留空，默认同 keystore 密码） |

未配置 `ANDROID_KEYSTORE_BASE64` 时 workflow 只打一条 warning 并回退 debug 签名，不会失败。签名密钥一经用于发布就**不可更换**（换了之后老用户无法覆盖升级），请务必备份 `release.jks` 与密码。

CI 每次构建都会在 `Verify APK signature` 步骤打印实际签名证书的 DN 与 SHA-256，可据此核对各次产物是否同一密钥。


