package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.VideoInfo
import com.example.util.FormatUtils

/**
 * Empty state: one job, one action. Headline plus a big standard
 * plus button — nothing else. No chips, no badges, no format list,
 * no claims about the device. The whole block is a plain centered
 * column — no card, no border — so it can sit vertically centered
 * in the screen instead of crowding the top. It looks native
 * because it is only stock Material 3: the theme's own colors
 * (dynamic color on the phone) and the Material type scale.
 *
 * Source loaded: a standard Card holding a standard ListItem.
 */
@Composable
fun VideoPickerCard(
    videoInfo: VideoInfo?,
    onPickVideoClick: () -> Unit,
    onTrySampleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (videoInfo == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("pick_video_card")
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Select Video to Convert",
                style = MaterialTheme.typography.headlineSmall
            )

            FilledTonalIconButton(
                onClick = onPickVideoClick,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("select_video_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Select video to convert",
                    modifier = Modifier.size(36.dp)
                )
            }

            onTrySampleClick?.let { onSample ->
                TextButton(
                    onClick = onSample,
                    modifier = Modifier.testTag("try_sample_clip_button")
                ) {
                    Text("Try sample clip")
                }
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("selected_video_card")
        ) {
            ListItem(
                leadingContent = {
                    Box(modifier = Modifier.size(56.dp)) {
                        if (videoInfo.thumbnailBitmap != null) {
                            Image(
                                bitmap = videoInfo.thumbnailBitmap.asImageBitmap(),
                                contentDescription = "Video thumbnail",
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                },
                headlineContent = {
                    Text(
                        text = videoInfo.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        text = buildString {
                            append(FormatUtils.formatFileSize(videoInfo.sizeBytes))
                            if (videoInfo.width > 0 && videoInfo.height > 0) {
                                append(" · ${videoInfo.width}×${videoInfo.height}")
                            }
                            append(" · ${FormatUtils.formatDuration(videoInfo.durationMs)}")
                        }
                    )
                },
                trailingContent = {
                    TextButton(
                        onClick = onPickVideoClick,
                        modifier = Modifier.testTag("change_video_button")
                    ) {
                        Text("Change")
                    }
                }
            )
        }
    }
}
