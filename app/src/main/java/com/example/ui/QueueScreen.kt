package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.QueueItemStatus
import com.example.ui.components.ConversionHistoryCard
import com.example.ui.components.ConversionSettingsCard
import com.example.ui.components.ConversionStatusBar
import com.example.ui.components.QueueItemRow
import com.example.ui.components.VideoPickerCard
import com.example.viewmodel.ConverterViewModel

/**
 * First screen: the conversion queue.
 *
 * TopAppBar: title only, with Play / Pause / Stop / Clear in the
 * top-right. The conversion settings sit in the center (one
 * joined card of dropdowns). The queue fills the middle — each
 * entry shows its thumbnail + length + target, with its own
 * progress while converting. The plus button is on the LEFT:
 * a FloatingActionButton at the bottom-start. Recent conversions
 * (history) sit below the queue.
 *
 * Stock Material 3 only — default shapes, theme colors, type
 * scale. No chips anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: ConverterViewModel,
    onOpenDetail: (String) -> Unit,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val globalSettings by viewModel.globalSettings.collectAsStateWithLifecycle()
    val isConverting by viewModel.isConverting.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val isLoadingVideo by viewModel.isLoadingVideo.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    val canStart = !isConverting && queue.any {
        it.status is QueueItemStatus.Queued ||
            it.status is QueueItemStatus.Paused ||
            it.status is QueueItemStatus.Error
    }
    val convertingItem = queue.firstOrNull { it.status is QueueItemStatus.Converting }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Convert264") },
                actions = {
                    IconButton(
                        enabled = canStart,
                        onClick = { viewModel.startQueue() },
                        modifier = Modifier.testTag("play_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start converting queue"
                        )
                    }
                    IconButton(
                        enabled = isConverting,
                        onClick = { viewModel.pauseQueue() },
                        modifier = Modifier.testTag("pause_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause conversion"
                        )
                    }
                    IconButton(
                        enabled = isConverting || isPaused,
                        onClick = { viewModel.stopQueue() },
                        modifier = Modifier.testTag("stop_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop conversion"
                        )
                    }
                    IconButton(
                        enabled = queue.isNotEmpty(),
                        onClick = { viewModel.clearQueue() },
                        modifier = Modifier.testTag("clear_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear queue"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPickVideo,
                modifier = Modifier.testTag("add_video_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add video to queue"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Conversion settings, center of the screen.
            ConversionSettingsCard(
                settings = globalSettings,
                onSettingsChange = { viewModel.setGlobalSettings(it) }
            )

            // The converting card: the current file's progress.
            val converting = convertingItem
            if (converting != null && converting.status is QueueItemStatus.Converting) {
                val progress = (converting.status as QueueItemStatus.Converting).progress
                ConversionStatusBar(
                    progress = progress,
                    onCancelClick = { viewModel.stopQueue() },
                    title = "Converting ${converting.video.name} → ${converting.settings.targetSummary}"
                )
            }

            // Queue + history, scrolling.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("queue_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (queue.isEmpty()) {
                    item {
                        VideoPickerCard(
                            onPickVideoClick = onPickVideo,
                            onTrySampleClick = { viewModel.loadSampleVideo() }
                        )
                    }
                } else {
                    items(queue, key = { it.id }) { item ->
                        QueueItemRow(
                            item = item,
                            onClick = { onOpenDetail(item.id) },
                            onDelete = { viewModel.removeQueueItem(item.id) },
                            onOpenResult = { result ->
                                openVideoFile(context, result.filePath, result.fileUri)
                            },
                            onShareResult = { result ->
                                shareVideoFile(context, result.filePath, result.fileUri, result.fileName)
                            }
                        )
                    }
                }

                item {
                    ConversionHistoryCard(
                        historyList = historyList,
                        onOpenItem = { historyItem ->
                            openVideoFile(
                                context,
                                historyItem.filePath,
                                android.net.Uri.parse(historyItem.fileUriString)
                            )
                        },
                        onShareItem = { historyItem ->
                            shareVideoFile(
                                context,
                                historyItem.filePath,
                                android.net.Uri.parse(historyItem.fileUriString),
                                historyItem.fileName
                            )
                        },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) },
                        onClearAll = { viewModel.clearAllHistory() }
                    )
                }
            }

            if (isLoadingVideo) {
                Text(
                    text = "Loading video…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("loading_video_text")
                )
            }
        }
    }
}
