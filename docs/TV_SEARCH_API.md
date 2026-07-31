# TV 端热词/联想词 API 文档

## 概述

TV 端搜索的热词和联想词功能直接调用外部第三方 API，不经过后端服务器。代码实现在 `TvSearchViewModel.kt` 中，使用原生 OkHttp 直接请求。

## API 提供商

系统支持两种 API 提供商组合，通过 `ApiProvider` 枚举切换：

| 枚举值 | 热词来源 | 联想词来源 |
|--------|---------|-----------|
| `TENCENT` | 腾讯热搜 | 腾讯智能建议 |
| `IQIYI_360`（默认） | 360kan 排行榜 | 爱奇艺 suggest |

## API 详情

### 1. 热词 API

#### 腾讯热搜

- **URL:** `https://node.video.qq.com/x/api/hot_search`
- **方法:** GET
- **请求头:** 无特殊要求
- **网终配置:** 连接超时 10s，读取超时 10s
- **响应解析:** JSON → `data[]` 或 `hotlist[]` → 取 `title` 或 `name` 字段

#### 360kan 排行榜

- **URL:** `https://api.web.360kan.com/v1/rank?cat=1`
- **方法:** GET
- **请求头:** **必须携带** `Referer: https://www.360kan.com/rank/general`
- **网络配置:** 连接超时 10s，读取超时 10s
- **响应解析:** 同上，JSON → `data[]` 或 `hotlist[]`

### 2. 联想词 API

调用时机：用户在搜索框输入文字后，**300ms 防抖**触发。

#### 腾讯智能建议

- **URL:** `https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box?format=json&page_num=0&page_size=20&key={keyword}`
- **方法:** GET
- **参数:** `keyword` 经过 `URLEncoder.encode(UTF-8)` 编码
- **网络配置:** 连接超时 10s，读取超时 10s
- **响应解析:** JSON → `data[]` → 取 `title` 或 `name`

#### 爱奇艺 suggest

- **URL:** `https://suggest.video.iqiyi.com/?if=mobile&key={keyword}`
- **方法:** GET
- **参数:** `keyword` 经过 `URLEncoder.encode(UTF-8)` 编码
- **网络配置:** 连接超时 10s，读取超时 10s
- **响应解析:** JSON → `data[]` 或整个 JSONArray → 取 `name` 或字符串值

## 网络客户端

```kotlin
private val externalClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()
```

- 直接使用 OkHttp，不经过 Retrofit
- 同步调用 `execute()`，运行在 `viewModelScope.launch` 协程中
- 所有异常静默捕获，不会崩溃

## 错误处理

所有 API 调用和 JSON 解析中的异常均**静默处理**（空 `catch` 块），不会显示错误提示。这意味着：

- 网络不通 → 无热词/联想词，无提示
- JSON 解析失败 → 返回空列表，无提示
- 超时 → 无提示

## 电视端无建议词排查指南

如果电视上不显示热词/联想词，但模拟器正常，按以下顺序排查：

### 1. 网络连通性

电视端能否访问外部 API：
```
node.video.qq.com           # 腾讯热词
api.web.360kan.com          # 360kan 热词
tv.aiseet.atianqi.com       # 腾讯联想词
suggest.video.iqiyi.com     # 爱奇艺联想词
```

可在电视上用浏览器或 `adb shell ping` 测试。

### 2. DNS 解析

电视的 DNS 可能与模拟器不同，导致域名无法解析。尝试：
- 电视 WiFi 设置中手动 DNS 改为 `114.114.114.114` 或 `8.8.8.8`
- 检查路由器是否有境外网站过滤

### 3. HTTPS/SSL 证书

电视系统时间不正确会导致 SSL 握手失败。检查：
- 电视日期时间是否为当前时间
- Android 系统证书存储是否正常

### 4. 360kan Referer 校验

360kan 热词接口要求 `Referer` 头，如果中间有代理删除了该头可能被拦截。

### 5. 加日志定位

在 `TvSearchViewModel.kt` 的 catch 块中添加日志：

```kotlin
// loadHotWords() 的 catch 块 (约第 180 行)
} catch (e: Exception) {
    Log.e("TvSearchVM", "loadHotWords failed: $e")  // 添加此行
}

// loadSuggestions() 的 catch 块 (约第 245 行)
} catch (e: Exception) {
    Log.e("TvSearchVM", "loadSuggestions failed: $e")  // 添加此行
}
```

然后通过 `adb logcat -s TvSearchVM` 查看具体错误信息。

### 6. 日志过滤命令

```bash
adb logcat -s TvSearchVM TvSearchResult
```

## 持久化

API 提供商选择通过 DataStore 持久化存储：

- Key: `search_api_provider`
- 值: `"tencent"` 或 `"iqiyi_360"`
- 默认: `"iqiyi_360"`（爱奇艺+360kan）
- 文件: `PreferencesRepository.kt`
