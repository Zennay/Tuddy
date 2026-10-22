package com.livelingo.android.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseTextPolicy
import com.livelingo.android.TranscriptExportRow
import java.util.Locale

/**
 * Deliberate authoring flow from an imported archive row. The archive is
 * reference-only: both card fields and both language choices start empty.
 * Nothing from the transcript is copied or persisted without explicit input.
 */
@Composable
internal fun ArchivePrivatePhraseCaptureDialog(
    row: TranscriptExportRow,
    stillCurrent: Boolean,
    existingPhrases: List<PersonalPhrase>?,
    onSave: (language: String, phrase: String, meaningLanguage: String, meaning: String) -> Boolean,
    onDismiss: () -> Unit
) {
    var language by remember(row) { mutableStateOf<String?>(null) }
    var phrase by remember(row) { mutableStateOf("") }
    var meaningLanguage by remember(row) { mutableStateOf<String?>(null) }
    var meaning by remember(row) { mutableStateOf("") }
    var saveError by remember(row) { mutableStateOf("") }

    val phraseText = phrase.trim()
    val meaningText = meaning.trim()
    val labels = listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val meaningLabels = listOf("nl" to "Nederlands", "en" to "Engels")
    val duplicate = language != null && existingPhrases?.any {
        it.language == language && it.phrase.equals(phraseText, ignoreCase = true)
    } == true
    val deckFull = existingPhrases?.size?.let { it >= 80 } == true
    val validInput = language != null && meaningLanguage != null &&
        PrivatePhraseTextPolicy.valid(phraseText, 120) &&
        PrivatePhraseTextPolicy.valid(meaningText, 180)
    val canSave = stillCurrent && existingPhrases != null &&
        validInput && !duplicate && !deckFull

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maak eigen privékaart uit archief") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "Het gekozen archief is alleen een referentie, geen gecontroleerde bron. " +
                        "Tuddy kopieert de regel niet naar je kaart. Schrijf zelf beide velden " +
                        "en bevestig pas daarna de privéopslag.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Archiefreferentie · " + row.sourceCode.uppercase(Locale.ROOT) +
                        " → " + row.targetCode.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    row.original,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (row.sourceCode in setOf("ckb", "ps"))
                            TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                Text(
                    if (row.status == "Lokaal vertaald" && row.translated.isNotBlank())
                        row.translated else "[Vertaling niet voltooid]",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = if (row.targetCode in setOf("ckb", "ps"))
                            TextDirection.Rtl else TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("1. In welke taal schrijf jij de eigen zin?",
                    style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    labels.forEach { (code, label) ->
                        TextButton(onClick = { language = code }) {
                            Text((if (language == code) "✓ " else "") + label)
                        }
                    }
                }
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { if (it.length <= 120) phrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mijn eigen zin (max. 120 tekens)") },
                    minLines = 2,
                    maxLines = 4
                )
                Text("2. Kies de taal van je zelfgeschreven betekenis",
                    style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    meaningLabels.forEach { (code, label) ->
                        TextButton(onClick = { meaningLanguage = code }) {
                            Text((if (meaningLanguage == code) "✓ " else "") + label)
                        }
                    }
                }
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { if (it.length <= 180) meaning = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mijn eigen betekenis (max. 180 tekens)") },
                    minLines = 2,
                    maxLines = 4
                )
                if (!stillCurrent) {
                    Text(
                        "Het archief is gesloten of vervangen. Niets wordt opgeslagen.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (existingPhrases == null) {
                    Text(
                        "Het privézinnenboek kan niet worden gelezen. Niets wordt opgeslagen.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (duplicate) {
                    Text(
                        "Deze zin bestaat al voor de gekozen taal. " +
                            "Wijzig de bestaande kaart bewust in Mijn eigen offline zinnen.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (deckFull) {
                    Text(
                        "Je privézinnenboek bevat al 80 kaarten. Verwijder eerst bewust een bestaande kaart.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (saveError.isNotBlank()) {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Alleen jouw eigen twee tekstvelden en de gekozen taalcodes worden " +
                        "app-privé opgeslagen na bevestiging. Geen transcript, audio, " +
                        "automatische vertaling of netwerkverzoek.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = {
                if (stillCurrent && existingPhrases != null && validInput && !duplicate) {
                    if (onSave(language!!, phraseText, meaningLanguage!!, meaningText)) {
                        onDismiss()
                    } else {
                        saveError = "Niet opgeslagen: het archief is gewijzigd of de kaart kon niet veilig worden toegevoegd."
                    }
                }
            }) { Text("Bewaar mijn eigen privékaart") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
