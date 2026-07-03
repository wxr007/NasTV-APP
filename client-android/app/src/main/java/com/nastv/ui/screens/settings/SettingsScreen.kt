package com.nastv.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nastv.ui.theme.LocalIsTv
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isTv = LocalIsTv.current
    val useImageProxy by viewModel.useImageProxy.collectAsState()
    val seekDuration by viewModel.seekDuration.collectAsState()
    val forceTvMode by viewModel.forceTvMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // 账户 - 放在顶部
            Text(
                text = "账户",
                style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 8.dp))

            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
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
        }
    }
}
