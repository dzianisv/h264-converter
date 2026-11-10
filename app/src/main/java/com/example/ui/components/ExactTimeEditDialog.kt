package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.util.FormatUtils

@Composable
fun ExactTimeEditDialog(
    initialStartMs: Long,
    initialEndMs: Long,
    totalDurationMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit
) {
    var startInput by remember { mutableStateOf(FormatUtils.formatDuration(initialStartMs)) }
    var endInput by remember { mutableStateOf(FormatUtils.formatDuration(initialEndMs)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Exact Trim Times",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter timestamps as mm:ss, hh:mm:ss, or seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = startInput,
                    onValueChange = {
                        startInput = it
                        errorMessage = null
                    },
                    label = { Text("Start Time") },
                    placeholder = { Text("e.g. 00:15 or 15") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exact_start_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endInput,
                    onValueChange = {
                        endInput = it
                        errorMessage = null
                    },
                    label = { Text("End Time") },
                    placeholder = { Text("e.g. 01:30 or 90") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exact_end_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total video duration: ${FormatUtils.formatDuration(totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedStart = FormatUtils.parseTimeToMs(startInput)
                    val parsedEnd = FormatUtils.parseTimeToMs(endInput)

                    if (parsedStart == null) {
                        errorMessage = "Invalid start time format. Use mm:ss or seconds."
                        return@Button
                    }
                    if (parsedEnd == null) {
                        errorMessage = "Invalid end time format. Use mm:ss or seconds."
                        return@Button
                    }
                    if (parsedStart < 0) {
                        errorMessage = "Start time cannot be negative."
                        return@Button
                    }
                    if (parsedEnd > totalDurationMs && totalDurationMs > 0) {
                        errorMessage = "End time exceeds video duration."
                        return@Button
                    }
                    if (parsedStart >= parsedEnd) {
                        errorMessage = "Start time must be earlier than end time."
                        return@Button
                    }

                    onConfirm(parsedStart, parsedEnd)
                },
                modifier = Modifier.testTag("confirm_exact_times_button")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
