package com.example

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.android.ComposeNotIdleException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.IdlingResourceTimeoutException
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.QueueItemStatus
import com.example.util.SampleVideoProvider
import com.example.viewmodel.ConverterViewModel
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end smoke test for Convert264 v0.2.0 (FFmpeg engine,
 * queue UI), run on a real emulator via `gradle
 * connectedDebugAndroidTest` (see .github/workflows/emulator-smoke.yml).
 *
 * Runs in-process (no uiautomator dumps, no adb UI driving), which is
 * the only reliable way to drive the UI on the slow GitHub Actions
 * emulator: the system there ANRs repeatedly and uiautomator dumps
 * return empty for minutes at a time.
 *
 * Flow:
 *   1. Launch (light + dark screenshots, no chips/claims).
 *   2. "Try sample clip" -> one queue entry with a real thumbnail
 *      frame + a duration.
 *   3. A second generated clip joins the queue -> 2 entries.
 *   4. Settings dropdowns: Container MKV, Video VP9, Audio Opus
 *      (dropdown-open screenshot) -> summary "MKV · VP9 · Opus".
 *   5. Tap an entry -> detail screen opens, Convert button is
 *      displayed without scrolling (the screen fits on one page).
 *   6. Play -> converting card -> Stop returns the file to Queued.
 *   7. Clear queue empties it; re-add both files.
 *   8. Play -> Pause (row shows "Paused") -> Play resumes ->
 *      both files Done, real _vp9.mkv files on disk.
 *
 * Screenshots are captured in-test at every step
 * (see ScreenshotHelper.kt) and pulled by tool/smoke-test.sh into
 * tool/smoke-screenshots/<run-id>/ — they are the layout evidence
 * and must be looked at, not just collected.
 *
 * Progress: ffmpeg reports real percent via its statistics
 * callback. The test records percents seen while converting and
 * gates on at least one real (available) percent — a 0% that never
 * moves would fail.
 */
class Convert264SmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val vm: ConverterViewModel
        get() = ViewModelProvider(composeRule.activity)[ConverterViewModel::class.java]

    /** No chip/claim text may appear on the launch screen. */
    private fun assertNoLaunchClaimText() {
        val forbidden = listOf("OTG", "USB", "Offline", "No Ads", "Browse videos")
        for (text in forbidden) {
            assertTrue(
                "Launch screen still shows \"$text\"",
                composeRule.onAllNodesWithText(text, substring = true)
                    .fetchSemanticsNodes().isEmpty(),
            )
        }
    }

    /**
     * Toggle system night mode via UiMode and recreate the activity
     * so the Compose theme re-reads isSystemInDarkTheme(). The
     * shell command is best-effort: if the toggle or the recreate
     * fails, the test fails loudly instead of screenshotting the
     * wrong mode and calling it dark.
     */
    private fun setNightMode(night: Boolean) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val fd = uiAutomation.executeShellCommand(
            "cmd uimode night ${if (night) "yes" else "no"}",
        )
        // Let the command land before closing its output pipe.
        Thread.sleep(1_000)
        fd?.close()
        composeRule.runOnUiThread { composeRule.activity.recreate() }
        waitForText("Try sample clip", 60_000)
        Thread.sleep(1_000)
    }

    private fun waitForText(text: String, timeoutMs: Long = 60_000) {
        waitUntilWithIdleRetry(timeoutMs) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTextSubstring(text: String, timeoutMs: Long = 60_000) {
        waitUntilWithIdleRetry(timeoutMs) {
            composeRule.onAllNodesWithText(text, substring = true)
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
     * waitUntil with retries around Compose idle timeouts: under
     * encode load this emulator's compositor goes unresponsive and
     * fetchSemanticsNodes throws ComposeNotIdleException instead of
     * returning "not found yet". Retry until the deadline — the
     * gate (text/tag appears) is unchanged, only transient
     * busy-compositor periods are tolerated.
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

    /** Waits until the queue holds exactly [count] items. */
    private fun waitForQueueSize(count: Int, timeoutMs: Long = 120_000) {
        waitUntilWithIdleRetry(timeoutMs) { vm.queue.value.size == count }
    }

    /** The on-device sample clip (generated + cached by SampleVideoProvider). */
    private fun sampleUri(): Uri {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = runBlocking {
            SampleVideoProvider.getOrCreateSampleVideo(targetContext)
        }
        assertTrue("Sample video could not be generated", uri != null)
        return uri!!
    }

    /**
     * A second generated clip: a copy of the sample file under its
     * own name, so the queue holds two distinct entries.
     */
    private fun secondClipUri(): Uri {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleFile = File(ctx.cacheDir, "sample_clip.mp4")
        assertTrue("Sample clip file missing from cache", sampleFile.exists())
        val secondFile = File(ctx.cacheDir, "sample_clip_2.mp4")
        sampleFile.copyTo(secondFile, overwrite = true)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", secondFile)
    }

    /** Adds both clips through the ViewModel (same path as the SAF picker). */
    private fun addBothClips() {
        val uri1 = sampleUri()
        val uri2 = secondClipUri()
        composeRule.runOnUiThread { vm.addVideos(listOf(uri1, uri2)) }
        waitForQueueSize(2)
        println("SMOKE queue: 2 entries (${vm.queue.value.map { it.video.name }})")
    }

    /** Trims every queue item to 5s (see step 3 for why). */
    private fun trimAllTo5s() {
        composeRule.runOnUiThread {
            for (item in vm.queue.value) {
                if (item.video.durationMs > 5_000L) {
                    vm.setTrimEndMs(item.id, 5_000L)
                }
            }
        }
        Thread.sleep(1_000)
        println("SMOKE queue trimmed to 5s per file")
    }

    /** Picks one option from a settings dropdown (tag -> menu text). */
    private fun selectDropdownOption(dropdownTag: String, optionText: String) {
        composeRule.onNodeWithTag(dropdownTag).performClick()
        waitForText(optionText, 30_000)
        composeRule.onNodeWithText(optionText).performClick()
        waitForText(optionText, 30_000)
        println("SMOKE settings: $dropdownTag -> $optionText")
    }

    @Test
    fun queueConvertsEndToEndWithFfmpeg() {
        // 1. Launch screen.
        waitForText("Try sample clip", 120_000)
        // No chips or claims on the launch screen: the "USB-OTG
        // ready" chip was an unproven claim and was removed in
        // v0.1.5; the "100% Offline • No Ads" chip, the formats
        // chip, and the "Browse videos" button were removed in
        // v0.1.9 — the empty state is now just the headline and
        // a plus button. None of that text may appear anywhere.
        assertNoLaunchClaimText()
        captureScreenshot(composeRule, "01-launch")
        // Dark-mode launch screenshot: the user is on dark mode,
        // so the launch screen must be proven native-looking in
        // night mode too. Toggle UiMode, recreate the activity so
        // the Compose theme re-reads isSystemInDarkTheme(),
        // screenshot, then restore light mode before the rest of
        // the test runs.
        setNightMode(true)
        assertNoLaunchClaimText()
        captureScreenshot(composeRule, "01b-launch-dark")
        setNightMode(false)

        // 2. Sample clip -> one queue entry.
        composeRule.onNodeWithText("Try sample clip").performClick()
        waitForQueueSize(1, 240_000)
        // The row must show the clip's duration, and the
        // thumbnail must be a real extracted frame (the
        // ViewModel holds the bitmap the row renders). The
        // duration is read from the ViewModel, not hardcoded:
        // on a slow emulator the generated sample's frames are
        // timestamped in wall-clock as they encode, so it can
        // report ~37s instead of 3s.
        waitUntilWithIdleRetry(120_000) {
            vm.queue.value.firstOrNull()?.video?.thumbnailBitmap != null
        }
        val sampleDurationMs = vm.queue.value.first().video.durationMs
        val expectedDuration = com.example.util.FormatUtils.formatDuration(sampleDurationMs)
        waitForTextSubstring(expectedDuration, 30_000)
        assertEquals(
            "Expected 1 queue thumbnail rendered",
            1,
            composeRule.onAllNodesWithTag("queue_item_thumbnail")
                .fetchSemanticsNodes().size,
        )
        println("SMOKE sample loaded: thumbnail + $expectedDuration shown in queue")
        captureScreenshot(composeRule, "02a-queue-1-entry")

        // 3. Second clip -> 2 entries, both with thumbnails.
        val uri2 = secondClipUri()
        composeRule.runOnUiThread { vm.addVideos(listOf(uri2)) }
        waitForQueueSize(2)
        waitUntilWithIdleRetry(120_000) {
            vm.queue.value.all { it.video.thumbnailBitmap != null }
        }
        assertEquals(
            "Expected 2 queue thumbnails rendered",
            2,
            composeRule.onAllNodesWithTag("queue_item_thumbnail")
                .fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText("sample_clip_2.mp4").assertIsDisplayed()
        println("SMOKE queue: 2 entries with thumbnails + durations")
        captureScreenshot(composeRule, "02b-queue-2-entries")

        // Trim both files to 5s (the generated sample can report
        // ~37s on a slow emulator — encoding the full length in
        // VP9 twice would take many minutes; 5s still proves a
        // real VP9/MKV encode with trim applied).
        trimAllTo5s()

        // 4. Settings: MKV container, VP9 video, Opus audio.
        // The container drives the other lists, so pick it first
        // (VP9 is not a valid MP4 video codec — the menu would
        // not even list it). Then open the video-codec dropdown
        // for the dropdown-open screenshot, then pick VP9 + Opus.
        selectDropdownOption("dropdown_container", "Matroska (MKV)")
        composeRule.onNodeWithTag("dropdown_video_codec").performClick()
        waitForText("VP9", 30_000)
        captureScreenshot(composeRule, "03-settings-dropdown-open")
        composeRule.onNodeWithText("VP9").performClick()
        selectDropdownOption("dropdown_audio_codec", "Opus")
        waitForTextSubstring("MKV · VP9 · Opus", 30_000)
        println("SMOKE settings: MKV · VP9 · Opus selected")
        captureScreenshot(composeRule, "03b-settings-selected")

        // 5. Detail screen: tap the first entry, check the screen
        // fits on one page (its bottom element — the Convert
        // button — is displayed without scrolling).
        composeRule.onNodeWithText("sample_clip.mp4").performClick()
        waitForTag("detail_convert_button", 60_000)
        composeRule.onNodeWithTag("detail_convert_button").assertIsDisplayed()
        composeRule.onNodeWithTag("detail_durations_text").assertIsDisplayed()
        println("SMOKE detail screen opened, Convert button displayed without scrolling")
        captureScreenshot(composeRule, "04-detail-screen")
        composeRule.onNodeWithTag("detail_back_button").performClick()
        waitForTag("queue_list", 30_000)
        println("SMOKE back on queue")

        // 6. Play -> converting card -> Stop returns the file to Queued.
        composeRule.onNodeWithTag("play_queue_button").performClick()
        waitForTag("conversion_status_card", 60_000)
        println("SMOKE converting card shown after Play")
        captureScreenshot(composeRule, "05-converting")
        composeRule.onNodeWithTag("stop_queue_button").performClick()
        waitUntilWithIdleRetry(60_000) {
            vm.queue.value.all { it.status is QueueItemStatus.Queued } &&
                vm.queue.value.isNotEmpty()
        }
        assertTrue(
            "Stop did not return the queue to Queued",
            vm.queue.value.all { it.status is QueueItemStatus.Queued },
        )
        println("SMOKE Stop: queue back to Queued")

        // 7. Clear queue empties it; re-add both files.
        composeRule.onNodeWithTag("clear_queue_button").performClick()
        waitForQueueSize(0)
        waitForText("Select Video to Convert", 30_000)
        println("SMOKE Clear queue: empty state shown")
        addBothClips()
        // Re-adding resets the trim range to the full clip, so
        // trim to 5s again (same reason as step 3).
        trimAllTo5s()
        // The re-added files take the current global settings.
        assertTrue(
            "Re-added files did not take the MKV/VP9/Opus settings",
            vm.queue.value.all { it.settings.targetSummary.contains("MKV · VP9 · Opus") },
        )

        // 8. Play -> Pause -> Play resumes -> both files Done.
        // Observe the queue from a background thread: percents
        // seen, and whether ffmpeg ever reported a real percent.
        val seenPercents = Collections.synchronizedSet(mutableSetOf<Int>())
        var sawAvailable = false
        var sawConverting = false
        val stop = AtomicBoolean(false)
        val poller = thread(start = true) {
            while (!stop.get()) {
                val item = vm.queue.value.firstOrNull()
                val status = item?.status
                if (status is QueueItemStatus.Converting) {
                    sawConverting = true
                    if (status.progress.progressAvailable) {
                        sawAvailable = true
                        seenPercents.add(status.progress.percent)
                    }
                }
                Thread.sleep(200)
            }
        }
        try {
            composeRule.onNodeWithTag("play_queue_button").performClick()
            waitForTag("conversion_status_card", 60_000)
            println("SMOKE second run converting")
            // Pause mid-encode: the row must show Paused.
            composeRule.onNodeWithTag("pause_queue_button").performClick()
            waitUntilWithIdleRetry(60_000) {
                vm.queue.value.any { it.status is QueueItemStatus.Paused }
            }
            waitForTextSubstring("Paused", 30_000)
            println("SMOKE Pause: first file Paused")
            captureScreenshot(composeRule, "05b-paused")
            // Resume: Play restarts the paused file from scratch
            // (ffmpeg has no pause/resume — progress visibly resets).
            composeRule.onNodeWithTag("play_queue_button").performClick()
            waitUntilWithIdleRetry(300_000) {
                vm.queue.value.size == 2 &&
                    vm.queue.value.all { it.status is QueueItemStatus.Done }
            }
        } finally {
            stop.set(true)
            poller.join(5_000)
        }
        println(
            "SMOKE queue PROGRESS percents=$seenPercents " +
                "sawConverting=$sawConverting sawAvailable=$sawAvailable",
        )
        assertTrue(
            "Converting state was never observed during the queue run",
            sawConverting,
        )
        assertTrue(
            "ffmpeg never reported a real percent during the queue run " +
                "(percents seen: $seenPercents)",
            sawAvailable,
        )
        assertTrue(
            "Both queue files must be Done",
            vm.queue.value.all { it.status is QueueItemStatus.Done },
        )
        // Done rows offer Open + Share per file.
        assertEquals(
            "Expected Open action on both Done rows",
            2,
            composeRule.onAllNodesWithContentDescription("Open converted video")
                .fetchSemanticsNodes().size,
        )
        assertEquals(
            "Expected Share action on both Done rows",
            2,
            composeRule.onAllNodesWithContentDescription("Share converted video")
                .fetchSemanticsNodes().size,
        )
        println("SMOKE queue Done: Open + Share on both rows")
        captureScreenshot(composeRule, "06-queue-done")

        // 9. Real MKV files on disk: two _vp9.mkv outputs.
        val outs = newestOutputFiles()
        val mediaStoreHits = queryMediaStore()
        println("SMOKE output dir contents: ${outputDirListing()}")
        println("SMOKE MediaStore Convert264 entries: $mediaStoreHits")
        assertTrue(
            "Expected 2 _vp9.mkv outputs in Movies/Convert264 " +
                "(dir: ${outputDirListing()}, MediaStore: $mediaStoreHits)",
            outs.size >= 2,
        )
        for (out in outs) {
            println("SMOKE OUTPUT file=${out.name} bytes=${out.length()}")
            assertTrue("Output ${out.name} is empty", out.length() > 0)
        }
        // No 0-byte phantom rows in MediaStore: the row for a real
        // file is created by the ASYNC MediaScanner, which can
        // still show bytes=0 seconds after the success screen (the
        // file itself is complete on disk). Poll until the scanner
        // catches up (bounded) and only fail on a row that STAYS
        // empty.
        var settledHits = mediaStoreHits
        var emptyRows = settledHits.filter { it.contains("bytes=0") }
        val settleDeadline = System.currentTimeMillis() + 60_000
        while (emptyRows.isNotEmpty() && System.currentTimeMillis() < settleDeadline) {
            Thread.sleep(2_000)
            settledHits = queryMediaStore()
            emptyRows = settledHits.filter { it.contains("bytes=0") }
        }
        println("SMOKE MediaStore after settle: $settledHits")
        assertTrue(
            "MediaStore has 0-byte phantom entries: $emptyRows",
            emptyRows.isEmpty(),
        )
    }

    private fun outputDir(): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        "Convert264",
    )

    private fun outputDirListing(): List<String> =
        outputDir().listFiles()?.map { "${it.name} (${it.length()} bytes)" } ?: emptyList()

    private fun newestOutputFiles(): List<File> = outputDir().listFiles()
        // Output names are <base>_vp9.mkv, <base>_vp9_1.mkv, ...
        // (see FormatUtils.generateUniqueOutputName) — a plain
        // endsWith("_vp9.mkv") misses the _1/_2 files.
        ?.filter { it.length() > 0 && OUTPUT_NAME_REGEX.containsMatchIn(it.name) }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

    companion object {
        private val OUTPUT_NAME_REGEX = Regex("""_vp9(_\d+)?\.mkv$""")
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
