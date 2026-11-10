package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConversionResult
import com.example.model.QueueItem
import com.example.model.QueueItemStatus
import com.example.util.FormatUtils

/**
 * One queue entry: standard ListItem with a real thumbnail frame,
 * filename, duration · size, and the target (e.g. MKV · VP9 ·
 * Opus). The row's trailing content depends on the status:
 * Queued/Paused/Error get a delete button, Converting shows its
 * own percent + determinate bar, Done gets Open + Share.
 * Tapping the row opens the detail screen (or opens the finished
 * video when Done).
 */
@Composable
fun QueueItemRow(
    item: QueueItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenResult: (ConversionResult) -> Unit,
    onShareResult: (ConversionResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val video = item.video
    val status = item.status
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .clickable(onClick = onClick)
                .testTag("queue_item_${item.id}"),
            leadingContent = {
                Box(modifier = Modifier.size(56.dp)) {
                    if (video.thumbnailBitmap != null) {
                        Image(
                            bitmap = video.thumbnailBitmap.asImageBitmap(),
                            contentDescription = "Video thumbnail",
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("queue_item_thumbnail"),
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
                    text = video.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = "${FormatUtils.formatDuration(video.durationMs)} · " +
                            FormatUtils.formatFileSize(video.sizeBytes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (status) {
                            is QueueItemStatus.Converting -> {
                                if (status.progress.progressAvailable) {
                                    "${item.settings.targetSummary} · ${status.progress.percent}%"
                                } else {
                                    "${item.settings.targetSummary} · converting…"
                                }
                            }
                            QueueItemStatus.Paused -> "${item.settings.targetSummary} · Paused"
                            is QueueItemStatus.Done -> "${item.settings.targetSummary} · Done"
                            is QueueItemStatus.Error -> "Failed: ${status.message}"
                            QueueItemStatus.Queued -> item.settings.targetSummary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (status is QueueItemStatus.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (status) {
                        is QueueItemStatus.Done -> {
                            IconButton(onClick = { onOpenResult(status.result) }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Open converted video"
                                )
                            }
                            IconButton(onClick = { onShareResult(status.result) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share converted video"
                                )
                            }
                        }
                        is QueueItemStatus.Converting -> {
                            Text(
                                text = if (status.progress.progressAvailable) {
                                    "${status.progress.percent}%"
                                } else {
                                    "…"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        else -> {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from queue"
                                )
                            }
                        }
                    }
                }
            }
        )
        if (status is QueueItemStatus.Converting) {
            if (status.progress.progressAvailable) {
                LinearProgressIndicator(
                    progress = { (status.progress.percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("queue_item_progress_${item.id}")
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("queue_item_progress_indeterminate_${item.id}")
                )
            }
        }
        if (status is QueueItemStatus.Done) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " ${status.result.fileName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
