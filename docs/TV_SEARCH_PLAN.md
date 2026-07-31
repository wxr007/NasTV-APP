# NasTV TV 端首字母搜索方案（修订版 v2）

## 一、目标

参考 TV (com.fongmi.android.tv) 的搜索页面，在 NasTV TV 模式下实现搜索功能：
- TV 遥控器友好的 A-Z 字母键盘输入
- 热词推荐（输入为空时显示）
- 联想词推荐（输入字母后实时显示）
- 搜索历史记录
- 站源多选弹窗，可勾选通过哪几个站源搜索
- **搜索结果在新页面展示：左侧站源列表 + 右侧海报网格**（模仿 TV 项目 CollectActivity）

---

## 二、参考项目 TV (fongmi) 的结果页分析

### 2.1 CollectActivity 页面结构

```
┌──────────────────────────────────────────────────────┐
│  "搜索: {keyword}" 标题                               │
├──────────────────────────────────────────────────────┤
│  HorizontalGridView（横向站源标签）                    │
│  [全部] [站源A] [站源B] [站源C] [站源D] ...           │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ViewPager（按站源分页）                              │
│  ┌────────────────────────────────────────────────┐  │
│  │ CollectFragment (每个站源一页)                   │  │
│  │ ┌────┬────┬────┬────┬────┬────┐               │  │
│  │ │海报│海报│海报│海报│海报│海报│  ← 每行N列      │  │
│  │ │名称│名称│名称│名称│名称│名称│               │  │
│  │ ├────┼────┼────┼────┼────┼────┤               │  │
│  │ │海报│海报│海报│海报│海报│海报│               │  │
│  │ │名称│名称│名称│名称│名称│名称│               │  │
│  │ └────┴────┴────┴────┴────┴────┘               │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### 2.2 NasTV Compose 适配

将 TV 项目的 XML/ViewPager 方案转为 Compose 等价实现：

```
┌──────────────────────────────────────────────────────┐
│  "搜索: {keyword}" 标题                    [Back]     │
├──────────┬───────────────────────────────────────────┤
│ 站源列表  │ 右侧：选中站源的海报网格                      │
│ (LazyCol) │                                           │
│           │ ┌──────┬──────┬──────┬──────┬──────┐      │
│ ▸ 全部    │ │ 海报  │ 海报  │ 海报  │ 海报  │ 海报  │      │
│   站源A   │ │ 名称  │ 名称  │ 名称  │ 名称  │ 名称  │      │
│   站源B   │ ├──────┼──────┼──────┼──────┼──────┤      │
│   站源C   │ │ 海报  │ 海报  │ 海报  │ 海报  │ 海报  │      │
│           │ │ 名称  │ 名称  │ 名称  │ 名称  │ 名称  │      │
│           │ ├──────┼──────┼──────┼──────┼──────┤      │
│           │ │ 海报  │ 海报  │ 海报  │ 海报  │ 海报  │      │
│           │ │ 名称  │ 名称  │ 名称  │ 名称  │ 名称  │      │
│           │ └──────┴──────┴──────┴──────┴──────┘      │
└──────────┴───────────────────────────────────────────┘
```

- "全部"页 = 合并所有站源结果
- 点击某个站源名 → 切换右侧只显示该站源的结果
- 每个站源的搜索结果异步加载，有结果就显示

---

## 三、总体架构

### 3.1 页面流

```
SearchScreen.kt (入口分流)
├── TV 模式 → TvSearchContent.kt
│               ├── 键盘 + 热词/联想词（用户输入）
│               └── 点击搜索 → 导航到 TvSearchResultScreen.kt
│                                ├── 并发搜索各站源
│                                └── 左侧站源列表 + 右侧海报网格
│
└── Phone 模式 → 现有逻辑（不变）
```

### 3.2 文件架构

```
┌─────────────────────────────────────────────────┐
│  SearchScreen.kt (入口分流 isTv)                 │
├──────────────────────┬──────────────────────────┤
│ TvSearchContent.kt   │ Phone 现有逻辑（不变）     │
│ (TV 搜索输入页)       │                          │
│ ├ TvSearchKeyboard   │                          │
│ ├ 热词/联想词 FlowRow│                          │
│ ├ 搜索历史 FlowRow   │                          │
│ └ 站源选择按钮→弹窗   │                          │
├──────────────────────┤                          │
│ TvSearchResultScreen │                          │
│ (TV 搜索结果页)       │                          │
│ ├ 左侧站源列表        │                          │
│ └ 右侧海报网格        │                          │
├──────────────────────┴──────────────────────────┤
│ TvSearchViewModel.kt                            │
│ (共享：搜索输入 + 站源搜索 + 历史管理)            │
└─────────────────────────────────────────────────┘
```

---

## 四、两个页面的详细设计

### 4.1 搜索输入页：TvSearchContent.kt

```
┌────────────────────────────────────────────────────────────┐
│ 左列（~35%）                 │ 右列（~65%，可滚动）          │
│                              │                              │
│  [站源(5)] [API:腾讯▼] [搜索框_______]  │  [搜索历史] (有历史时显示)    │
│                              │  历史1  历史2  历史3         │
│  ┌─ 键盘 7列 ─────────────┐ │                              │
│  │ A  B  C  D  E  F  G   │ │  [热词] / [联想词]            │
│  │ H  I  J  K  L  M  N   │ │  热词1  热词2  热词3         │
│  │ O  P  Q  R  S  T  U   │ │  热词4  热词5  热词6         │
│  │ V  W  X  Y  Z  0  1   │ │  ...                         │
│  │ 2  3  4  5  6  7  8 9 │ │                              │
│  │ [清空]        [删除]   │ │                              │
│  └────────────────────────┘ │                              │
│                              │                              │
└────────────────────────────────────────────────────────────┘
```

功能：
- 键盘输入 → 更新搜索框文本
- 搜索框有内容 → 调用 iQiyi 联想 API，显示联想词
- 搜索框为空 → 显示 360kan 热词
- 点击联想词/热词 → 填入搜索框，并直接导航到结果页
- 键盘"删除"键只删光标前字符（不做退格导航）
- "清空"键清空搜索框
- 站源选择按钮 → 弹出 TvSourceSelectDialog
- API提供商按钮 → 弹出 TvApiProviderDialog（腾讯/爱奇艺+360kan）

### 4.2 搜索结果页：TvSearchResultScreen.kt

```
┌──────────────────────────────────────────────────────────┐
│  "搜索: {keyword}"                        [返回]          │
├─────────────┬────────────────────────────────────────────┤
│ 站源列表     │ 选中站源的海报网格（5列）                    │
│ (~20%宽度)  │                                            │
│             │ ┌──────┬──────┬──────┬──────┬──────┐       │
│ ▸ 全部(35)  │ │ 海报  │ 海报  │ 海报  │ 海报  │ 海报  │       │
│   站源A(12) │ │ 名称  │ 名称  │ 名称  │ 名称  │ 名称  │       │
│   站源B(8)  │ ├──────┼──────┼──────┼──────┼──────┤       │
│   站源C(0)  │ │ 海报  │ 海报  │ 海报  │ 海报  │ 海报  │       │
│   站源D(15) │ │ 名称  │ 名称  │ 名称  │ 名称  │ 名称  │       │
│             │ └──────┴──────┴──────┴──────┴──────┘       │
│             │                                            │
└─────────────┴────────────────────────────────────────────┘
```

功能：
- 进入页面即开始对已勾选的站源并发搜索
- 每个站源搜索完成 → 左侧对应站源显示结果数量
- 默认选"全部"：显示所有站源结果的合并视图
- 点击站源名 → 切换右侧仅显示该站源结果
- 结果数为0的站源灰显，不可点击
- 点击海报 → 跳转播放页
- Back/返回键 → 回到搜索输入页
- 支持加载更多（翻页）

焦点导航：
```
 ┌──焦点组1───┐    ┌───焦点组2────┐
 │ 站源列表     │    │ 海报网格      │
 │             │    │              │
 │ ▸ 全部      │ ←→ │ 组内移动      │
 │   站源A     │    │ 上=阻止      │
 │   站源B     │    │ 下=滚动      │
 │   站源C     │    │              │
 │             │    │              │
 │ 上下移动     │    │              │
 │ 右→焦点组2  │    │ 左→焦点组1   │
 └─────────────┘    └──────────────┘
```

---

## 五、搜索数据流（直连站源）

### 5.1 搜索流程

```
用户点击联想词/热词
  │
  ▼
导航到 TvSearchResultScreen(keyword)
  │
  ▼
init 中启动并发搜索:
  sourceRepository.getVideos(sourceId_A, keyword=keyword)  ──┐
  sourceRepository.getVideos(sourceId_B, keyword=keyword)  ──┤ coroutineScope
  sourceRepository.getVideos(sourceId_C, keyword=keyword)  ──┘ async 并发
  │
  ▼
每个结果返回 → 更新 sourceResults Map<sourceId, List<Video>>
  │
  ▼
UI: sourceResults 汇总 → 左侧计数 + 右侧网格
```

### 5.2 不再经过后端聚合

```
旧: 用户 → SearchApi.search(keyword) → 后端 → 合并 → 返回
新: 用户 → SourceApi.getVideos(sourceId, keyword) × N → 客户端合并
```

### 5.3 联想词/热词：可切换外部 API 提供商

提供 API 提供商选择弹窗，用户可在搜索框旁切换：

| 提供商 | 热词 API | 联想词 API |
|--------|---------|-----------|
| **腾讯（默认）** | `https://node.video.qq.com/x/api/hot_search` | `https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box?key={keyword}` |
| **爱奇艺+360kan** | `https://api.web.360kan.com/v1/rank?cat=1`（需 Referer 头） | `https://suggest.video.iqiyi.com/?if=mobile&key={keyword}` |

- 选择持久化到 DataStore
- 切换提供商后自动刷新热词
- A-Z 键盘输入 = 拼音直连 API，无需本地转换

---

## 六、站源多选弹窗

```
┌─────────────────────────────────┐
│  选择搜索站源                    │
│  [全选]  [取消全选]             │
│                                 │
│  ☑ 站源A        ☐ 站源E        │
│  ☑ 站源B        ☐ 站源F        │
│  ☐ 站源C        ☑ 站源G        │
│  ☐ 站源D        ☑ 站源H        │
│                                 │
│             [确定]              │
└─────────────────────────────────┘
```

- AlertDialog + LazyVerticalGrid（3~4列自适应）
- 每项：Checkbox + 站源名，带 tvFocusable() 焦点
- 勾选状态持久化 DataStore
- 至少勾选1个

---

## 七、实现步骤

### 步骤 1：新建 TvSearchKeyboard 组件

**文件：** `ui/components/TvSearchKeyboard.kt`

- 7列网格：A-Z + 0-9 = 36键
- 底部：[清空]（占4列）、[删除]（占3列）
- 每键 40dp×40dp，焦点金色边框
- 回调：`onKeyPress(key: String)`, `onClear()`, `onDelete()`
- 复用 `tvFocusable()` + `FocusRequester`

### 步骤 2：新建 TvSourceSelectDialog 组件

**文件：** `ui/components/TvSourceSelectDialog.kt`

- AlertDialog + LazyVerticalGrid
- 加载站源列表 + Checkbox 勾选
- 全选/取消全选
- 持久化 DataStore

### 步骤 3：新建 TvSearchContent（搜索输入页）

**文件：** `ui/screens/search/TvSearchContent.kt`

- 左侧：站源按钮 + 搜索框 + 键盘
- 右侧：历史 FlowRow + 热词/联想词 FlowRow
- 点击联想词/热词 → 填充搜索框 → 导航到结果页
- 焦点管理：键盘↔右侧跨区域导航

### 步骤 4：新建 TvSearchResultScreen（搜索结果页）

**文件：** `ui/screens/search/TvSearchResultScreen.kt`

- 顶部标题栏 + 返回按钮
- 左侧 LazyColumn 站源列表（带结果数量）
- 右侧 5列手动海报网格（选中站源的结果）
- "全部"：合并所有站源结果
- BackHandler：返回搜索输入页
- 加载更多（翻页）

### 步骤 5：新建 TvApiProviderDialog 组件

**文件：** `ui/components/TvApiProviderDialog.kt`

```
- AlertDialog + 2个 RadioButton 选项
- 腾讯（默认）：热词=腾讯热搜API + 联想=腾讯智能建议API
- 爱奇艺+360kan：热词=360kan排行榜 + 联想=爱奇艺suggest API
- 选择持久化 DataStore
- 切换后自动刷新热词
```

### 步骤 6：新建 TvSearchViewModel

**文件：** `ui/screens/search/TvSearchViewModel.kt`（共享 ViewModel，两个页面共用）

```kotlin
@HiltViewModel
class TvSearchViewModel @Inject constructor(
    private val sourceRepository: SourceRepository
) : ViewModel() {

    // === 输入页状态 ===
    val inputText = MutableStateFlow("")
    val hotWords = MutableStateFlow<List<String>>(emptyList())
    val suggestionWords = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = MutableStateFlow<List<String>>(emptyList())

    // === API 提供商 ===
    val apiProvider = MutableStateFlow(ApiProvider.TENCENT)  // 默认腾讯

    // === 站源选择 ===
    val allSources = MutableStateFlow<List<VideoSource>>(emptyList())
    val selectedSourceIds = MutableStateFlow<Set<String>>(emptySet())

    // === 结果页状态 ===
    val searchKeyword = MutableStateFlow("")
    val sourceResults = MutableStateFlow<Map<String, List<Video>>>(emptyMap())
    val selectedSourceId = MutableStateFlow("all")
    val isLoading = MutableStateFlow(false)
    val isSearching = MutableStateFlow(false)

    // 外部 API（根据 apiProvider 选择对应接口）
    fun loadHotWords()
    fun loadSuggestions(keyword: String)  // 带防抖
    fun setApiProvider(provider: ApiProvider)

    // 站源
    fun loadSources()
    fun toggleSource(sourceId: String)

    // 搜索
    fun search(keyword: String)
    fun searchMore(page: Int)

    // 历史
    fun addToHistory(keyword: String)
    fun clearHistory()
}

enum class ApiProvider { TENCENT, IQIYI_360 }
```

### 步骤 7：添加导航路由

**文件：** `ui/navigation/Screen.kt`（新增路由）

```kotlin
object TvSearchResult : Screen("tv_search_result/{keyword}") {
    fun createRoute(keyword: String) = "tv_search_result/$keyword"
}
```

**文件：** `ui/navigation/NavGraph.kt`（注册路由）

```kotlin
composable(Screen.TvSearchResult.route) { backStackEntry ->
    val keyword = backStackEntry.arguments?.getString("keyword") ?: ""
    TvSearchResultScreen(
        keyword = keyword,
        viewModel = hiltViewModel(),
        onNavigateToPlay = { ... },
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### 步骤 8：修改 SearchScreen.kt 入口

```kotlin
@Composable
fun SearchScreen(...) {
    if (LocalIsTv.current) {
        TvSearchContent(...)
    } else {
        // 现有 Phone 逻辑（不变）
    }
}
```

---

## 八、文件清单

| 文件 | 操作 | 用途 |
|------|------|------|
| `ui/components/TvSearchKeyboard.kt` | **新增** | 7列 A-Z 键盘 |
| `ui/components/TvSourceSelectDialog.kt` | **新增** | 站源多选弹窗 |
| `ui/components/TvApiProviderDialog.kt` | **新增** | API 提供商选择弹窗 |
| `ui/screens/search/TvSearchContent.kt` | **新增** | TV 搜索输入页 |
| `ui/screens/search/TvSearchResultScreen.kt` | **新增** | TV 搜索结果页（左站源+右海报） |
| `ui/screens/search/TvSearchViewModel.kt` | **新增** | TV 搜索 ViewModel（共享） |
| `ui/navigation/Screen.kt` | **修改** | 加 TvSearchResult 路由 |
| `ui/navigation/NavGraph.kt` | **修改** | 注册结果页路由 |
| `ui/screens/search/SearchScreen.kt` | **修改** | 入口分流 TV/Phone |

---

## 九、风险与注意事项

1. **外部 API 稳定性**：360kan/iQiyi 接口可能变更，做好异常处理（静默失败，不影响搜索流程）
2. **热词缓存**：首次加载后缓存 StateFlow，提供手动刷新
3. **并发搜索限流**：最多 5 个站源同时搜索，`coroutineScope` + `async` 控制
4. **键盘焦点约束**：最后一行按"下"阻止焦点跑出（`focusProperties { exit = FocusRequester.Cancel }`）
5. **搜索结果页返回**：BackHandler 回到搜索输入页，保留之前的输入状态（ViewModel 共享）
6. **结果数为0的站源**：左侧灰显，不可点击；"全部"页不包含
7. **Phone 搜索不受影响**：TV 和 Phone 使用独立 ViewModel
