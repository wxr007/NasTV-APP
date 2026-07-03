package com.nastv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nastv.data.model.Video
import com.nastv.utils.ImageUrlHelper

@Composable
fun TVVideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = if (isFocused) Color(0xFFFFD700) else Color(0xFF666666)

    Column(
        modifier = modifier
            .width(180.dp)
            .focusProperties { canFocus = true }
            .clickable(onClick = onClick)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .background(borderColor, RoundedCornerShape(8.dp))
            .padding(2.dp)
            .background(Color.Transparent, RoundedCornerShape(6.dp))
    ) {
        Box {
            // Poster (vertical, 2:3 aspect ratio)
            val imageUrl = if (ImageUrlHelper.isUseImageProxy()) {
                video.pic?.let { pic ->
                    val relativeUrl = "/api/play/image?url=${java.net.URLEncoder.encode(pic, "UTF-8")}&sourceId=${video.sourceId}&videoId=${video.id}"
                    ImageUrlHelper.toFullUrl(relativeUrl)
                }
            } else {
                video.pic
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = video.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                contentScale = ContentScale.Crop
            )

            // Remark badge at top-left
            video.remark?.let { remark ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = remark,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Title
        Text(
            text = video.name,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
