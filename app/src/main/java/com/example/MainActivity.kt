package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainScreen
import com.example.ui.theme.Convert264Theme
import com.example.util.ReviewPrompt
import com.example.viewmodel.ConverterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ConverterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            Convert264Theme {
                // Play review prompt (v0.2.1): once, after the 3rd
                // successful conversion. Play decides whether to show
                // it; whatever it does, we never ask again.
                val reviewDue by viewModel.reviewDue.collectAsState()
                LaunchedEffect(reviewDue) {
                    if (reviewDue) {
                        ReviewPrompt.launch(this@MainActivity) {
                            viewModel.markReviewRequested()
                        }
                    }
                }
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Accepts a video shared or opened from any sender.
     *
     * Real senders differ in how they hand the video over, so every
     * plausible carrier is checked, in order: EXTRA_STREAM (single Uri
     * or list), clipData, intent.data. The MIME type is NOT trusted as
     * a gate — Files, Drive and some messengers share videos with a
     * null or wildcard type, so the stream is accepted whenever it looks
     * like a video by its content type or file extension instead.
     * Only the first video is loaded; the rest are ignored.
     * Every video that looks like a video joins the queue.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE, Intent.ACTION_VIEW -> {
                val uris = collectSharedUris(intent).filter { isVideoUri(it) }
                if (uris.isNotEmpty()) viewModel.addVideos(uris)
            }
        }
    }

    private fun collectSharedUris(intent: Intent): List<Uri> {
        val uris = mutableListOf<Uri>()

        // EXTRA_STREAM: a single Uri (ACTION_SEND) or a list
        // (ACTION_SEND_MULTIPLE — some senders also send a list
        // with ACTION_SEND, and vice versa, so check both).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { uris += it }
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                ?.let { uris += it }
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris += it }
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris += it }
        }

        // clipData: some senders (notably share sheets) put the
        // stream here and leave EXTRA_STREAM empty.
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i)?.uri?.let { uris += it }
            }
        }

        // intent.data: ACTION_VIEW, and some ACTION_SEND senders.
        intent.data?.let { uris += it }

        return uris.distinct()
    }

    private fun isVideoUri(uri: Uri): Boolean {
        // ContentResolver type is authoritative when the provider
        // reports one.
        try {
            val type = contentResolver.getType(uri)
            if (type != null) return type.startsWith("video/")
        } catch (_: Exception) {
            // Fall through to the extension check.
        }
        // Fallback: file extension from the display name or path.
        val name = displayNameOf(uri).ifBlank { uri.lastPathSegment.orEmpty() }
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    private fun displayNameOf(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) cursor.getString(idx) ?: "" else ""
                    } else ""
                } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "m4v", "mov", "mkv", "avi", "webm",
            "3gp", "3gpp", "ts", "mts", "m2ts", "wmv", "flv", "mpg", "mpeg"
        )
    }
}
