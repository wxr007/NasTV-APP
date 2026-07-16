package com.nastv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isTv) Color(0xFFFFD700) else Color.Unspecified
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select source",
                            tint = if (isTv) Color(0xFFFFD700) else Color.Unspecified
                        )
                    }
                }
            },
            actions = {
                TvIconButton(
                    onClick = onNavigateToSearch,
                    isTv = isTv,
                    icon = Icons.Default.Search,
                    contentDescription = "Search"
                )
                TvIconButton(
                    onClick = onNavigateToFavorites,
                    isTv = isTv,
                    icon = Icons.Default.Favorite,
                    contentDescription = "Favorites"
                )
                TvIconButton(
                    onClick = onNavigateToHistory,
                    isTv = isTv,
                    icon = Icons.Default.History,
                    contentDescription = "History"
                )
                TvIconButton(
                    onClick = onNavigateToCache,
                    isTv = isTv,
                    icon = Icons.Default.Storage,
                    contentDescription = "Cache"
                )
                TvIconButton(
                    onClick = onNavigateToSettings,
                    isTv = isTv,
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        )

        // Parent category selector
        if (parentCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TvFilterChip(
                        selected = selectedParentCategory == null,
                        onClick = { viewModel.selectParentCategory(null) },
                        label = "全部",
                        isTv = isTv
                    )
                }
                items(items = parentCategories) { category ->
                    TvFilterChip(
                        selected = category == selectedParentCategory,
                        onClick = { viewModel.selectParentCategory(category) },
                        label = category.name,
                        isTv = isTv
                    )
                }
            }
        }

        // Sub-category selector
        if (subCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TvFilterChip(
                        selected = selectedSubCategory == null,
                        onClick = { viewModel.selectSubCategory(null) },
                        label = "全部",
                        isTv = isTv
                    )
                }
                items(items = subCategories) { category ->
                    TvFilterChip(
                        selected = category == selectedSubCategory,
                        onClick = { viewModel.selectSubCategory(category) },
                        label = category.name,
                        isTv = isTv
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
                    modifier = Modifier.fillMaxSize(),
                    savedScrollIndex = viewModel.savedScrollIndex,
                    savedScrollOffset = viewModel.savedScrollOffset,
                    onScrollStateChanged = { index, offset ->
                        viewModel.saveScrollState(index, offset)
                    }
                )
            }
        }
    }

    // 站源选择弹窗
    if (sourceDropdownExpanded) {
        AlertDialog(
            onDismissRequest = { sourceDropdownExpanded = false },
            title = { Text("选择源") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sources) { source ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (source.id == selectedSource?.id)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            border = if (source.id == selectedSource?.id)
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (source.id != selectedSource?.id) {
                                        viewModel.selectSource(source)
                                    }
                                    sourceDropdownExpanded = false
                                }
                        ) {
                            val nameColor = if (source.id == selectedSource?.id)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (source.frontendProxy) Icons.Default.PhoneAndroid else Icons.Default.Storage,
                                    contentDescription = if (source.frontendProxy) "前端代理" else "后端代理",
                                    tint = nameColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = nameColor,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sourceDropdownExpanded = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TvFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    isTv: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) Color(0xFFFFD700) else if (selected) Color(0xFFFF5722) else Color(0xFF666666)

    if (isTv) {
        // TV 模式：使用简单的 Box 作为按钮，避免 FilterChip 的双层边框
        Box(
            modifier = Modifier
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onClick)
                .background(
                    color = if (selected) Color(0xFFFF5722).copy(alpha = 0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = Color.White
            )
        }
    } else {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) }
        )
    }
}

@Composable
private fun TvIconButton(
    onClick: () -> Unit,
    isTv: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = if (isFocused) Color(0xFFFFD700) else Color.Transparent

    if (isTv) {
        Box(
            modifier = Modifier
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onClick)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color.Black else Color.White
            )
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}
