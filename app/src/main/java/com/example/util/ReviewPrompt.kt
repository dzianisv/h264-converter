package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play in-app review + store listing (v0.2.1 — the feedback loop for
 * the Play Store launch).
 *
 * The prompt fires once, after the 3rd successful conversion: a user
 * who just got three videos converted is the one whose rating is
 * worth asking for. Play itself decides whether to show the dialog
 * and how often; we only ever ask once per install.
 */
object ReviewPrompt {
    const val PACKAGE_NAME = "dev.dzianisv.convert264"
    const val MIN_SUCCESSFUL_CONVERSIONS = 3

    fun shouldAsk(successfulConversions: Int, alreadyAsked: Boolean): Boolean =
        !alreadyAsked && successfulConversions >= MIN_SUCCESSFUL_CONVERSIONS

    /**
     * Requests the Play review flow. [onDone] runs whatever Play does —
     * shown, skipped, or failed — so the caller can mark it asked and
     * never nag again.
     */
    fun launch(activity: Activity, onDone: () -> Unit) {
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

    /** Opens the Play Store listing (market app first, web fallback). */
    fun openStoreListing(context: Context) {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$PACKAGE_NAME"),
        )
        try {
            context.startActivity(market)
        } catch (_: Exception) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE_NAME"),
                ),
            )
        }
    }
}
