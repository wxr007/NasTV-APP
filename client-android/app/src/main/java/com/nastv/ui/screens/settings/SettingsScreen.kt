package com.nastv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.theme.LocalIsTv
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    val useImageProxy by viewModel.useImageProxy.collectAsState()
    val seekDuration by viewModel.seekDuration.collectAsState()
    val forceTvMode by viewModel.forceTvMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
    val bufferMaxDuration by viewModel.bufferMaxDuration.collectAsState()
    val bufferMinDuration by viewModel.bufferMinDuration.collectAsState()
    val username by viewModel.username.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val serverName by viewModel.serverName.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    var scrollTarget by remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollTarget) {
        if (scrollTarget > 0) {
            scrollState.animateScrollTo(scrollTarget)
        }
    }

    // TV端遥控器退出键返回
    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    if (!isTv) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(if (isTv) 32.dp else 16.dp)
                .verticalScroll(scrollState)
        ) {
            // 账户信息卡片
            Text(
                text = "账户",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 12.dp))

            // 账户信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isTv) 24.dp else 16.dp)
                ) {
                    // 用户头像和名称
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 用户头像
                        Box(
                            modifier = Modifier
                                .size(if (isTv) 64.dp else 56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username?.take(1)?.uppercase() ?: "U",
                                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(if (isTv) 16.dp else 12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = username ?: "未登录",
                                style = if (isTv) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (userRole != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 角色标签
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = when (userRole) {
                                                "admin" -> "管理员"
                                                "user" -> "普通用户"
                                                else -> userRole ?: "未知"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 服务器信息
                    if (serverName != null) {
                        Spacer(modifier = Modifier.height(if (isTv) 16.dp else 12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(if (isTv) 16.dp else 12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isTv) 24.dp else 20.dp)
                            )
                            Column {
                                Text(
                                    text = "登录服务器",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = serverName ?: "",
                                    style = if (isTv) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 12.dp))

            var isLogoutFocused by remember { mutableStateOf(false) }
            var logoutYPos by remember { mutableFloatStateOf(0f) }
            val logoutFocusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isTv) Modifier
                            .focusRequester(logoutFocusRequester)
                            .focusProperties { canFocus = true }
                            .onFocusChanged { focusState ->
                                isLogoutFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    val targetPx = (logoutYPos - 400).roundToInt().coerceAtLeast(0)
                                    scrollTarget = targetPx.coerceAtLeast(0)
                                }
                            }
                            .onGloballyPositioned { logoutYPos = it.positionInWindow().y }
                            .clickable {
                                viewModel.logout()
                                onLogout()
                            }
                            .background(
                                if (isLogoutFocused) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.error,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                if (isLogoutFocused) 3.dp else 0.dp,
                                if (isLogoutFocused) Color(0xFFFFD700) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 12.dp)
                        else Modifier
                    )
                    .then(
                        if (!isTv) Modifier
                            .clickable {
                                viewModel.logout()
                                onLogout()
                            }
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "退出登录",
                    style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(if (isTv) 48.dp else 24.dp))

            // ========== 播放设置 ==========
            Text(
                text = "播放设置",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            // Seek duration
            Text(
                text = "快进/快退时长：${seekDuration}秒",
                style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(if (isTv) 12.dp else 4.dp))

            var isSeekSliderFocused by remember { mutableStateOf(false) }
            var seekYPos by remember { mutableFloatStateOf(0f) }
            val seekSliderFocusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isTv) Modifier
                            .focusRequester(seekSliderFocusRequester)
                            .focusProperties { canFocus = true }
                            .onFocusChanged { focusState ->
                                isSeekSliderFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    val targetPx = (seekYPos - 400).roundToInt().coerceAtLeast(0)
                                    scrollTarget = targetPx.coerceAtLeast(0)
                                }
                            }
                            .onGloballyPositioned { seekYPos = it.positionInWindow().y }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            viewModel.setSeekDuration((seekDuration - 1).coerceAtLeast(5))
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            viewModel.setSeekDuration((seekDuration + 1).coerceAtMost(30))
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(
                                if (isSeekSliderFocused) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isSeekSliderFocused) 3.dp else 0.dp,
                                if (isSeekSliderFocused) Color(0xFFFFD700) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                        else Modifier
                    )
            ) {
                Slider(
                    value = seekDuration.toFloat(),
                    onValueChange = { viewModel.setSeekDuration(it.roundToInt()) },
                    valueRange = 5f..30f,
                    steps = 24,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("5秒", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("30秒", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(if (isTv) 24.dp else 12.dp))

            // 最大缓冲时长
            Text(
                text = "最大缓冲：${bufferMaxDuration}秒",
                style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(if (isTv) 12.dp else 4.dp))

            var isMaxBufferFocused by remember { mutableStateOf(false) }
            var maxBufferYPos by remember { mutableFloatStateOf(0f) }
            val maxBufferFocusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isTv) Modifier
                            .focusRequester(maxBufferFocusRequester)
                            .focusProperties { canFocus = true }
                            .onFocusChanged { focusState ->
                                isMaxBufferFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    val targetPx = (maxBufferYPos - 400).roundToInt().coerceAtLeast(0)
                                    scrollTarget = targetPx.coerceAtLeast(0)
                                }
                            }
                            .onGloballyPositioned { maxBufferYPos = it.positionInWindow().y }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            viewModel.updateBufferSettings(bufferMinDuration, (bufferMaxDuration - 10).coerceAtLeast(50))
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            viewModel.updateBufferSettings(bufferMinDuration, (bufferMaxDuration + 10).coerceAtMost(300))
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(
                                if (isMaxBufferFocused) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isMaxBufferFocused) 3.dp else 0.dp,
                                if (isMaxBufferFocused) Color(0xFFFFD700) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                        else Modifier
                    )
            ) {
                Slider(
                    value = bufferMaxDuration.toFloat(),
                    onValueChange = { viewModel.updateBufferSettings(bufferMinDuration, it.roundToInt()) },
                    valueRange = 50f..300f,
                    steps = 24,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "提示: 缓冲越大播放越流畅，但占用内存越多",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(if (isTv) 20.dp else 12.dp))

            // 最小缓冲时长
            Text(
                text = "最小缓冲：${bufferMinDuration}秒",
                style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(if (isTv) 12.dp else 4.dp))

            var isMinBufferFocused by remember { mutableStateOf(false) }
            var minBufferYPos by remember { mutableFloatStateOf(0f) }
            val minBufferFocusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isTv) Modifier
                            .focusRequester(minBufferFocusRequester)
                            .focusProperties { canFocus = true }
                            .onFocusChanged { focusState ->
                                isMinBufferFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    val targetPx = (minBufferYPos - 400).roundToInt().coerceAtLeast(0)
                                    scrollTarget = targetPx.coerceAtLeast(0)
                                }
                            }
                            .onGloballyPositioned { minBufferYPos = it.positionInWindow().y }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            viewModel.updateBufferSettings((bufferMinDuration - 10).coerceAtLeast(20), bufferMaxDuration)
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            viewModel.updateBufferSettings((bufferMinDuration + 10).coerceAtMost(150), bufferMaxDuration)
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .background(
                                if (isMinBufferFocused) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isMinBufferFocused) 3.dp else 0.dp,
                                if (isMinBufferFocused) Color(0xFFFFD700) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                        else Modifier
                    )
            ) {
                Slider(
                    value = bufferMinDuration.toFloat(),
                    onValueChange = { viewModel.updateBufferSettings(it.roundToInt(), bufferMaxDuration) },
                    valueRange = 20f..150f,
                    steps = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "提示: 最小缓冲确保视频播放前有足够的缓存数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(if (isTv) 24.dp else 12.dp))

            // 广告屏蔽开关
            var isAdBlockFocused by remember { mutableStateOf(false) }
            var adBlockYPos by remember { mutableFloatStateOf(0f) }
            val adBlockFocusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isTv) 8.dp else 4.dp)
                    .then(
                        if (isTv) Modifier
                            .focusRequester(adBlockFocusRequester)
                            .focusProperties { canFocus = true }
                            .onFocusChanged { focusState ->
                                isAdBlockFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    val targetPx = (adBlockYPos - 400).roundToInt().coerceAtLeast(0)
                                    scrollTarget = targetPx.coerceAtLeast(0)
                                }
                            }
                            .onGloballyPositioned { adBlockYPos = it.positionInWindow().y }
                            .background(
                                if (isAdBlockFocused) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isAdBlockFocused) 3.dp else 0.dp,
                                if (isAdBlockFocused) Color(0xFFFFD700) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                        else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "广告屏蔽",
                            style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (adBlockEnabled) "播放时过滤 m3u8 中的广告片段" else "关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = adBlockEnabled,
                        onCheckedChange = { viewModel.setAdBlockEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTv) 48.dp else 24.dp))

            // ========== 图片设置 ==========
            Text(
                text = "图片设置",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            // 图片代理
            var isImgProxyFocused by remember { mutableStateOf(false) }
            var imgProxyYPos by remember { mutableFloatStateOf(0f) }
            val imgProxyFocusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isTv) 8.dp else 4.dp)
                    .then(
                        if (isTv) Modifier
                            .focusRequester(imgProxyFocusRequester)
                            .focusProperties { canFocus = true }
                            .onFocusChanged { focusState ->
                                isImgProxyFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    val targetPx = (imgProxyYPos - 400).roundToInt().coerceAtLeast(0)
                                    scrollTarget = targetPx.coerceAtLeast(0)
                                }
                            }
                            .onGloballyPositioned { imgProxyYPos = it.positionInWindow().y }
                            .background(
                                if (isImgProxyFocused) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isImgProxyFocused) 3.dp else 0.dp,
                                if (isImgProxyFocused) Color(0xFFFFD700) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                        else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "图片代理",
                            style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (useImageProxy) "通过后端代理加载图片" else "直接从CDN加载图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useImageProxy,
                        onCheckedChange = { viewModel.setUseImageProxy(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTv) 48.dp else 24.dp))

            // ========== 开发设置 ==========
            Text(
                text = "开发设置",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            // 强制TV模式
            var isForceTvFocused by remember { mutableStateOf(false) }
            val forceTvFocusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isTv) 8.dp else 4.dp)
                    .then(
                        if (isTv) Modifier
                            .focusRequester(forceTvFocusRequester)
                            .focusProperties { canFocus = true }
                            .onFocusChanged { isForceTvFocused = it.isFocused }
                            .background(
                                if (isForceTvFocused) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isForceTvFocused) 3.dp else 0.dp,
                                if (isForceTvFocused) Color(0xFFFFD700) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                        else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "强制TV模式",
                            style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (forceTvMode) "强制使用TV布局（重启应用生效）" else "使用设备默认布局",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = forceTvMode,
                        onCheckedChange = { viewModel.setForceTvMode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTv) 48.dp else 24.dp))

            Text(
                text = "关于",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            Text(
                text = "NasTV v1.0",
                style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!isTv) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "外观",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "主题",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (themeMode) {
                                "dark" -> "暗色主题"
                                "light" -> "亮色主题"
                                else -> "跟随系统"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showThemeDialog = true }) {
                        Text("更改")
                    }
                }
            }
        }
    }
    
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    listOf(
                        "system" to "跟随系统",
                        "dark" to "暗色主题",
                        "light" to "亮色主题"
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(value)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == value,
                                onClick = {
                                    viewModel.setThemeMode(value)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
