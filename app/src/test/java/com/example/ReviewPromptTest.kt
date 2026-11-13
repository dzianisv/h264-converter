package com.example

import com.example.util.ReviewPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptTest {

    @Test
    fun `asks at 3 successful conversions`() {
        assertTrue(ReviewPrompt.shouldAsk(3, false))
    }

    @Test
    fun `does not ask before 3 conversions`() {
        assertFalse(ReviewPrompt.shouldAsk(2, false))
    }

    @Test
    fun `never asks twice`() {
        assertFalse(ReviewPrompt.shouldAsk(10, true))
    }
}
