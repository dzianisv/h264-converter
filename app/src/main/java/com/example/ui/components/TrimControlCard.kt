package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.util.FormatUtils

/**
 * Trim section, stock components only: Card, RangeSlider with
 * default colors, time readouts that open the exact-input dialog,
 * and small TextButtons for fine steps.
 */
@Composable
fun TrimControlCard(
    totalDurationMs: Long,
    trimRange: ClosedFloatingPointRange<Float>,
    onTrimRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onAdjustStart: (Long) -> Unit,
    onAdjustEnd: (Long) -> Unit,
    onOpenExactEditDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startMs = trimRange.start.toLong().coerceAtLeast(0L)
    val endMs = trimRange.endInclusive.toLong().coerceAtMost(totalDurationMs)
    val trimmedDurationMs = (endMs - startMs).coerceAtLeast(0L)
    val savedDurationMs = (totalDurationMs - trimmedDurationMs).coerceAtLeast(0L)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trim_control_card")
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
                    text = "Trim video",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onOpenExactEditDialog) {
                    Text("Exact input")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Original",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatDuration(totalDurationMs),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Trimmed output",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildString {
                            append(FormatUtils.formatDuration(trimmedDurationMs))
                            if (savedDurationMs > 500L) {
                                append(" (-${FormatUtils.formatDuration(savedDurationMs)})")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            RangeSlider(
                value = trimRange,
                onValueChange = { onTrimRangeChange(it) },
                valueRange = 0f..(if (totalDurationMs > 0) totalDurationMs.toFloat() else 1000f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trim_range_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrimTimeBox(
                    label = "Start",
                    timeMs = startMs,
                    onMinus1s = { onAdjustStart(-1000L) },
                    onPlus1s = { onAdjustStart(1000L) },
                    onMinusTenth = { onAdjustStart(-100L) },
                    onPlusTenth = { onAdjustStart(100L) },
                    onTapToEdit = onOpenExactEditDialog,
                    modifier = Modifier.weight(1f),
                    testTagPrefix = "start_time"
                )
                TrimTimeBox(
                    label = "End",
                    timeMs = endMs,
                    onMinus1s = { onAdjustEnd(-1000L) },
                    onPlus1s = { onAdjustEnd(1000L) },
                    onMinusTenth = { onAdjustEnd(-100L) },
                    onPlusTenth = { onAdjustEnd(100L) },
                    onTapToEdit = onOpenExactEditDialog,
                    modifier = Modifier.weight(1f),
                    testTagPrefix = "end_time"
                )
            }
        }
    }
}

@Composable
private fun TrimTimeBox(
    label: String,
    timeMs: Long,
    onMinus1s: () -> Unit,
    onPlus1s: () -> Unit,
    onMinusTenth: () -> Unit,
    onPlusTenth: () -> Unit,
    onTapToEdit: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = FormatUtils.formatDurationWithMillis(timeMs),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.clickable { onTapToEdit() }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onMinus1s) { Text("-1s") }
            TextButton(onClick = onMinusTenth) { Text("-0.1s") }
            TextButton(onClick = onPlusTenth) { Text("+0.1s") }
            TextButton(onClick = onPlus1s) { Text("+1s") }
        }
    }
}
