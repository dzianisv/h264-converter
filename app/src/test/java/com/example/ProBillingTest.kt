package com.example

import com.example.util.ProGate
import com.example.util.ProProduct
import com.example.util.ProPurchaseLogic
import com.example.util.PurchaseInfo
import com.example.util.PurchaseState
import com.example.util.UnavailableProBilling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Pro gate + purchase logic (v0.2.6, WL-14).
 * Pure logic only — no BillingClient, no Play dependency — so
 * these run on the JVM for both flavors.
 */
class ProBillingTest {

    private fun purchase(
        productIds: List<String> = listOf(ProProduct.PRODUCT_ID),
        state: PurchaseState = PurchaseState.PURCHASED,
        acknowledged: Boolean = true,
    ) = PurchaseInfo(productIds, state, acknowledged, "token-1")

    @Test
    fun productIdAndFallbackPrice() {
        assertEquals("convert264_pro", ProProduct.PRODUCT_ID)
        assertEquals("$9.99", ProProduct.FALLBACK_PRICE)
        assertEquals("$9.99", ProProduct.displayPrice(null))
        assertEquals("€10.99", ProProduct.displayPrice("€10.99"))
    }

    @Test
    fun gate_batchAndBackgroundNeedPro() {
        assertFalse(ProGate.canBatch(false))
        assertTrue(ProGate.canBatch(true))
        assertFalse(ProGate.canBackground(false))
        assertTrue(ProGate.canBackground(true))
    }

    @Test
    fun isOwned_requiresPurchasedStateAndProduct() {
        assertTrue(ProPurchaseLogic.isOwned(listOf(purchase())))
        assertFalse(ProPurchaseLogic.isOwned(emptyList()))
        assertFalse(
            "A pending purchase is not owned",
            ProPurchaseLogic.isOwned(listOf(purchase(state = PurchaseState.PENDING))),
        )
        assertFalse(
            "Another product is not Pro",
            ProPurchaseLogic.isOwned(listOf(purchase(productIds = listOf("something_else")))),
        )
        assertTrue(
            "Owned among others still counts",
            ProPurchaseLogic.isOwned(
                listOf(
                    purchase(productIds = listOf("something_else")),
                    purchase(),
                ),
            ),
        )
    }

    @Test
    fun needsAcknowledge_onlyUnacknowledgedPurchasedPro() {
        assertTrue(
            ProPurchaseLogic.needsAcknowledge(purchase(acknowledged = false)),
        )
        assertFalse(
            "Already acknowledged",
            ProPurchaseLogic.needsAcknowledge(purchase(acknowledged = true)),
        )
        assertFalse(
            "Pending purchases are not acknowledged",
            ProPurchaseLogic.needsAcknowledge(
                purchase(state = PurchaseState.PENDING, acknowledged = false),
            ),
        )
        assertFalse(
            "Other products are not acknowledged here",
            ProPurchaseLogic.needsAcknowledge(
                purchase(productIds = listOf("something_else"), acknowledged = false),
            ),
        )
    }

    @Test
    fun unavailableBilling_reportsUnavailable() {
        // The stub's launchPurchase is a one-liner (onDone(false))
        // and needs a real Activity, so it is covered by the
        // instrumented upsell test (button disabled) instead.
        assertFalse(UnavailableProBilling.available)
        var sawState: com.example.util.ProState? = null
        UnavailableProBilling.refresh { state, price, ready ->
            sawState = state
            assertEquals(null, price)
            assertFalse(ready)
        }
        assertEquals(com.example.util.ProState.UNAVAILABLE, sawState)
    }
}
