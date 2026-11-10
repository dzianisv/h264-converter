package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.Dp
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
 * Video preview for the trim screen: the user must SEE what they
 * are cutting.
 *
 * Two layers, deliberately NOT stacked:
 * - Paused (the state the trim screen is in most of the time): a
 *   real frame of the video, extracted with MediaMetadataRetriever
 *   at the current trim position and drawn as a plain Compose
 *   Image. Dragging a trim handle re-extracts the frame at the
 *   moved handle (debounced 300ms), so the still follows the cut.
 *   A Compose Image is always visible — on screen and in
 *   screenshots — which a live video surface is not guaranteed to
 *   be (see below).
 * - Playing: the stock Media3 ExoPlayer rendering into a
 *   TextureView, playing the trimmed range only, muted (volume 0 —
 *   no audio surprise), pausing at the trim end and parking back
 *   at the trim start.
 *
 * Why not a live surface all the time? A TextureView is a real
 * Android View, and child Views paint above Compose-drawn siblings
 * in the same window — a still frame drawn by Compose underneath
 * it is hidden, not overlaid. And on a slow emulator a paused
 * player can take seconds to output its first frame, leaving a
 * black box (exactly what the v0.1.5 screenshot showed). So the
 * still is the preview when paused, and the live surface only
 * appears while actually playing — when the player is provably
 * rendering. The still is a real frame of the same video either
 * way, never a placeholder.
 *
 * Works for the same URIs the converter loads (content:// from SAF
 * and shares, file:// for the generated sample clip) — both the
 * retriever and ExoPlayer resolve them through the
 * ContentResolver, no file path needed. If ExoPlayer cannot play a
 * source, play is refused and the still stays.
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
    modifier: Modifier = Modifier,
    /**
     * Caps the preview frame height (detail screen: the whole
     * screen must fit without scrolling, and a portrait video's
     * aspect ratio would otherwise make the frame ~675dp tall).
     * The frame crops to fill — it is a preview, not a player.
     */
    maxPreviewHeight: Dp = Dp.Unspecified
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
    var playing by remember(sourceUri) { mutableStateOf(false) }
    var previewPositionMs by remember(sourceUri) { mutableStateOf(startMs) }
    var stillBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }

    val player = remember(sourceUri) {
        ExoPlayer.Builder(appContext).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
            setMediaItem(MediaItem.fromUri(sourceUri))
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    previewFailed = true
                    playing = false
                }
            })
            prepare()
            seekTo(startMs)
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // Follow the trim handles: when the start handle moves, the
    // preview goes to the new start; when the end handle moves, to
    // the new end. The player is seeked too, so pressing play starts
    // from the frame being shown.
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

    // Extract the still frame for the current preview position.
    // Debounced: while the user drags, every move restarts the
    // delay, so only the settled position is extracted.
    LaunchedEffect(sourceUri, previewPositionMs) {
        delay(300)
        stillBitmap = extractFrame(context, sourceUri, previewPositionMs)
    }

    // While playing, stop at the trim end and park back at the start.
    LaunchedEffect(playing) {
        while (playing) {
            val pos = player.currentPosition
            previewPositionMs = pos
            if (endMs > startMs && pos >= endMs) {
                player.pause()
                player.seekTo(startMs)
                previewPositionMs = startMs
                playing = false
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
                .aspectRatio(aspectRatio)
                .then(
                    if (maxPreviewHeight != Dp.Unspecified) {
                        Modifier.heightIn(max = maxPreviewHeight)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (playing) {
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
                if (stillBitmap != null) {
                    Image(
                        bitmap = stillBitmap!!.asImageBitmap(),
                        contentDescription = "Video frame at trim position",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .testTag("trim_preview_still"),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("trim_preview_placeholder")
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (playing) {
                        player.pause()
                        previewPositionMs = player.currentPosition
                        playing = false
                    } else if (!previewFailed) {
                        if (player.currentPosition >= endMs ||
                            player.currentPosition < startMs
                        ) {
                            player.seekTo(startMs)
                        }
                        player.play()
                        playing = true
                    }
                },
                modifier = Modifier.testTag("trim_preview_play_button")
            ) {
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause preview" else "Play preview",
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
 * One frame of [uri] at [positionMs], or null if unreadable.
 *
 * The retriever is picky about exact timestamps: the same call that
 * works at 1s can return null at 0 for a given file (the thumbnail
 * helper in VideoMetadataHelper already falls back the same way),
 * so try the requested position twice (closest sync, then closest
 * frame) and finally the start of the file. Logs the outcome so a
 * smoke run can show whether extraction worked or silently failed.
 */
private suspend fun extractFrame(
    context: Context,
    uri: Uri,
    positionMs: Long
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(
                positionMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.getFrameAtTime(
                positionMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: retriever.getFrameAtTime(
                0L,
                MediaMetadataRetriever.OPTION_CLOSEST
            )
            println(
                "TRIM-PREVIEW extractFrame uri=$uri positionMs=$positionMs " +
                    "frame=${if (frame != null) "${frame.width}x${frame.height}" else "null"}"
            )
            frame
        }
    } catch (e: Exception) {
        println("TRIM-PREVIEW extractFrame FAILED uri=$uri: ${e.message}")
        null
    }
}
