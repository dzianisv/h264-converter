package com.example.model

import android.graphics.Bitmap
import android.net.Uri

data class VideoInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val thumbnailBitmap: Bitmap?
)

data class ConversionProgress(
    val percent: Int = 0,
    val currentTrimProgressMs: Long = 0L,
    val totalTrimDurationMs: Long = 0L,
    val elapsedSeconds: Long = 0L,
    /**
     * True only when the encoder actually reported a percentage
     * (ffmpeg statistics callback). When false, [percent] is 0
     * and means "unknown", NOT "0% done" — the UI must render an
     * indeterminate bar, never a determinate 0%.
     */
    val progressAvailable: Boolean = false
)

data class ConversionResult(
    val fileName: String,
    val filePath: String,
    val fileUri: Uri,
    val sizeBytes: Long,
    val durationMs: Long,
    val originalSizeBytes: Long,
    val originalDurationMs: Long,
    /** Short target summary, e.g. "MKV · VP9 · Opus". */
    val targetLabel: String = "",
)

/**
 * One entry of the conversion queue. Each item carries its own
 * trim range and its own settings (default = the main screen's
 * settings when the file was added; the detail screen edits them
 * per file and marks the item customized, so later changes to the
 * main-screen settings no longer overwrite it).
 */
data class QueueItem(
    val id: String,
    val video: VideoInfo,
    val trimRange: ClosedFloatingPointRange<Float>,
    val settings: ConversionSettings,
    val customized: Boolean = false,
    val status: QueueItemStatus = QueueItemStatus.Queued,
)

sealed interface QueueItemStatus {
    data object Queued : QueueItemStatus
    data class Converting(val progress: ConversionProgress) : QueueItemStatus
    data object Paused : QueueItemStatus
    data class Done(val result: ConversionResult) : QueueItemStatus
    data class Error(val message: String) : QueueItemStatus
}
