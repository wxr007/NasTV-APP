package com.nastv.ui.screens.settings

import androidx.compose.foundation.background
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
    val username by viewModel.username.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val serverName by viewModel.serverName.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
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

            Button(
                onClick = { 
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("退出登录", style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(if (isTv) 48.dp else 24.dp))

            Text(
                text = "播放设置",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            // Seek duration setting
            Text(
                text = "快进/快退时长：${seekDuration}秒",
                style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
            )

            Slider(
                value = seekDuration.toFloat(),
                onValueChange = { viewModel.setSeekDuration(it.roundToInt()) },
                valueRange = 5f..30f,
                steps = 24, // 5..30 with step 1 = 25 stops = 24 steps
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("5秒", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("30秒", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(if (isTv) 24.dp else 12.dp))

            // 广告屏蔽开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isTv) 16.dp else 8.dp),
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

            Spacer(modifier = Modifier.height(if (isTv) 48.dp else 24.dp))

            Text(
                text = "图片设置",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isTv) 16.dp else 8.dp),
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

            Spacer(modifier = Modifier.height(if (isTv) 48.dp else 24.dp))

            Text(
                text = "开发设置",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isTv) 16.dp else 8.dp),
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
