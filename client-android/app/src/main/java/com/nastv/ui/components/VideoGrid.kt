package com.nastv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nastv.data.model.Video
import com.nastv.utils.ImageUrlHelper
import kotlinx.coroutines.delay

@Composable
fun VideoGrid(
    videos: List<Video>,
    onVideoClick: (String, String) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    isTv: Boolean = false,
    modifier: Modifier = Modifier,
    savedScrollIndex: Int = 0,
    savedScrollOffset: Int = 0,
    onScrollStateChanged: (index: Int, offset: Int) -> Unit = { _, _ -> },
    sourceNameMap: Map<String, String> = emptyMap()
) {
    // TV 模式使用 5 列，手机端使用 3 列
    val columns = if (isTv) 5 else 3
    val spacing = if (isTv) 16.dp else 8.dp

    if (isTv) {
        TvModeGrid(
            videos = videos,
            onVideoClick = onVideoClick,
            onLoadMore = onLoadMore,
            isLoadingMore = isLoadingMore,
            hasMore = hasMore,
            columns = columns,
            spacing = spacing,
            modifier = modifier,
            savedScrollIndex = savedScrollIndex,
            savedScrollOffset = savedScrollOffset,
            onScrollStateChanged = onScrollStateChanged,
            sourceNameMap = sourceNameMap
        )
    } else {
        PhoneModeGrid(
            videos = videos,
            onVideoClick = onVideoClick,
            onLoadMore = onLoadMore,
            isLoadingMore = isLoadingMore,
            hasMore = hasMore,
            columns = columns,
            spacing = spacing,
            modifier = modifier,
            savedScrollIndex = savedScrollIndex,
            savedScrollOffset = savedScrollOffset,
            onScrollStateChanged = onScrollStateChanged,
            sourceNameMap = sourceNameMap
        )
    }
}

@Composable
private fun TvModeGrid(
    videos: List<Video>,
    onVideoClick: (String, String) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    savedScrollIndex: Int,
    savedScrollOffset: Int,
    onScrollStateChanged: (index: Int, offset: Int) -> Unit,
    sourceNameMap: Map<String, String> = emptyMap()
) {
    val listState = rememberLazyListState()

    // 将视频按行分组
    val rows by remember(videos, columns) {
        derivedStateOf { videos.chunked(columns) }
    }

    // 从播放页返回时，savedScrollIndex 是视频索引，转为行索引
    val savedRowIndex = if (savedScrollIndex > 0) savedScrollIndex / columns else 0

    // 保存滚动位置（保存为行索引）
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { rowIndex ->
            onScrollStateChanged(rowIndex * columns, 0)
        }
    }

    // 从播放页返回后恢复滚动位置
    LaunchedEffect(Unit) {
        if (savedRowIndex > 0 && rows.size > savedRowIndex) {
            delay(100)
            listState.scrollToItem(savedRowIndex)
            delay(300)
            listState.scrollToItem(savedRowIndex)
        }
    }

    // 加载更多检测（最后一行可见时触发）
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && !isLoadingMore && rows.isNotEmpty() && lastVisibleItem >= rows.size - 2
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            delay(200)
            onLoadMore()
        }
    }

    // 初始加载更多
    LaunchedEffect(rows.size, hasMore, isLoadingMore) {
        if (hasMore && !isLoadingMore && rows.isNotEmpty() && rows.size < 4) {
            delay(300)
            onLoadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing),
            contentPadding = PaddingValues(vertical = spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            itemsIndexed(
                items = rows,
                key = { rowIndex, _ -> "row_$rowIndex" }
            ) { rowIndex, rowVideos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowVideos.forEachIndexed { colIndex, video ->
                        val videoIndex = rowIndex * columns + colIndex
                        TVVideoCard(
                            video = video,
                            onClick = { onVideoClick(video.sourceId, video.id) },
                            modifier = Modifier.weight(1f),
                            shouldAutoFocus = savedScrollIndex > 0 && videoIndex == savedScrollIndex,
                            sourceName = sourceNameMap[video.sourceId]
                        )
                    }
                    // 填充剩余空间，保持最后一行的对齐
                    val remaining = columns - rowVideos.size
                    if (remaining > 0) {
                        Spacer(modifier = Modifier.weight(remaining.toFloat()))
                    }
                }
            }
        }

        // 底部加载指示器
        if (isLoadingMore) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PhoneModeGrid(
    videos: List<Video>,
    onVideoClick: (String, String) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    savedScrollIndex: Int,
    savedScrollOffset: Int,
    onScrollStateChanged: (index: Int, offset: Int) -> Unit,
    sourceNameMap: Map<String, String> = emptyMap()
) {
    val gridState = rememberLazyGridState()
    var lastTriggeredSize by remember { mutableIntStateOf(0) }

    // 保存滚动位置
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onScrollStateChanged(index, offset)
        }
    }

    // 在 composable 销毁时也保存一次位置（导航离开时）
    DisposableEffect(gridState) {
        onDispose {
            onScrollStateChanged(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        }
    }

    // 从播放页返回后恢复滚动位置和焦点
    val shouldRestore = savedScrollIndex > 0 && videos.size > savedScrollIndex
    var restoringFocus by remember { mutableStateOf(false) }
    LaunchedEffect(shouldRestore) {
        if (shouldRestore) {
            restoringFocus = true
            delay(100)
            gridState.scrollToItem(savedScrollIndex, savedScrollOffset)
            delay(300)
            gridState.scrollToItem(savedScrollIndex, savedScrollOffset)
            restoringFocus = false
        }
    }

    fun checkAndLoadMore() {
        val totalItems = gridState.layoutInfo.totalItemsCount
        val lastVisibleIndex = gridState.firstVisibleItemIndex + gridState.layoutInfo.visibleItemsInfo.size - 1
        if (hasMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 6 && videos.size != lastTriggeredSize) {
            lastTriggeredSize = videos.size
            onLoadMore()
        }
    }

    LaunchedEffect(gridState, hasMore, isLoadingMore) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.layoutInfo.visibleItemsInfo.size to gridState.layoutInfo.totalItemsCount
        }.collect { (firstVisibleAndVisibleSize, totalItems) ->
            val (firstVisibleIndex, visibleSize) = firstVisibleAndVisibleSize
            val lastVisibleIndex = firstVisibleIndex + visibleSize - 1
            if (hasMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 6 && videos.size != lastTriggeredSize) {
                lastTriggeredSize = videos.size
                onLoadMore()
            }
        }
    }

    LaunchedEffect(videos.size, hasMore, isLoadingMore) {
        delay(200)
        checkAndLoadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .padding(spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        contentPadding = PaddingValues(spacing)
    ) {
        itemsIndexed(videos) { index, video ->
            VideoCard(
                video = video,
                onClick = { onVideoClick(video.sourceId, video.id) },
                sourceName = sourceNameMap[video.sourceId]
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sourceName: String? = null
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // Poster (vertical, 2:3 aspect ratio)
            val imageUrl = if (ImageUrlHelper.isUseImageProxy()) {
                video.pic?.let { pic ->
                    val relativeUrl = "/api/play/image?url=${java.net.URLEncoder.encode(pic, "UTF-8")}&sourceId=${video.sourceId}&videoId=${video.id}"
                    ImageUrlHelper.toFullUrl(relativeUrl)
                }
            } else {
                video.pic
            }
            AsyncImage(
                model = imageUrl,
                contentDescription = video.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            // Remark badge at top-left
            video.remark?.takeIf { it.isNotBlank() }?.let { remark ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = remark,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            // Source name at bottom of image
            if (!sourceName.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = sourceName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Title
        Text(
            text = video.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
