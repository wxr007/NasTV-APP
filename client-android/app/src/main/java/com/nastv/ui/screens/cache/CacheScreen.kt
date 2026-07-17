package com.nastv.ui.screens.cache

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.data.model.Video
import com.nastv.ui.components.TVVideoCard
import com.nastv.ui.components.VideoCard
import com.nastv.ui.config.GridDefaults
import com.nastv.ui.theme.LocalIsTv
import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CacheViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sourceNameMap by viewModel.sourceNameMap.collectAsState()

    // TV端遥控器退出键返回
    BackHandler { onNavigateBack() }

    LaunchedEffect(Unit) {
        viewModel.loadCachedVideos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已缓存") },
                navigationIcon = {
                    if (!isTv) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
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
            videos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无缓存剧集")
                }
            }
            else -> {
                if (isTv) {
                    // TV 模式：使用 LazyColumn + Row 手动网格（解决焦点导航问题）
                    val columns = GridDefaults.TV_COLUMNS
                    val spacing = GridDefaults.TV_SPACING
                    val rows = videos.chunked(columns)
                    val listState = rememberLazyListState()

                    // 焦点恢复目标索引（-1 表示不需要恢复）
                    var focusTargetIndex by remember { mutableStateOf(-1) }
                    var isRestoringScroll by remember { mutableStateOf(false) }
                    var lastFocusedRow by remember { mutableIntStateOf(-1) }

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

                    // 焦点行居中滚动
                    LaunchedEffect(lastFocusedRow) {
                        if (lastFocusedRow >= 0) {
                            try {
                                if (lastFocusedRow == 0) {
                                    listState.animateScrollToItem(0)
                                } else {
                                    delay(100)
                                    val info = listState.layoutInfo
                                    val item = info.visibleItemsInfo.find { it.index == lastFocusedRow }
                                    if (item != null) {
                                        val vpHeight = info.viewportEndOffset - info.viewportStartOffset
                                        val itemTopInVp = item.offset - info.viewportStartOffset
                                        val targetTop = (vpHeight - item.size) / 2f
                                        val delta = itemTopInVp - targetTop
                                        if (abs(delta) > 10f) {
                                            listState.animateScrollBy(delta)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CacheScreen", "居中异常", e)
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        itemsIndexed(
                            items = rows,
                            key = { rowIndex, _ -> "cache_row_$rowIndex" }
                        ) { rowIndex, rowVideos ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowVideos.forEachIndexed { colIndex, cachedVideo ->
                                    val videoIndex = rowIndex * columns + colIndex
                                    val video = Video(
                                        id = cachedVideo.videoId,
                                        name = cachedVideo.title,
                                        pic = cachedVideo.picUrl,
                                        sourceId = cachedVideo.sourceId
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        TVVideoCard(
                                            video = video,
                                            onClick = {
                                                viewModel.saveScrollState(videoIndex)
                                                onNavigateToPlay(cachedVideo.sourceId, cachedVideo.videoId)
                                            },
                                            shouldAutoFocus = videoIndex == focusTargetIndex,
                                            sourceName = sourceNameMap[cachedVideo.sourceId],
                                            onFocusGained = { if (rowIndex != lastFocusedRow) { lastFocusedRow = rowIndex } }
                                        )
                                        // 左上角缓存集数角标
                                        if (cachedVideo.cachedCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xCCFF0000))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${cachedVideo.cachedCount}集",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                // 填充剩余空间
                                val remaining = columns - rowVideos.size
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
                        items(items = videos, key = { "${it.sourceId}:${it.videoId}" }) { cachedVideo ->
                            val video = Video(
                                id = cachedVideo.videoId,
                                name = cachedVideo.title,
                                pic = cachedVideo.picUrl,
                                sourceId = cachedVideo.sourceId
                            )
                            Box {
                                VideoCard(
                                    video = video,
                                    onClick = { onNavigateToPlay(cachedVideo.sourceId, cachedVideo.videoId) },
                                    sourceName = sourceNameMap[cachedVideo.sourceId]
                                )
                                // 左上角缓存集数角标
                                if (cachedVideo.cachedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xCCFF0000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${cachedVideo.cachedCount}集",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
