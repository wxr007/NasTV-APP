package com.nastv.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.nastv.ui.components.TvApiProviderDialog
import com.nastv.ui.components.TvSearchKeyboard
import com.nastv.ui.components.TvSourceSelectDialog
import com.nastv.ui.theme.NasTVBackground
import com.nastv.ui.theme.NasTVPrimaryGold
import com.nastv.ui.theme.NasTVSurface
import com.nastv.ui.theme.NasTVSurfaceLight
import com.nastv.ui.theme.NasTVTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvSearchContent(
    viewModel: TvSearchViewModel,
    onSearch: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val inputText by viewModel.inputText.collectAsState()
    val hotWords by viewModel.hotWords.collectAsState()
    val suggestionWords by viewModel.suggestionWords.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val apiProvider by viewModel.apiProvider.collectAsState()
    val allSources by viewModel.allSources.collectAsState()
    val selectedSourceIds by viewModel.selectedSourceIds.collectAsState()

    var showSourceDialog by remember { mutableStateOf(false) }
    var showApiProviderDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(searchHistory.isNotEmpty()) }

    val goldColor = NasTVPrimaryGold

    if (showSourceDialog) {
        TvSourceSelectDialog(
            sources = allSources,
            selectedIds = selectedSourceIds,
            onDismiss = { showSourceDialog = false },
            onConfirm = { ids ->
                viewModel.setSelectedSourceIds(ids)
                showSourceDialog = false
            }
        )
    }

    if (showApiProviderDialog) {
        TvApiProviderDialog(
            currentProvider = apiProvider,
            onDismiss = { showApiProviderDialog = false },
            onConfirm = { provider ->
                viewModel.setApiProvider(provider)
                showApiProviderDialog = false
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NasTVBackground)
            .padding(24.dp)
    ) {
        // 左侧：键盘区域 (~35%)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(360.dp)
                .padding(end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 按钮行：站源选择 + API 提供商
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var sourceFocused by remember { mutableStateOf(false) }
                Text(
                    text = "站源(${selectedSourceIds.size})",
                    color = goldColor,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .onFocusChanged { sourceFocused = it.isFocused }
                        .clickable { showSourceDialog = true }
                        .border(2.dp, if (sourceFocused) goldColor else Color.Transparent, RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                var apiFocused by remember { mutableStateOf(false) }
                Text(
                    text = when (apiProvider) {
                        ApiProvider.TENCENT -> "腾讯▼"
                        ApiProvider.DOUBAN -> "豆瓣▼"
                        else -> "爱奇艺▼"
                    },
                    color = goldColor,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .onFocusChanged { apiFocused = it.isFocused }
                        .clickable { showApiProviderDialog = true }
                        .border(2.dp, if (apiFocused) goldColor else Color.Transparent, RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索框（仅展示，不获取焦点，通过键盘输入）
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.setInputText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = false },
                placeholder = { Text("搜索...", color = Color.White.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 键盘
            TvSearchKeyboard(
                onKeyPress = { viewModel.appendInput(it) },
                onClear = { viewModel.clearInput() },
                onDelete = { viewModel.deleteLastChar() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 右侧：热词/联想词/历史区域 (~65%)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp)
        ) {
            // 搜索历史
            if (searchHistory.isNotEmpty() && showHistory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "搜索历史",
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    var clearFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .onFocusChanged { clearFocused = it.isFocused }
                            .clickable { viewModel.clearHistory(); showHistory = false }
                            .border(2.dp, if (clearFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("清除", color = NasTVTextSecondary, fontSize = 14.sp)
                    }
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    searchHistory.forEach { item ->
                        HistoryChip(
                            text = item,
                            onClick = {
                                viewModel.setInputText(item)
                                viewModel.addToHistory(item)
                                onSearch(item)
                            }
                        )
                    }
                }
            }

            // 热词 / 联想词
            val displayWords = if (inputText.isNotBlank()) suggestionWords else hotWords
            val title = if (inputText.isNotBlank()) "联想词" else "热词"

            if (displayWords.isNotEmpty()) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    displayWords.forEach { word ->
                        WordChip(
                            text = word,
                            onClick = {
                                viewModel.setInputText(word)
                                viewModel.addToHistory(word)
                                onSearch(word)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordChip(text: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .border(2.dp, if (isFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun HistoryChip(text: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 16.sp,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .border(2.dp, if (isFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(8.dp))
            .background(NasTVSurface, RoundedCornerShape(8.dp))
            .border(1.dp, NasTVTextSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
