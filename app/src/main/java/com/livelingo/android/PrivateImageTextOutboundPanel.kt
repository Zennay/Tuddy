package com.livelingo.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Explicit destinations for *reviewed* camera/photo OCR. Nothing is copied,
 * saved or shared by recomposition, image recognition or selection alone.
 */
@Composable
internal fun PrivateImageTextOutboundPanel(
    enabled: Boolean,
    expirySeconds: Int,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onExpiryChange: (Int) -> Unit
) {
    HorizontalDivider()
    Text(
        "Gebruik je gecontroleerde afbeeldingstekst",
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        "Dit zijn de originele door jou nagekeken OCR-woorden, niet een vertaling. " +
            "Kopiëren, opslaan en delen zijn elk een afzonderlijke keuze. " +
            "Android-apps buiten Tuddy kunnen de tekst bewaren of synchroniseren.",
        style = MaterialTheme.typography.bodySmall
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCopy
        ) { Text("Kopieer gecontroleerde tekst") }
        OutlinedButton(
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSave
        ) { Text("Bewaar als .txt via Android…") }
        OutlinedButton(
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = onShare
        ) { Text("Deel tekst via Android…") }
    }
    Text(
        "Klembord automatisch wissen · poging na:",
        style = MaterialTheme.typography.bodySmall
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((seconds, label) in listOf(
            0 to "Uit", 60 to "1 minuut", 300 to "5 minuten"
        )) {
            TextButton(
                enabled = expirySeconds != seconds,
                onClick = { onExpiryChange(seconds) }
            ) { Text(if (expirySeconds == seconds) "$label ✓" else label) }
        }
    }
    Text(
        "Deze gedeelde instelling geldt ook voor andere Tuddy-kopieeracties. " +
            "Een klembordtimer is best-effort: andere apps of Androids " +
            "klembordgeschiedenis kunnen tekst al hebben bewaard.",
        style = MaterialTheme.typography.bodySmall
    )
}
