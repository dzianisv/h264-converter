package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ConversionProgress
import com.example.util.FormatUtils

/**
 * Converting state, stock components only: Card, a default
 * LinearProgressIndicator (determinate only when the encoder
 * actually reports a percentage, indeterminate while it does not),
 * percent + times as plain text, and a plain default TextButton
 * to cancel.
 */
@Composable
fun ConversionStatusBar(
    progress: ConversionProgress,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.percent / 100f).coerceIn(0f, 1f),
        label = "conversionProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversion_status_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Converting to H.264…",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    // When the encoder does not report progress, "0%"
                    // would be a lie — show an ellipsis instead and
                    // render the bar indeterminate below.
                    text = if (progress.progressAvailable) "${progress.percent}%" else "…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("conversion_percent_text")
                )
            }

            if (progress.progressAvailable) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("conversion_linear_progress")
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("conversion_linear_progress_indeterminate")
                )
                Text(
                    text = "This device's encoder doesn't report progress — elapsed time is live",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transcoded video",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${FormatUtils.formatDuration(progress.currentTrimProgressMs)} / ${FormatUtils.formatDuration(progress.totalTrimDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("conversion_time_progress_text")
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Elapsed time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${FormatUtils.formatDuration(progress.elapsedSeconds * 1000L)} elapsed",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("conversion_elapsed_time_text")
                    )
                }
            }

            TextButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cancel_conversion_button")
            ) {
                Text("Cancel Conversion")
            }
        }
    }
}
