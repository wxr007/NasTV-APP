package com.nastv.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.components.VideoCard
import com.nastv.ui.theme.NasTVTextSecondary

/**
 * 手机端搜索页：按站源并发直连搜索，结果按站源顺序汇总展示。
 */
@Composable
fun SearchScreen(
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val sourceNameMap by viewModel.sourceNameMap.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchedKeyword by viewModel.searchedKeyword.collectAsState()

    BackHandler { onNavigateBack() }

    val submit: (String) -> Unit = { keyword ->
        query = keyword
        viewModel.search(keyword)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(query) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFFFFD700),
                    cursorColor = Color(0xFFFFD700)
                ),
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { submit(query) }) {
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

        // 并发搜索中：顶部细进度条，已返回的结果可以边看边刷新
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 联想词
        if (suggestions.isNotEmpty() && query.isNotBlank()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .padding(horizontal = 16.dp)
            ) {
                items(items = suggestions) { suggestion ->
                    TextButton(
                        onClick = { submit(suggestion) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(suggestion, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        when {
            // 有结果就先展示（并发过程中会逐步追加）
            searchResults.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = searchResults) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onNavigateToPlay(video.sourceId, video.id) },
                            sourceName = sourceNameMap[video.sourceId]
                        )
                    }
                }
            }

            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            searchedKeyword.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有找到「$searchedKeyword」相关内容",
                        color = NasTVTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "输入关键词，将在所有已启用站源中并发搜索",
                        color = NasTVTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
