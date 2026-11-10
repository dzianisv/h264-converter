package com.example

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.util.SampleVideoProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Exercises the share-intent entry point end to end.
 *
 * Two layers, kept separate on purpose:
 *
 * 1. RESOLUTION — PackageManager.queryIntentActivities must list
 *    Convert264 for the intents real senders fire (ACTION_SEND and
 *    ACTION_SEND_MULTIPLE with video/*, ACTION_VIEW with a
 *    content:// URI + video/* type). This is what the share sheet
 *    and "Open with" use; if it fails here, the app never appears
 *    in the list at all. (A null or wildcard type can NOT resolve —
 *    no filter can match it without a blanket wildcard filter, which would
 *    put the app in every share sheet on the phone. That is a
 *    platform limitation, not a bug in this app.)
 *
 * 2. HANDLING — launching MainActivity with a real share intent
 *    (EXTRA_STREAM, clipData-only, SEND_MULTIPLE, null type,
 *    wildcard type) must load the video. The null/wildcard cases are tested
 *    with the intent pinned to our own activity, because
 *    resolution cannot match them (see above) — they prove the
 *    handler, not the filter.
 *
 * The shared URI is the on-device sample clip (generated + cached by
 * SampleVideoProvider), so no external file is needed.
 */
class ShareIntentSmokeTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun sampleUri(): Uri {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = runBlocking {
            SampleVideoProvider.getOrCreateSampleVideo(targetContext)
        }
        assertNotNull("Sample video could not be generated", uri)
        return uri!!
    }

    private fun resolves(intent: Intent): Boolean {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .any { it.activityInfo.packageName == pkg }
    }

    private fun waitForVideoLoaded() {
        composeRule.waitUntil(180_000) {
            composeRule.onAllNodesWithTag("selected_video_card")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun shareTargetResolvesForVideoSend() {
        val uri = sampleUri()

        val sendMp4 = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        assertTrue("ACTION_SEND video/mp4 does not resolve Convert264", resolves(sendMp4))
        println("SMOKE share-resolve: ACTION_SEND video/mp4 resolves")

        val sendWildcard = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        assertTrue("ACTION_SEND video/* does not resolve Convert264", resolves(sendWildcard))
        println("SMOKE share-resolve: ACTION_SEND video/* resolves")

        val sendMultiple = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_DEFAULT)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
        }
        assertTrue("ACTION_SEND_MULTIPLE video/* does not resolve Convert264", resolves(sendMultiple))
        println("SMOKE share-resolve: ACTION_SEND_MULTIPLE video/* resolves")

        // "Open with" from Files / Gallery: ACTION_VIEW with BOTH a
        // content:// URI and a video/* type. The old mimeType-only
        // VIEW filter could never match this — it needed a scheme too.
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        assertTrue("ACTION_VIEW content:// + video/mp4 does not resolve Convert264", resolves(view))
        println("SMOKE share-resolve: ACTION_VIEW content:// + video/mp4 resolves")
    }

    @Test
    fun sharedVideoLoadsViaActionSend() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = sampleUri()
        println("SMOKE share-intent: sharing sample URI $uri")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // ActivityScenario cannot launch an implicit intent: the
            // system resolves ACTION_SEND to the chooser
            // ("resolved to different process system:ui"). Pin the
            // intent to our own MainActivity — action, type and
            // extras stay exactly as a real share would send them.
            component = ComponentName(targetContext, MainActivity::class.java)
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            waitForVideoLoaded()
            println("SMOKE share-intent: shared video loaded (source card shown)")
            captureScreenshot(composeRule, "05-share-intent-loaded")
        }
    }

    @Test
    fun sharedVideoLoadsViaClipDataOnly() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = sampleUri()

        // Some senders put the stream in clipData and leave
        // EXTRA_STREAM empty.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            clipData = ClipData.newUri(targetContext.contentResolver, "video", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            component = ComponentName(targetContext, MainActivity::class.java)
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            waitForVideoLoaded()
            println("SMOKE share-intent: clipData-only share loaded the video")
        }
    }

    @Test
    fun sharedVideoLoadsViaSendMultiple() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = sampleUri()

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            component = ComponentName(targetContext, MainActivity::class.java)
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            waitForVideoLoaded()
            println("SMOKE share-intent: SEND_MULTIPLE loaded the first video")
        }
    }

    @Test
    fun sharedVideoLoadsWhenTypeIsNullOrWildcard() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = sampleUri()

        // Files / Drive / some messengers share videos with a null
        // or */* type. These intents cannot resolve through the
        // filter (see class doc), so the intent is pinned — this
        // proves the handler accepts the stream anyway.
        for (type in listOf<String?>(null, "*/*")) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                this.type = type
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                component = ComponentName(targetContext, MainActivity::class.java)
            }
            ActivityScenario.launch<MainActivity>(intent).use {
                waitForVideoLoaded()
                println("SMOKE share-intent: type=$type share loaded the video")
            }
        }
    }
}
