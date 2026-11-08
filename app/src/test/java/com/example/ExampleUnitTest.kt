package com.example

import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", FormatUtils.formatDuration(0L))
        assertEquals("00:45", FormatUtils.formatDuration(45_000L))
        assertEquals("01:25", FormatUtils.formatDuration(85_000L))
        assertEquals("01:05:30", FormatUtils.formatDuration(3930_000L))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FormatUtils.formatFileSize(0L))
        assertEquals("500 B", FormatUtils.formatFileSize(500L))
        assertEquals("1.5 KB", FormatUtils.formatFileSize(1536L))
        assertEquals("10.0 MB", FormatUtils.formatFileSize(10 * 1024 * 1024L))
        assertEquals("1.50 GB", FormatUtils.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testParseTimeToMs() {
        assertEquals(15_000L, FormatUtils.parseTimeToMs("15"))
        assertEquals(85_000L, FormatUtils.parseTimeToMs("01:25"))
        assertEquals(3665_000L, FormatUtils.parseTimeToMs("01:01:05"))
        assertNull(FormatUtils.parseTimeToMs("invalid"))
        assertNull(FormatUtils.parseTimeToMs(""))
    }

    @Test
    fun testUniqueOutputNamingNeverOverwrites() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "convert264_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val name1 = FormatUtils.generateUniqueOutputName("vacation.mov", tempDir)
        assertEquals("vacation_h264.mp4", name1)

        // Create the file so it exists
        File(tempDir, name1).createNewFile()

        val name2 = FormatUtils.generateUniqueOutputName("vacation.mov", tempDir)
        assertEquals("vacation_h264_1.mp4", name2)

        File(tempDir, name2).createNewFile()
        val name3 = FormatUtils.generateUniqueOutputName("vacation.mov", tempDir)
        assertEquals("vacation_h264_2.mp4", name3)

        tempDir.deleteRecursively()
    }
}
