package com.nastv.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nastv.ui.theme.NasTVTextPrimary
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CLOCK_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * TV 端顶栏系统时间（HH:mm）。
 * 每个整分钟刷新一次，避免每秒重组；跨天后仍走同一循环，不会卡在旧日期。
 */
@Composable
fun TvClock(
    modifier: Modifier = Modifier,
    color: Color = NasTVTextPrimary,
    fontSize: TextUnit = 20.sp
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now()
            now = current
            // 睡到下一个整分钟再刷新
            val msToNextMinute = 60_000L - (current.second * 1000L + current.nano / 1_000_000L)
            delay(msToNextMinute.coerceAtLeast(200L))
        }
    }

    Text(
        text = now.format(CLOCK_FORMATTER),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium
    )
}
