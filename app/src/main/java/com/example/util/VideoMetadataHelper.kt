package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoMetadataHelper {

    suspend fun extractVideoInfo(context: Context, uri: Uri): VideoInfo? = withContext(Dispatchers.IO) {
        var displayName = "video.mp4"
        var sizeBytes = 0L

        // 1. Query display name and size from ContentResolver
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to URI last path segment
            uri.lastPathSegment?.let { segment ->
                val clean = segment.substringAfterLast('/')
                if (clean.isNotBlank()) displayName = clean
            }
        }

        // If size is still 0, try openFileDescriptor statSize
        if (sizeBytes <= 0L) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    sizeBytes = pfd.statSize
                }
            } catch (_: Exception) {
                // Ignore
            }
        }

        // 2. Extract metadata and thumbnail using MediaMetadataRetriever
        var durationMs = 0L
        var width = 0
        var height = 0
        var mimeType = "video/*"
        var thumbnail: Bitmap? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            val parsedWidth = widthStr?.toIntOrNull() ?: 0
            val parsedHeight = heightStr?.toIntOrNull() ?: 0
            val rotation = rotationStr?.toIntOrNull() ?: 0

            // Account for display rotation
            if (rotation == 90 || rotation == 270) {
                width = parsedHeight
                height = parsedWidth
            } else {
                width = parsedWidth
                height = parsedHeight
            }

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                mimeType = it
            }

            // Extract high quality thumbnail at 1 second or middle or 0
            val timeToSeekUs = if (durationMs > 2000L) 1_000_000L else 0L
            thumbnail = retriever.getFrameAtTime(timeToSeekUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (_: Exception) {
            // If retriever fails, we still return the basic info if duration is unknown
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // Ignore
            }
        }

        VideoInfo(
            uri = uri,
            name = displayName,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            width = width,
            height = height,
            mimeType = mimeType,
            thumbnailBitmap = thumbnail
        )
    }
}
