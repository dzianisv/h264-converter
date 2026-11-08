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

        // Progress polling loop
        val progressHolder = ProgressHolder()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val progressState = transformer.getProgress(progressHolder)
                if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val percent = progressHolder.progress.coerceIn(0, 100)
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                    val currentTrimProgressMs = (totalTrimDurationMs * (percent / 100.0)).toLong()

                    onProgress(
                        ConversionProgress(
                            percent = percent,
                            currentTrimProgressMs = currentTrimProgressMs,
                            totalTrimDurationMs = totalTrimDurationMs,
                            elapsedSeconds = elapsedSec
                        )
                    )
                }
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

        // Also index into Android's MediaStore so other apps and Gallery instantly recognize it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Convert264")
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { mediaStoreUri ->
                    // Copy bytes into MediaStore if direct file write was restricted
                    if (!copied || !targetFile.exists() || targetFile.length() == 0L) {
                        context.contentResolver.openOutputStream(mediaStoreUri)?.use { out ->
                            FileInputStream(stagingFile).use { input ->
                                input.copyTo(out)
                            }
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
