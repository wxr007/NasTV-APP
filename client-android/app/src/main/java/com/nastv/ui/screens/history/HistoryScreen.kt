package com.nastv.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.components.TVVideoCard
import com.nastv.ui.components.VideoCard
import com.nastv.ui.theme.LocalIsTv
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("观看历史") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            history.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无观看记录")
                }
            }
            else -> {
                if (isTv) {
                    // TV 模式：使用 LazyColumn + Row 手动网格（解决焦点导航问题）
                    val columns = 5
                    val spacing = 16.dp
                    val rows = history.chunked(columns)
                    val listState = rememberLazyListState()

                    // 焦点恢复目标索引（-1 表示不需要恢复）
                    var focusTargetIndex by remember { mutableStateOf(-1) }
                    var isRestoringScroll by remember { mutableStateOf(false) }

                    // 保存滚动位置（恢复期间跳过）
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.firstVisibleItemIndex }.collect { rowIndex ->
                            if (!isRestoringScroll) {
                                viewModel.saveScrollState(rowIndex * columns)
                            }
                        }
                    }

                    // 从播放页返回后：先滚动到目标行，再触发焦点恢复
                    val savedRowIndex = viewModel.savedScrollIndex / columns
                    val savedIndex = viewModel.savedScrollIndex
                    LaunchedEffect(Unit) {
                        if (savedRowIndex > 0 && rows.size > savedRowIndex) {
                            isRestoringScroll = true
                            delay(100)
                            listState.scrollToItem(savedRowIndex)
                            delay(300)
                            listState.scrollToItem(savedRowIndex)
                            delay(200)
                            isRestoringScroll = false
                            focusTargetIndex = savedIndex
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        itemsIndexed(
                            items = rows,
                            key = { rowIndex, _ -> "history_row_$rowIndex" }
                        ) { rowIndex, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowItems.forEachIndexed { colIndex, item ->
                                    val videoIndex = rowIndex * columns + colIndex
                                    val video = com.nastv.data.model.Video(
                                        id = item.videoId,
                                        name = item.name,
                                        pic = item.pic,
                                        remark = item.episode,
                                        sourceId = item.sourceId
                                    )
                                    TVVideoCard(
                                        video = video,
                                        onClick = {
                                            viewModel.saveScrollState(videoIndex)
                                            onNavigateToPlay(item.sourceId, item.videoId)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shouldAutoFocus = videoIndex == focusTargetIndex
                                    )
                                }
                                // 填充剩余空间
                                val remaining = columns - rowItems.size
                                if (remaining > 0) {
                                    repeat(remaining) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 手机模式：使用三列网格
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = history) { item ->
                            val video = com.nastv.data.model.Video(
                                id = item.videoId,
                                name = item.name,
                                pic = item.pic,
                                remark = item.episode,
                                sourceId = item.sourceId
                            )
                            VideoCard(
                                video = video,
                                onClick = { onNavigateToPlay(item.sourceId, item.videoId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
