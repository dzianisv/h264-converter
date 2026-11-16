package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Play in-app review + store listing (v0.2.1 — the feedback loop for
 * the Play Store launch).
 *
 * The prompt fires once, after the 3rd successful conversion: a user
 * who just got three videos converted is the one whose rating is
 * worth asking for. Play itself decides whether to show the dialog
 * and how often; we only ever ask once per install.
 *
 * Flavor split (WL-35): the pure gate ([shouldAsk]) and the store
 * link ([openStoreListing]) live here in main; the actual prompt is
 * flavor-provided ([ReviewLauncher]) — the Play flavor shows the
 * in-app review flow, the foss flavor is a no-op so the build links
 * no non-free library.
 */
interface ReviewLauncher {
    fun launch(activity: Activity, onDone: () -> Unit)
}

object ReviewPrompt {
    const val PACKAGE_NAME = "dev.dzianisv.convert264"
    const val MIN_SUCCESSFUL_CONVERSIONS = 3

    fun shouldAsk(successfulConversions: Int, alreadyAsked: Boolean): Boolean =
        !alreadyAsked && successfulConversions >= MIN_SUCCESSFUL_CONVERSIONS

    /**
     * Requests the review flow. [onDone] runs whatever happens —
     * shown, skipped, failed, or not available (foss) — so the
     * caller can mark it asked and never nag again.
     */
    fun launch(activity: Activity, onDone: () -> Unit) {
        ReviewLauncherProvider.launcher.launch(activity, onDone)
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
