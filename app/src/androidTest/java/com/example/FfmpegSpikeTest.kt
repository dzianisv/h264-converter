package com.example

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.example.util.SampleVideoProvider
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FFmpeg spike for the v0.2.0 universal converter (branch spike/ffmpeg).
 *
 * Proves, on the real CI emulator, that:
 *  1. com.arthenica:ffmpeg-kit-full-gpl:6.0 resolves from Maven Central
 *     and the app builds with it (if this file even compiles, that
 *     part is proven by the build itself).
 *  2. A real conversion runs: sample clip -> MP4/H.264/AAC.
 *  3. A real conversion runs: sample clip -> MKV/VP9/Opus.
 *  4. AV1 timing: sample clip -> WEBM/AV1/Opus, capped — the test
 *     only LOGS the outcome, it does not fail on AV1 being slow.
 *     The matrix decision (include AV1 or not) comes from this log.
 *
 * All results are printed with a "SPIKE" prefix so they can be
 * grepped out of the JUnit XML / smoke log. The H.264 and VP9
 * conversions are hard gates (asserted); AV1 is not.
 */
class FfmpegSpikeTest {

    private val ctx: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun stageInput(): File {
        val sampleUri = runBlockingGetSample()
        val staged = File(ctx.cacheDir, "spike_input.mp4")
        ctx.contentResolver.openInputStream(sampleUri)!!.use { input ->
            FileOutputStream(staged).use { out -> input.copyTo(out) }
        }
        println("SPIKE input staged: ${staged.absolutePath} bytes=${staged.length()}")
        return staged
    }

    /** SampleVideoProvider is a suspend fun — run it off the test thread. */
    private fun runBlockingGetSample(): android.net.Uri {
        val latch = CountDownLatch(1)
        val result = AtomicReference<android.net.Uri?>()
        thread(start = true) {
            try {
                result.set(
                    kotlinx.coroutines.runBlocking {
                        SampleVideoProvider.getOrCreateSampleVideo(ctx)
                    },
                )
            } finally {
                latch.countDown()
            }
        }
        assertTrue("Sample video was not generated in 300s", latch.await(300, TimeUnit.SECONDS))
        val uri = result.get()
        assertTrue("Sample video URI is null", uri != null)
        return uri!!
    }

    /**
     * Runs an ffmpeg command on a worker thread and returns
     * (returnCode, elapsedMs). If [timeoutMs] elapses first, the
     * session is cancelled and (null, timeoutMs) is returned.
     */
    private fun runFfmpeg(command: String, timeoutMs: Long): Pair<ReturnCode?, Long> {
        val start = System.currentTimeMillis()
        val sessionRef = AtomicReference<com.arthenica.ffmpegkit.FFmpegSession?>()
        val done = CountDownLatch(1)
        thread(start = true) {
            try {
                sessionRef.set(FFmpegKit.execute(command))
            } catch (e: Exception) {
                println("SPIKE execute threw: $e")
            } finally {
                done.countDown()
            }
        }
        val finished = done.await(timeoutMs, TimeUnit.MILLISECONDS)
        val elapsed = System.currentTimeMillis() - start
        if (!finished) {
            println("SPIKE TIMEOUT after ${timeoutMs}ms, cancelling session: $command")
            try {
                FFmpegKit.cancel()
            } catch (_: Exception) {
            }
            done.await(15, TimeUnit.SECONDS)
            return Pair(null, elapsed)
        }
        val rc = sessionRef.get()?.returnCode
        println("SPIKE command done rc=${rc?.value} elapsed=${elapsed}ms: $command")
        return Pair(rc, elapsed)
    }

    @Test
    fun ffmpegSpike() {
        println("SPIKE ffmpeg version: ${FFmpegKitConfig.getFFmpegVersion()}")
        val input = stageInput()
        val outDir = File(ctx.cacheDir, "spike_out").apply { mkdirs() }

        // 1. H.264 + AAC -> MP4 (the baseline: must work).
        val mp4 = File(outDir, "spike_h264.mp4")
        val (rc264, ms264) = runFfmpeg(
            "-y -i \"${input.absolutePath}\" " +
                "-map 0:v:0 -map 0:a:0? " +
                "-c:v libx264 -preset veryfast -b:v 2M " +
                "-c:a aac -b:a 128k -movflags +faststart " +
                "\"${mp4.absolutePath}\"",
            timeoutMs = 180_000,
        )
        println("SPIKE h264 mp4 rc=${rc264?.value} elapsedMs=$ms264 bytes=${if (mp4.exists()) mp4.length() else -1}")
        assertTrue("H.264/MP4 conversion failed rc=$rc264", ReturnCode.isSuccess(rc264))
        assertTrue("H.264/MP4 output missing or empty", mp4.exists() && mp4.length() > 0)

        // 2. VP9 + Opus -> MKV (the universal-converter proof).
        val mkv = File(outDir, "spike_vp9.mkv")
        val (rcVp9, msVp9) = runFfmpeg(
            "-y -i \"${input.absolutePath}\" " +
                "-map 0:v:0 -map 0:a:0? " +
                "-c:v libvpx-vp9 -b:v 2M -cpu-used 4 " +
                "-c:a libopus -b:a 128k " +
                "\"${mkv.absolutePath}\"",
            timeoutMs = 300_000,
        )
        println("SPIKE vp9 mkv rc=${rcVp9?.value} elapsedMs=$msVp9 bytes=${if (mkv.exists()) mkv.length() else -1}")
        assertTrue("VP9/MKV conversion failed rc=$rcVp9", ReturnCode.isSuccess(rcVp9))
        assertTrue("VP9/MKV output missing or empty", mkv.exists() && mkv.length() > 0)

        // 3. AV1 + Opus -> WEBM, capped: log only, never fails.
        // libaom is famously slow; if this does not finish in the
        // cap, AV1 stays OUT of the shipped matrix.
        val webm = File(outDir, "spike_av1.webm")
        val (rcAv1, msAv1) = runFfmpeg(
            "-y -i \"${input.absolutePath}\" " +
                "-map 0:v:0 -map 0:a:0? " +
                "-c:v libaom-av1 -b:v 2M -cpu-used 8 " +
                "-c:a libopus -b:a 128k " +
                "\"${webm.absolutePath}\"",
            timeoutMs = 150_000,
        )
        val av1Ok = ReturnCode.isSuccess(rcAv1) && webm.exists() && webm.length() > 0
        println(
            "SPIKE av1 webm rc=${rcAv1?.value} elapsedMs=$msAv1 " +
                "bytes=${if (webm.exists()) webm.length() else -1} " +
                "usable=$av1Ok (3s source clip; if not usable, AV1 is excluded)",
        )
        println("SPIKE DONE h264Ms=$ms264 vp9Ms=$msVp9 av1Usable=$av1Ok")
    }
}
