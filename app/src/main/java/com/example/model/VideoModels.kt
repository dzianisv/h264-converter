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

enum class QualityPreset(
    val label: String,
    val description: String,
    val targetHeight: Int?
) {
    ORIGINAL("Original", "Keep original resolution", null),
    P1080("1080p", "Full HD (1920x1080 max)", 1080),
    P720("720p", "HD (1280x720 max)", 720)
}

data class ConversionProgress(
    val percent: Int = 0,
    val currentTrimProgressMs: Long = 0L,
    val totalTrimDurationMs: Long = 0L,
    val elapsedSeconds: Long = 0L,
    /**
     * True only when Media3 actually reported a percentage
     * (Transformer.getProgress() == PROGRESS_STATE_AVAILABLE).
     * When false, [percent] is 0 and means "unknown", NOT "0% done" —
     * the UI must render an indeterminate bar, never a determinate 0%.
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
    val originalDurationMs: Long
)

sealed interface ConversionState {
    data object Idle : ConversionState
    data class Converting(val progress: ConversionProgress) : ConversionState
    data class Success(val result: ConversionResult) : ConversionState
    data class Error(val message: String) : ConversionState
}
