package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import com.livelingo.android.model.SafeCommunityPhrasebook
import java.util.Locale

private data class StarterPhraseSetDraft(
    val sourceText: String,
    val phrase: String,
    val meaning: String,
    val included: Boolean,
    val available: Boolean
)

/**
 * Lets a user review an entire five-word starter set before one atomic local
 * write. Bundled examples are not presented as verified translations.
 */
@Composable
internal fun OfflineStarterPhraseBatchSaveDialog(
    starter: List<Pair<String, String>>,
    target: String,
    source: String,
    existingNotes: List<PersonalPhrase>?,
    onSaveBatch: (List<PersonalPhrase>) -> Boolean,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val targetLabel = when (target) {
        "kmr" -> "Kurmanji"
        "ckb" -> "Sorani"
        "ps" -> "Pasjtoe"
        else -> "onbekende taal"
    }
    val sourceLabel = if (source == "en") "English" else "Nederlands"
    val seeded = remember(target, source, starter, existingNotes) {
        starter.map { (dutch, english) ->
            val sourceText = if (source == "nl") dutch else english
            val translated = SafeCommunityPhrasebook.translate(source, target, sourceText).getOrNull()
            StarterPhraseSetDraft(
                sourceText = sourceText,
                phrase = translated.orEmpty(),
                meaning = sourceText,
                included = translated != null,
                available = translated != null
            )
        }
    }
    var drafts by remember(target, source, starter, existingNotes) { mutableStateOf(seeded) }
    var approved by remember(target, source, starter, existingNotes) { mutableStateOf(false) }
    var message by remember(target, source, starter, existingNotes) { mutableStateOf("") }

    fun updateDraft(index: Int, updated: StarterPhraseSetDraft) {
        drafts = drafts.toMutableList().also { it[index] = updated }
        approved = false
        message = ""
    }

    val selected = drafts.filter { it.included && it.available }
    val duplicatePhrases = selected.filter { draft ->
        existingNotes?.any { note ->
            note.language == target && note.phrase.trim().equals(draft.phrase.trim(), ignoreCase = true)
        } == true
    }
    val selectedKeys = selected.map { it.phrase.trim().lowercase(Locale.ROOT) }
    val duplicateWithinSet = selectedKeys.toSet().size != selectedKeys.size
    val invalidSelected = selected.any { draft ->
        !PrivatePhraseTextPolicy.valid(draft.phrase, 120) ||
            !PrivatePhraseTextPolicy.valid(draft.meaning, 180)
    }
    val overCapacity = existingNotes != null && existingNotes.size + selected.size > 80
    val validSelection = existingNotes != null && selected.isNotEmpty() &&
        !invalidSelected && duplicatePhrases.isEmpty() && !duplicateWithinSet && !overCapacity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bewaar voorbeelden als eigen offline set?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Controleer alle geselecteerde woorden en betekenissen. Dit zijn vaste " +
                        "voorbeeldparen, niet onafhankelijk door moedertaalsprekers geverifieerd. " +
                        "Je kunt een woord uitsluiten of beide velden aanpassen.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${selected.size} geselecteerd · ${existingNotes?.size ?: "?"}/80 eigen zinnen · $targetLabel",
                    style = MaterialTheme.typography.labelMedium
                )
                if (existingNotes == null) {
                    Text(
                        "Eigen zinnen konden niet veilig worden gelezen. Er kan niets worden opgeslagen.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                drafts.forEachIndexed { index, draft ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Checkbox(
                            checked = draft.included,
                            enabled = draft.available,
                            onCheckedChange = { checked ->
                                updateDraft(index, draft.copy(included = checked))
                            }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "${index + 1}. Voorbeeld: ${draft.sourceText}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (!draft.available) {
                                Text(
                                    "Geen vaste invoer beschikbaar; dit voorbeeld kan niet worden bewaard.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                OutlinedTextField(
                                    value = draft.phrase,
                                    onValueChange = { incoming ->
                                        if (incoming.length <= 2400 &&
                                            PrivatePhraseTextPolicy.wellFormed(incoming)) {
                                            updateDraft(index, draft.copy(phrase = incoming))
                                        }
                                    },
                                    label = { Text("Eigen zin · $targetLabel (max. 120)") },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrRtl
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 1,
                                    maxLines = 3
                                )
                                if (!PrivatePhraseTextPolicy.valid(draft.phrase, 120)) {
                                    Text(
                                        "Maximaal 120 tekens; ongeldige of lege tekst kan niet worden opgeslagen.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedTextField(
                                    value = draft.meaning,
                                    onValueChange = { incoming ->
                                        if (incoming.length <= 720 &&
                                            PrivatePhraseTextPolicy.wellFormed(incoming)) {
                                            updateDraft(index, draft.copy(meaning = incoming))
                                        }
                                    },
                                    label = { Text("Eigen betekenis · $sourceLabel (max. 180)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 1,
                                    maxLines = 3
                                )
                                if (!PrivatePhraseTextPolicy.valid(draft.meaning, 180)) {
                                    Text(
                                        "Maximaal 180 tekens; schrijf een eigen betekenis.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                val duplicate = existingNotes?.any { note ->
                                    note.language == target &&
                                        note.phrase.trim().equals(draft.phrase.trim(), ignoreCase = true)
                                } == true
                                if (draft.included && duplicate) {
                                    Text(
                                        "Deze zin staat al in je zinnenboek. Haal het vinkje weg of pas de zin aan; bestaande notities worden niet overschreven.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
                if (duplicateWithinSet) {
                    Text(
                        "De selectie bevat dezelfde doelzin meer dan één keer. Pas aan of vink een kopie uit.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (overCapacity) {
                    Text(
                        "De set past niet in de limiet van 80 eigen zinnen. Vink minder voorbeelden aan.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Opslaan schrijft de geselecteerde set in één atomaire lokale transactie: " +
                        "bij één duplicaat, ongeldige regel of opslagfout wordt niets toegevoegd. " +
                        "Geen netwerk, audio of vertaalmodel wordt gebruikt.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Checkbox(
                        checked = approved,
                        enabled = validSelection,
                        onCheckedChange = { approved = it; message = "" }
                    )
                    Text(
                        "Ik heb alle geselecteerde paren nagelezen en wil precies deze set als eigen notities bewaren.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = validSelection && approved, onClick = {
                val batch = selected.map { draft ->
                    PersonalPhrase(target, draft.phrase.trim(), source, draft.meaning.trim())
                }
                if (validSelection && approved && onSaveBatch(batch)) {
                    onSaved()
                } else {
                    approved = false
                    message = "Niets opgeslagen: de set kan een duplicaat, ongeldige tekst, " +
                        "volle lijst of opslagprobleem bevatten. Bestaande notities zijn niet overschreven."
                }
            }) { Text("Bewaar geselecteerde set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleer · niets opslaan") }
        }
    )
}
