package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Empty queue state: one job, one action. Headline plus a big
 * standard plus button — nothing else. No chips, no badges, no
 * format list, no claims about the device. A plain centered
 * column — no card, no border. Stock Material 3 only: the
 * theme's own colors (dynamic color on the phone) and the
 * Material type scale.
 */
@Composable
fun VideoPickerCard(
    onPickVideoClick: () -> Unit,
    onTrySampleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pick_video_card")
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Select Video to Convert",
            style = MaterialTheme.typography.headlineSmall
        )

        FilledTonalIconButton(
            onClick = onPickVideoClick,
            modifier = Modifier
                .size(72.dp)
                .testTag("select_video_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Select video to convert",
                modifier = Modifier.size(36.dp)
            )
        }

        onTrySampleClick?.let { onSample ->
            TextButton(
                onClick = onSample,
                modifier = Modifier.testTag("try_sample_clip_button")
            ) {
                Text("Try sample clip")
            }
        }
    }
}
