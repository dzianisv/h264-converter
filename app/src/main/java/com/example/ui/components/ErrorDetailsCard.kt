package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * The full error, shown in full (WL-17).
 *
 * The v0.2.2 failure UI was one truncated red line in the queue
 * row ("Failed: ..." with maxLines=1 + Ellipsis) and nothing at
 * all on the detail screen — the user could not see the cause,
 * could not copy it, and support could not diagnose it. This card
 * is the fix, used in both places:
 *
 * - the plain-language summary, one line, always readable;
 * - the complete technical details (return code, ffmpeg log tail,
 *   file + target), selectable text in a scrollable block, never
 *   truncated, never ellipsized;
 * - "Copy error" — the whole detail text onto the clipboard, so
 *   the user can paste it to support (or to the developer);
 * - "Try again" — re-queues the file, when the host passes it.
 *
 * Standard Material 3 components only: Card, Text, TextButton.
 */
@Composable
fun ErrorDetailsCard(
    summary: String,
    details: String,
    onRetry: (() -> Unit)?,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = summary,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.testTag("${testTagPrefix}_summary")
            )
            SelectionContainer {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("${testTagPrefix}_details_text")
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Convert264 error", details)
                        )
                        copied = true
                        Toast.makeText(
                            context,
                            "Error details copied",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_copy_button")
                ) {
                    Text(if (copied) "Copied" else "Copy error")
                }
                if (onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("${testTagPrefix}_retry_button")
                    ) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}
