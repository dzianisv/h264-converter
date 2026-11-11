package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AudioCodec
import com.example.model.Container
import com.example.model.ConversionSettings
import com.example.model.QualityPreset
import com.example.model.VideoCodec
import com.example.model.audioCodecsFor
import com.example.model.encoderPresetOptions
import com.example.model.videoCodecsFor

/**
 * The conversion settings, as one joined section: container,
 * video codec, audio codec, quality (bitrate preset), and the
 * encoder preset where the codec has one.
 *
 * Stock Material 3 only: ExposedDropdownMenuBox is the standard
 * Android dropdown. The dropdowns filter each other — the options
 * for video/audio codec come from the chosen container, so an
 * impossible combination can never be selected. [compact] lays
 * the dropdowns out in a two-column grid (detail screen, which
 * must fit on one screen); the full layout is one dropdown per
 * row (main screen).
 *
 * The custom bitrate is an AlertDialog with a plain
 * OutlinedTextField — the standard way to ask for one number.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionSettingsCard(
    settings: ConversionSettings,
    onSettingsChange: (ConversionSettings) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showBitrateDialog by remember { mutableStateOf(false) }

    if (showBitrateDialog) {
        CustomBitrateDialog(
            currentKbps = settings.customVideoBitrateKbps,
            onDismiss = { showBitrateDialog = false },
            onConfirm = { kbps ->
                onSettingsChange(
                    settings.copy(
                        quality = QualityPreset.CUSTOM,
                        customVideoBitrateKbps = kbps,
                    ),
                )
                showBitrateDialog = false
            },
        )
    }

    val videoCodecOptions = videoCodecsFor(settings.container)
    val audioCodecOptions = audioCodecsFor(settings.container)
    val presetOptions = encoderPresetOptions(settings.videoCodec)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversion_settings_card")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            Text(
                text = "Conversion settings",
                style = MaterialTheme.typography.titleMedium
            )

            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsDropdown(
                        label = "Container",
                        options = Container.entries,
                        selected = settings.container,
                        optionLabel = { it.shortLabel },
                        onSelect = { onSettingsChange(settings.copy(container = it)) },
                        testTag = "dropdown_container",
                        modifier = Modifier.weight(1f)
                    )
                    SettingsDropdown(
                        label = "Video codec",
                        options = videoCodecOptions,
                        selected = settings.videoCodec,
                        optionLabel = { it.label },
                        onSelect = { onSettingsChange(settings.copy(videoCodec = it)) },
                        testTag = "dropdown_video_codec",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsDropdown(
                        label = "Audio codec",
                        options = audioCodecOptions,
                        selected = settings.audioCodec,
                        optionLabel = { it.label },
                        onSelect = { onSettingsChange(settings.copy(audioCodec = it)) },
                        testTag = "dropdown_audio_codec",
                        modifier = Modifier.weight(1f)
                    )
                    SettingsDropdown(
                        label = "Quality",
                        options = QualityPreset.entries,
                        selected = settings.quality,
                        optionLabel = { it.shortLabel },
                        onSelect = { preset ->
                            if (preset == QualityPreset.CUSTOM) {
                                showBitrateDialog = true
                            } else {
                                onSettingsChange(settings.copy(quality = preset))
                            }
                        },
                        testTag = "dropdown_quality",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (presetOptions.isNotEmpty()) {
                    SettingsDropdown(
                        label = "Encoder preset",
                        options = presetOptions,
                        selected = presetOptions.firstOrNull { it.value == settings.encoderPreset }
                            ?: presetOptions.first(),
                        optionLabel = { it.label },
                        onSelect = { onSettingsChange(settings.copy(encoderPreset = it.value)) },
                        testTag = "dropdown_encoder_preset",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                SettingsDropdown(
                    label = "Container",
                    options = Container.entries,
                    selected = settings.container,
                    optionLabel = { it.label },
                    onSelect = { onSettingsChange(settings.copy(container = it)) },
                    testTag = "dropdown_container",
                    modifier = Modifier.fillMaxWidth()
                )
                SettingsDropdown(
                    label = "Video codec",
                    options = videoCodecOptions,
                    selected = settings.videoCodec,
                    optionLabel = { it.label },
                    onSelect = { onSettingsChange(settings.copy(videoCodec = it)) },
                    testTag = "dropdown_video_codec",
                    modifier = Modifier.fillMaxWidth()
                )
                SettingsDropdown(
                    label = "Audio codec",
                    options = audioCodecOptions,
                    selected = settings.audioCodec,
                    optionLabel = { it.label },
                    onSelect = { onSettingsChange(settings.copy(audioCodec = it)) },
                    testTag = "dropdown_audio_codec",
                    modifier = Modifier.fillMaxWidth()
                )
                SettingsDropdown(
                    label = "Quality",
                    options = QualityPreset.entries,
                    selected = settings.quality,
                    optionLabel = { it.label },
                    onSelect = { preset ->
                        if (preset == QualityPreset.CUSTOM) {
                            showBitrateDialog = true
                        } else {
                            onSettingsChange(settings.copy(quality = preset))
                        }
                    },
                    testTag = "dropdown_quality",
                    modifier = Modifier.fillMaxWidth()
                )
                if (presetOptions.isNotEmpty()) {
                    SettingsDropdown(
                        label = "Encoder preset",
                        options = presetOptions,
                        selected = presetOptions.firstOrNull { it.value == settings.encoderPreset }
                            ?: presetOptions.first(),
                        optionLabel = { it.label },
                        onSelect = { onSettingsChange(settings.copy(encoderPreset = it.value)) },
                        testTag = "dropdown_encoder_preset",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                text = buildString {
                    append("Output: ${settings.targetSummary}")
                    append(" · ${settings.videoBitrateKbps} kbps")
                    if (settings.videoCodec != VideoCodec.COPY && presetOptions.isNotEmpty()) {
                        append(" · preset ${settings.encoderPreset}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("settings_summary_text")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            // Single line: a long value must ellipsize, never
            // grow the field taller (in the compact two-column
            // grid a wrapped value made the row ~76dp and pushed
            // the detail screen over the fold).
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(testTag)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    modifier = Modifier.testTag("${testTag}_option_${optionLabel(option)}")
                )
            }
        }
    }
}

@Composable
private fun CustomBitrateDialog(
    currentKbps: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(currentKbps.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in 100..100_000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom video bitrate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Bitrate (kbps)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_bitrate_input")
                )
                Text(
                    text = "100 – 100,000 kbps. Higher means bigger files, better quality.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { if (valid) onConfirm(parsed!!) },
                modifier = Modifier.testTag("custom_bitrate_confirm")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = Modifier.testTag("custom_bitrate_dialog")
    )
}
