package com.nastv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.components.TVVideoCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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

    val columns = 5
    val spacing = 16.dp

    val rows by remember(videos, columns) {
        derivedStateOf { videos.chunked(columns) }
    }

    val listState = rememberLazyListState()

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
        TopAppBar(
            title = {
                Box {
                    TextButton(onClick = { sourceDropdownExpanded = true }) {
                        Text(selectedSource?.name ?: "选择源", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFD700))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select source", tint = Color(0xFFFFD700))
                    }
                    DropdownMenu(expanded = sourceDropdownExpanded, onDismissRequest = { sourceDropdownExpanded = false }) {
                        sources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    if (source.id != selectedSource?.id) { viewModel.selectSource(source) }
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            },
            actions = {
                Box(modifier = Modifier.padding(end = 40.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        TvIconButton(onClick = onNavigateToSearch, icon = Icons.Default.Search, contentDescription = "Search")
                        TvIconButton(onClick = onNavigateToFavorites, icon = Icons.Default.Favorite, contentDescription = "Favorites")
                        TvIconButton(onClick = onNavigateToHistory, icon = Icons.Default.History, contentDescription = "History")
                        TvIconButton(onClick = onNavigateToCache, icon = Icons.Default.Storage, contentDescription = "Cache")
                        TvIconButton(onClick = onNavigateToSettings, icon = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        )

        if (parentCategories.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TvFilterChip(selected = selectedParentCategory == null, onClick = { viewModel.selectParentCategory(null) }, label = "全部") }
                items(items = parentCategories) { category ->
                    TvFilterChip(selected = category == selectedParentCategory, onClick = { viewModel.selectParentCategory(category) }, label = category.name)
                }
            }
        }
        if (subCategories.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        .fillMaxWidth(),
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
                                    shouldAutoFocus = videoIndex == focusTargetIndex
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
private fun TvFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) Color(0xFFFFD700) else if (selected) Color(0xFFFF5722) else Color(0xFF666666)
    Box(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .background(color = if (selected) Color(0xFFFF5722).copy(alpha = 0.2f) else Color.Transparent, shape = RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(text = label, color = Color.White) }
}

@Composable
private fun TvIconButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = if (isFocused) Color(0xFFFFD700) else Color.Transparent
    Box(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) { Icon(imageVector = icon, contentDescription = contentDescription, tint = if (isFocused) Color.Black else Color.White) }
}
