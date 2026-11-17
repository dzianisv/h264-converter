package com.example.util

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/** Play flavor: the real Google Play Billing client (WL-14). */
object ProBillingProvider {
    fun create(context: Context): ProBilling = PlayProBilling(context.applicationContext)
}

private class PlayProBilling(context: Context) : ProBilling {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    override val available: Boolean = true

    private var productDetails: ProductDetails? = null
    private var connected = false
    private var connecting = false
    private var playMissing = false
    private val pendingAfterConnect = ArrayDeque<(Boolean) -> Unit>()
    private var purchaseCallback: ((Boolean) -> Unit)? = null

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    private fun post(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    private fun Purchase.toInfo(): PurchaseInfo = PurchaseInfo(
        productIds = products,
        state = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
            else -> PurchaseState.UNSPECIFIED
        },
        acknowledged = isAcknowledged,
        token = purchaseToken,
    )

    /** Runs [block] once connected; false = no Play on this device, or the connection failed. */
    private fun ensureConnected(block: (Boolean) -> Unit) {
        if (connected) {
            block(true)
            return
        }
        if (playMissing) {
            block(false)
            return
        }
        pendingAfterConnect.add(block)
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                } else if (result.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    // No Play Store on this device — do not retry every refresh.
                    playMissing = true
                }
                val waiting = pendingAfterConnect.toList()
                pendingAfterConnect.clear()
                waiting.forEach { it(connected) }
            }

            override fun onBillingServiceDisconnected() {
                connected = false
            }
        })
    }

    private fun queryProductDetails(onDone: (ProductDetails?) -> Unit) {
        productDetails?.let { onDone(it); return }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProProduct.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            ).build()
        client.queryProductDetailsAsync(params) { _, detailsList ->
            val found = detailsList.firstOrNull { it.productId == ProProduct.PRODUCT_ID }
            productDetails = found
            onDone(found)
        }
    }

    private fun queryOwned(onDone: (Boolean) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { _, purchases ->
            val infos = purchases.map { it.toInfo() }
            val owned = ProPurchaseLogic.isOwned(infos)
            // Acknowledge any unacknowledged purchase of our product —
            // Play re-delivers unacknowledged purchases, and an
            // unacknowledged purchase is refunded after 3 days.
            infos.filter { ProPurchaseLogic.needsAcknowledge(it) }
                .forEach { acknowledge(it.token) }
            onDone(owned)
        }
    }

    private fun acknowledge(token: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()
        client.acknowledgePurchase(params) { /* best effort */ }
    }

    override fun refresh(onResult: (ProState, String?, Boolean) -> Unit) {
        ensureConnected { ok ->
            if (!ok) {
                post {
                    onResult(
                        if (playMissing) ProState.UNAVAILABLE else ProState.UNKNOWN,
                        null,
                        false,
                    )
                }
                return@ensureConnected
            }
            queryProductDetails { details ->
                queryOwned { owned ->
                    post {
                        onResult(
                            if (owned) ProState.PRO else ProState.FREE,
                            details?.oneTimePurchaseOfferDetails?.formattedPrice,
                            details != null,
                        )
                    }
                }
            }
        }
    }

    override fun launchPurchase(activity: Activity, onDone: (Boolean) -> Unit) {
        purchaseCallback = onDone
        ensureConnected { ok ->
            if (!ok) {
                takePurchaseCallback()?.let { post { it(false) } }
                return@ensureConnected
            }
            queryProductDetails { details ->
                if (details == null) {
                    // The product does not exist in Play Console yet —
                    // no purchase can be launched. The upsell disables
                    // the button until billing is ready, so this is
                    // the safety net, not the normal path.
                    takePurchaseCallback()?.let { post { it(false) } }
                    return@queryProductDetails
                }
                val params = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build(),
                        ),
                    ).build()
                val result = client.launchBillingFlow(activity, params)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    takePurchaseCallback()?.let { post { it(false) } }
                }
                // The success path resolves in onPurchasesUpdated.
            }
        }
    }

    private fun takePurchaseCallback(): ((Boolean) -> Unit)? {
        val cb = purchaseCallback
        purchaseCallback = null
        return cb
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val cb = purchaseCallback ?: return
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val infos = (purchases ?: emptyList()).map { it.toInfo() }
                val owned = ProPurchaseLogic.isOwned(infos)
                infos.filter { ProPurchaseLogic.needsAcknowledge(it) }
                    .forEach { acknowledge(it.token) }
                purchaseCallback = null
                post { cb(owned) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                purchaseCallback = null
                queryOwned { owned -> post { cb(owned) } }
            }
            else -> {
                // USER_CANCELED and every other failure: no purchase.
                purchaseCallback = null
                post { cb(false) }
            }
        }
    }
}
