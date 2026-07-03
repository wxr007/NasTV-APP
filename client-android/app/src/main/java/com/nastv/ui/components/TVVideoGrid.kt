package com.nastv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nastv.data.model.Video
import kotlinx.coroutines.delay

@Composable
fun TVVideoGrid(
    videos: List<Video>,
    onVideoClick: (String, String) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    
    // Trigger load more on scroll
    LaunchedEffect(gridState, hasMore, isLoadingMore) {
        snapshotFlow {
            Triple(
                gridState.firstVisibleItemIndex,
                gridState.layoutInfo.visibleItemsInfo.size,
                gridState.layoutInfo.totalItemsCount
            )
        }.collect { (firstVisibleIndex, visibleSize, totalItems) ->
            val lastVisibleIndex = firstVisibleIndex + visibleSize - 1
            if (hasMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 6) {
                onLoadMore()
            }
        }
    }

    // Also check when videos list changes (after loading more data)
    LaunchedEffect(videos.size, hasMore, isLoadingMore) {
        delay(200) // Wait for layout to settle
        val totalItems = gridState.layoutInfo.totalItemsCount
        val lastVisibleIndex = gridState.firstVisibleItemIndex + gridState.layoutInfo.visibleItemsInfo.size - 1
        if (hasMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 6) {
            onLoadMore()
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(5), // TV 模式 5 列
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), // TV 模式间距 16dp
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(videos) { video ->
            TVVideoCard(
                video = video,
                onClick = { onVideoClick(video.sourceId, video.id) }
            )
        }
    }
}
