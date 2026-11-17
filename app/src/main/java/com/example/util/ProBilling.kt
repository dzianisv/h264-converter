package com.example.util

import android.app.Activity

/**
 * Play Billing — one-time Pro unlock (v0.2.6, WL-14, decided Nov 17
 * by the product lead: free core + one-time Pro $9.99).
 *
 * Free: every conversion (all containers, all codecs, trim), no ads,
 * no watermark, no account. Pro: batch conversion (multi-select /
 * whole folder) + background conversion.
 *
 * Flavor split (same pattern as ReviewPrompt / WL-35): the contract,
 * the product id, the price fallback and the pure purchase logic
 * live here in main; the actual BillingClient lives in the play
 * flavor behind [ProBillingProvider]. The foss flavor reports
 * billing unavailable, so the build links no non-free library.
 * The Play Console product must exist before a purchase can
 * complete — until then [ProBilling.refresh] reports billing not
 * ready and the upsell disables the purchase button.
 */
object ProProduct {
    /** One-time product id — must match the product created in Play Console. */
    const val PRODUCT_ID = "convert264_pro"

    /** Display price when Play has not returned one yet (the decided store price). */
    const val FALLBACK_PRICE = "$9.99"

    fun displayPrice(playPrice: String?): String = playPrice ?: FALLBACK_PRICE
}

/** What the app knows about the Pro purchase. */
enum class ProState {
    /** Not checked yet, or Play could not be reached (transient). */
    UNKNOWN,
    /** Checked: not owned. */
    FREE,
    /** Checked: owned. */
    PRO,
    /** This build/device cannot do Play Billing at all (foss, or no Play Store). */
    UNAVAILABLE,
}

/** Pure gate: which features need Pro. Free is every conversion; batch + background are Pro. */
object ProGate {
    fun canBatch(isPro: Boolean): Boolean = isPro
    fun canBackground(isPro: Boolean): Boolean = isPro
}

/** A purchase, in the app's own terms — no BillingClient types cross into main. */
enum class PurchaseState { PURCHASED, PENDING, UNSPECIFIED }

data class PurchaseInfo(
    val productIds: List<String>,
    val state: PurchaseState,
    val acknowledged: Boolean,
    val token: String,
)

/** Pure purchase logic: owned? acknowledge? Unit-tested, no Play dependency. */
object ProPurchaseLogic {
    fun isOwned(purchases: List<PurchaseInfo>): Boolean =
        purchases.any {
            it.productIds.contains(ProProduct.PRODUCT_ID) &&
                it.state == PurchaseState.PURCHASED
        }

    fun needsAcknowledge(purchase: PurchaseInfo): Boolean =
        purchase.productIds.contains(ProProduct.PRODUCT_ID) &&
            purchase.state == PurchaseState.PURCHASED &&
            !purchase.acknowledged
}

/** Flavor-provided Play Billing. */
interface ProBilling {
    /** True when this build can talk to Google Play Billing at all (false in foss). */
    val available: Boolean

    /**
     * Connect (if needed) and refresh the owned state + price.
     * [onResult] gets (state, formatted Play price or null,
     * billingReady) — billingReady is true only when Play returned
     * the Pro product, i.e. a purchase can actually be launched.
     * Callbacks run on the main thread.
     */
    fun refresh(onResult: (ProState, String?, Boolean) -> Unit)

    /**
     * Launch the purchase flow. [onDone] gets true only when the
     * purchase completed AND was acknowledged. Callbacks run on the
     * main thread.
     */
    fun launchPurchase(activity: Activity, onDone: (Boolean) -> Unit)
}

/** Billing is unavailable on this build — the foss answer, and the fallback. */
object UnavailableProBilling : ProBilling {
    override val available: Boolean = false

    override fun refresh(onResult: (ProState, String?, Boolean) -> Unit) {
        onResult(ProState.UNAVAILABLE, null, false)
    }

    override fun launchPurchase(activity: Activity, onDone: (Boolean) -> Unit) {
        onDone(false)
    }
}
