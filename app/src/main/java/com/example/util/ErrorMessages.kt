package com.example.util

/**
 * Plain-language summaries for conversion failures (WL-17).
 *
 * FFmpeg's log lines are jargon ("Invalid data found when
 * processing input", "moov atom not found"). The user sees the
 * summary — one line in plain words — and the full log tail lives
 * in the error details, unabridged, so the cause is never hidden
 * behind an ellipsis. Only patterns this file recognizes get a
 * specific summary; anything else is honestly "Conversion failed."
 * with the raw log in the details.
 *
 * Pure function — no Android calls — so it is unit-tested, not
 * eyeballed.
 */
object ErrorMessages {

    fun summarizeFfmpegLog(logTail: String): String {
        val log = logTail.lowercase()
        return when {
            "moov atom not found" in log ||
                "invalid data found when processing input" in log ->
                "This video file looks damaged or incomplete."
            "unknown encoder" in log ||
                ("encoder" in log && "not found" in log) ->
                "That video codec isn't available on this phone."
            "permission denied" in log ->
                "The app couldn't read the video. Check storage permission and try again."
            "no space left on device" in log ->
                "The phone ran out of storage space."
            "operation not permitted" in log ->
                "The phone blocked this conversion."
            "invalid argument" in log ->
                "The conversion settings don't work for this video."
            else -> "Conversion failed."
        }
    }
}
