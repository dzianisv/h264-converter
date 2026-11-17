package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented test for the Pro upsell (v0.2.6, WL-14).
 *
 * On the CI emulator there is no Play Store, so BillingClient can
 * never connect: the upsell dialog must still open, and the Get Pro
 * button must be disabled (no purchase can be launched). The real
 * purchase flow is verified by the sandbox purchase once the Play
 * Console product exists (WL-36).
 *
 * The Pro buttons live at the bottom of the queue LazyColumn, below
 * the history left by earlier tests in the same run — so they are
 * not composed until scrolled into view. performScrollTo() cannot
 * find an uncomposed lazy item (v0.2.7 smoke: node not found), so
 * this test swipes the list up until the button exists.
 */
class ProUpsellTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Swipes the queue list up until the tagged node is actually displayed, then returns. */
    private fun scrollTo(tag: String) {
        repeat(10) {
            // Presence in the tree is not enough: a LazyColumn
            // composes items just beyond the viewport, so the node
            // can exist while still off-screen (v0.2.7 smoke re-run:
            // node found, assertIsDisplayed failed).
            val nodes = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
            if (nodes.isNotEmpty() && nodes[0].isDisplayed) return
            composeRule.onRoot().performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test
    fun freeUserSeesUpsellAndPurchaseIsDisabledWithoutPlay() {
        // Fresh install on the emulator: not Pro.
        scrollTo("get_pro_button")
        composeRule.onNodeWithTag("get_pro_button").performClick()
        composeRule.onNodeWithText("Convert264 Pro").assertIsDisplayed()
        composeRule.onNodeWithText("One-time purchase, \$9.99. No subscription.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("buy_pro_button").assertIsNotEnabled()
        composeRule.onNodeWithText("Not now").performClick()

        // The whole-folder entry opens the same upsell for free users.
        scrollTo("add_folder_button")
        composeRule.onNodeWithTag("add_folder_button").performClick()
        composeRule.onNodeWithText("Convert264 Pro").assertIsDisplayed()
    }
}
