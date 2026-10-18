package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue

/**
 * UI for an explicitly selected print worksheet. The Activity, not the
 * disposable SRT/VTT dialog, owns CreateDocument: Android may stop the app
 * while an external SAF provider is choosing the destination.
 */
@Composable
internal fun PrivateSubtitleWorksheetExportAction(
    selected: List<PrivateSubtitleCue>,
    onExport: (List<PrivateSubtitleCue>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = selected.isNotEmpty(),
            onClick = { onExport(selected.take(24).toList()) }
        ) { Text("▤ Bewaar geselecteerde regels als oefenblad…") }
        Text(
            "Maximaal 24 gevonden regels: oefenvragen en een aparte " +
                "antwoordpagina. Alleen na bewuste Android-bestandskeuze. " +
                "De provider kan het document extern synchroniseren.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
