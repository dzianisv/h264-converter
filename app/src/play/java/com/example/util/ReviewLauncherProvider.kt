package com.example.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/** Play flavor: the real in-app review flow (review-ktx). */
object ReviewLauncherProvider {
    val launcher: ReviewLauncher = object : ReviewLauncher {
        override fun launch(activity: Activity, onDone: () -> Unit) {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { request ->
                if (!request.isSuccessful) {
                    onDone()
                    return@addOnCompleteListener
                }
                manager.launchReviewFlow(activity, request.result)
                    .addOnCompleteListener { onDone() }
            }
        }
    }
}
