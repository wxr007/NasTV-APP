package com.nastv.ui.screens.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
 */
@Composable
fun VideoSeekBar(
    value: Float,
    bufferedValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackHeight: Dp = 6.dp,
    thumbRadius: Dp = 8.dp,
    playedColor: Color = Color.White,
    bufferedColor: Color = Color(0xFF4CAF50),
    unplayedColor: Color = Color.White.copy(alpha = 0.3f),
    thumbColor: Color = Color.White
) {
    // 使用 rememberUpdatedState 保持回调引用稳定，避免 pointerInput 因 recomposition 重启
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    Box(modifier = modifier.height(thumbRadius * 2)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbRadius * 2)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                        currentOnValueChange(newValue)
                        currentOnValueChangeFinished(newValue)
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    var lastDragValue = value
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                            currentOnValueChange(newValue)
                            lastDragValue = newValue
                        },
                        onHorizontalDrag = { change, _ ->
                            val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                            currentOnValueChange(newValue)
                            lastDragValue = newValue
                        },
                        onDragEnd = {
                            currentOnValueChangeFinished(lastDragValue)
                        }
                    )
                }
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
