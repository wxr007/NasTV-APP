package com.nastv.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nastv.ui.screens.search.ApiProvider
import com.nastv.ui.theme.NasTVPrimaryGold
import com.nastv.ui.theme.NasTVSurface
import com.nastv.ui.theme.NasTVTextSecondary

@Composable
fun TvApiProviderDialog(
    currentProvider: ApiProvider,
    onDismiss: () -> Unit,
    onConfirm: (ApiProvider) -> Unit
) {
    var selected by remember { mutableStateOf(currentProvider) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NasTVSurface,
        titleContentColor = Color.White,
        title = {
            Text(
                "选择热词/联想词来源",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                // 腾讯
                ProviderOption(
                    label = "腾讯",
                    description = "热词：360kan | 联想：腾讯 smartbox",
                    isSelected = selected == ApiProvider.TENCENT,
                    onClick = { selected = ApiProvider.TENCENT }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 爱奇艺+360kan
                ProviderOption(
                    label = "爱奇艺 + 360kan",
                    description = "热词：360kan | 联想：爱奇艺 suggest",
                    isSelected = selected == ApiProvider.IQIYI_360,
                    onClick = { selected = ApiProvider.IQIYI_360 }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 豆瓣
                ProviderOption(
                    label = "豆瓣",
                    description = "热词：360kan | 联想：豆瓣 suggest",
                    isSelected = selected == ApiProvider.DOUBAN,
                    onClick = { selected = ApiProvider.DOUBAN }
                )
            }
        },
        confirmButton = {
            var confirmFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .onFocusChanged { confirmFocused = it.isFocused }
                    .clickable { onConfirm(selected) }
                    .border(2.dp, if (confirmFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("确定", color = NasTVPrimaryGold)
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

@Composable
private fun ProviderOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(2.dp, if (isFocused) NasTVPrimaryGold else Color.Transparent, RoundedCornerShape(8.dp))
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = NasTVPrimaryGold,
                unselectedColor = NasTVTextSecondary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = description,
                color = NasTVTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
