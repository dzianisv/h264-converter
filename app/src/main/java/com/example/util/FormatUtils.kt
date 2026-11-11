package com.example.util

import java.io.File
import java.util.Locale

object FormatUtils {

    fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDurationWithMillis(ms: Long): String {
        if (ms <= 0L) return "00:00.0"
        val totalSeconds = ms / 1000
        val tenths = (ms % 1000) / 100
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d.%d", hours, minutes, seconds, tenths)
        } else {
            String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Given an original file name, returns a non-overwriting output
     * file name: <original>_<suffix>.<extension>, e.g.
     * clip_vp9.mkv, clip_h264.mp4, clip_h264_1.mp4.
     */
    fun generateUniqueOutputName(
        originalName: String,
        targetDirectory: File,
        suffix: String,
        extension: String,
    ): String {
        val baseName = sanitizeFileName(originalName.substringBeforeLast('.'))
        var candidate = "${baseName}_$suffix.$extension"
        var counter = 1

        while (File(targetDirectory, candidate).exists()) {
            candidate = "${baseName}_${suffix}_$counter.$extension"
            counter++
        }
        return candidate
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return cleaned.ifBlank { "video" }
    }

    /**
     * Parses a time string like "01:23" or "01:23:45" or "83" into milliseconds.
     * Returns null if invalid.
     */
    fun parseTimeToMs(timeStr: String): Long? {
        val trimmed = timeStr.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(":").map { it.trim() }
        return try {
            when (parts.size) {
                1 -> {
                    // Seconds (could have decimal, e.g. "12.5")
                    val secs = parts[0].toDouble()
                    (secs * 1000).toLong()
                }
                2 -> {
                    // mm:ss
                    val mins = parts[0].toLong()
                    val secs = parts[1].toDouble()
                    (mins * 60 * 1000) + (secs * 1000).toLong()
                }
                3 -> {
                    // hh:mm:ss
                    val hours = parts[0].toLong()
                    val mins = parts[1].toLong()
                    val secs = parts[2].toDouble()
                    (hours * 3600 * 1000) + (mins * 60 * 1000) + (secs * 1000).toLong()
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
