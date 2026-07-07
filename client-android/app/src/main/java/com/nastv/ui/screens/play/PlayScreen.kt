package com.nastv.ui.screens.play

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
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

    // Player state for custom controls
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    var isBuffering by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableStateOf(0L) }

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
            } else if (isTv) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Keep screen on during video playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 全屏时按返回键退出全屏而不是退出页面
    androidx.activity.compose.BackHandler(enabled = isFullscreen) {
        viewModel.toggleFullscreen()
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) { Text("返回") }
                    }
                }
            }
            videoDetail != null -> {
                videoDetail?.let { detail ->
                        val context = LocalContext.current

                        val bandwidthMeter = remember {
                            DefaultBandwidthMeter.Builder(context).build()
                        }

                        var transferredBytes by remember { mutableLongStateOf(0L) }
                        var lastSpeedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
                        var lastTransferredBytes by remember { mutableLongStateOf(0L) }

                        val transferListener = object : TransferListener {
                            override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
                                android.util.Log.d("SpeedDebug", "onTransferInitializing: isNetwork=$isNetwork, source=${source.javaClass.simpleName}")
                            }
                            override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
                                android.util.Log.d("SpeedDebug", "onTransferStart: isNetwork=$isNetwork, source=${source.javaClass.simpleName}")
                            }
                            override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
                                if (isNetwork) {
                                    transferredBytes += bytesTransferred.toLong()
                                }
                                android.util.Log.d("SpeedDebug", "onBytesTransferred: isNetwork=$isNetwork, bytes=$bytesTransferred, total=$transferredBytes")
                            }
                            override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
                                android.util.Log.d("SpeedDebug", "onTransferEnd: isNetwork=$isNetwork")
                            }
                        }

                        val dataSourceFactory = DefaultDataSource.Factory(context)
                            .setTransferListener(transferListener)

                        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

                        val exoPlayer = remember {
                            ExoPlayer.Builder(context)
                                .setMediaSourceFactory(mediaSourceFactory)
                                .setBandwidthMeter(bandwidthMeter)
                                .build().apply {
                                playWhenReady = true
                                addListener(object : Player.Listener {
                                    override fun onIsPlayingChanged(playing: Boolean) {
                                        android.util.Log.d("ExoPlayer", "onIsPlayingChanged: $playing")
                                        isPlaying = playing
                                    }
                                    override fun onPlaybackStateChanged(state: Int) {
                                        val stateStr = when (state) {
                                            Player.STATE_IDLE -> "IDLE"
                                            Player.STATE_BUFFERING -> "BUFFERING"
                                            Player.STATE_READY -> "READY"
                                            Player.STATE_ENDED -> "ENDED"
                                            else -> "UNKNOWN"
                                        }
                                        android.util.Log.d("ExoPlayer", "onPlaybackStateChanged: $stateStr, duration=$duration")
                                        if (state == Player.STATE_READY) totalDuration = duration
                                        isBuffering = state == Player.STATE_BUFFERING
                                    }
                                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                        android.util.Log.e("ExoPlayer", "onPlayerError: ${error.errorCodeName}, ${error.message}", error)
                                    }
                                })
                            }
                        }

                        LaunchedEffect(exoPlayer, bandwidthMeter) {
                            while (true) {
                                delay(500)
                                val now = System.currentTimeMillis()
                                val timeDiff = now - lastSpeedTime
                                val byteDiff = transferredBytes - lastTransferredBytes
                                val manualSpeed = if (timeDiff > 0) byteDiff * 1000L / timeDiff else 0L
                                val bitrateEstimate = bandwidthMeter.bitrateEstimate
                                val meterSpeed = if (transferredBytes > 0 && byteDiff > 0 && bitrateEstimate > 0 && bitrateEstimate < Long.MAX_VALUE) {
                                    bitrateEstimate / 8
                                } else {
                                    0L
                                }
                                downloadSpeed = maxOf(manualSpeed, meterSpeed)
                                android.util.Log.d("SpeedDebug", "speed: manual=${formatSpeed(manualSpeed)}, meter=${formatSpeed(meterSpeed)}, final=${formatSpeed(downloadSpeed)}, totalTransferred=$transferredBytes, bitrateEstimate=$bitrateEstimate")
                                lastSpeedTime = now
                                lastTransferredBytes = transferredBytes
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
                            // 缓存 URL 不以 .m3u8 结尾，需要显式指定 MIME 类型
                            val isCacheUrl = url.contains("/api/cache/m3u8/")
                            val mediaItem = if (isCacheUrl) {
                                MediaItem.Builder()
                                    .setUri(url)
                                    .setMimeType("application/vnd.apple.mpegurl")
                                    .build()
                            } else {
                                MediaItem.fromUri(url)
                            }
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                        }
                    }

                    // Restore play position from history
                    var positionRestored by remember { mutableStateOf(false) }
                    LaunchedEffect(restorePosition) {
                        restorePosition?.let { pos ->
                            if (!positionRestored) {
                                var attempts = 0
                                while (exoPlayer.duration <= 0 && attempts < 10) { delay(500); attempts++ }
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
                                sourceId = detail.sourceId, videoId = detail.id, episode = name,
                                videoName = detail.name, pic = detail.pic, progress = 0f, duration = null,
                                totalEpisodes = detail.episodes.getOrNull(selectedLine)?.size,
                                activeSource = selectedLine,
                                activeEpisodeIndex = detail.episodes.getOrNull(selectedLine)
                                    ?.indexOfFirst { it.name == name }?.takeIf { it >= 0 }
                            )
                        }
                    }

                    // Periodically update play progress
                    LaunchedEffect(currentEpisodeName, detail.id) {
                        while (true) {
                            if (isPlaying && totalDuration > 0) {
                                viewModel.recordPlayHistory(
                                    sourceId = detail.sourceId, videoId = detail.id,
                                    episode = currentEpisodeName ?: "", videoName = detail.name,
                                    pic = detail.pic,
                                    progress = currentPosition.toFloat() / 1000f,
                                    duration = totalDuration.toFloat() / 1000f,
                                    totalEpisodes = detail.episodes.getOrNull(selectedLine)?.size,
                                    activeSource = selectedLine,
                                    activeEpisodeIndex = detail.episodes.getOrNull(selectedLine)
                                        ?.indexOfFirst { it.name == currentEpisodeName }?.takeIf { it >= 0 }
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

                    // Common gesture handler
                    val gestureModifier = Modifier
                        .onSizeChanged { playerWidth = it.width.toFloat() }
                        .pointerInput(seekDuration) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible },
                                onDoubleTap = { offset ->
                                    val fraction = if (playerWidth > 0) offset.x / playerWidth else 0.5f
                                    when {
                                        fraction < 0.2f -> {
                                            val durMs = seekDuration * 1000L
                                            exoPlayer.seekTo((exoPlayer.currentPosition - durMs).coerceAtLeast(0))
                                            seekIndicatorText = "◀◀ ${seekDuration}s"
                                        }
                                        fraction > 0.8f -> {
                                            val durMs = seekDuration * 1000L
                                            exoPlayer.seekTo((exoPlayer.currentPosition + durMs).coerceAtMost(exoPlayer.duration))
                                            seekIndicatorText = "${seekDuration}s ▶▶"
                                        }
                                        else -> {
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
                        // Fullscreen mode (shared between TV and phone)
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black)
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
                            Box(modifier = Modifier.fillMaxSize().then(gestureModifier))

                            seekIndicatorText?.let { text ->
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = text, color = Color.White, fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            if (controlsVisible) {
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

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 32.dp)
                                ) {
                                    Slider(
                                        value = if (isSeeking) seekPosition
                                            else if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f,
                                        onValueChange = { seekPosition = it; isSeeking = true },
                                        onValueChangeFinished = {
                                            exoPlayer.seekTo((seekPosition * totalDuration).toLong())
                                            currentPosition = (seekPosition * totalDuration).toLong()
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
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = formatTime(currentPosition), style = MaterialTheme.typography.labelMedium, color = Color.White)
                                        Text(text = formatTime(totalDuration), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    } else if (isTv) {
                        // TV 端布局
                        TvPlayContent(
                            detail = detail,
                            exoPlayer = exoPlayer,
                            gestureModifier = Modifier,  // TV模式不使用触屏手势
                            seekIndicatorText = seekIndicatorText,
                            controlsVisible = controlsVisible,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            totalDuration = totalDuration,
                            isSeeking = isSeeking,
                            seekPosition = seekPosition,
                            isFullscreen = isFullscreen,
                            selectedLine = selectedLine,
                            currentEpisodeName = currentEpisodeName,
                            isFavorite = isFavorite,
                            cachedEpisodeIndices = cachedEpisodeIndices,
                            isBuffering = isBuffering,
                            downloadSpeed = downloadSpeed,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            onSelectLine = { viewModel.selectLine(it) },
                            onPlayEpisode = { viewModel.playEpisode(it) },
                            onToggleFullscreen = { viewModel.toggleFullscreen() },
                            onSeekFinished = { pos ->
                                exoPlayer.seekTo(pos)
                                currentPosition = pos
                                isSeeking = false
                            },
                            onSeekChange = { seekPosition = it },
                            onSeekStart = { isSeeking = true },
                            onPlayPause = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            seekDuration = seekDuration,
                        )
                    } else {
                        // 手机端布局
                        PhonePlayContent(
                            detail = detail,
                            exoPlayer = exoPlayer,
                            gestureModifier = gestureModifier,
                            seekIndicatorText = seekIndicatorText,
                            controlsVisible = controlsVisible,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            totalDuration = totalDuration,
                            isSeeking = isSeeking,
                            seekPosition = seekPosition,
                            isFullscreen = isFullscreen,
                            selectedLine = selectedLine,
                            currentEpisodeName = currentEpisodeName,
                            isFavorite = isFavorite,
                            cachedEpisodeIndices = cachedEpisodeIndices,
                            isBuffering = isBuffering,
                            downloadSpeed = downloadSpeed,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            onSelectLine = { viewModel.selectLine(it) },
                            onPlayEpisode = { viewModel.playEpisode(it) },
                            onToggleFullscreen = { viewModel.toggleFullscreen() },
                            onSeekFinished = { pos ->
                                exoPlayer.seekTo(pos)
                                currentPosition = pos
                                isSeeking = false
                            },
                            onSeekChange = { seekPosition = it },
                            onSeekStart = { isSeeking = true },
                            onPlayPause = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            paddingValues = paddingValues,
                        )
                    }
                }
            }
        }
    }
}

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1_000_000 -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000.0)
        bytesPerSecond >= 1_000 -> String.format("%.1f KB/s", bytesPerSecond / 1_000.0)
        else -> "$bytesPerSecond B/s"
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
