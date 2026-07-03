package com.nastv.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.components.VideoGrid
import com.nastv.ui.theme.LocalIsTv

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCache: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
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

    LaunchedEffect(Unit) {
        viewModel.loadSources()
    }

    // Load categories when source changes
    LaunchedEffect(selectedSource) {
        if (selectedSource != null) {
            viewModel.loadCategories(selectedSource!!.id)
        }
    }

    // Load videos when source or category selection changes
    LaunchedEffect(selectedSource, selectedParentCategory, selectedSubCategory) {
        if (selectedSource != null) {
            val categoryId = selectedSubCategory?.id ?: selectedParentCategory?.id
            viewModel.loadVideos(selectedSource!!.id, categoryId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with source selector as title
        TopAppBar(
            title = {
                Box {
                    TextButton(onClick = { sourceDropdownExpanded = true }) {
                        Text(
                            text = selectedSource?.name ?: "选择源",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select source")
                    }
                    DropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        sources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    viewModel.selectSource(source)
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onNavigateToFavorites) {
                    Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                }
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Default.History, contentDescription = "History")
                }
                IconButton(onClick = onNavigateToCache) {
                    Icon(Icons.Default.Storage, contentDescription = "Cache")
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Parent category selector
        if (parentCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedParentCategory == null,
                        onClick = { viewModel.selectParentCategory(null) },
                        label = { Text("全部") }
                    )
                }
                items(items = parentCategories) { category ->
                    FilterChip(
                        selected = category == selectedParentCategory,
                        onClick = { viewModel.selectParentCategory(category) },
                        label = { Text(category.name) }
                    )
                }
            }
        }

        // Sub-category selector
        if (subCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSubCategory == null,
                        onClick = { viewModel.selectSubCategory(null) },
                        label = { Text("全部") }
                    )
                }
                items(items = subCategories) { category ->
                    FilterChip(
                        selected = category == selectedSubCategory,
                        onClick = { viewModel.selectSubCategory(category) },
                        label = { Text(category.name) }
                    )
                }
            }
        }

        // Video grid
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                VideoGrid(
                    videos = videos,
                    onVideoClick = onNavigateToPlay,
                    onLoadMore = { viewModel.loadMoreVideos() },
                    isLoadingMore = isLoadingMore,
                    hasMore = hasMore,
                    isTv = isTv,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
