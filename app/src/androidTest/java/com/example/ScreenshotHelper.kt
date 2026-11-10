package com.example

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Captures the current Compose root as a PNG.
 *
 * Screenshots are written via MediaStore into the public
 * Pictures/Convert264-shots/ directory (NOT the app-private external
 * files dir — adb shell cannot read app-private scoped storage on
 * API 30+, so screenshots written there could never be pulled off
 * the emulator). The CI runner script pulls
 * /sdcard/Pictures/Convert264-shots/ into
 * tool/smoke-screenshots/<run-id>/screenshots/, so screenshots
 * survive the run under unique, non-overwriting names.
 *
 * Capture is best-effort evidence: it never fails the test.
 */
fun captureScreenshot(
    rule: SemanticsNodeInteractionsProvider,
    name: String
) {
    try {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Convert264-shots")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = ctx.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ) ?: run {
            println("SMOKE screenshot FAILED for $name: MediaStore insert returned null")
            return
        }
        ctx.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ctx.contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                },
                null,
                null,
            )
        }
        println("SMOKE screenshot saved: $name.png (${bitmap.width}x${bitmap.height})")
    } catch (e: Throwable) {
        // Screenshots are evidence, not the test — never fail the run
        // because a capture failed on a slow emulator. PixelCopy
        // throws AssertionError ("Failed waiting for PixelCopy!"),
        // not Exception, when the emulator's compositor is
        // unresponsive — catch Throwable so the best-effort
        // contract above actually holds.
        println("SMOKE screenshot FAILED for $name: ${e.message}")
    }
}
