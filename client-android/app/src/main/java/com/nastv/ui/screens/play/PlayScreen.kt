package com.nastv.ui.screens.play

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nastv.ui.theme.LocalIsTv
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    sourceId: String,
    videoId: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    val videoDetail by viewModel.videoDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedLine by viewModel.selectedLine.collectAsState()
    val currentEpisodeUrl by viewModel.currentEpisodeUrl.collectAsState()
    val currentEpisodeName by viewModel.currentEpisodeName.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val proxyUrl by viewModel.proxyUrl.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val restorePosition by viewModel.restorePosition.collectAsState()
    val seekDuration by viewModel.seekDuration.collectAsState()
    val cachedEpisodeIndices by viewModel.cachedEpisodeIndices.collectAsState()

    var episodesExpanded by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(true) }

    // Player state for custom controls
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }

    // Seek indicator
    var seekIndicatorText by remember { mutableStateOf<String?>(null) }

    // Player width for double-tap position detection
    var playerWidth by remember { mutableFloatStateOf(0f) }

    val activity = LocalContext.current as? ComponentActivity

    LaunchedEffect(sourceId, videoId) {
        viewModel.loadVideoDetail(sourceId, videoId)
    }

    // Handle fullscreen orientation & system bars
    LaunchedEffect(isFullscreen, isTv) {
        activity?.let { act ->
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else if (isTv) {
                // TV 模式：播放页始终横屏
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Auto-hide controls when playing
    LaunchedEffect(isPlaying, controlsVisible) {
        if (isPlaying && controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Auto-hide seek indicator
    LaunchedEffect(seekIndicatorText) {
        if (seekIndicatorText != null) {
            delay(1000)
            seekIndicatorText = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("返回")
                        }
                    }
                }
            }
            videoDetail != null -> {
                videoDetail?.let { detail ->
                    val context = LocalContext.current
                    val exoPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            playWhenReady = true
                            addListener(object : Player.Listener {
                                override fun onIsPlayingChanged(playing: Boolean) {
                                    isPlaying = playing
                                }
                                override fun onPlaybackStateChanged(state: Int) {
                                    if (state == Player.STATE_READY) {
                                        totalDuration = duration
                                    }
                                }
                            })
                        }
                    }

                    // Update player state
                    LaunchedEffect(exoPlayer) {
                        while (true) {
                            if (!isSeeking) {
                                currentPosition = exoPlayer.currentPosition
                                totalDuration = exoPlayer.duration.coerceAtLeast(0)
                            }
                            delay(500)
                        }
                    }

                    val playUrl = proxyUrl ?: currentEpisodeUrl

                    LaunchedEffect(playUrl) {
                        playUrl?.let { url ->
                            val mediaItem = MediaItem.fromUri(url)
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                        }
                    }

                    // Restore play position from history when player is ready
                    var positionRestored by remember { mutableStateOf(false) }
                    LaunchedEffect(restorePosition) {
                        restorePosition?.let { pos ->
                            if (!positionRestored) {
                                var attempts = 0
                                while (exoPlayer.duration <= 0 && attempts < 10) {
                                    delay(500)
                                    attempts++
                                }
                                if (exoPlayer.duration > 0) {
                                    exoPlayer.seekTo(pos.coerceAtMost(exoPlayer.duration))
                                    positionRestored = true
                                }
                            }
                        }
                    }

                    // Record play history when episode changes
                    LaunchedEffect(currentEpisodeName, detail.id) {
                        currentEpisodeName?.let { name ->
                            viewModel.recordPlayHistory(
                                sourceId = detail.sourceId,
                                videoId = detail.id,
                                episode = name,
                                videoName = detail.name,
                                pic = detail.pic,
                                progress = 0f,
                                duration = null,
                                totalEpisodes = detail.episodes.getOrNull(selectedLine)?.size,
                                activeSource = selectedLine,
                                activeEpisodeIndex = detail.episodes.getOrNull(selectedLine)
                                    ?.indexOfFirst { it.name == name }
                                    ?.takeIf { it >= 0 }
                            )
                        }
                    }

                    // Periodically update play progress while playing
                    LaunchedEffect(currentEpisodeName, detail.id) {
                        while (true) {
                            if (isPlaying && totalDuration > 0) {
                                viewModel.recordPlayHistory(
                                    sourceId = detail.sourceId,
                                    videoId = detail.id,
                                    episode = currentEpisodeName ?: "",
                                    videoName = detail.name,
                                    pic = detail.pic,
                                    progress = currentPosition.toFloat() / 1000f,
                                    duration = totalDuration.toFloat() / 1000f,
                                    totalEpisodes = detail.episodes.getOrNull(selectedLine)?.size,
                                    activeSource = selectedLine,
                                    activeEpisodeIndex = detail.episodes.getOrNull(selectedLine)
                                        ?.indexOfFirst { it.name == currentEpisodeName }
                                        ?.takeIf { it >= 0 }
                                )
                            }
                            delay(10000)
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            exoPlayer.release()
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }

                    // Common gesture handler: single tap = toggle controls, double tap = seek or pause
                    val gestureModifier = Modifier
                        .onSizeChanged { playerWidth = it.width.toFloat() }
                        .pointerInput(seekDuration) {
                            detectTapGestures(
                                onTap = {
                                    controlsVisible = !controlsVisible
                                },
                                onDoubleTap = { offset ->
                                    val fraction = if (playerWidth > 0) offset.x / playerWidth else 0.5f
                                    when {
                                        fraction < 0.2f -> {
                                            // Rewind
                                            val durMs = seekDuration * 1000L
                                            val newPos = (exoPlayer.currentPosition - durMs).coerceAtLeast(0)
                                            exoPlayer.seekTo(newPos)
                                            seekIndicatorText = "◀◀ ${seekDuration}s"
                                        }
                                        fraction > 0.8f -> {
                                            // Fast forward
                                            val durMs = seekDuration * 1000L
                                            val newPos = (exoPlayer.currentPosition + durMs).coerceAtMost(exoPlayer.duration)
                                            exoPlayer.seekTo(newPos)
                                            seekIndicatorText = "${seekDuration}s ▶▶"
                                        }
                                        else -> {
                                            // Center: pause/resume
                                            if (exoPlayer.isPlaying) {
                                                exoPlayer.pause()
                                                seekIndicatorText = "暂停"
                                            } else {
                                                exoPlayer.play()
                                                seekIndicatorText = "播放"
                                            }
                                        }
                                    }
                                }
                            )
                        }

                    if (isFullscreen) {
                        // Fullscreen mode
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
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

                            // Gesture layer
                            Box(modifier = Modifier.fillMaxSize().then(gestureModifier))

                            // Seek indicator
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

                            if (controlsVisible) {
                                // Exit fullscreen button
                                IconButton(
                                    onClick = { viewModel.toggleFullscreen() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .size(40.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.FullscreenExit,
                                        contentDescription = "退出全屏",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Bottom: progress bar + time
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 32.dp)
                                ) {
                                    Slider(
                                        value = if (isSeeking) seekPosition else if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f,
                                        onValueChange = { ratio ->
                                            isSeeking = true
                                            seekPosition = ratio
                                        },
                                        onValueChangeFinished = {
                                            val targetPosition = (seekPosition * totalDuration).toLong()
                                            exoPlayer.seekTo(targetPosition)
                                            currentPosition = targetPosition
                                            isSeeking = false
                                        },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatTime(currentPosition),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = formatTime(totalDuration),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isTv) {
                        // Focus manager for keyboard/DPAD navigation
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

                        // TV 模式：先上下分，上面再左右分
                        // 注意：根 Box 不能有 onPreviewKeyEvent，否则会拦截子按钮的按键事件
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1A1A1A))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                // 上半部分：左侧播放器 + 右侧视频信息
                                Row(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // 左侧：视频播放器
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(Color.Black)
                                    ) {
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

                                        // Gesture layer
                                        Box(modifier = Modifier.fillMaxSize().then(gestureModifier))

                                        // Seek indicator
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

                                        if (controlsVisible) {
                                            // Bottom: progress bar + time
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                            ) {
                                                Slider(
                                                    value = if (isSeeking) seekPosition else if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f,
                                                    onValueChange = { ratio ->
                                                        isSeeking = true
                                                        seekPosition = ratio
                                                    },
                                                    onValueChangeFinished = {
                                                        val targetPosition = (seekPosition * totalDuration).toLong()
                                                        exoPlayer.seekTo(targetPosition)
                                                        currentPosition = targetPosition
                                                        isSeeking = false
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.White,
                                                        activeTrackColor = Color.White,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                                    )
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = formatTime(currentPosition),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = formatTime(totalDuration),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(24.dp))

                                    // 右侧：视频信息
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        // 视频标题
                                        Text(
                                            text = detail.name,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 元信息
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            detail.year?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                            detail.type?.let { type ->
                                                Text(
                                                    text = type,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color(0xFF4CAF50)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // 简介
                                        detail.content?.let { desc ->
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.6f),
                                                maxLines = 5,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // 收藏按钮
                                        var isFavoriteFocused by remember { mutableStateOf(false) }
                                        val favoriteBorderColor = if (isFavoriteFocused) Color(0xFFFFD700) else Color(0xFF666666)
                                        Box(
                                            modifier = Modifier
                                                .tvFocusable()
                                                .onFocusChanged { isFavoriteFocused = it.isFocused }
                                                .clickable { viewModel.toggleFavorite() }
                                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                                .border(2.dp, favoriteBorderColor, RoundedCornerShape(8.dp))
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
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 下半部分：线路选择 + 选集（在播放器下方）
                                // 线路选择器
                                if (detail.playFrom.size > 1) {
                                    Text(
                                        text = "播放线路",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(items = detail.playWithIndex) { (index, name) ->
                                            var isFocused by remember { mutableStateOf(false) }
                                            val lineBorderColor = if (isFocused) Color(0xFFFFD700) else Color(0xFF666666)
                                            val bgColor = if (index == selectedLine) Color(0xFF4CAF50) else Color(0xFF333333)
                                            Box(
                                                modifier = Modifier
                                                    .tvFocusable()
                                                    .onFocusChanged { isFocused = it.isFocused }
                                                    .clickable { viewModel.selectLine(index) }
                                                    .background(bgColor, RoundedCornerShape(8.dp))
                                                    .border(2.dp, lineBorderColor, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = name,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // 剧集选择器标题
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "选集",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    var isSortFocused by remember { mutableStateOf(false) }
                                    val sortBorderColor = if (isSortFocused) Color(0xFFFFD700) else Color(0xFF666666)

                                    Row(
                                        modifier = Modifier
                                            .tvFocusable()
                                            .onFocusChanged { isSortFocused = it.isFocused }
                                            .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                            .border(2.dp, sortBorderColor, RoundedCornerShape(8.dp))
                                            .clickable { sortAscending = !sortAscending }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (sortAscending) "正序" else "倒序",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "排序",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 剧集列表
                                val episodes = detail.episodes.getOrNull(selectedLine) ?: emptyList()
                                val sortedEpisodes = if (sortAscending) episodes else episodes.reversed()
                                if (sortedEpisodes.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(items = sortedEpisodes) { index, episode ->
                                            val isCurrentEpisode = episode.name == currentEpisodeName
                                            val originalIndex = if (sortAscending) index else episodes.size - 1 - index
                                            val isCached = originalIndex in cachedEpisodeIndices
                                            var isFocused by remember { mutableStateOf(false) }
                                            val episodeBorderColor = if (isFocused) Color(0xFFFFD700) else Color(0xFF666666)
                                            val episodeBgColor = if (isCurrentEpisode) Color(0xFF4CAF50) else Color(0xFF333333)

                                            Box(
                                                modifier = Modifier
                                                    .width(100.dp)
                                                    .height(60.dp)
                                                    .tvFocusable()
                                                    .onFocusChanged { isFocused = it.isFocused }
                                                    .clickable { viewModel.playEpisode(episode) }
                                                    .background(episodeBgColor, RoundedCornerShape(8.dp))
                                                    .border(2.dp, episodeBorderColor, RoundedCornerShape(8.dp))
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isCurrentEpisode)
                                                        Color(0xFF4CAF50)
                                                    else
                                                        Color(0xFF333333),
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = episode.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                // Cached indicator dot
                                                if (isCached) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(8.dp)
                                                            .background(
                                                                color = Color(0xFFFFD700),
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 手机模式：竖屏布局
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            item {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .background(Color.Black)
                                    ) {
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

                                        // Gesture layer
                                        Box(modifier = Modifier.fillMaxSize().then(gestureModifier))

                                        // Seek indicator
                                        seekIndicatorText?.let { text ->
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = text,
                                                    color = Color.White,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        if (controlsVisible) {
                                            // Fullscreen button
                                            IconButton(
                                                onClick = { viewModel.toggleFullscreen() },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .size(36.dp)
                                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            ) {
                                                Icon(
                                                    Icons.Default.Fullscreen,
                                                    contentDescription = "全屏",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Bottom: progress bar + time
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                            ) {
                                                Slider(
                                                    value = if (isSeeking) seekPosition else if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f,
                                                    onValueChange = { ratio ->
                                                        isSeeking = true
                                                        seekPosition = ratio
                                                    },
                                                    onValueChangeFinished = {
                                                        val targetPosition = (seekPosition * totalDuration).toLong()
                                                        exoPlayer.seekTo(targetPosition)
                                                        currentPosition = targetPosition
                                                        isSeeking = false
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.White,
                                                        activeTrackColor = Color.White,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                                    )
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = formatTime(currentPosition),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = formatTime(totalDuration),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Content below player
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Title row with favorite button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = detail.name,
                                            style = MaterialTheme.typography.headlineMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "收藏",
                                                tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Meta info row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = detail.playFrom.getOrNull(selectedLine) ?: "",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                            detail.year?.let {
                                                Text(text = it, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            detail.type?.let { type ->
                                                Text(
                                                    text = type,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF4CAF50)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "详情 >",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Description
                                    detail.content?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                    }

                                    // Line selector
                                    if (detail.playFrom.size > 1) {
                                        Text(
                                            text = "播放线路",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(items = detail.playWithIndex) { (index, name) ->
                                                FilterChip(
                                                    selected = index == selectedLine,
                                                    onClick = { viewModel.selectLine(index) },
                                                    label = { Text(name) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // Episode section header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "选集",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Row(
                                                modifier = Modifier.clickable { sortAscending = !sortAscending },
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (sortAscending) "正序" else "倒序",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.SwapVert,
                                                    contentDescription = "排序",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.clickable { episodesExpanded = !episodesExpanded },
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (episodesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = if (episodesExpanded) "收起" else "展开",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (episodesExpanded) "收起" else "展开",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Episode grid
                            item {
                                val episodes = detail.episodes.getOrNull(selectedLine) ?: emptyList()
                                val sortedEpisodes = if (sortAscending) episodes else episodes.reversed()
                                if (sortedEpisodes.isNotEmpty()) {
                                    // 手机模式：使用网格
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .heightIn(
                                                min = if (episodesExpanded) 0.dp else 60.dp,
                                                max = if (episodesExpanded) 300.dp else 60.dp
                                            )
                                    ) {
                                        itemsIndexed(items = sortedEpisodes) { index, episode ->
                                            val isCurrentEpisode = episode.name == currentEpisodeName
                                            val originalIndex = if (sortAscending) index else episodes.size - 1 - index
                                            val isCached = originalIndex in cachedEpisodeIndices
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1.5f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isCurrentEpisode)
                                                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                    else
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                    border = if (isCurrentEpisode)
                                                        androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4CAF50))
                                                    else
                                                        null,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable { viewModel.playEpisode(episode) }
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = episode.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isCurrentEpisode) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                // Cached indicator dot
                                                if (isCached) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(8.dp)
                                                            .background(
                                                                color = Color(0xFFFFD700),
                                                                shape = CircleShape
                                                            )
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
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
