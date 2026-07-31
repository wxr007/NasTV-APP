package com.nastv.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.nastv.ui.components.TVVideoCard
import com.nastv.ui.config.GridDefaults
import com.nastv.ui.theme.NasTVBackground
import com.nastv.ui.theme.NasTVAccent
import com.nastv.ui.theme.NasTVPrimaryGold
import com.nastv.ui.theme.NasTVTextSecondary
import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun TvSearchResultScreen(
    keyword: String,
    viewModel: TvSearchViewModel,
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val sourceResults by viewModel.sourceResults.collectAsState()
    val selectedSourceId by viewModel.selectedSourceId.collectAsState()
    val allSources by viewModel.allSources.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedIds by viewModel.selectedSourceIds.collectAsState()

    BackHandler { onNavigateBack() }

    // 首次加载触发搜索（已有结果则跳过，从播放页返回时不再重复搜索）
    LaunchedEffect(keyword) {
        Log.d("TvSearchResult", "LaunchedEffect keyword=$keyword, existing results=${sourceResults.size}")
        if (keyword.isNotBlank() && sourceResults.isEmpty()) {
            viewModel.search(keyword)
        }
    }

    // 选中的站源列表（只显示已选中的站源，按结果数排序）
    val sourceEntries = remember(selectedIds, sourceResults, allSources) {
        val nameMap = allSources.associate { it.id to it.name }
        val entries = mutableListOf<SourceEntry>()
        entries.add(SourceEntry("all", "全部"))
        selectedIds.sortedBy { id ->
            // 有结果的站源排在前面
            val count = sourceResults[id]?.size ?: 0
            -count
        }.forEach { id ->
            val count = sourceResults[id]?.size ?: 0
            entries.add(SourceEntry(id, nameMap[id] ?: id, count))
        }
        entries
    }

    // 当前显示的视频列表
    val displayVideos = remember(sourceResults, selectedSourceId) {
        if (selectedSourceId == "all") {
            sourceResults.values.flatten()
        } else {
            sourceResults[selectedSourceId] ?: emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(NasTVBackground)) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索: $keyword",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            if (isLoading) {
                Spacer(modifier = Modifier.width(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                    color = NasTVPrimaryGold
                )
            }
        }

        // 主体：左侧站源列表 + 右侧海报网格
        Row(modifier = Modifier.fillMaxSize()) {
            // 左侧站源列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(180.dp)
                    .padding(start = 24.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sourceEntries) { entry ->
                    val isSelected = entry.id == selectedSourceId
                    val isDisabled = entry.id != "all" && entry.count == 0
                    var isFocused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .clickable(enabled = !isDisabled) { viewModel.selectSource(entry.id) }
                            .background(
                                if (isSelected) NasTVAccent
                                else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .border(2.dp, if (isFocused) NasTVPrimaryGold else if (isSelected) Color.White else NasTVTextSecondary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.displayName,
                            color = if (isDisabled) NasTVTextSecondary.copy(alpha = 0.4f)
                                    else Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (entry.id == "all") {
                            val totalCount = sourceResults.values.sumOf { it.size }
                            Text(
                                "($totalCount)",
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else NasTVTextSecondary,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                "(${entry.count})",
                                color = if (isDisabled) NasTVTextSecondary.copy(alpha = 0.3f)
                                        else if (isSelected) Color.White.copy(alpha = 0.8f)
                                        else NasTVTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 右侧海报网格
            if (displayVideos.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无结果", color = NasTVTextSecondary, fontSize = 18.sp)
                }
            } else if (displayVideos.isNotEmpty()) {
                val columns = 4  // 因为左侧站源面板占位，用4列
                val spacing = GridDefaults.TV_SPACING
                val rows = displayVideos.chunked(columns)
                val listState = rememberLazyListState()
                var lastFocusedRow by remember { mutableIntStateOf(-1) }

                LaunchedEffect(lastFocusedRow) {
                    if (lastFocusedRow >= 0) {
                        try {
                            if (lastFocusedRow == 0) {
                                listState.animateScrollToItem(0)
                            } else {
                                delay(100)
                                val info = listState.layoutInfo
                                val item = info.visibleItemsInfo.find { it.index == lastFocusedRow }
                                if (item != null) {
                                    val vpHeight = info.viewportEndOffset - info.viewportStartOffset
                                    val itemTopInVp = item.offset - info.viewportStartOffset
                                    val targetTop = (vpHeight - item.size) / 2f
                                    val delta = itemTopInVp - targetTop
                                    if (abs(delta) > 10f) {
                                        listState.animateScrollBy(delta)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TvSearchResult", "居中异常", e)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    items(rows) { rowVideos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            rowVideos.forEachIndexed { colIndex, video ->
                                val rowIdx = rows.indexOf(rowVideos)
                                TVVideoCard(
                                    video = video,
                                    onClick = { onNavigateToPlay(video.sourceId, video.id) },
                                    modifier = Modifier.weight(1f),
                                    shouldAutoFocus = colIndex == 0 && rowIdx == lastFocusedRow,
                                    sourceName = allSources.find { it.id == video.sourceId }?.name,
                                    onFocusGained = {
                                        if (rowIdx != lastFocusedRow) {
                                            lastFocusedRow = rowIdx
                                        }
                                    }
                                )
                            }
                            val remaining = columns - rowVideos.size
                            if (remaining > 0) {
                                repeat(remaining) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SourceEntry(
    val id: String,
    val displayName: String,
    val count: Int = 0
)
