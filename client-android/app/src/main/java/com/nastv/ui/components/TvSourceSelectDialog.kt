package com.nastv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nastv.data.model.VideoSource
import com.nastv.ui.theme.NasTVPrimaryGold
import com.nastv.ui.theme.NasTVSurface
import com.nastv.ui.theme.NasTVTextSecondary

@Composable
fun TvSourceSelectDialog(
    sources: List<VideoSource>,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val checkedState = remember(sources, selectedIds) {
        val map = mutableStateMapOf<String, Boolean>()
        sources.forEach { map[it.id] = selectedIds.contains(it.id) }
        map
    }

    val columns = if (sources.size <= 3) 2 else if (sources.size <= 6) 3 else 4

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NasTVSurface,
        titleContentColor = Color.White,
        title = {
            Text(
                "选择搜索站源",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    var selectAllFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .onFocusChanged { selectAllFocused = it.isFocused }
                            .clickable { sources.forEach { checkedState[it.id] = true } }
                            .border(2.dp, if (selectAllFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("全选", color = NasTVPrimaryGold)
                    }
                    var cancelAllFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .onFocusChanged { cancelAllFocused = it.isFocused }
                            .clickable { sources.forEach { checkedState[it.id] = false } }
                            .border(2.dp, if (cancelAllFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("取消全选", color = NasTVPrimaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sources, key = { it.id }) { source ->
                        val isChecked = checkedState[source.id] ?: false
                        var isFocused by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .onFocusChanged { isFocused = it.isFocused }
                                .border(2.dp, if (isFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(6.dp))
                                .then(
                                    if (isChecked)
                                        Modifier.background(
                                            NasTVPrimaryGold.copy(alpha = 0.1f),
                                            RoundedCornerShape(6.dp)
                                        )
                                    else Modifier
                                )
                                .clickable {
                                    checkedState[source.id] = !isChecked
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checkedState[source.id] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NasTVPrimaryGold,
                                    uncheckedColor = NasTVTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = source.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val checkedCount = checkedState.values.count { it }
                Text(
                    text = "已选择 $checkedCount / ${sources.size} 个站源",
                    color = NasTVTextSecondary,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            var confirmFocused by remember { mutableStateOf(false) }
            val hasSelection = checkedState.values.any { it }
            Box(
                modifier = Modifier
                    .onFocusChanged { confirmFocused = it.isFocused }
                    .then(
                        if (hasSelection) Modifier.clickable {
                            val selected = checkedState.filter { it.value }.keys
                            if (selected.isNotEmpty()) onConfirm(selected)
                        } else Modifier
                    )
                    .border(2.dp, if (confirmFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    "确定",
                    color = if (hasSelection) NasTVPrimaryGold else NasTVTextSecondary
                )
            }
        },
        dismissButton = {
            var cancelFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .onFocusChanged { cancelFocused = it.isFocused }
                    .clickable { onDismiss() }
                    .border(2.dp, if (cancelFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("取消", color = NasTVTextSecondary)
            }
        }
    )
}
