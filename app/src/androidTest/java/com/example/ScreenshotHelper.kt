package com.example

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Captures the current Compose root as a PNG under the app's external
 * files dir (world-readable via adb pull, no run-as needed):
 * /sdcard/Android/data/dev.dzianisv.convert264/files/smoke-shots/<name>.png
 *
 * The CI runner script pulls this directory into
 * tool/smoke-screenshots/<run-id>/, so screenshots survive the run
 * under unique, non-overwriting names.
 */
fun <A : androidx.activity.ComponentActivity> captureScreenshot(
    rule: AndroidComposeTestRule<*, A>,
    name: String
) {
    try {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getExternalFilesDir(null),
            "smoke-shots",
        )
        dir.mkdirs()
        FileOutputStream(File(dir, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        println("SMOKE screenshot saved: $name.png (${bitmap.width}x${bitmap.height})")
    } catch (e: Exception) {
        // Screenshots are evidence, not the test — never fail the run
        // because a capture failed on a slow emulator.
        println("SMOKE screenshot FAILED for $name: ${e.message}")
    }
}
