package com.livelingo.android

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Main-app entrypoint. Image selection and the private OCR session live in
 * a separate Activity, not in the already-large microphone Compose screen.
 */
@Composable
internal fun PrivateImageTextEntryPanel(enabled: Boolean) {
    val context = LocalContext.current
    OutlinedButton(
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            context.startActivity(Intent(context, PrivateImageTextActivity::class.java))
        }
    ) {
        Text("Lees tekst uit afbeelding · volledig lokaal")
    }
    Text(
        "Kies één foto, controleer de privévoorvertoning en draai indien " +
            "nodig 90° vóór je expliciet offline tekstherkenning start. " +
            "Beoordeel daarna de Latijnse OCR-tekst vóór vertalen. " +
            "Geen automatische opname, upload of opslag van de afbeelding.",
        style = MaterialTheme.typography.bodySmall
    )
}
