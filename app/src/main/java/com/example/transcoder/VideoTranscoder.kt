package com.example.transcoder

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.model.ConversionProgress
import com.example.model.ConversionResult
import com.example.model.QualityPreset
import com.example.model.VideoInfo
import com.example.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Diagnostics for the last transcode run, for tests and debugging.
 *
 * Media3's Transformer.getProgress() returns a *state*, not just a
 * percent: NOT_STARTED (0), WAITING_FOR_AVAILABILITY (1), AVAILABLE
 * (2), UNAVAILABLE (3, permanently unavailable). Earlier versions of
 * this app only recorded the percent when the state was AVAILABLE, so
 * a run that stayed at 0% was indistinguishable from "never polled".
 * These counters record the raw state on every poll, so a test can
 * tell WAITING (might become available on another encoder) apart
 * from UNAVAILABLE (never will, on this encoder).
 */
object TranscodeDiagnostics {
    @Volatile var lastRunStateCounts: Map<Int, Int> = emptyMap()
    @Volatile var lastRunMaxPercent: Int = 0
}

class VideoTranscoder(private val context: Context) {

    private var currentTransformer: Transformer? = null
    private var progressJob: Job? = null
    private var tempOutputFile: File? = null

    /**
     * Start conversion to H.264 MP4 with optional trimming and quality preset.
     */
    suspend fun transcode(
        videoInfo: VideoInfo,
        startMs: Long,
        endMs: Long,
        qualityPreset: QualityPreset,
        onProgress: (ConversionProgress) -> Unit,
        onSuccess: (ConversionResult) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.Main) {
        cancel() // cancel any existing conversion

        val originalDuration = videoInfo.durationMs
        val trimStart = startMs.coerceAtLeast(0L)
        val trimEnd = if (endMs in 1L..originalDuration) endMs else originalDuration
        val totalTrimDurationMs = (trimEnd - trimStart).coerceAtLeast(1000L)

        // Target directory: Movies/Convert264
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val convert264Dir = File(moviesDir, "Convert264")
        if (!convert264Dir.exists()) {
            convert264Dir.mkdirs()
        }

        // Generate unique output name that never overwrites existing files
        val uniqueOutputName = FormatUtils.generateUniqueOutputName(videoInfo.name, convert264Dir)
        val finalOutputFile = File(convert264Dir, uniqueOutputName)

        // Use a staging file in app cache so cancellation/errors leave no partial files
        val tempFile = File(context.cacheDir, "staging_${UUID.randomUUID()}_$uniqueOutputName")
        tempOutputFile = tempFile

        // Build MediaItem with clipping configuration
        val clippingConfig = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(trimStart)
            .setEndPositionMs(trimEnd)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(videoInfo.uri)
            .setClippingConfiguration(clippingConfig)
            .build()

        // Video effects (scaling according to Quality Preset)
        val videoEffects = mutableListOf<Effect>()
        if (qualityPreset.targetHeight != null) {
            // Apply scale effect for 1080p or 720p
            videoEffects.add(Presentation.createForHeight(qualityPreset.targetHeight))
        }

        val effects = Effects(
            /* audioProcessors = */ emptyList<AudioProcessor>(),
            /* videoEffects = */ videoEffects
        )

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(effects)
            .build()

        val startTime = System.currentTimeMillis()

        val transformerListener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                stopProgressTracking()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Move or copy staging file to final output location
                        publishFinalFile(tempFile, finalOutputFile, uniqueOutputName)

                        val finalSizeBytes = if (finalOutputFile.exists()) finalOutputFile.length() else tempFile.length()
                        val finalDurationMs = totalTrimDurationMs

                        // Generate shareable FileProvider URI
                        val contentUri = try {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                finalOutputFile
                            )
                        } catch (_: Exception) {
                            Uri.fromFile(finalOutputFile)
                        }

                        val conversionResult = ConversionResult(
                            fileName = uniqueOutputName,
                            filePath = finalOutputFile.absolutePath,
                            fileUri = contentUri,
                            sizeBytes = finalSizeBytes,
                            durationMs = finalDurationMs,
                            originalSizeBytes = videoInfo.sizeBytes,
                            originalDurationMs = videoInfo.durationMs
                        )

                        withContext(Dispatchers.Main) {
                            onSuccess(conversionResult)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onError("Failed to save final video: ${e.localizedMessage ?: "Unknown error"}")
                        }
                    } finally {
                        tempFile.delete()
                    }
                }
            }

            override fun onError(
                composition: Composition,
                result: ExportResult,
                exception: ExportException
            ) {
                stopProgressTracking()
                tempFile.delete()
                val errorMsg = exception.localizedMessage ?: "Transcoding failed (Error code: ${exception.errorCode})"
                onError(errorMsg)
            }
        }

        // Build Transformer for H.264 video + AAC audio
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(transformerListener)
            .build()

        currentTransformer = transformer

        try {
            transformer.start(editedMediaItem, tempFile.absolutePath)
        } catch (e: Exception) {
            tempFile.delete()
            onError("Cannot start conversion: ${e.localizedMessage ?: "Unknown error"}")
            return@withContext
        }

        // Progress polling loop. Polls Transformer.getProgress() and
        // reports on EVERY poll — including when the state is not
        // AVAILABLE — so the elapsed time keeps ticking in the UI and
        // the caller can see the raw state. A time-based percent
        // estimate would be fabrication (encode speed is not linear
        // in time), so when progress is unavailable we report
        // percent=0 with progressAvailable=false and the UI renders
        // an indeterminate bar instead of a fake 0%.
        val progressHolder = ProgressHolder()
        val stateCounts = mutableMapOf<Int, Int>()
        var lastState = -1
        var pollIndex = 0
        TranscodeDiagnostics.lastRunStateCounts = emptyMap()
        TranscodeDiagnostics.lastRunMaxPercent = 0
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val progressState = transformer.getProgress(progressHolder)
                stateCounts[progressState] = (stateCounts[progressState] ?: 0) + 1
                TranscodeDiagnostics.lastRunStateCounts = stateCounts.toMap()
                if (progressState != lastState || pollIndex % 40 == 0) {
                    Log.d(
                        "Convert264",
                        "progress state=$progressState " +
                            "(0=NOT_STARTED 1=WAITING 2=AVAILABLE 3=UNAVAILABLE) " +
                            "percent=${if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) progressHolder.progress else "n/a"} " +
                            "elapsed=${(System.currentTimeMillis() - startTime) / 1000}s",
                    )
                    lastState = progressState
                }
                pollIndex++
                val available = progressState == Transformer.PROGRESS_STATE_AVAILABLE
                val percent = if (available) {
                    progressHolder.progress.coerceIn(0, 100).also {
                        if (it > TranscodeDiagnostics.lastRunMaxPercent) {
                            TranscodeDiagnostics.lastRunMaxPercent = it
                        }
                    }
                } else {
                    0
                }
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                val currentTrimProgressMs = (totalTrimDurationMs * (percent / 100.0)).toLong()

                onProgress(
                    ConversionProgress(
                        percent = percent,
                        currentTrimProgressMs = currentTrimProgressMs,
                        totalTrimDurationMs = totalTrimDurationMs,
                        elapsedSeconds = elapsedSec,
                        progressAvailable = available,
                    )
                )
                delay(250L)
            }
        }
    }

    /**
     * Copies staging file to destination in Movies/Convert264, registers with MediaStore & MediaScanner.
     */
    private fun publishFinalFile(stagingFile: File, targetFile: File, displayName: String) {
        if (!targetFile.parentFile!!.exists()) {
            targetFile.parentFile!!.mkdirs()
        }

        // Direct file copy / rename
        val copied = try {
            if (stagingFile.renameTo(targetFile)) {
                true
            } else {
                FileInputStream(stagingFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                stagingFile.delete()
                true
            }
        } catch (_: Exception) {
            false
        }

        // Index into Android's MediaStore so other apps and Gallery
        // instantly recognize the file — but ONLY as a fallback when
        // the direct file write failed. Inserting a row
        // unconditionally left a 0-byte duplicate entry
        // ("<name> (1).mp4", duration 0) next to every real file,
        // because the bytes were only written into the row when the
        // direct copy had failed.
        val directWriteOk = copied && targetFile.exists() && targetFile.length() > 0L
        if (!directWriteOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Convert264")
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { mediaStoreUri ->
                    context.contentResolver.openOutputStream(mediaStoreUri)?.use { out ->
                        FileInputStream(stagingFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            } catch (_: Exception) {
                // MediaStore optional fallback
            }
        }

        // Request MediaScanner to index the file path
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )
        } catch (_: Exception) {
            // Ignore
        }
    }

    fun cancel() {
        stopProgressTracking()
        try {
            currentTransformer?.cancel()
        } catch (_: Exception) {
            // Ignore
        }
        currentTransformer = null
        tempOutputFile?.delete()
        tempOutputFile = null
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}
