# TV 端焦点导航问题解决记录

## 问题描述

TV 端首页海报墙使用遥控器方向键导航时存在以下问题：

1. **上下导航错位**：按"下"键选中了下一排的第一个/最后一个海报，而不是正下方的海报
2. **焦点跳跃**：滚动时焦点突然向上跳几排
3. **焦点回到第一排**：按"下"键时焦点总是回到第一排第二个位置
4. **海报重叠**：某些布局方案下海报叠在一起

## 环境

- Compose BOM: 2024.05.00（Compose UI ~1.6.7）
- 目标设备：Android TV（遥控器 D-pad 导航）
- 关键 API：`LazyVerticalGrid`、`LazyColumn`、`Column(verticalScroll)`、`focusProperties`、`FocusRequester`、`onKeyEvent`

## 探索过程

### 方案一：LazyVerticalGrid（初始方案）

TV 和手机共用 `VideoGrid`，内部使用 `LazyVerticalGrid` + `TVVideoCard`。

**结果**：布局正常，但焦点导航不准——按"下"总是跳到下一行的第一个或最后一个。

**原因**：`LazyVerticalGrid` 的虚拟化布局导致焦点系统的"最近邻"算法计算偏差。海报固定宽度 `180.dp` 与网格实际列宽不一致，焦点边界错位。

### 方案二：Column(verticalScroll) + Row 手动网格

用 `Column(verticalScroll)` 替代 `LazyVerticalGrid`，每行用 `Row` 包裹，海报用 `weight(1f)` 均分宽度。

```kotlin
Column(verticalScroll) {
    videos.chunked(5).forEach { row ->
        Row {
            row.forEach { TVVideoCard(Modifier.weight(1f)) }
        }
    }
}
```

**结果**：焦点比之前好很多，但"有时候向下翻的时候会突然向上跳几排"。

**原因**：`verticalScroll` 有内置的焦点自动滚动功能。当焦点移动到屏幕边缘时，`verticalScroll` 自动滚动，改变了所有项的位置，焦点系统重新计算"最近邻"时选错了目标。

### 方案三：LazyColumn + Row

```kotlin
LazyColumn {
    itemsIndexed(rows) { rowIndex, rowVideos ->
        Row { items }
    }
}
```

**结果**：布局正常，但焦点偶尔"向上跳跃"。

### 方案四：独立 TVHomeScreen + FocusRequester 显式绑定

创建独立的 `TVHomeScreen.kt`，为每个海报创建 `FocusRequester`，用 `focusProperties` 显式绑定上下左右的目标。

```kotlin
val requesters = remember(videos.size) { List(videos.size) { FocusRequester() } }

TVVideoCard(
    modifier = Modifier
        .focusRequester(requesters[index])
        .focusProperties {
            up = requesters[index - columns]
            down = requesters[index + columns]
            left = requesters[index - 1]
            right = requesters[index + 1]
        }
)
```

**结果**：焦点总是回到"第一排第二个"。`requesters` 列表随 `videos.size` 变化而重建，旧 requester 失效，绑定关系断裂。

### 方案五：去掉 verticalScroll（纯 Column）

去掉所有滚动修饰符，用纯 `Column` 布局。

**结果**：焦点导航正常（不再跳第一排），但海报重叠（无高度约束），无法滚动。

### 方案六：scrollable() + offset() 手动滚动

用底层 `scrollable()`（无焦点自动滚动）替代 `verticalScroll`，用 `offset()` 实现视觉滚动，用 `clipToBounds()` 裁剪溢出。

```kotlin
Box(clipToBounds, scrollable) {
    Column(offset = -scrollOffset) { rows }
}
```

**结果**：焦点正常，但 `scrollable()` 只响应触摸手势，不响应遥控器方向键。添加 `onKeyEvent` 处理 D-pad 滚动后布局仍异常（海报重叠）。

### 方案七：Column(verticalScroll) + onKeyEvent + moveFocus()

回到 `Column(weight(1f).verticalScroll)`，每行用测量到的固定高度，添加 `onKeyEvent` 拦截 D-pad 事件并调用 `focusManager.moveFocus()`。

```kotlin
TVVideoCard(
    modifier = Modifier
        .onKeyEvent {
            Key.DirectionDown -> { focusManager.moveFocus(Down); true }
            ...
        }
)
```

**结果**：又回到"第一排第二个"问题。`moveFocus()` 的焦点计算与自然 D-pad 导航不同。

### 方案八：LazyColumn + restoredOnce 标记（最终方案）

回到 `LazyColumn`，但添加 `restoredOnce` 标记防止 `shouldAutoFocus` 在正常滚动时干扰焦点。

```kotlin
var restoredOnce by remember { mutableStateOf(false) }

// 首次恢复完成后标记
LaunchedEffect(Unit) {
    // ...恢复滚动位置...
    restoredOnce = true
}

// 只有首次恢复时激活 shouldAutoFocus
TVVideoCard(
    shouldAutoFocus = !restoredOnce && ...
)
```

**结果**：✅ 焦点导航正常，布局正常，滚动正常。

## 最终方案详解

### 核心思路

1. **LazyColumn** 处理布局和滚动，它的焦点自动滚动比 `verticalScroll` 更可靠
2. **无任何额外焦点拦截**：没有 `onKeyEvent`、没有 `focusManager.moveFocus()`、没有显式 `FocusRequester` 绑定
3. **restoredOnce 标记**：防止 `shouldAutoFocus`（`TVVideoCard` 内部 `LaunchedEffect` 调用 `FocusRequester.requestFocus()`）在 LazyColumn 重新组合项目时意外偷走焦点

### 架构变更

- **TVHomeScreen.kt**（新建）：独立的 TV 端首页，与手机端完全分离
- **NavGraph.kt**：根据 `LocalIsTv.current` 分发到 `TVHomeScreen` 或 `HomeScreen`
- **VideoGrid.kt**：简化为仅手机端使用
- **TVVideoCard.kt**：`width(180.dp)` → `fillMaxWidth()`

### 关键教训

1. **不要混用滚动和焦点拦截**：`verticalScroll` 和 `onKeyEvent` + `moveFocus()` 组合会产生不可预测的焦点行为
2. **LazyColumn 的焦点自动滚动比 verticalScroll 更可靠**：LazyColumn 作为专为懒加载设计的组件，对焦点导航的支持更完善
3. **FocusRequester 在动态列表中不可靠**：`remember(videos.size)` 重建列表时所有引用断裂
4. **`shouldAutoFocus` 会在正常滚动时偷走焦点**：需要防止它在正常导航时触发
5. **TV 和手机代码分离是必要的**：共享组件时 TV 的焦点需求会与手机的触摸交互产生冲突
