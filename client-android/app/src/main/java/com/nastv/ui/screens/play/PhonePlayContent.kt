package com.nastv.ui.screens.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nastv.data.model.VideoDetail

/**
 * 手机端播放页布局（竖屏，触摸操作）
 */
@Composable
fun PhonePlayContent(
    detail: VideoDetail,
    exoPlayer: ExoPlayer,
    gestureModifier: Modifier,
    seekIndicatorText: String?,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    isSeeking: Boolean,
    seekPosition: Float,
    isFullscreen: Boolean,
    selectedLine: Int,
    currentEpisodeName: String?,
    isFavorite: Boolean,
    cachedEpisodeIndices: Set<Int>,
    isBuffering: Boolean,
    downloadSpeed: Long,
    onToggleFavorite: () -> Unit,
    onSelectLine: (Int) -> Unit,
    onPlayEpisode: (com.nastv.data.model.Episode) -> Unit,
    onToggleFullscreen: () -> Unit,
    onSeekFinished: (Long) -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onPlayPause: () -> Unit,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
) {
    var episodesExpanded by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(true) }
    var detailExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        item {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                setBackgroundColor(android.graphics.Color.BLACK)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(modifier = Modifier.fillMaxSize().then(gestureModifier))

                    seekIndicatorText?.let { text ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // 右上角始终显示下载速度
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = formatSpeed(downloadSpeed),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }

                    // 缓冲时显示加载动画
                    if (isBuffering) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    if (controlsVisible) {
                        IconButton(
                            onClick = { onToggleFullscreen() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "全屏",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Slider(
                                value = if (isSeeking) seekPosition
                                    else if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat()
                                    else 0f,
                                onValueChange = onSeekChange,
                                onValueChangeFinished = {
                                    onSeekStart()
                                    onSeekFinished((seekPosition * totalDuration).toLong())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                Text(text = formatTime(totalDuration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        // Content below player
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title row with favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detail.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onToggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Meta info row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = detail.playFrom.getOrNull(selectedLine) ?: "",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        detail.year?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                        detail.type?.let {
                            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50))
                        }
                    }
                    Text(
                        text = if (detailExpanded) "收起详情" else "展开详情",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { detailExpanded = !detailExpanded }
                    )
                }

                // Description
                detail.content?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (detailExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Line selector
                if (detail.playFrom.size > 1) {
                    Text(text = "播放线路", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = detail.playWithIndex) { (index, name) ->
                            FilterChip(
                                selected = index == selectedLine,
                                onClick = { onSelectLine(index) },
                                label = { Text(name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Episode section header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "选集", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.clickable { sortAscending = !sortAscending },
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (sortAscending) "正序" else "倒序",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "排序",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.clickable { episodesExpanded = !episodesExpanded },
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (episodesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (episodesExpanded) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (episodesExpanded) "收起" else "展开",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Episode grid
        item {
            val episodes = detail.episodes.getOrNull(selectedLine) ?: emptyList()
            val sortedEpisodes = if (sortAscending) episodes else episodes.reversed()
            if (sortedEpisodes.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(
                            min = if (episodesExpanded) 0.dp else 60.dp,
                            max = if (episodesExpanded) 300.dp else 60.dp
                        )
                ) {
                    itemsIndexed(items = sortedEpisodes) { index, episode ->
                        val isCurrentEpisode = episode.name == currentEpisodeName
                        val originalIndex = if (sortAscending) index else episodes.size - 1 - index
                        val isCached = originalIndex in cachedEpisodeIndices
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrentEpisode)
                                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isCurrentEpisode)
                                    androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4CAF50))
                                else
                                    null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onPlayEpisode(episode) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = episode.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCurrentEpisode) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // Cached indicator dot
                            if (isCached) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(8.dp)
                                        .background(
                                            color = Color(0xFFFFD700),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
