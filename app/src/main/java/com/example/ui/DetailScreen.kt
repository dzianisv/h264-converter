package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.QueueItemStatus
import com.example.ui.components.ConversionSettingsCard
import com.example.ui.components.ExactTimeEditDialog
import com.example.ui.components.TrimVideoPreview
import com.example.util.FormatUtils
import com.example.viewmodel.ConverterViewModel
import java.io.File

/**
 * Second screen: one file's trim + conversion properties.
 * A real navigation destination, NOT a dialog — and it must fit
 * on ONE Pixel-size screen without scrolling, so everything is
 * compact: a capped preview, one-line durations, one slider,
 * one row of start/end times with inline step buttons, the
 * settings in a two-column grid, one action button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    viewModel: ConverterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val item = queue.firstOrNull { it.id == itemId }
    var showExactTimeDialog by remember { mutableStateOf(false) }

    // The item can disappear (deleted from the queue) — go back
    // instead of showing a dead screen.
    LaunchedEffect(item) {
        if (item == null) onBack()
    }
    if (item == null) return

    val video = item.video
    val trimRange = item.trimRange
    val startMs = trimRange.start.toLong().coerceAtLeast(0L)
    val endMs = trimRange.endInclusive.toLong().coerceAtMost(video.durationMs)
    val trimmedDurationMs = (endMs - startMs).coerceAtLeast(0L)

    if (showExactTimeDialog) {
        ExactTimeEditDialog(
            initialStartMs = startMs,
            initialEndMs = endMs,
            totalDurationMs = video.durationMs,
            onDismiss = { showExactTimeDialog = false },
            onConfirm = { newStart, newEnd ->
                viewModel.setTrimStartMs(item.id, newStart)
                viewModel.setTrimEndMs(item.id, newEnd)
                showExactTimeDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = video.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to queue"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Preview: the real frame at the trim position (capped
            // height — the screen must not scroll, and a portrait
            // video's aspect ratio would otherwise make the frame
            // ~675dp tall).
            TrimVideoPreview(
                sourceUri = video.uri,
                videoWidth = video.width,
                videoHeight = video.height,
                trimRange = trimRange,
                maxPreviewHeight = 140.dp
            )

            // Durations, one line.
            Text(
                text = "Original ${FormatUtils.formatDuration(video.durationMs)}  →  " +
                    "Trimmed ${FormatUtils.formatDuration(trimmedDurationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("detail_durations_text")
            )

            RangeSlider(
                value = trimRange,
                onValueChange = { viewModel.onTrimSliderChange(item.id, it) },
                valueRange = 0f..(if (video.durationMs > 0) video.durationMs.toFloat() else 1000f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_trim_range_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactTrimTime(
                    label = "Start",
                    timeMs = startMs,
                    onAdjust = { viewModel.adjustTrimStart(item.id, it) },
                    onTapToEdit = { showExactTimeDialog = true },
                    modifier = Modifier.weight(1f),
                    testTagPrefix = "detail_start_time"
                )
                CompactTrimTime(
                    label = "End",
                    timeMs = endMs,
                    onAdjust = { viewModel.adjustTrimEnd(item.id, it) },
                    onTapToEdit = { showExactTimeDialog = true },
                    modifier = Modifier.weight(1f),
                    testTagPrefix = "detail_end_time"
                )
            }

            // Per-file settings (default = the main screen's
            // choices; editing here marks the file customized).
            ConversionSettingsCard(
                settings = item.settings,
                onSettingsChange = { viewModel.updateItemSettings(item.id, it) },
                compact = true
            )

            // Action: convert this file, watch its progress, or
            // open/share the finished result.
            when (val status = item.status) {
                is QueueItemStatus.Converting -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (status.progress.progressAvailable) {
                            LinearProgressIndicator(
                                progress = { (status.progress.percent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_progress")
                            )
                            Text(
                                text = "Converting… ${status.progress.percent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("detail_progress_text")
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_progress_indeterminate")
                            )
                            Text(
                                text = "Converting…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is QueueItemStatus.Done -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Done: ${status.result.fileName} · " +
                                FormatUtils.formatFileSize(status.result.sizeBytes),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("detail_done_text")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { openVideoFile(context, status.result.filePath, status.result.fileUri) },
                                modifier = Modifier.testTag("detail_open_button")
                            ) {
                                Text("Open")
                            }
                            TextButton(
                                onClick = {
                                    shareVideoFile(
                                        context,
                                        status.result.filePath,
                                        status.result.fileUri,
                                        status.result.fileName
                                    )
                                },
                                modifier = Modifier.testTag("detail_share_button")
                            ) {
                                Text("Share")
                            }
                            TextButton(
                                onClick = {
                                    viewModel.requeueItem(item.id)
                                    viewModel.startQueue(fromId = item.id)
                                },
                                modifier = Modifier.testTag("detail_convert_again_button")
                            ) {
                                Text("Convert again")
                            }
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            viewModel.requeueItem(item.id)
                            viewModel.startQueue(fromId = item.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_convert_button")
                    ) {
                        Text("Convert this file")
                    }
                }
            }
        }
    }
}

/**
 * One side of the trim range, compact: label + time (tap for
 * exact input) + four step buttons in a single row.
 */
@Composable
private fun CompactTrimTime(
    label: String,
    timeMs: Long,
    onAdjust: (Long) -> Unit,
    onTapToEdit: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = FormatUtils.formatDurationWithMillis(timeMs),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable { onTapToEdit() }
                .testTag("${testTagPrefix}_text")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { onAdjust(-1000L) }) { Text("-1s") }
            TextButton(onClick = { onAdjust(-100L) }) { Text("-0.1s") }
            TextButton(onClick = { onAdjust(100L) }) { Text("+0.1s") }
            TextButton(onClick = { onAdjust(1000L) }) { Text("+1s") }
        }
    }
}

/** Open a converted video in an external player (FileProvider). */
fun openVideoFile(context: Context, filePath: String, fallbackUri: Uri) {
    try {
        val file = File(filePath)
        val uri = if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            fallbackUri
        }
        val mime = when (file.extension.lowercase()) {
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            else -> "video/mp4"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open video"))
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open video. No compatible video player found.", Toast.LENGTH_SHORT).show()
    }
}

/** Share a converted video via the Android share sheet. */
fun shareVideoFile(context: Context, filePath: String, fallbackUri: Uri, title: String) {
    try {
        val file = File(filePath)
        val uri = if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            fallbackUri
        }
        val mime = when (file.extension.lowercase()) {
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            else -> "video/mp4"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share converted video"))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to share video file.", Toast.LENGTH_SHORT).show()
    }
}
