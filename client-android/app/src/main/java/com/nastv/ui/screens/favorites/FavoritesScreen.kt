package com.nastv.ui.screens.favorites

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
import com.nastv.ui.components.TVVideoCard
import com.nastv.ui.components.VideoCard
import com.nastv.ui.theme.LocalIsTv

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    val favorites by viewModel.favorites.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏") },
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
            favorites.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无收藏")
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
                    verticalArrangement = Arrangement.spacedBy(if (isTv) 16.dp else 8.dp)
                ) {
                    items(items = favorites) { entry ->
                        val video = com.nastv.data.model.Video(
                            id = entry.videoId,
                            name = entry.item.name,
                            pic = entry.item.pic,
                            remark = entry.item.remark,
                            sourceId = entry.sourceId
                        )
                        if (isTv) {
                            TVVideoCard(
                                video = video,
                                onClick = { onNavigateToPlay(entry.sourceId, entry.videoId) }
                            )
                        } else {
                            VideoCard(
                                video = video,
                                onClick = { onNavigateToPlay(entry.sourceId, entry.videoId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
