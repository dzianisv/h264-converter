package com.example

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.android.ComposeNotIdleException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.IdlingResourceTimeoutException
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.ConversionState
import com.example.transcoder.TranscodeDiagnostics
import com.example.viewmodel.ConverterViewModel
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end smoke test for Convert264, run on a real emulator via
 * `gradle connectedDebugAndroidTest` (see .github/workflows/emulator-smoke.yml).
 *
 * Runs in-process (no uiautomator dumps, no adb UI driving), which is
 * the only reliable way to drive the UI on the slow GitHub Actions
 * emulator: the system there ANRs repeatedly and uiautomator dumps
 * return empty for minutes at a time.
 *
 * Flow, twice (run 1: Original preset, run 2: 720p preset — the 720p
 * run exercises the Presentation scale-effect path, to check whether
 * Media3 reports progress on a different encode path):
 *   Try sample clip -> trim END to 15s via Exact input -> Convert
 *   -> converting status bar shown -> "Conversion Complete!" with
 *   Open + Share -> real _h264.mp4 output file on disk.
 *
 * Screenshots are captured in-test at every step
 * (see ScreenshotHelper.kt) and pulled by tool/smoke-test.sh into
 * tool/smoke-screenshots/<run-id>/ — they are the layout evidence
 * and must be looked at, not just collected.
 *
 * Progress: the test records Media3's RAW progress state on every
 * poll (see TranscodeDiagnostics: 0=NOT_STARTED, 1=WAITING,
 * 2=AVAILABLE, 3=UNAVAILABLE) and the max percent/elapsed seen.
 * The % is NOT gated — on this emulator's software encoder
 * getProgress() has never become AVAILABLE, so the bar cannot ramp
 * here and that needs a real device. What IS gated: the converting
 * status bar is shown, the elapsed time advances (live feedback even
 * without a percent), and the conversion completes.
 */
class Convert264SmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, timeoutMs: Long = 60_000) {
        waitUntilWithIdleRetry(timeoutMs) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTag(tag: String, timeoutMs: Long = 60_000) {
        waitUntilWithIdleRetry(timeoutMs) {
            composeRule.onAllNodesWithTag(tag)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * waitUntil with retries around Compose idle timeouts: under the
     * 720p encode load this emulator's compositor goes unresponsive
     * and fetchSemanticsNodes throws ComposeNotIdleException instead
     * of returning "not found yet" (killed the v0.1.2 smoke run in
     * run 2). Retry until the deadline — the gate (text/tag appears)
     * is unchanged, only transient busy-compositor periods are
     * tolerated.
     */
    private fun waitUntilWithIdleRetry(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) {
                if (lastError != null) throw lastError
                fail("Timed out waiting for condition after ${timeoutMs}ms")
            }
            try {
                composeRule.waitUntil(remaining) { condition() }
                return
            } catch (e: ComposeNotIdleException) {
                lastError = e
                Thread.sleep(2_000)
            } catch (e: IdlingResourceTimeoutException) {
                lastError = e
                Thread.sleep(2_000)
            }
        }
    }

    /** Runs one conversion end to end and returns the output file found. */
    private fun runConversion(label: String, presetTag: String?): File? {
        if (presetTag != null) {
            composeRule.onNodeWithTag(presetTag)
                .performScrollTo()
                .performClick()
            println("SMOKE $label: preset $presetTag selected")
        }

        // Trim END to 15s via the Exact Input dialog. 15s (not 5s):
        // on this emulator the Media3 setup phase takes ~20s during
        // which progress is unavailable, and a 5s clip then encodes
        // in ~2s — the percent jumps 0 -> 100 with no observable
        // intermediates. A 15s clip encodes for ~60s+.
        composeRule.onNodeWithText("Exact input").performClick()
        waitForText("Set Exact Trim Times")
        composeRule.onNodeWithTag("exact_end_input").performTextClearance()
        composeRule.onNodeWithTag("exact_end_input").performTextInput("15")
        composeRule.onNodeWithText("Apply").performClick()
        waitForText("00:15")
        println("SMOKE $label: trim applied, end time 00:15")

        // Convert (button is below the fold — scroll to it first).
        composeRule.onNodeWithTag("convert_button")
            .performScrollTo()
            .performClick()

        // Observe the ViewModel's conversionState from a background
        // thread at 200ms intervals: percents seen, max elapsed, and
        // whether progress was ever reported as available.
        val vm = ViewModelProvider(composeRule.activity)[ConverterViewModel::class.java]
        val seenPercents = Collections.synchronizedSet(mutableSetOf<Int>())
        var maxElapsed = 0L
        var sawAvailable = false
        var pollCount = 0
        val stop = AtomicBoolean(false)
        val poller = thread(start = true) {
            while (!stop.get()) {
                val state = vm.conversionState.value
                if (state is ConversionState.Converting) {
                    seenPercents.add(state.progress.percent)
                    if (state.progress.elapsedSeconds > maxElapsed) {
                        maxElapsed = state.progress.elapsedSeconds
                    }
                    if (state.progress.progressAvailable) sawAvailable = true
                    pollCount++
                }
                Thread.sleep(200)
            }
        }
        try {
            waitForTag("conversion_status_card")
            println("SMOKE $label: converting status bar shown")
            captureScreenshot(composeRule, "03-converting-$label")
            waitUntilWithIdleRetry(300_000) {
                composeRule.onAllNodesWithText("Conversion Complete!")
                    .fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag("conversion_error_card")
                        .fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            stop.set(true)
            poller.join(5_000)
        }
        val stateCounts = TranscodeDiagnostics.lastRunStateCounts
        val maxPercent = TranscodeDiagnostics.lastRunMaxPercent
        println(
            "SMOKE $label PROGRESS-DIAG polls=$pollCount percents=$seenPercents " +
                "maxElapsed=${maxElapsed}s sawAvailable=$sawAvailable " +
                "rawStates=$stateCounts maxPercent=$maxPercent " +
                "(states: 0=NOT_STARTED 1=WAITING 2=AVAILABLE 3=UNAVAILABLE)",
        )
        if (composeRule.onAllNodesWithTag("conversion_error_card")
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            fail("Conversion failed in the app UI (error card shown) [$label]")
        }
        assertTrue(
            "Conversion did not complete within 300s [$label]; percents seen: $seenPercents",
            composeRule.onAllNodesWithText("Conversion Complete!")
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertTrue(
            "Converting state was never observed during the export [$label]; polls: $pollCount",
            pollCount > 0,
        )
        assertTrue(
            "Elapsed time never advanced during the export [$label] — the " +
                "status bar gave no live feedback at all",
            maxElapsed > 0,
        )
        assertTrue(
            "No Media3 progress-state diagnostics were recorded [$label] — " +
                "the progress investigation has no data",
            stateCounts.isNotEmpty(),
        )

        // Success screen with Open + Share.
        composeRule.onNodeWithText("Open").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
        println("SMOKE $label: success screen shown with Open + Share")
        captureScreenshot(composeRule, "04-success-$label")

        // A real output file on disk.
        val out = newestOutputFile()
        val mediaStoreHits = queryMediaStore()
        println("SMOKE $label: output dir contents: ${outputDirListing()}")
        println("SMOKE $label: MediaStore Convert264 entries: $mediaStoreHits")
        assertTrue(
            "No _h264.mp4 output in Movies/Convert264 and none in MediaStore " +
                "[$label] (dir: ${outputDirListing()}, MediaStore: $mediaStoreHits)",
            out != null || mediaStoreHits.isNotEmpty(),
        )
        if (out != null) {
            println("SMOKE $label OUTPUT file=${out.name} bytes=${out.length()}")
        }
        // No 0-byte phantom rows in MediaStore: publishFinalFile used
        // to insert a MediaStore row unconditionally and only write
        // bytes into it when the direct copy failed, leaving an empty
        // "<name> (1).mp4" entry next to every real file. That is
        // fixed — but the row for a real file is created by the ASYNC
        // MediaScanner, which can still show bytes=0/durationMs=0
        // seconds after the success screen (the file itself is
        // complete on disk; killed the v0.1.2 validation run in
        // run 2). So poll until the scanner catches up (bounded) and
        // only fail on a row that STAYS empty — a genuinely stuck
        // 0-byte row still fails.
        var settledHits = mediaStoreHits
        var emptyRows = settledHits.filter { it.contains("bytes=0") }
        val settleDeadline = System.currentTimeMillis() + 60_000
        while (emptyRows.isNotEmpty() && System.currentTimeMillis() < settleDeadline) {
            Thread.sleep(2_000)
            settledHits = queryMediaStore()
            emptyRows = settledHits.filter { it.contains("bytes=0") }
        }
        println("SMOKE $label: MediaStore after settle: $settledHits")
        assertTrue(
            "MediaStore has 0-byte phantom entries after $label: $emptyRows",
            emptyRows.isEmpty(),
        )
        return out
    }

    @Test
    fun sampleClipConvertsEndToEnd() {
        // 1. Launch screen.
        waitForText("Try sample clip", 120_000)
        captureScreenshot(composeRule, "01-launch")
        composeRule.onNodeWithText("Try sample clip").performClick()

        // 2. Sample loaded (generation is wall-clock bound on a slow
        // emulator: 90 frames can take ~90s to encode on first run;
        // the file is cached in the app cache dir afterwards).
        waitForTag("selected_video_card", 240_000)
        println("SMOKE sample loaded")
        captureScreenshot(composeRule, "02-sample-loaded")

        // 3. Run 1: Original preset (no scale effect).
        val out1 = runConversion("run1-original", null)
        assertTrue("Run 1 produced no output file on disk", out1 != null)

        // 4. Run 2: 720p preset (Presentation scale effect — a
        // different encode path; does Media3 report progress there?).
        composeRule.onNodeWithTag("convert_another_button")
            .performScrollTo()
            .performClick()
        waitForText("Try sample clip", 120_000)
        composeRule.onNodeWithText("Try sample clip").performClick()
        waitForTag("selected_video_card", 120_000)
        println("SMOKE sample reloaded for run 2 (cached file)")
        val out2 = runConversion("run2-720p", "preset_p720")
        assertTrue("Run 2 produced no output file on disk", out2 != null)
        assertTrue(
            "Run 2 output is the same file as run 1 — expected a second file",
            out2 != out1,
        )
    }

    private fun outputDir(): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        "Convert264",
    )

    private fun outputDirListing(): List<String> =
        outputDir().listFiles()?.map { "${it.name} (${it.length()} bytes)" } ?: emptyList()

    private fun newestOutputFile(): File? = outputDir().listFiles()
        // Output names are <base>_h264.mp4, <base>_h264_1.mp4, ...
        // (see FormatUtils.generateUniqueOutputName) — a plain
        // endsWith("_h264.mp4") misses the _1/_2 files, which is
        // exactly how run 2 once "found" run 1's file again.
        ?.filter { it.length() > 0 && OUTPUT_NAME_REGEX.containsMatchIn(it.name) }
        ?.maxByOrNull { it.lastModified() }

    companion object {
        private val OUTPUT_NAME_REGEX = Regex("""_h264(_\d+)?\.mp4$""")
    }

    private fun queryMediaStore(): List<String> {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val found = mutableListOf<String>()
        ctx.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
            ),
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%Convert264%"),
            null,
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                found.add(
                    "${cursor.getString(nameIdx)} " +
                        "durationMs=${cursor.getLong(durIdx)} " +
                        "bytes=${cursor.getLong(sizeIdx)}",
                )
            }
        }
        return found
    }
}
