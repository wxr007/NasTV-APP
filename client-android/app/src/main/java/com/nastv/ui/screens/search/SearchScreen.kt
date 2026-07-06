package com.nastv.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.components.TVVideoCard
import com.nastv.ui.components.VideoCard
import com.nastv.ui.theme.LocalIsTv
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.getSuggestions(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索视频...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFFFFD700),
                    cursorColor = Color(0xFFFFD700)
                ),
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { viewModel.search(query) }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            )
        }

        // Suggestions dropdown
        if (suggestions.isNotEmpty() && query.isNotBlank()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(items = suggestions) { suggestion ->
                    TextButton(
                        onClick = {
                            query = suggestion
                            viewModel.search(suggestion)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(suggestion, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Search results
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
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            searchResults.isNotEmpty() -> {
                if (isTv) {
                    // TV 模式：使用 LazyColumn + Row 手动网格（解决焦点导航问题）
                    val columns = 5
                    val spacing = 16.dp
                    val rows = searchResults.chunked(columns)
                    val listState = rememberLazyListState()

                    // 焦点恢复目标索引（-1 表示不需要恢复）
                    var focusTargetIndex by remember { mutableStateOf(-1) }
                    var isRestoringScroll by remember { mutableStateOf(false) }

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

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items(rows) { rowVideos ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowVideos.forEachIndexed { colIndex, video ->
                                    val videoIndex = (rows.indexOf(rowVideos) * columns) + colIndex
                                    TVVideoCard(
                                        video = video,
                                        onClick = {
                                            viewModel.saveScrollState(videoIndex)
                                            onNavigateToPlay(video.sourceId, video.id)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shouldAutoFocus = videoIndex == focusTargetIndex
                                    )
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = searchResults) { video ->
                            VideoCard(
                                video = video,
                                onClick = { onNavigateToPlay(video.sourceId, video.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
