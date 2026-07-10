package com.nastv.ui.screens.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 自定义视频进度条，支持显示缓冲进度
 *
 * @param value 当前播放进度 (0.0 - 1.0)
 * @param bufferedValue 缓冲进度 (0.0 - 1.0)
 * @param onValueChange 进度变化回调
 * @param onValueChangeFinished 拖动结束回调
 * @param modifier Modifier
 * @param enabled 是否可交互
 * @param trackHeight 轨道高度
 * @param thumbRadius 滑块半径
 * @param playedColor 已播放进度颜色
 * @param bufferedColor 缓冲进度颜色
 * @param unplayedColor 未播放轨道颜色
 * @param thumbColor 滑块颜色
 */
@Composable
fun VideoSeekBar(
    value: Float,
    bufferedValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackHeight: Dp = 6.dp,
    thumbRadius: Dp = 8.dp,
    playedColor: Color = Color.White,
    bufferedColor: Color = Color(0xFF4CAF50),
    unplayedColor: Color = Color.White.copy(alpha = 0.3f),
    thumbColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .height(thumbRadius * 2)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    },
                    onHorizontalDrag = { change, _ ->
                        val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    },
                    onDragEnd = {
                        onValueChangeFinished()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbRadius * 2)
        ) {
            val trackHeightPx = trackHeight.toPx()
            val thumbRadiusPx = thumbRadius.toPx()
            val centerY = size.height / 2

            // 绘制未播放轨道
            drawProgressBar(
                progress = 1f,
                color = unplayedColor,
                trackHeight = trackHeightPx,
                centerY = centerY
            )

            // 绘制缓冲进度
            drawProgressBar(
                progress = bufferedValue,
                color = bufferedColor,
                trackHeight = trackHeightPx,
                centerY = centerY
            )

            // 绘制已播放进度
            drawProgressBar(
                progress = value,
                color = playedColor,
                trackHeight = trackHeightPx,
                centerY = centerY
            )

            // 绘制滑块
            val thumbX = size.width * value
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

private fun DrawScope.drawProgressBar(
    progress: Float,
    color: Color,
    trackHeight: Float,
    centerY: Float
) {
    val width = size.width * progress
    drawRect(
        color = color,
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight)
    )
}
