package com.nastv.ui.screens.cache

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCachedVideos()
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTv) 5 else 3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(if (isTv) 16.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 16.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isTv) 16.dp else 8.dp),
                    contentPadding = if (isTv) PaddingValues(16.dp) else PaddingValues(0.dp)
                ) {
                    items(items = videos, key = { "${it.sourceId}:${it.videoId}" }) { cachedVideo ->
                        val video = Video(
                            id = cachedVideo.videoId,
                            name = cachedVideo.title,
                            pic = cachedVideo.picUrl,
                            sourceId = cachedVideo.sourceId
                        )
                        Box {
                            if (isTv) {
                                TVVideoCard(
                                    video = video,
                                    onClick = { onNavigateToPlay(cachedVideo.sourceId, cachedVideo.videoId) }
                                )
                            } else {
                                VideoCard(
                                    video = video,
                                    onClick = { onNavigateToPlay(cachedVideo.sourceId, cachedVideo.videoId) }
                                )
                            }
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
                                        fontSize = if (isTv) 14.sp else 12.sp,
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
