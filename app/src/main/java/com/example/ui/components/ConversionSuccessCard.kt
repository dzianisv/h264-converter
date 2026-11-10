package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConversionResult
import com.example.util.FormatUtils

/**
 * Success state, stock components only: one filled button to open
 * the result, text buttons to share and to start over.
 */
@Composable
fun ConversionSuccessCard(
    result: ConversionResult,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit,
    onConvertAnotherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversion_success_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Conversion Complete!",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "H.264 MP4 exported successfully",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = result.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(FormatUtils.formatFileSize(result.sizeBytes))
                        append(" · ${FormatUtils.formatDuration(result.durationMs)}")
                        if (result.originalSizeBytes > 0 && result.sizeBytes < result.originalSizeBytes) {
                            val percentSaved =
                                ((result.originalSizeBytes - result.sizeBytes) * 100 / result.originalSizeBytes).toInt()
                            if (percentSaved > 0) append(" · -$percentSaved%")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Saved to Movies/Convert264/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onOpenClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_converted_video_button")
            ) {
                Text("Open")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onShareClick,
                    modifier = Modifier.testTag("share_converted_video_button")
                ) {
                    Text("Share")
                }
                TextButton(
                    onClick = onConvertAnotherClick,
                    modifier = Modifier.testTag("convert_another_button")
                ) {
                    Text("Convert another")
                }
            }
        }
    }
}
