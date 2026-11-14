package com.example

import android.net.Uri
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.ConversionSettings
import com.example.model.QueueItem
import com.example.model.QueueItemStatus
import com.example.model.VideoInfo
import com.example.ui.components.ErrorDetailsCard
import com.example.ui.components.QueueItemRow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for the full-error UI (WL-17).
 *
 * The v0.2.2 failure UI was one truncated red line in the queue
 * row and nothing on the detail screen. These tests pin the fix:
 * the summary line is visible, the full details are hidden until
 * the user asks, "Show details" reveals the complete log, and
 * Copy + Try again are present and work.
 */
class ErrorDetailsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val summary = "This video file looks damaged or incomplete."
    private val details =
        "ffmpeg exited with code 1\n" +
            "File: clip.mp4 → MKV · VP9 · Opus\n\n" +
            "[mov,mp4,m4a,3gp,3g2,mj2 @ 0x7f] moov atom not found\n" +
            "clip.mp4: Invalid data found when processing input"

    private fun failedItem() = QueueItem(
        id = "err1",
        video = VideoInfo(
            uri = Uri.parse("content://test/video.mp4"),
            name = "clip.mp4",
            sizeBytes = 1_000_000L,
            durationMs = 60_000L,
            width = 1920,
            height = 1080,
            mimeType = "video/mp4",
            thumbnailBitmap = null,
        ),
        trimRange = 0f..60_000f,
        settings = ConversionSettings(),
        status = QueueItemStatus.Error(summary = summary, details = details),
    )

    @Test
    fun errorCardShowsFullDetailsAndActions() {
        var retried = false
        composeRule.setContent {
            ErrorDetailsCard(
                summary = summary,
                details = details,
                onRetry = { retried = true },
                testTagPrefix = "test_error",
            )
        }
        composeRule.onNodeWithText(summary).assertIsDisplayed()
        composeRule.onNodeWithText("moov atom not found", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("ffmpeg exited with code 1", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("test_error_copy_button").assertIsDisplayed()
        composeRule.onNodeWithTag("test_error_retry_button").performClick()
        assertTrue(retried)
    }

    @Test
    fun failedRowExpandsToFullError() {
        var retried = false
        composeRule.setContent {
            QueueItemRow(
                item = failedItem(),
                onClick = {},
                onDelete = {},
                onOpenResult = {},
                onShareResult = {},
                onRetry = { retried = true },
            )
        }
        // The summary line is visible; the full log is not — yet.
        composeRule.onNodeWithText("Failed: $summary").assertIsDisplayed()
        composeRule.onNodeWithText("moov atom not found", substring = true)
            .assertDoesNotExist()
        // "Show details" reveals the complete error.
        composeRule.onNodeWithTag("error_details_button_err1").performClick()
        composeRule.onNodeWithText("moov atom not found", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("ffmpeg exited with code 1", substring = true)
            .assertIsDisplayed()
        // Copy confirms, Try again fires.
        composeRule.onNodeWithTag("error_err1_copy_button").performClick()
        composeRule.onNodeWithText("Copied").assertIsDisplayed()
        composeRule.onNodeWithTag("error_err1_retry_button").performClick()
        assertTrue(retried)
        // Hiding collapses it again.
        composeRule.onNodeWithTag("error_details_button_err1").performClick()
        composeRule.onNodeWithText("moov atom not found", substring = true)
            .assertDoesNotExist()
    }
}
