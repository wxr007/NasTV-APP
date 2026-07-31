package com.nastv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.components.TVVideoCard
import com.nastv.ui.config.GridDefaults
import com.nastv.ui.theme.NasTVAccent
import com.nastv.ui.theme.NasTVPrimaryGold
import com.nastv.ui.theme.NasTVSurface
import com.nastv.ui.theme.NasTVSurfaceLight
import com.nastv.ui.theme.NasTVTextSecondary
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TVHomeScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCache: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val sources by viewModel.sources.collectAsState()
    val parentCategories by viewModel.parentCategories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val selectedParentCategory by viewModel.selectedParentCategory.collectAsState()
    val selectedSubCategory by viewModel.selectedSubCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    // 为每个分类项创建 FocusRequester，用于返回时聚焦到当前选中的分类
    val categoryFocusRequesters = remember(parentCategories.size) {
        List(parentCategories.size + 1) { FocusRequester() } // +1 for "全部"
    }
    var isGridFocused by remember { mutableStateOf(false) }
    var lastFocusedRow by remember { mutableIntStateOf(-1) }

    // 当焦点在海报墙上时，按返回键先移动焦点到当前选中的类型
    BackHandler(enabled = isGridFocused) {
        isGridFocused = false
        // 找到当前选中的分类索引
        val selectedIndex = if (selectedParentCategory == null) {
            0 // "全部"
        } else {
            parentCategories.indexOf(selectedParentCategory) + 1 // +1 因为"全部"在索引0
        }
        if (selectedIndex in categoryFocusRequesters.indices) {
            categoryFocusRequesters[selectedIndex].requestFocus()
        }
    }

    // 再按一次退出程序
    val context = LocalContext.current
    var lastBackPressedTime by remember { mutableStateOf(0L) }
    var exitToast by remember { mutableStateOf<Toast?>(null) }
    BackHandler(enabled = !isGridFocused) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressedTime < 2000) {
            // 2秒内再次按下，退出应用
            exitToast?.cancel()
            val activity = context as? android.app.Activity
            activity?.finishAffinity()
        } else {
            // 第一次按下，显示提示
            lastBackPressedTime = currentTime
            exitToast?.cancel()
            exitToast = Toast.makeText(context, "再按一次退出程序", Toast.LENGTH_SHORT).also { it.show() }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadSources() }
    LaunchedEffect(selectedSource) {
        if (selectedSource != null) viewModel.loadCategories(selectedSource!!.id)
    }
    LaunchedEffect(selectedSource, selectedParentCategory, selectedSubCategory) {
        if (selectedSource != null) {
            val categoryId = selectedSubCategory?.id ?: selectedParentCategory?.id
            viewModel.loadVideos(selectedSource!!.id, categoryId)
        }
    }

    val columns = GridDefaults.TV_COLUMNS
    val spacing = GridDefaults.TV_SPACING

    val rows by remember(videos, columns) {
        derivedStateOf { videos.chunked(columns) }
    }

    val listState = rememberLazyListState()

    // 焦点行居中滚动（在 LaunchedEffect 中执行，避免组合阶段的 MutationInterruptedException）
    LaunchedEffect(lastFocusedRow) {
        if (lastFocusedRow >= 0) {
            try {
                if (lastFocusedRow == 0) {
                    // 首行：滚动回顶部
                    listState.animateScrollToItem(0)
                } else {
                    // 等待布局稳定后直接计算居中偏移
                    delay(100)
                    val info = listState.layoutInfo
                    val item = info.visibleItemsInfo.find { it.index == lastFocusedRow }
                    if (item != null) {
                        val vpHeight = info.viewportEndOffset - info.viewportStartOffset
                        val itemTopInVp = item.offset - info.viewportStartOffset
                        val targetTop = (vpHeight - item.size) / 2f
                        val delta = itemTopInVp - targetTop
                        Log.d("TVHomeScreen", "居中: row=$lastFocusedRow vp=$vpHeight itemH=${item.size} topInVp=$itemTopInVp targetTop=$targetTop delta=$delta")
                        if (kotlin.math.abs(delta) > 10f) {
                            listState.animateScrollBy(delta)
                            Log.d("TVHomeScreen", "animateScrollBy完成: delta=$delta")
                        }
                    } else {
                        Log.d("TVHomeScreen", "居中失败: row=$lastFocusedRow 不在可见范围")
                    }
                }
            } catch (e: Exception) {
                Log.e("TVHomeScreen", "居中异常: row=$lastFocusedRow", e)
            }
        }
    }

    // 焦点恢复目标索引（-1 表示不需要恢复）。滚动完成后才设置，确保卡片已渲染
    var focusTargetIndex by remember { mutableStateOf(-1) }
    // 恢复滚动时禁止更新 savedScrollIndex
    var isRestoringScroll by remember { mutableStateOf(false) }

    // 保存滚动位置（恢复期间跳过）
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { rowIndex ->
            if (!isRestoringScroll) {
                viewModel.saveScrollState(rowIndex * columns, 0)
            }
        }
    }

    // 切换站源或分类时回到顶部
    LaunchedEffect(selectedSource?.id, selectedParentCategory?.id, selectedSubCategory?.id) {
        listState.scrollToItem(0)
        focusTargetIndex = -1
        lastFocusedRow = -1
    }

    // 从播放页返回后：先滚动到目标行，再触发焦点恢复
    val savedRowIndex = viewModel.savedScrollIndex / columns
    val savedIndex = viewModel.savedScrollIndex
    LaunchedEffect(Unit) {
        if (savedRowIndex > 0 && rows.size > savedRowIndex) {
            isRestoringScroll = true
            listState.scrollToItem(savedRowIndex)
            isRestoringScroll = false
            // 使用保存的原始索引，而不是被 snapshotFlow 覆盖的值
            focusTargetIndex = savedIndex
        }
    }

    // 加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && !isLoadingMore && rows.isNotEmpty() && lastVisible >= rows.size - 2
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) { delay(200); viewModel.loadMoreVideos() }
    }
    LaunchedEffect(videos.size, hasMore, isLoadingMore) {
        if (hasMore && !isLoadingMore && videos.isNotEmpty() && videos.size < 20) {
            delay(300); viewModel.loadMoreVideos()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 自定义顶栏：与海报墙对齐（水平 padding = spacing）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = spacing, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvSourceButton(
                sourceName = selectedSource?.name ?: "选择源",
                onClick = { sourceDropdownExpanded = true }
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                TvIconButton(onClick = onNavigateToSearch, icon = Icons.Default.Search, contentDescription = "Search")
                TvIconButton(onClick = onNavigateToFavorites, icon = Icons.Default.Favorite, contentDescription = "Favorites")
                TvIconButton(onClick = onNavigateToHistory, icon = Icons.Default.History, contentDescription = "History")
                TvIconButton(onClick = onNavigateToCache, icon = Icons.Default.Storage, contentDescription = "Cache")
                TvIconButton(onClick = onNavigateToSettings, icon = Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        // 站源选择弹窗
        if (sourceDropdownExpanded) {
            AlertDialog(
                onDismissRequest = { sourceDropdownExpanded = false },
                containerColor = NasTVSurface,
                titleContentColor = Color.White,
                title = {
                    Text(
                        "选择站源",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sources) { source ->
                            val isSelected = source.id == selectedSource?.id
                            var isFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .clickable {
                                        if (source.id != selectedSource?.id) {
                                            viewModel.selectSource(source)
                                        }
                                        sourceDropdownExpanded = false
                                    }
                                    .background(
                                        if (isSelected) NasTVAccent
                                        else NasTVSurfaceLight,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(2.dp, if (isFocused) NasTVPrimaryGold else if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val nameColor = if (isSelected) Color.White else NasTVTextSecondary
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (source.frontendProxy) Icons.Default.PhoneAndroid else Icons.Default.Storage,
                                        contentDescription = if (source.frontendProxy) "前端代理" else "后端代理",
                                        tint = nameColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = source.name,
                                        color = nameColor,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = { }
            )
        }

        if (parentCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TvFilterChip(
                        selected = selectedParentCategory == null,
                        onClick = { viewModel.selectParentCategory(null) },
                        label = "更新",
                        modifier = Modifier.focusRequester(categoryFocusRequesters[0])
                    )
                }
                itemsIndexed(items = parentCategories) { index, category ->
                    TvFilterChip(
                        selected = category == selectedParentCategory,
                        onClick = { viewModel.selectParentCategory(category) },
                        label = category.name,
                        modifier = Modifier.focusRequester(categoryFocusRequesters[index + 1])
                    )
                }
            }
        }
        if (subCategories.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = spacing, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TvFilterChip(selected = selectedSubCategory == null, onClick = { viewModel.selectSubCategory(null) }, label = "全部") }
                items(items = subCategories) { category ->
                    TvFilterChip(selected = category == selectedSubCategory, onClick = { viewModel.selectSubCategory(category) }, label = category.name)
                }
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onFocusChanged { isGridFocused = it.isFocused || it.hasFocus },
                    contentPadding = PaddingValues(horizontal = spacing, vertical = spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    itemsIndexed(
                        items = rows,
                        key = { rowIndex, _ -> "tv_row_$rowIndex" }
                    ) { rowIndex, rowVideos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            rowVideos.forEachIndexed { colIndex, video ->
                                val videoIndex = rowIndex * columns + colIndex
                                TVVideoCard(
                                    video = video,
                                    onClick = {
                                        // 点击时立即保存准确的焦点索引
                                        viewModel.saveScrollState(videoIndex, 0)
                                        onNavigateToPlay(video.sourceId, video.id)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shouldAutoFocus = videoIndex == focusTargetIndex,
                                    onFocusGained = {
                                        if (rowIndex != lastFocusedRow) {
                                            lastFocusedRow = rowIndex
                                        }
                                    }
                                )
                            }
                            val remaining = columns - rowVideos.size
                            if (remaining > 0) {
                                Spacer(modifier = Modifier.weight(remaining.toFloat()))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvFilterChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) NasTVPrimaryGold else if (selected) Color.White else NasTVTextSecondary
    Box(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .background(color = if (selected) NasTVAccent else Color.Transparent, shape = RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(text = label, color = Color.White) }
}

@Composable
private fun TvSourceButton(sourceName: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                color = if (isFocused) NasTVPrimaryGold.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .border(2.dp, if (isFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sourceName,
                style = MaterialTheme.typography.titleLarge,
                color = NasTVPrimaryGold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select source",
                tint = NasTVPrimaryGold
            )
        }
    }
}

@Composable
private fun TvIconButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = if (isFocused) NasTVPrimaryGold else Color.Transparent
    Box(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) { Icon(imageVector = icon, contentDescription = contentDescription, tint = if (isFocused) Color.Black else Color.White) }
}
