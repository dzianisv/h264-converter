package com.example

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.util.SampleVideoProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * Exercises the share-intent entry point end to end: launches
 * MainActivity with ACTION_SEND + a video content URI, exactly as
 * File Manager / Gallery does, and requires the video to load
 * (SOURCE VIDEO shown) — i.e. MainActivity.handleIntent() actually
 * ran and ConverterViewModel.loadVideo() accepted the shared URI.
 *
 * The shared URI is the on-device sample clip (generated + cached by
 * SampleVideoProvider), so no external file is needed.
 */
class ShareIntentSmokeTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun sharedVideoLoadsViaActionSend() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleUri = runBlocking {
            SampleVideoProvider.getOrCreateSampleVideo(targetContext)
        }
        assertNotNull("Sample video could not be generated", sampleUri)
        println("SMOKE share-intent: sharing sample URI $sampleUri")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, sampleUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitUntil(180_000) {
                composeRule.onAllNodesWithText("SOURCE VIDEO")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            println("SMOKE share-intent: shared video loaded (SOURCE VIDEO shown)")
            captureScreenshot(composeRule, "05-share-intent-loaded")
        }
    }
}
