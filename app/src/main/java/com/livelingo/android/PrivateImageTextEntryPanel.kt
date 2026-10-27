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
        "Kies één foto via Android, controleer het Latijnse OCR-resultaat en " +
            "beslis daarna pas over offline vertalen. Geen automatische opname.",
        style = MaterialTheme.typography.bodySmall
    )
}
