package com.example

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
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
 * Flow: Try Sample Clip -> trim END to 5s via Exact Input -> Convert
 * -> require the progress percent to actually advance -> require the
 * "Conversion Complete!" screen with Open + Share -> require a real
 * _h264.mp4 output file on disk.
 */
class Convert264SmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sampleClipConvertsEndToEnd() {
        // 1. Try Sample Clip
        composeRule.waitUntil(120_000) {
            composeRule.onAllNodesWithText("Try Sample")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Try Sample").performClick()

        // 2. Sample loaded (generation is wall-clock bound on a slow
        // emulator: 90 frames can take ~90s to encode).
        composeRule.waitUntil(240_000) {
            composeRule.onAllNodesWithText("SOURCE VIDEO")
                .fetchSemanticsNodes().isNotEmpty()
        }
        println("SMOKE sample loaded")

        // 3. Trim END to 5s via the Exact Input dialog (keeps the
        // transcode fast even though the sample is ~35s long here).
        composeRule.onNodeWithText("Exact Input").performClick()
        composeRule.waitUntil(60_000) {
            composeRule.onAllNodesWithText("Set Exact Trim Times")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("exact_end_input").performTextClearance()
        composeRule.onNodeWithTag("exact_end_input").performTextInput("5")
        composeRule.onNodeWithText("Apply").performClick()
        composeRule.waitUntil(60_000) {
            composeRule.onAllNodesWithText("00:05")
                .fetchSemanticsNodes().isNotEmpty()
        }
        println("SMOKE trim applied: end time 00:05")

        // 4. Convert (button is below the fold — scroll to it first).
        composeRule.onNodeWithTag("convert_button")
            .performScrollTo()
            .performClick()

        // 5. Poll the progress percent while converting; require it
        // to actually advance (not just 0% -> done).
        val seen = mutableSetOf<Int>()
        val deadline = System.currentTimeMillis() + 300_000
        var success = false
        while (System.currentTimeMillis() < deadline) {
            if (composeRule.onAllNodesWithText("Conversion Complete!")
                    .fetchSemanticsNodes().isNotEmpty()
            ) {
                success = true
                break
            }
            val errors = composeRule.onAllNodesWithTag("conversion_error_card")
                .fetchSemanticsNodes()
            if (errors.isNotEmpty()) {
                fail("Conversion failed in the app UI (error card shown)")
            }
            for (node in composeRule.onAllNodesWithTag("conversion_percent_text")
                .fetchSemanticsNodes()
            ) {
                val text = node.config
                    .getOrElse(SemanticsProperties.Text) { emptyList() }
                    .firstOrNull()?.text ?: continue
                text.trim().removeSuffix("%").toIntOrNull()?.let { seen.add(it) }
            }
            Thread.sleep(1000)
        }
        println("SMOKE progress values observed: $seen")
        assertTrue(
            "Conversion did not complete within 300s; progress seen: $seen",
            success,
        )
        assertTrue(
            "Progress bar never advanced (no intermediate %); values seen: $seen",
            seen.any { it in 1..99 },
        )

        // 6. Success screen with Open + Share.
        composeRule.onNodeWithText("Open").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
        println("SMOKE success screen shown with Open + Share")

        // 7. A real output file on disk.
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Convert264",
        )
        val outputs = dir.listFiles()
            ?.filter { it.name.endsWith("_h264.mp4") && it.length() > 0 }
            ?: emptyList()
        val mediaStoreHits = queryMediaStore()
        println("SMOKE output dir ${dir.absolutePath}: ${dir.listFiles()?.map { "${it.name} (${it.length()} bytes)" }}")
        println("SMOKE MediaStore Convert264 entries: $mediaStoreHits")
        assertTrue(
            "No _h264.mp4 output in ${dir.absolutePath} and none in MediaStore " +
                "(dir contents: ${dir.listFiles()?.map { it.name }}, MediaStore: $mediaStoreHits)",
            outputs.isNotEmpty() || mediaStoreHits.isNotEmpty(),
        )
        val out = outputs.maxByOrNull { it.lastModified() }
        if (out != null) {
            println("SMOKE OUTPUT file=${out.name} bytes=${out.length()}")
        }
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
