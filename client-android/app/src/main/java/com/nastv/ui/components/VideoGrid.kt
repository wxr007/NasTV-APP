package com.nastv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    var lastTriggeredSize by remember { mutableIntStateOf(0) }

    // TV 模式使用 5 列，手机端使用 3 列
    val columns = if (isTv) 5 else 3
    val spacing = if (isTv) 16.dp else 8.dp

    fun checkAndLoadMore() {
        val totalItems = gridState.layoutInfo.totalItemsCount
        val lastVisibleIndex = gridState.firstVisibleItemIndex + gridState.layoutInfo.visibleItemsInfo.size - 1
        if (hasMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 6 && videos.size != lastTriggeredSize) {
            lastTriggeredSize = videos.size
            onLoadMore()
        }
    }

    // Trigger load more on scroll
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

    // Also check when videos list changes (after loading more data)
    LaunchedEffect(videos.size, hasMore, isLoadingMore) {
        delay(200) // Wait for layout to settle
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
        items(videos) { video ->
            if (isTv) {
                TVVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.sourceId, video.id) }
                )
            } else {
                VideoCard(
                    video = video,
                    onClick = { onVideoClick(video.sourceId, video.id) }
                )
            }
        }
    }
}

@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .graphicsLayer {
                shadowElevation = 4.dp.toPx()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
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
            video.remark?.let { remark ->
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
        }

        // Title
        Text(
            text = video.name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
