package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleFileReader

/**
 * Intentional manual paste only: never reads Android's clipboard, stores the
 * submitted text, edits a live transcript or sends it to a model/network.
 * The same bounded UTF-8 SRT/VTT parser is used by the document chooser.
 */
@Composable
internal fun SubtitlePasteDialog(
    title: String,
    onDismiss: () -> Unit,
    onAccept: (List<PrivateSubtitleCue>) -> Unit
) {
    var draftText by remember { mutableStateOf("") }
    val bytes = remember(draftText) { draftText.toByteArray(Charsets.UTF_8) }
    val parsed = remember(bytes) {
        PrivateSubtitleFileReader.read(bytes)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Plak zelf een compleet SRT- of WebVTT-document. " +
                        "Tuddy leest het klembord niet automatisch. Alleen geldig " +
                        "UTF-8, maximaal 256 KiB en 500 ondertitelregels.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = draftText,
                    onValueChange = {
                        // Limit the temporary input even for very large clipboard pastes.
                        draftText = it.take(PrivateSubtitleFileReader.MAX_BYTES)
                    },
                    label = { Text("Zelf geplakte SRT/WebVTT-tekst") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12
                )
                if (parsed == null) {
                    Text(
                        "Nog geen geldig volledig ondertiteldocument. Controleer " +
                            "WEBVTT/SRT-tijdregels en de grootte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "${parsed.size} regels gevonden. Voorbeeld (maximaal 5):",
                        style = MaterialTheme.typography.titleSmall
                    )
                    parsed.take(5).forEach { cue ->
                        Text(
                            "${cue.startLabel} → ${cue.endLabel}\n${cue.text.take(240)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "Het origineel en het live gesprek blijven intact. " +
                            "Alleen bewust bewaren als nieuw bestand kan via " +
                            "een door jou gekozen Android-bestandsprovider gebeuren; " +
                            "die provider kan extern synchroniseren.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = {
                // Reparse exact current bytes at consent. No stale preview accepted.
                val confirmed = PrivateSubtitleFileReader.read(
                    draftText.toByteArray(Charsets.UTF_8)
                ) ?: return@TextButton
                onAccept(confirmed)
            }) { Text("Open tijdelijk concept") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
