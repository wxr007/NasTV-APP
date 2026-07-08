package com.nastv.ui.screens.play

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nastv.data.model.VideoDetail

/**
 * TV 端播放页布局（横屏，遥控器/键盘导航）
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvPlayContent(
    detail: VideoDetail,
    exoPlayer: ExoPlayer,
    gestureModifier: Modifier,
    seekIndicatorText: String?,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    bufferedPosition: Long,
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
    bufferMaxDuration: Int,
    onToggleFavorite: () -> Unit,
    onSelectLine: (Int) -> Unit,
    onPlayEpisode: (com.nastv.data.model.Episode) -> Unit,
    onToggleFullscreen: () -> Unit,
    onSeekFinished: (Long) -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleControls: () -> Unit,
    onUpdateBufferSettings: (Int) -> Unit,
    seekDuration: Int = 10,
) {
    var sortAscending by remember { mutableStateOf(true) }
    var isPlayerMaximized by remember { mutableStateOf(false) }
    var isPlayerFocused by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var isEpisodeFocused by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var tempBufferMax by remember { mutableStateOf(bufferMaxDuration) }

    val maximizedFocusRequester = remember { FocusRequester() }
    val episodeRangeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isPlayerMaximized) {
        if (isPlayerMaximized) {
            maximizedFocusRequester.requestFocus()
        }
    }

    // 全屏时按返回键先退出全屏
    androidx.activity.compose.BackHandler(enabled = isPlayerMaximized) {
        isPlayerMaximized = false
    }

    val focusManager = LocalFocusManager.current

    fun Modifier.tvFocusable(): Modifier = this
        .focusProperties { canFocus = true }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionRight -> { focusManager.moveFocus(FocusDirection.Right); true }
                    Key.DirectionLeft -> { focusManager.moveFocus(FocusDirection.Left); true }
                    Key.DirectionUp -> { focusManager.moveFocus(FocusDirection.Up); true }
                    Key.DirectionDown -> { focusManager.moveFocus(FocusDirection.Down); true }
                    else -> false
                }
            } else false
        }

    // 共享的播放器内容（只有一个 PlayerView，在普通/最大化模式间移动）
    val playerContent = remember {
        movableContentOf {
            Box(modifier = Modifier.fillMaxSize()) {
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
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
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        if (isPlayerMaximized) {
            // 最大化模式：全屏播放器 + 控制层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { onPlayPause() }
                    .focusRequester(maximizedFocusRequester)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Back -> { isPlayerMaximized = false; true }
                                Key.DirectionLeft -> {
                                    val newPos = (currentPosition - seekDuration * 1000L).coerceAtLeast(0)
                                    exoPlayer.seekTo(newPos)
                                    true
                                }
                                Key.DirectionRight -> {
                                    val newPos = (currentPosition + seekDuration * 1000L).coerceAtMost(totalDuration)
                                    exoPlayer.seekTo(newPos)
                                    true
                                }
                                Key.DirectionUp, Key.DirectionDown -> true
                                else -> false
                            }
                        } else false
                    }
            ) {
                playerContent()

                // 右上角始终显示下载速度
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatSpeed(downloadSpeed).also { android.util.Log.d("SpeedDebugUI", "TvMaximized speed: $it, raw=$downloadSpeed") },
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                // 暂停时显示半透明播放图标 + 时间（AndroidView 之上无法用普通 Compose 覆盖）
                if (!isPlaying && !controlsVisible) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "已暂停",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (controlsVisible) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        VideoSeekBar(
                            value = if (isSeeking) seekPosition
                                else if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat()
                                else 0f,
                            bufferedValue = if (totalDuration > 0) bufferedPosition.toFloat() / totalDuration.toFloat() else 0f,
                            onValueChange = onSeekChange,
                            onValueChangeFinished = {
                                onSeekStart()
                                onSeekFinished((seekPosition * totalDuration).toLong())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false },
                            playedColor = Color.White,
                            bufferedColor = Color(0xFF4CAF50),
                            unplayedColor = Color.White.copy(alpha = 0.3f),
                            thumbColor = Color.White
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatTime(currentPosition), style = MaterialTheme.typography.labelMedium, color = Color.White)
                            Text(text = formatTime(totalDuration), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    if (!isPlaying) {
                        // 暂停时：大图标 + 时间
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "已暂停",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        // 播放时：小暂停按钮
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "暂停",
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center).size(36.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "退出最大化",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(24.dp)
                        )
                    }
                }
            }
        } else {
            // 普通模式
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 上半部分：左侧播放器 + 右侧视频信息
                Row(modifier = Modifier.weight(1f)) {
                    // 左侧：视频播放器
                    val playerBorderColor = if (isPlayerFocused) Color(0xFFFFD700) else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black)
                            .border(3.dp, playerBorderColor, RoundedCornerShape(4.dp))
                            .tvFocusable()
                            .onFocusChanged { isPlayerFocused = it.isFocused }
                            .clickable { isPlayerMaximized = true }
                    ) {
                        playerContent()

                        // 右上角始终显示下载速度
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatSpeed(downloadSpeed).also { android.util.Log.d("SpeedDebugUI", "TvNormal speed: $it, raw=$downloadSpeed") },
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        if (controlsVisible) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                VideoSeekBar(
                                    value = if (isSeeking) seekPosition
                                        else if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat()
                                        else 0f,
                                    bufferedValue = if (totalDuration > 0) bufferedPosition.toFloat() / totalDuration.toFloat() else 0f,
                                    onValueChange = onSeekChange,
                                    onValueChangeFinished = {
                                        onSeekStart()
                                        onSeekFinished((seekPosition * totalDuration).toLong())
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusProperties { canFocus = false },
                                    playedColor = Color.White,
                                    bufferedColor = Color(0xFF4CAF50),
                                    unplayedColor = Color.White.copy(alpha = 0.3f),
                                    thumbColor = Color.White
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = formatTime(currentPosition), style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    Text(text = formatTime(totalDuration), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 右侧：视频信息
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text(
                            text = detail.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            detail.year?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f)) }
                            detail.type?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4CAF50)) }
                            detail.area?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f)) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        detail.actor?.let {
                            Text(text = "演员：$it", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        detail.director?.let {
                            Text(text = "导演：$it", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(text = "站源：${detail.sourceId}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        detail.content?.let {
                            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 收藏 + 简介 按钮
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            var isFavoriteFocused by remember { mutableStateOf(false) }
                            val favBorderColor = if (isFavoriteFocused) Color(0xFFFFD700) else Color(0xFF666666)
                            Box(
                                modifier = Modifier
                                    .tvFocusable()
                                    .onFocusChanged { isFavoriteFocused = it.isFocused }
                                    .clickable { onToggleFavorite() }
                                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                    .border(2.dp, favBorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "收藏",
                                        tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isFavorite) "已收藏" else "收藏",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isFavorite) Color(0xFFFF4081) else Color.White
                                    )
                                }
                            }

                            // 简介按钮（与收藏按钮等大）
                            if (detail.content != null) {
                                var isDescFocused by remember { mutableStateOf(false) }
                                val descBorderColor = if (isDescFocused) Color(0xFFFFD700) else Color(0xFF666666)
                                Box(
                                    modifier = Modifier
                                        .tvFocusable()
                                        .onFocusChanged { isDescFocused = it.isFocused }
                                        .clickable { showDescriptionDialog = true }
                                        .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                        .border(2.dp, descBorderColor, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "简介",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "简介", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                    }
                                }
                            }

                            // 设置按钮
                            var isSettingsFocused by remember { mutableStateOf(false) }
                            val settingsBorderColor = if (isSettingsFocused) Color(0xFFFFD700) else Color(0xFF666666)
                            Box(
                                modifier = Modifier
                                    .tvFocusable()
                                    .onFocusChanged { isSettingsFocused = it.isFocused }
                                    .clickable { showSettingsDrawer = true }
                                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                    .border(2.dp, settingsBorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "设置",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "设置", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 下半部分：线路选择 + 选集
                Column(modifier = Modifier.weight(1f)) {
                    // 线路选择器
                    if (detail.playFrom.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = detail.playWithIndex) { (index, name) ->
                                var isFocused by remember { mutableStateOf(false) }
                                val lineBorderColor = if (isFocused) Color(0xFFFFD700) else Color(0xFF666666)
                                val bgColor = if (index == selectedLine) Color(0xFF4CAF50) else Color(0xFF333333)
                                Box(
                                    modifier = Modifier
                                        .tvFocusable()
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .clickable { onSelectLine(index) }
                                        .background(bgColor, RoundedCornerShape(8.dp))
                                        .border(2.dp, lineBorderColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(text = name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 排序 + 范围分组按钮行
                    val episodes = detail.episodes.getOrNull(selectedLine) ?: emptyList()
                    val sortedEpisodes = if (sortAscending) episodes else episodes.reversed()
                    val groupSize = 40
                    val totalEpisodes = episodes.size
                    val groupCount = (totalEpisodes + groupSize - 1) / groupSize
                    val episodeCols = 8

                    val episodeScrollState = rememberScrollState()
                    val density = LocalDensity.current
                    var scrollTarget by remember { mutableStateOf<Int?>(null) }
                    LaunchedEffect(scrollTarget) {
                        scrollTarget?.let { targetPx ->
                            episodeScrollState.animateScrollTo(targetPx)
                            scrollTarget = null
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.focusRequester(episodeRangeFocusRequester)
                    ) {
                        // 排序按钮
                        item {
                            var isSortFocused by remember { mutableStateOf(false) }
                            val sortBorderColor = if (isSortFocused) Color(0xFFFFD700) else Color(0xFF666666)
                            Box(
                                modifier = Modifier
                                    .tvFocusable()
                                    .onFocusChanged { isSortFocused = it.isFocused }
                                    .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                    .border(2.dp, sortBorderColor, RoundedCornerShape(8.dp))
                                    .clickable { sortAscending = !sortAscending }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = if (sortAscending) "倒序" else "正序", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }
                        }

                        // 箭头按钮
                        item {
                            var isArrowFocused by remember { mutableStateOf(false) }
                            val arrowBorderColor = if (isArrowFocused) Color(0xFFFFD700) else Color(0xFF666666)
                            Box(
                                modifier = Modifier
                                    .tvFocusable()
                                    .onFocusChanged { isArrowFocused = it.isFocused }
                                    .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                    .border(2.dp, arrowBorderColor, RoundedCornerShape(8.dp))
                                    .clickable { sortAscending = !sortAscending }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (sortAscending) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = "方向",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 范围分组按钮
                        for (g in 0 until groupCount) {
                            item {
                                val startIdx = g * groupSize + 1
                                val endIdx = minOf((g + 1) * groupSize, totalEpisodes)
                                var isGroupFocused by remember { mutableStateOf(false) }
                                val groupBorderColor = if (isGroupFocused) Color(0xFFFFD700) else Color(0xFF666666)
                                val targetRow = (startIdx - 1) / episodeCols
                                val scrollPositionPx = with(density) { (targetRow * 56).dp.roundToPx() }
                                Box(
                                    modifier = Modifier
                                        .tvFocusable()
                                        .onFocusChanged { isGroupFocused = it.isFocused }
                                        .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                        .border(2.dp, groupBorderColor, RoundedCornerShape(8.dp))
                                        .clickable { scrollTarget = scrollPositionPx }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "$startIdx-$endIdx", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 可滚动区域：剧集网格
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(episodeScrollState)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Back && isEpisodeFocused) {
                                    episodeRangeFocusRequester.requestFocus()
                                    true
                                } else false
                            }
                    ) {
                        if (sortedEpisodes.isNotEmpty()) {
                            val episodeRows = (sortedEpisodes.size + episodeCols - 1) / episodeCols
                            for (row in 0 until episodeRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (col in 0 until episodeCols) {
                                        val epIndex = row * episodeCols + col
                                        if (epIndex < sortedEpisodes.size) {
                                            val episode = sortedEpisodes[epIndex]
                                            val isCurrentEpisode = episode.name == currentEpisodeName
                                            val originalIndex = if (sortAscending) epIndex else episodes.size - 1 - epIndex
                                            val isCached = originalIndex in cachedEpisodeIndices
                                            var isFocused by remember { mutableStateOf(false) }
                                            val epBorderColor = if (isFocused) Color(0xFFFFD700) else Color(0xFF666666)
                                            val epBgColor = if (isCurrentEpisode) Color(0xFF4CAF50) else Color(0xFF333333)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .tvFocusable()
                                                    .onFocusChanged { 
                                                        isFocused = it.isFocused
                                                        isEpisodeFocused = it.isFocused
                                                    }
                                                    .clickable { onPlayEpisode(episode) }
                                                    .background(epBgColor, RoundedCornerShape(8.dp))
                                                    .border(2.dp, epBorderColor, RoundedCornerShape(8.dp))
                                            ) {
                                                Text(
                                                    text = episode.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 4.dp)
                                                )
                                                if (isCached) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(6.dp)
                                                            .background(color = Color(0xFFFFD700), shape = CircleShape)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f).height(48.dp))
                                        }
                                    }
                                }
                                if (row < episodeRows - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 简介弹窗 - 直接布局内渲染，避免遮罩变暗
        if (showDescriptionDialog) {
            val descCloseFocusRequester = remember { FocusRequester() }
            val descScrollState = rememberScrollState()
            var descScrollDelta by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) { descCloseFocusRequester.requestFocus() }
            LaunchedEffect(descScrollDelta) {
                if (descScrollDelta != 0) {
                    val target = (descScrollState.value + descScrollDelta).coerceIn(0, descScrollState.maxValue)
                    descScrollState.animateScrollTo(target)
                    descScrollDelta = 0
                }
            }

            // 拦截返回键，不要退出整个页面
            androidx.activity.compose.BackHandler(enabled = showDescriptionDialog) {
                showDescriptionDialog = false
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Back -> { showDescriptionDialog = false; true }
                                Key.DirectionUp -> {
                                    descScrollDelta = -120
                                    true
                                }
                                Key.DirectionDown -> {
                                    descScrollDelta = 120
                                    true
                                }
                                Key.DirectionLeft, Key.DirectionRight -> true
                                else -> false
                            }
                        } else false
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // 弹窗内容定位到下半部分
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .fillMaxHeight(0.45f)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // 标题
                        Text(
                            text = detail.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // 内容
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(descScrollState)
                        ) {
                            Text(
                                text = detail.content ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 关闭按钮
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(
                                onClick = { showDescriptionDialog = false },
                                modifier = Modifier
                                    .focusRequester(descCloseFocusRequester)
                                    .tvFocusable()
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                            showDescriptionDialog = false
                                            true
                                        } else false
                                    }
                            ) {
                                Text("关闭")
                            }
                        }
                    }
                }
            }
        }

        // 设置抽屉 - 从右侧滑出
        if (showSettingsDrawer) {
            val settingsCloseFocusRequester = remember { FocusRequester() }
            val settingsScrollState = rememberScrollState()
            var settingsScrollDelta by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) { settingsCloseFocusRequester.requestFocus() }
            LaunchedEffect(settingsScrollDelta) {
                if (settingsScrollDelta != 0) {
                    val target = (settingsScrollState.value + settingsScrollDelta).coerceIn(0, settingsScrollState.maxValue)
                    settingsScrollState.animateScrollTo(target)
                    settingsScrollDelta = 0
                }
            }

            // 拦截返回键
            androidx.activity.compose.BackHandler(enabled = showSettingsDrawer) {
                showSettingsDrawer = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusable()
                    .focusProperties {
                        // 限制焦点只能在抽屉内部移动
                        exit = { FocusRequester.Cancel }
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Back -> {
                                    showSettingsDrawer = false
                                    true
                                }
                                Key.DirectionUp -> {
                                    settingsScrollDelta = -80
                                    true
                                }
                                Key.DirectionDown -> {
                                    settingsScrollDelta = 80
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                // 半透明遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showSettingsDrawer = false }
                )

                // 右侧抽屉内容
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(400.dp)
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(settingsScrollState)
                    ) {
                        // 标题
                        Text(
                            text = "播放设置",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // 缓冲设置
                        Text(
                            text = "缓冲配置",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 最大缓冲时长
                        Text(
                            text = "最大缓冲: ${tempBufferMax}秒",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var isSliderFocused by remember { mutableStateOf(false) }
                        val sliderFocusRequester = remember { FocusRequester() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(sliderFocusRequester)
                                .tvFocusable()
                                .onFocusChanged { isSliderFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.key) {
                                            Key.DirectionLeft -> {
                                                tempBufferMax = (tempBufferMax - 10).coerceAtLeast(50)
                                                true
                                            }
                                            Key.DirectionRight -> {
                                                tempBufferMax = (tempBufferMax + 10).coerceAtMost(300)
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .border(
                                    if (isSliderFocused) 2.dp else 0.dp,
                                    if (isSliderFocused) Color(0xFFFFD700) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Slider(
                                value = tempBufferMax.toFloat(),
                                onValueChange = { tempBufferMax = it.toInt() },
                                valueRange = 50f..300f,
                                steps = 24,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "提示: 缓冲越大播放越流畅，但占用内存越多",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        // 按钮行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 取消按钮
                            var isCancelFocused by remember { mutableStateOf(false) }
                            val cancelBorderColor = if (isCancelFocused) Color(0xFFFFD700) else Color(0xFF666666)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .tvFocusable()
                                    .onFocusChanged { isCancelFocused = it.isFocused }
                                    .clickable { showSettingsDrawer = false }
                                    .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                    .border(2.dp, cancelBorderColor, RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("取消", color = Color.White)
                            }

                            // 保存按钮
                            var isSaveFocused by remember { mutableStateOf(false) }
                            val saveBorderColor = if (isSaveFocused) Color(0xFFFFD700) else Color(0xFF666666)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(settingsCloseFocusRequester)
                                    .tvFocusable()
                                    .onFocusChanged { isSaveFocused = it.isFocused }
                                    .clickable {
                                        onUpdateBufferSettings(tempBufferMax)
                                        showSettingsDrawer = false
                                    }
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                                    .border(2.dp, saveBorderColor, RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("保存", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
