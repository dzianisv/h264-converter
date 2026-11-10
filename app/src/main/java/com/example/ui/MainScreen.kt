package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConversionState
import com.example.ui.components.ConversionHistoryCard
import com.example.ui.components.ConversionStatusBar
import com.example.ui.components.ConversionSuccessCard
import com.example.ui.components.ExactTimeEditDialog
import com.example.ui.components.PresetSelectorCard
import com.example.ui.components.TrimControlCard
import com.example.ui.components.VideoPickerCard
import com.example.viewmodel.ConverterViewModel
import java.io.File

/**
 * One job per screen, one primary action, content first.
 *
 * Stock Material 3 components only — default shapes, default
 * colors from the theme colorScheme, Material type scale only,
 * 16dp content padding, 16dp gaps. Nothing custom.
 *
 * The content column centers its group vertically when it is
 * shorter than the screen (launch state), so the screen is never
 * a top-heavy block over an empty bottom half; when the content
 * is longer it scrolls from the top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val isLoadingVideo by viewModel.isLoadingVideo.collectAsStateWithLifecycle()
    val trimRange by viewModel.trimRange.collectAsStateWithLifecycle()
    val qualityPreset by viewModel.qualityPreset.collectAsStateWithLifecycle()
    val conversionState by viewModel.conversionState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    var showExactTimeDialog by remember { mutableStateOf(false) }

    // Storage Access Framework picker: lets the user pick from
    // internal storage, an attached drive, or an SD card, wherever
    // the system file picker exposes it.
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Request persistable permission where supported
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Ignore for SAF URIs that don't support persistence
            }
            viewModel.loadVideo(it)
        }
    }

    if (showExactTimeDialog && selectedVideo != null) {
        ExactTimeEditDialog(
            initialStartMs = trimRange.start.toLong(),
            initialEndMs = trimRange.endInclusive.toLong(),
            totalDurationMs = selectedVideo?.durationMs ?: 0L,
            onDismiss = { showExactTimeDialog = false },
            onConfirm = { startMs, endMs ->
                viewModel.setTrimStartMs(startMs)
                viewModel.setTrimEndMs(endMs)
                showExactTimeDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Convert264") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Video Picker / empty state
            VideoPickerCard(
                videoInfo = selectedVideo,
                onPickVideoClick = { pickVideoLauncher.launch(arrayOf("video/*")) },
                onTrySampleClick = { viewModel.loadSampleVideo() },
                modifier = Modifier.widthIn(max = 680.dp)
            )

            if (isLoadingVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // If Error occurred
            if (conversionState is ConversionState.Error) {
                val errorMsg = (conversionState as ConversionState.Error).message
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .testTag("conversion_error_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.cancelConversion() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // 2. Active Converting State
            if (conversionState is ConversionState.Converting) {
                val progress = (conversionState as ConversionState.Converting).progress
                ConversionStatusBar(
                    progress = progress,
                    onCancelClick = { viewModel.cancelConversion() },
                    modifier = Modifier.widthIn(max = 680.dp)
                )
            }

            // 3. Success State
            if (conversionState is ConversionState.Success) {
                val result = (conversionState as ConversionState.Success).result
                ConversionSuccessCard(
                    result = result,
                    onOpenClick = { openVideoFile(context, result.filePath, result.fileUri) },
                    onShareClick = { shareVideoFile(context, result.filePath, result.fileUri, result.fileName) },
                    onConvertAnotherClick = { viewModel.resetForNewConversion() },
                    modifier = Modifier.widthIn(max = 680.dp)
                )
            }

            // 4. Trimming & Preset & Convert Button (when video is selected and not in success/converting)
            if (selectedVideo != null && conversionState !is ConversionState.Converting && conversionState !is ConversionState.Success) {
                TrimControlCard(
                    totalDurationMs = selectedVideo?.durationMs ?: 0L,
                    trimRange = trimRange,
                    onTrimRangeChange = { viewModel.onTrimSliderChange(it) },
                    onAdjustStart = { viewModel.adjustTrimStart(it) },
                    onAdjustEnd = { viewModel.adjustTrimEnd(it) },
                    onOpenExactEditDialog = { showExactTimeDialog = true },
                    sourceUri = selectedVideo?.uri,
                    videoWidth = selectedVideo?.width ?: 0,
                    videoHeight = selectedVideo?.height ?: 0,
                    modifier = Modifier.widthIn(max = 680.dp)
                )

                PresetSelectorCard(
                    selectedPreset = qualityPreset,
                    onPresetSelect = { viewModel.setQualityPreset(it) },
                    modifier = Modifier.widthIn(max = 680.dp)
                )

                Button(
                    onClick = { viewModel.startConversion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .testTag("convert_button")
                ) {
                    Text("Convert to H.264")
                }
            }

            // 5. Recent Conversions History List
            ConversionHistoryCard(
                historyList = historyList,
                onOpenItem = { item -> openVideoFile(context, item.filePath, Uri.parse(item.fileUriString)) },
                onShareItem = { item -> shareVideoFile(context, item.filePath, Uri.parse(item.fileUriString), item.fileName) },
                onDeleteItem = { item -> viewModel.deleteHistoryItem(item) },
                onClearAll = { viewModel.clearAllHistory() },
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }
    }
}

/**
 * Open video in external video player safely using FileProvider
 */
private fun openVideoFile(context: Context, filePath: String, fallbackUri: Uri) {
    try {
        val file = File(filePath)
        val uri = if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            fallbackUri
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open video"))
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open video. No compatible video player found.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Share video via Android share sheet
 */
private fun shareVideoFile(context: Context, filePath: String, fallbackUri: Uri, title: String) {
    try {
        val file = File(filePath)
        val uri = if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            fallbackUri
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share converted video"))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to share video file.", Toast.LENGTH_SHORT).show()
    }
}
