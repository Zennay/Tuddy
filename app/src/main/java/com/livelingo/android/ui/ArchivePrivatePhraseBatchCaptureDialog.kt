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

private data class ArchivePrivatePhraseDraft(
    val language: String? = null,
    val phrase: String = "",
    val meaningLanguage: String? = null,
    val meaning: String = ""
)

/**
 * Deliberate multi-row authoring flow. Imported captions are shown only as
 * unverified reference. Every card starts blank and the entire batch is saved
 * atomically only after the user authors every field and confirms once.
 */
@Composable
internal fun ArchivePrivatePhraseBatchCaptureDialog(
    rows: List<TranscriptExportRow>,
    stillCurrent: Boolean,
    existingPhrases: List<PersonalPhrase>?,
    onSave: (List<PersonalPhrase>) -> Boolean,
    onDismiss: () -> Unit
) {
    var drafts by remember(rows) {
        mutableStateOf(List(rows.size) { ArchivePrivatePhraseDraft() })
    }
    var currentIndex by remember(rows) { mutableStateOf(0) }
    var saveError by remember(rows) { mutableStateOf("") }

    val labels = listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val meaningLabels = listOf("nl" to "Nederlands", "en" to "Engels")
    val current = drafts.getOrNull(currentIndex) ?: ArchivePrivatePhraseDraft()
    val candidates = drafts.mapNotNull { draft ->
        val language = draft.language ?: return@mapNotNull null
        val meaningLanguage = draft.meaningLanguage ?: return@mapNotNull null
        val phrase = draft.phrase.trim()
        val meaning = draft.meaning.trim()
        if (!PrivatePhraseTextPolicy.valid(phrase, 120) ||
            !PrivatePhraseTextPolicy.valid(meaning, 180)) return@mapNotNull null
        PersonalPhrase(language, phrase, meaningLanguage, meaning)
    }
    val allComplete = drafts.size == rows.size && candidates.size == rows.size
    val candidateKeys = candidates.map {
        it.language + "|" + it.phrase.lowercase(Locale.ROOT)
    }
    val duplicateInBatch = allComplete && candidateKeys.toSet().size != candidateKeys.size
    val existingKeys = existingPhrases.orEmpty().map {
        it.language + "|" + it.phrase.lowercase(Locale.ROOT)
    }.toSet()
    val duplicateExisting = allComplete && candidateKeys.any { it in existingKeys }
    val deckFull = existingPhrases?.let { it.size + rows.size > 80 } == true
    val rowCountInvalid = rows.size !in 1..10
    val canSave = stillCurrent && !rowCountInvalid && existingPhrases != null &&
        allComplete && !duplicateInBatch && !duplicateExisting && !deckFull

    fun updateCurrent(transform: (ArchivePrivatePhraseDraft) -> ArchivePrivatePhraseDraft) {
        if (currentIndex !in drafts.indices) return
        drafts = drafts.toMutableList().also { it[currentIndex] = transform(it[currentIndex]) }
        saveError = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schrijf ${rows.size} eigen privékaarten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "Kaart ${currentIndex + 1} van ${rows.size}. Elke archiefregel is alleen " +
                        "een niet-geverifieerde referentie. Tuddy kopieert geen tekst naar de " +
                        "kaart; schrijf elke zin en betekenis zelf. Er wordt niets opgeslagen " +
                        "tot je de volledige batch hieronder bevestigt.",
                    style = MaterialTheme.typography.bodySmall
                )
                val row = rows.getOrNull(currentIndex)
                if (row != null) {
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
                }
                Text("1. In welke taal schrijf jij deze eigen zin?",
                    style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    labels.forEach { (code, label) ->
                        TextButton(onClick = {
                            updateCurrent { it.copy(language = code) }
                        }) {
                            Text((if (current.language == code) "✓ " else "") + label)
                        }
                    }
                }
                OutlinedTextField(
                    value = current.phrase,
                    onValueChange = { candidate ->
                        if (candidate.length <= 120) {
                            updateCurrent { it.copy(phrase = candidate) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mijn eigen zin (max. 120 tekens)") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (current.language == "ckb" || current.language == "ps")
                            TextDirection.Rtl else TextDirection.Ltr
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(autoCorrect = false),
                    minLines = 2,
                    maxLines = 4
                )
                Text("2. Kies de taal van je zelfgeschreven betekenis",
                    style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    meaningLabels.forEach { (code, label) ->
                        TextButton(onClick = {
                            updateCurrent { it.copy(meaningLanguage = code) }
                        }) {
                            Text((if (current.meaningLanguage == code) "✓ " else "") + label)
                        }
                    }
                }
                OutlinedTextField(
                    value = current.meaning,
                    onValueChange = { candidate ->
                        if (candidate.length <= 180) {
                            updateCurrent { it.copy(meaning = candidate) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mijn eigen betekenis (max. 180 tekens)") },
                    minLines = 2,
                    maxLines = 4
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = currentIndex > 0,
                        onClick = { currentIndex--; saveError = "" }
                    ) { Text("← Vorige kaart") }
                    TextButton(
                        enabled = currentIndex < rows.lastIndex,
                        onClick = { currentIndex++; saveError = "" }
                    ) { Text("Volgende kaart →") }
                }
                Text(
                    "Alleen jouw eigen tekst en gekozen taalcodes worden lokaal bewaard. " +
                        "Geen transcript, audio, automatische vertaling of netwerkverzoek.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!stillCurrent) {
                    Text("Het archief is gesloten of vervangen. Niets wordt opgeslagen.",
                        color = MaterialTheme.colorScheme.error)
                }
                if (existingPhrases == null) {
                    Text("Het privézinnenboek kan niet veilig worden gelezen. Niets wordt opgeslagen.",
                        color = MaterialTheme.colorScheme.error)
                }
                if (rowCountInvalid) {
                    Text("Kies maximaal 10 archiefregels per batch.",
                        color = MaterialTheme.colorScheme.error)
                }
                if (!allComplete && existingPhrases != null) {
                    Text("Vul bij iedere kaart beide velden in en kies beide talen.",
                        color = MaterialTheme.colorScheme.error)
                }
                if (duplicateExisting) {
                    Text("Een of meer zinnen bestaan al in dezelfde taal. Bewerk die kaarten bewust in Mijn eigen offline zinnen.",
                        color = MaterialTheme.colorScheme.error)
                }
                if (duplicateInBatch) {
                    Text("Deze batch bevat dubbele zinnen in dezelfde taal. Maak elke kaart uniek.",
                        color = MaterialTheme.colorScheme.error)
                }
                if (deckFull) {
                    Text("Er is niet genoeg ruimte: het privézinnenboek heeft maximaal 80 kaarten.",
                        color = MaterialTheme.colorScheme.error)
                }
                if (saveError.isNotBlank()) {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = {
                if (canSave && onSave(candidates)) onDismiss()
                else saveError =
                    "Niet opgeslagen: de batch botste met de actuele kaartstatus of het archief veranderde."
            }) { Text("Bewaar alle ${rows.size} kaarten") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
