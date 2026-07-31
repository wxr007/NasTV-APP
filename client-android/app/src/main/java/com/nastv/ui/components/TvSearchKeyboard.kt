@file:OptIn(ExperimentalComposeUiApi::class)
package com.nastv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nastv.ui.theme.NasTVPrimaryGold

@Composable
fun TvSearchKeyboard(
    onKeyPress: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    keySize: Dp = 40.dp
) {
    val keys = remember {
        listOf(
            "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T", "U",
            "V", "W", "X", "Y", "Z", "0", "1",
            "2", "3", "4", "5", "6", "7", "8", "9"
        )
    }

    Column(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            // 字母/数字键盘：36键
            items(items = keys) { key ->
                KeyboardKey(
                    text = key,
                    onClick = { onKeyPress(key) },
                    size = keySize
                )
            }

            // 底部功能键：清空（占3列）
            item(span = { GridItemSpan(3) }) {
                KeyboardKey(
                    text = "清空",
                    onClick = onClear,
                    size = keySize,
                    isAction = true
                )
            }
            // 删除（占3列）
            item(span = { GridItemSpan(3) }) {
                KeyboardKey(
                    text = "删除",
                    onClick = onDelete,
                    size = keySize,
                    isAction = true
                )
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    text: String,
    onClick: () -> Unit,
    size: Dp,
    isAction: Boolean = false
) {
    val goldColor = NasTVPrimaryGold
    var isFocused by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(3.dp)
            .width(size)
            .height(size)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .then(
                if (isFocused) Modifier.border(2.dp, goldColor, RoundedCornerShape(6.dp))
                else Modifier
            )
            .then(
                if (isAction) Modifier.focusProperties {
                    exit = { FocusRequester.Cancel }
                } else Modifier
            )
            .then(
                if (isAction) {
                    Modifier.background(
                        Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(6.dp)
                    )
                } else Modifier
            ),
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = Color.White,
            fontSize = if (isAction) 13.sp else 15.sp,
            fontWeight = if (isAction) FontWeight.Normal else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
