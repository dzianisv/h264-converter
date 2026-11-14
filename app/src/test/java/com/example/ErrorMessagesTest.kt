package com.example

import com.example.util.ErrorMessages
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the plain-language error summaries (WL-17).
 * The mapper is a pure function: every pattern it recognizes is
 * pinned here, and anything it does not recognize must fall back
 * to an honest "Conversion failed." — never a guessed cause.
 */
class ErrorMessagesTest {

    @Test
    fun damagedFileMoovAtom() {
        assertEquals(
            "This video file looks damaged or incomplete.",
            ErrorMessages.summarizeFfmpegLog(
                "[mov,mp4,m4a,3gp,3g2,mj2 @ 0x7f] moov atom not found\n" +
                    "clip.mp4: Invalid data found when processing input"
            )
        )
    }

    @Test
    fun damagedFileInvalidData() {
        assertEquals(
            "This video file looks damaged or incomplete.",
            ErrorMessages.summarizeFfmpegLog("Invalid data found when processing input")
        )
    }

    @Test
    fun unknownEncoder() {
        assertEquals(
            "That video codec isn't available on this phone.",
            ErrorMessages.summarizeFfmpegLog("Unknown encoder 'libx265'")
        )
    }

    @Test
    fun permissionDenied() {
        assertEquals(
            "The app couldn't read the video. Check storage permission and try again.",
            ErrorMessages.summarizeFfmpegLog(
                "clip.mp4: Permission denied"
            )
        )
    }

    @Test
    fun noSpaceLeft() {
        assertEquals(
            "The phone ran out of storage space.",
            ErrorMessages.summarizeFfmpegLog(
                "av_interleaved_write_frame(): No space left on device"
            )
        )
    }

    @Test
    fun invalidArgument() {
        assertEquals(
            "The conversion settings don't work for this video.",
            ErrorMessages.summarizeFfmpegLog(
                "Error initializing output stream: Invalid argument"
            )
        )
    }

    @Test
    fun unknownLogFallsBackHonestly() {
        assertEquals(
            "Conversion failed.",
            ErrorMessages.summarizeFfmpegLog("some ffmpeg line nobody mapped yet")
        )
        assertEquals("Conversion failed.", ErrorMessages.summarizeFfmpegLog(""))
    }
}
