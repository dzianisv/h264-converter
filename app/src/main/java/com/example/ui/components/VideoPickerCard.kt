package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
 * Empty state: one job, one primary action. The whole block is a
 * plain centered column — no card, no border, no custom chip row —
 * so it can sit vertically centered in the screen instead of
 * crowding the top.
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Convert any video to H.264",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Pick a video, or share one here. Everything stays on your device — no uploads, no ads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickVideoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_video_button")
                ) {
                    Text("Browse videos")
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

            Text(
                text = "MKV · MOV · AVI · MP4 · WEBM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
