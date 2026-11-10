package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Live preview of the video being trimmed, using the stock Media3
 * player (ExoPlayer) rendering into a plain TextureView — not a
 * custom player.
 *
 * The preview shows the frame at the current trim position: dragging
 * a trim handle seeks the preview to that handle, so the user sees
 * the frame they are cutting at. Play plays the trimmed range only,
 * muted (volume 0 — no audio surprise), and pauses at the trim end.
 *
 * A TextureView (not PlayerView's default SurfaceView) is used
 * deliberately: a SurfaceView renders on a separate surface that
 * screenshots cannot capture, so a SurfaceView preview would
 * photograph as a black box. A TextureView renders inside the view
 * hierarchy, so the frame is visible both on screen and in test
 * screenshots. PlayerView adds nothing here — its controller is
 * disabled anyway and play/pause is a standard IconButton below.
 *
 * Works for the same URIs the converter loads (content:// from SAF
 * and shares, file:// for the generated sample clip) — ExoPlayer
 * resolves them through the ContentResolver, no file path needed.
 * If ExoPlayer cannot play a source at all, the preview falls back
 * to a single representative frame extracted with
 * MediaMetadataRetriever instead of a blank box.
 *
 * The player is released when the preview leaves composition or the
 * source changes, so it never holds the file while converting.
 */
@Composable
fun TrimVideoPreview(
    sourceUri: Uri?,
    videoWidth: Int,
    videoHeight: Int,
    trimRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    if (sourceUri == null) return
    val context = LocalContext.current
    val appContext = context.applicationContext

    val startMs = trimRange.start.toLong().coerceAtLeast(0L)
    val endMs = trimRange.endInclusive.toLong().coerceAtLeast(startMs)
    val aspectRatio = if (videoWidth > 0 && videoHeight > 0) {
        videoWidth.toFloat() / videoHeight.toFloat()
    } else {
        16f / 9f
    }

    var previewFailed by remember(sourceUri) { mutableStateOf(false) }
    var isPlaying by remember(sourceUri) { mutableStateOf(false) }
    var previewPositionMs by remember(sourceUri) { mutableStateOf(startMs) }

    val player = remember(sourceUri) {
        ExoPlayer.Builder(appContext).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
            setMediaItem(MediaItem.fromUri(sourceUri))
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    previewFailed = true
                }
            })
            prepare()
            seekTo(startMs)
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // Follow the trim handles: when the start handle moves, show the
    // frame at the new start; when the end handle moves, show the
    // frame at the new end.
    var lastStartMs by remember(sourceUri) { mutableStateOf(startMs) }
    var lastEndMs by remember(sourceUri) { mutableStateOf(endMs) }
    LaunchedEffect(trimRange) {
        if (startMs != lastStartMs) {
            player.seekTo(startMs)
            previewPositionMs = startMs
        } else if (endMs != lastEndMs) {
            player.seekTo(endMs)
            previewPositionMs = endMs
        }
        lastStartMs = startMs
        lastEndMs = endMs
    }

    // While playing, stop at the trim end and park back at the start.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val pos = player.currentPosition
            previewPositionMs = pos
            if (endMs > startMs && pos >= endMs) {
                player.pause()
                player.seekTo(startMs)
                previewPositionMs = startMs
                isPlaying = false
            } else {
                delay(200)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            contentAlignment = Alignment.Center
        ) {
            if (!previewFailed) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            player.setVideoTextureView(this)
                        }
                    },
                    onRelease = { textureView ->
                        player.clearVideoTextureView(textureView)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .testTag("trim_video_preview")
                )
            } else {
                TrimFallbackFrame(
                    sourceUri = sourceUri,
                    seekMs = startMs,
                    aspectRatio = aspectRatio
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        if (player.currentPosition >= endMs ||
                            player.currentPosition < startMs
                        ) {
                            player.seekTo(startMs)
                        }
                        player.play()
                        isPlaying = true
                    }
                },
                modifier = Modifier.testTag("trim_preview_play_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = FormatUtils.formatDurationWithMillis(previewPositionMs),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Preview (muted)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Single-frame fallback when ExoPlayer cannot play the source:
 * one representative frame at the trim start, extracted on a
 * background thread. A static frame is still more useful than a
 * blank box, and it proves the file itself is readable.
 */
@Composable
private fun TrimFallbackFrame(
    sourceUri: Uri,
    seekMs: Long,
    aspectRatio: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(sourceUri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, sourceUri)
                    retriever.getFrameAtTime(
                        seekMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .testTag("trim_preview_fallback"),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Video frame at trim start",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
