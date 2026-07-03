package com.nastv.ui.screens.cache

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.data.model.Video
import com.nastv.ui.components.TVVideoCard
import com.nastv.ui.components.VideoCard
import com.nastv.ui.theme.LocalIsTv

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CacheViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    val episodes by viewModel.episodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCachedEpisodes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已缓存") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            episodes.isEmpty() -> {
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTv) 5 else 3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(if (isTv) 16.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 24.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isTv) 24.dp else 8.dp)
                ) {
                    items(items = episodes, key = { "${it.sourceId}:${it.videoId}:${it.episodeIndex}" }) { episode ->
                        val video = Video(
                            id = episode.videoId,
                            name = episode.status?.title ?: "未命名",
                            pic = episode.status?.picUrl,
                            sourceId = episode.sourceId
                        )
                        // 给每个 item 添加足够的 padding，让缩放后的内容有空间显示
                        Box(
                            modifier = Modifier.padding(if (isTv) 24.dp else 0.dp)
                        ) {
                            if (isTv) {
                                TVVideoCard(
                                    video = video,
                                    onClick = { onNavigateToPlay(episode.sourceId, episode.videoId) }
                                )
                            } else {
                                VideoCard(
                                    video = video,
                                    onClick = { onNavigateToPlay(episode.sourceId, episode.videoId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
