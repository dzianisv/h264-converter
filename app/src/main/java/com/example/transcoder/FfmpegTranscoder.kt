package com.example.transcoder

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.example.model.ConversionProgress
import com.example.model.ConversionResult
import com.example.model.ConversionSettings
import com.example.model.VideoInfo
import com.example.model.buildFfmpegCommand
import com.example.util.FormatUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The conversion engine (v0.2.0): FFmpeg via the maintained
 * ffmpeg-kit fork (dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl,
 * FFmpeg 8.1 LTS). Replaces the Media3 Transformer engine, which
 * could only mux MP4 with device-dependent encoders.
 *
 * One run: the input Uri is staged to a plain file in the app
 * cache (ffmpeg reads file paths, not content:// URIs), the
 * command is built from [ConversionSettings], progress comes from
 * ffmpeg's own statistics callback (output time / trimmed
 * duration — a real percent, never a time-based estimate), and
 * the finished file is published to Movies/Convert264 exactly
 * like the old engine did.
 *
 * Cancellation: [cancel] cancels the running FFmpeg session.
 * The session's completion callback then fires with a cancel
 * return code — that is swallowed (not reported as an error),
 * because the caller (pause/stop) already owns the resulting
 * state. Pause is cancel + re-run from scratch on resume:
 * ffmpeg has no pause/resume of an in-progress encode.
 */
class FfmpegTranscoder(private val context: Context) {

    @Volatile
    private var currentSessionId: Long? = null
    private var tempInputFile: File? = null
    private var tempOutputFile: File? = null
    private val cancelledByUser = AtomicBoolean(false)

    suspend fun transcode(
        videoInfo: VideoInfo,
        startMs: Long,
        endMs: Long,
        settings: ConversionSettings,
        onProgress: (ConversionProgress) -> Unit,
        onSuccess: (ConversionResult) -> Unit,
        onError: (String) -> Unit,
        /** Called when the run was cancelled via [cancel] (pause / stop). */
        onCancelled: () -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        cancelledByUser.set(false)
        val trimStart = startMs.coerceAtLeast(0L)
        val trimEnd = if (endMs in 1L..videoInfo.durationMs) endMs else videoInfo.durationMs
        val totalTrimDurationMs = (trimEnd - trimStart).coerceAtLeast(100L)

        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val convert264Dir = File(moviesDir, "Convert264").apply { if (!exists()) mkdirs() }

        val outputName = FormatUtils.generateUniqueOutputName(
            originalName = videoInfo.name,
            targetDirectory = convert264Dir,
            suffix = settings.videoCodec.shortName,
            extension = settings.container.extension,
        )
        val finalOutputFile = File(convert264Dir, outputName)

        // Stage input + output in the app cache so cancellation /
        // errors leave no partial files in Movies/.
        val inputExt = videoInfo.name.substringAfterLast('.', "").lowercase()
            .takeIf { it.isNotBlank() } ?: "mp4"
        val stagedInput = File(context.cacheDir, "input_${UUID.randomUUID()}.$inputExt")
        val stagedOutput = File(context.cacheDir, "staging_${UUID.randomUUID()}.$outputName")
        tempInputFile = stagedInput
        tempOutputFile = stagedOutput

        try {
            context.contentResolver.openInputStream(videoInfo.uri)?.use { input ->
                FileOutputStream(stagedInput).use { out -> input.copyTo(out) }
            } ?: run {
                withContext(Dispatchers.Main) { onError("Cannot read the source video.") }
                return@withContext
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Cannot read the source video: ${e.localizedMessage ?: "unknown error"}")
            }
            return@withContext
        }

        val command = buildFfmpegCommand(
            inputPath = stagedInput.absolutePath,
            outputPath = stagedOutput.absolutePath,
            startMs = trimStart,
            endMs = trimEnd,
            settings = settings,
        )
        val startTime = System.currentTimeMillis()
        val lastLogs = ArrayDeque<String>()
        var statsSeen = false

        // executeAsync returns the session immediately; the
        // callbacks below run on FFmpegKit's own threads, so
        // onProgress/onSuccess/onError must be thread-safe (they
        // only touch StateFlow / post back to Main where needed).
        val session = FFmpegKit.executeAsync(
            command,
            { completed ->
                val rc = completed.returnCode
                if (cancelledByUser.get()) {
                    // Pause / Stop / Clear owns the state now.
                    // Still notify, or the queue runner waiting on
                    // this run would hang forever.
                    cleanupStaging()
                    onCancelled()
                    return@executeAsync
                }
                if (ReturnCode.isSuccess(rc)) {
                    try {
                        publishFinalFile(
                            stagingFile = stagedOutput,
                            targetFile = finalOutputFile,
                            displayName = outputName,
                            mimeType = settings.container.mimeType,
                        )
                        val finalSize = if (finalOutputFile.exists()) finalOutputFile.length() else stagedOutput.length()
                        val contentUri = try {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                finalOutputFile,
                            )
                        } catch (_: Exception) {
                            Uri.fromFile(finalOutputFile)
                        }
                        val result = ConversionResult(
                            fileName = outputName,
                            filePath = finalOutputFile.absolutePath,
                            fileUri = contentUri,
                            sizeBytes = finalSize,
                            durationMs = totalTrimDurationMs,
                            originalSizeBytes = videoInfo.sizeBytes,
                            originalDurationMs = videoInfo.durationMs,
                            targetLabel = settings.targetSummary,
                        )
                        onSuccess(result)
                    } catch (e: Exception) {
                        onError("Failed to save final video: ${e.localizedMessage ?: "Unknown error"}")
                    } finally {
                        cleanupStaging()
                    }
                } else if (ReturnCode.isCancel(rc)) {
                    cleanupStaging()
                    onCancelled()
                } else {
                    val tail = lastLogs.takeLast(3).joinToString(" | ").take(300)
                    onError(
                        "Conversion failed (ffmpeg rc=${rc?.value}): " +
                            tail.ifBlank { "no ffmpeg output captured" },
                    )
                    cleanupStaging()
                }
            },
            { log ->
                synchronized(lastLogs) {
                    lastLogs.addLast(log.message)
                    while (lastLogs.size > 30) lastLogs.removeFirst()
                }
            },
            { statistics: Statistics ->
                statsSeen = true
                // Statistics.getTime() is a Double (ms) in
                // ffmpeg-kit 8.x; the rest of the app is Long ms.
                val outputTimeMs = statistics.time.toLong()
                val percent = ((outputTimeMs * 100) / totalTrimDurationMs)
                    .toInt().coerceIn(0, 100)
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                onProgress(
                    ConversionProgress(
                        percent = percent,
                        currentTrimProgressMs = outputTimeMs.coerceAtMost(totalTrimDurationMs),
                        totalTrimDurationMs = totalTrimDurationMs,
                        elapsedSeconds = elapsedSec,
                        progressAvailable = true,
                    ),
                )
            },
        )
        currentSessionId = session.sessionId
        // Report the waiting state until the first statistics
        // callback arrives, so the UI shows an indeterminate bar
        // instead of a fake 0%.
        if (!statsSeen) {
            onProgress(
                ConversionProgress(
                    percent = 0,
                    currentTrimProgressMs = 0L,
                    totalTrimDurationMs = totalTrimDurationMs,
                    elapsedSeconds = 0L,
                    progressAvailable = false,
                ),
            )
        }
    }

    /**
     * Copies the staging file to Movies/Convert264, registers with
     * MediaStore & MediaScanner. Same behaviour as the old Media3
     * engine: the MediaStore insert is a fallback ONLY when the
     * direct file write failed — inserting unconditionally left
     * 0-byte phantom rows next to every real file.
     */
    private fun publishFinalFile(
        stagingFile: File,
        targetFile: File,
        displayName: String,
        mimeType: String,
    ) {
        if (targetFile.parentFile?.exists() != true) {
            targetFile.parentFile?.mkdirs()
        }
        val copied = try {
            if (stagingFile.renameTo(targetFile)) {
                true
            } else {
                FileInputStream(stagingFile).use { input ->
                    FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                }
                stagingFile.delete()
                true
            }
        } catch (_: Exception) {
            false
        }

        val directWriteOk = copied && targetFile.exists() && targetFile.length() > 0L
        if (!directWriteOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Convert264")
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values,
                )?.let { mediaStoreUri ->
                    context.contentResolver.openOutputStream(mediaStoreUri)?.use { out ->
                        FileInputStream(stagingFile).use { input -> input.copyTo(out) }
                    }
                }
            } catch (_: Exception) {
                // MediaStore optional fallback
            }
        }

        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType),
                null,
            )
        } catch (_: Exception) {
            // Ignore
        }
    }

    fun cancel() {
        cancelledByUser.set(true)
        try {
            currentSessionId?.let { FFmpegKit.cancel(it) }
        } catch (_: Exception) {
            // Ignore
        }
        currentSessionId = null
        cleanupStaging()
    }

    private fun cleanupStaging() {
        try {
            tempInputFile?.delete()
        } catch (_: Exception) {
        }
        try {
            tempOutputFile?.delete()
        } catch (_: Exception) {
        }
        tempInputFile = null
        tempOutputFile = null
    }
}
