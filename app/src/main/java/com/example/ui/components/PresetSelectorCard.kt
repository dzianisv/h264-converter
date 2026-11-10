package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.QualityPreset

/**
 * Resolution presets as a standard single-choice segmented button
 * row — the Material 3 control for picking one of a few options.
 * The selected preset's description is plain supporting text below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSelectorCard(
    selectedPreset: QualityPreset,
    onPresetSelect: (QualityPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preset_selector_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Resolution",
                style = MaterialTheme.typography.titleMedium
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                QualityPreset.entries.forEachIndexed { index, preset ->
                    SegmentedButton(
                        selected = preset == selectedPreset,
                        onClick = { onPresetSelect(preset) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = QualityPreset.entries.size
                        ),
                        label = { Text(preset.label) },
                        modifier = Modifier.testTag("preset_${preset.name.lowercase()}")
                    )
                }
            }

            Text(
                text = buildString {
                    append(selectedPreset.description)
                    append(" · H.264 · AAC · MP4")
                    if (selectedPreset == QualityPreset.ORIGINAL) append(" · Best quality")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
