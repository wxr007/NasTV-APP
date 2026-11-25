package com.nastv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nastv.ui.theme.NasTVPrimaryGold
import com.nastv.ui.theme.NasTVSurface
import com.nastv.ui.theme.NasTVTextSecondary
import com.nastv.utils.generateQrCode

/**
 * TV 端扫码输入搜索词：展示二维码，手机扫码后在网页里输入，关键词会回传到 TV。
 * [url] 为 null 表示服务启动失败（没有局域网 IP 或端口全被占用）。
 */
@Composable
fun TvQrInputDialog(
    url: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NasTVSurface,
        titleContentColor = Color.White,
        title = {
            Text(
                "扫码输入搜索词",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (url == null) {
                    Text(
                        text = "无法启动扫码输入。请确认电视已连接局域网，或稍后重试（端口可能被占用）。",
                        color = NasTVTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val qrSize = 240.dp
                    val px = with(LocalDensity.current) { qrSize.roundToPx() }
                    val qr = remember(url, px) { generateQrCode(url, px) }

                    Box(
                        modifier = Modifier
                            .size(qrSize)
                            .border(2.dp, NasTVPrimaryGold, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qr != null) {
                            Image(
                                bitmap = qr,
                                contentDescription = "扫码输入二维码",
                                modifier = Modifier
                                    .size(qrSize - 20.dp)
                                    .padding(4.dp)
                            )
                        } else {
                            Text("二维码生成失败", color = NasTVTextSecondary, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = url,
                        color = NasTVPrimaryGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "手机与电视连同一个 Wi-Fi 后扫码，在打开的网页里输入片名，电视会立即开始搜索。",
                        color = NasTVTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            var closeFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .onFocusChanged { closeFocused = it.isFocused }
                    .clickable { onDismiss() }
                    .border(
                        2.dp,
                        if (closeFocused) NasTVPrimaryGold else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("关闭", color = Color.White)
            }
        }
    )
}
