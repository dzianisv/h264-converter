package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ConversionHistoryEntity
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

    // Storage Access Framework picker (Supports Internal Storage, USB-OTG flash drives, SD cards)
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Convert264",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "H.264 MP4 Transcoder • USB-OTG Ready",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Offline • No Ads",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Video Picker Section
                VideoPickerCard(
                    videoInfo = selectedVideo,
                    onPickVideoClick = { pickVideoLauncher.launch(arrayOf("video/*")) },
                    onTrySampleClick = { viewModel.loadSampleVideo() }
                )

                if (isLoadingVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // If Error occurred
                if (conversionState is ConversionState.Error) {
                    val errorMsg = (conversionState as ConversionState.Error).message
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversion_error_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
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
                        onCancelClick = { viewModel.cancelConversion() }
                    )
                }

                // 3. Success State
                if (conversionState is ConversionState.Success) {
                    val result = (conversionState as ConversionState.Success).result
                    ConversionSuccessCard(
                        result = result,
                        onOpenClick = { openVideoFile(context, result.filePath, result.fileUri) },
                        onShareClick = { shareVideoFile(context, result.filePath, result.fileUri, result.fileName) },
                        onConvertAnotherClick = { viewModel.resetForNewConversion() }
                    )
                }

                // 4. Trimming & Preset & Convert Button (when video is selected and not in success/converting)
                if (selectedVideo != null && conversionState !is ConversionState.Converting && conversionState !is ConversionState.Success) {
                    // Trim Card
                    TrimControlCard(
                        totalDurationMs = selectedVideo?.durationMs ?: 0L,
                        trimRange = trimRange,
                        onTrimRangeChange = { viewModel.onTrimSliderChange(it) },
                        onAdjustStart = { viewModel.adjustTrimStart(it) },
                        onAdjustEnd = { viewModel.adjustTrimEnd(it) },
                        onOpenExactEditDialog = { showExactTimeDialog = true }
                    )

                    // Presets Card (1080p, 720p, original)
                    PresetSelectorCard(
                        selectedPreset = qualityPreset,
                        onPresetSelect = { viewModel.setQualityPreset(it) }
                    )

                    // Large Prominent Primary Action Button
                    Button(
                        onClick = { viewModel.startConversion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("convert_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Transform,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Convert to H.264 MP4",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            fontSize = 16.sp
                        )
                    }
                }

                // 5. Recent Conversions History List
                ConversionHistoryCard(
                    historyList = historyList,
                    onOpenItem = { item -> openVideoFile(context, item.filePath, Uri.parse(item.fileUriString)) },
                    onShareItem = { item -> shareVideoFile(context, item.filePath, Uri.parse(item.fileUriString), item.fileName) },
                    onDeleteItem = { item -> viewModel.deleteHistoryItem(item) },
                    onClearAll = { viewModel.clearAllHistory() }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
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
