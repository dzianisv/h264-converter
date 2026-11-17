package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 */
class ProUpsellTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freeUserSeesUpsellAndPurchaseIsDisabledWithoutPlay() {
        // Fresh install on the emulator: not Pro.
        composeRule.onNodeWithTag("get_pro_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Convert264 Pro").assertIsDisplayed()
        composeRule.onNodeWithText("One-time purchase, \$9.99. No subscription.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("buy_pro_button").assertIsNotEnabled()
        composeRule.onNodeWithText("Not now").performClick()

        // The whole-folder entry opens the same upsell for free users.
        composeRule.onNodeWithTag("add_folder_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Convert264 Pro").assertIsDisplayed()
    }
}
