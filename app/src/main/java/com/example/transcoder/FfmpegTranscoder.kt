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
 *
 * Session-scoped lifecycle (WL-12, issue #1): each [transcode]
 * call owns its staging files and its cancelled flag in locals
 * the session's callbacks close over. A session's completion
 * callback may only touch that session's own staging files and
 * flags — never shared mutable state — so a late callback from
 * a cancelled session is a no-op for any newer run. [cancel]
 * only signals the current session; it never deletes files.
 * The session's own completion callback deletes its staging
 * files (FFmpegKit.cancel() is async — the callback is the
 * only place that knows the session is really finished).
 */
class FfmpegTranscoder(private val context: Context) {

    /**
     * Per-session cancellation state (WL-12): one handle per
     * [transcode] call. [cancel] signals the current handle
     * only; the session's completion callback closes over its
     * own handle and staging files, never shared state.
     */
    private class SessionHandle {
        @Volatile
        var sessionId: Long? = null
        val cancelled = AtomicBoolean(false)
    }

    @Volatile
    private var currentSession: SessionHandle? = null

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

        try {
            context.contentResolver.openInputStream(videoInfo.uri)?.use { input ->
                FileOutputStream(stagedInput).use { out -> input.copyTo(out) }
            } ?: run {
                cleanupStaging(stagedInput, stagedOutput)
                withContext(Dispatchers.Main) { onError("Cannot read the source video.") }
                return@withContext
            }
        } catch (e: Exception) {
            cleanupStaging(stagedInput, stagedOutput)
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
        // The handle is registered BEFORE executeAsync so cancel()
        // can signal this session even if it fires immediately;
        // the session id lands as soon as executeAsync returns
        // (and a cancel that slipped in between is re-issued below).
        val handle = SessionHandle()
        currentSession = handle
        val session = FFmpegKit.executeAsync(
            command,
            { completed ->
                val rc = completed.returnCode
                // This callback belongs to ONE session: it may only
                // touch that session's own staging files/flag.
                // A late callback (cancel fired, a newer run has
                // already started) is a no-op for the newer run:
                // it cleans up only its own staging files and
                // notifies only its own waiter (already gone by
                // then — runOne guards on cont.isActive).
                val superseded = currentSession !== handle
                if (handle.cancelled.get() || superseded || ReturnCode.isCancel(rc)) {
                    // Pause / Stop / Clear owns the state now.
                    // Still notify, or the queue runner waiting on
                    // this run would hang forever.
                    cleanupStaging(stagedInput, stagedOutput)
                    if (currentSession === handle) currentSession = null
                    onCancelled()
                    return@executeAsync
                }
                if (ReturnCode.isSuccess(rc)) {
                    try {
                        val published = publishFinalFile(
                            stagingFile = stagedOutput,
                            targetFile = finalOutputFile,
                            displayName = outputName,
                            mimeType = settings.container.mimeType,
                        )
                        if (!published || !finalOutputFile.exists() || finalOutputFile.length() <= 0L) {
                            onError("Conversion produced an empty file. Please try again.")
                        } else {
                            val finalSize = finalOutputFile.length()
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
                        }
                    } catch (e: Exception) {
                        onError("Failed to save final video: ${e.localizedMessage ?: "Unknown error"}")
                    } finally {
                        cleanupStaging(stagedInput, stagedOutput)
                        if (currentSession === handle) currentSession = null
                    }
                } else {
                    val tail = lastLogs.takeLast(3).joinToString(" | ").take(300)
                    onError(
                        "Conversion failed (ffmpeg rc=${rc?.value}): " +
                            tail.ifBlank { "no ffmpeg output captured" },
                    )
                    cleanupStaging(stagedInput, stagedOutput)
                    if (currentSession === handle) currentSession = null
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
        handle.sessionId = session.sessionId
        if (handle.cancelled.get()) {
            // Cancel landed between registering the handle and
            // the session existing: re-issue it now that the
            // session id is known.
            try {
                FFmpegKit.cancel(session.sessionId)
            } catch (_: Exception) {
                // Ignore
            }
        }
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
     *
     * Returns true only if the published file exists and is
     * non-empty (WL-12): the MediaStore fallback insert creates
     * a 0-byte row when its copy fails, and a 0-byte "success"
     * must be an Error, never Done.
     */
    private fun publishFinalFile(
        stagingFile: File,
        targetFile: File,
        displayName: String,
        mimeType: String,
    ): Boolean {
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

        // Success means the file the result points at is real
        // and non-empty. The MediaStore fallback above lands at
        // this same path (Movies/Convert264/<displayName>), so a
        // fallback-only success still counts — but a 0-byte row
        // left behind by a failed fallback copy does not.
        return targetFile.exists() && targetFile.length() > 0L
    }

    /**
     * Signals the current session to cancel (WL-12). Never
     * deletes files: the session's own completion callback
     * deletes its staging files when it fires — FFmpegKit.cancel()
     * is async, so deleting here could remove a newer run's
     * staging files. No-op when nothing is running.
     */
    fun cancel() {
        val handle = currentSession ?: return
        handle.cancelled.set(true)
        try {
            handle.sessionId?.let { FFmpegKit.cancel(it) }
        } catch (_: Exception) {
            // Ignore
        }
    }

    /** Deletes ONLY the given staging files (WL-12: never shared state). */
    private fun cleanupStaging(vararg files: File?) {
        for (file in files) {
            try {
                file?.delete()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }
}
