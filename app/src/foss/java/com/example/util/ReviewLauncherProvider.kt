package com.example.util

import android.app.Activity

/**
 * Foss flavor: no Play services on the device, so there is no review
 * flow to show — just report done so the caller marks it asked and
 * never nags.
 */
object ReviewLauncherProvider {
    val launcher: ReviewLauncher = object : ReviewLauncher {
        override fun launch(activity: Activity, onDone: () -> Unit) {
            onDone()
        }
    }
}
