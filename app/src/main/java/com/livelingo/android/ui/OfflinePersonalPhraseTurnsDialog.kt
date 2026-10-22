package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseDeck
import java.util.Locale

private data class LocalPersonalTurn(val note: PersonalPhrase, val fromMeaning: Boolean)

/**
 * Finite phrase-memory conversation, not machine translation: a turn is
 * available ONLY if a saved, user-authored sentence matches exactly.
 * Entire interaction is Compose RAM; it is never a persisted transcript.
 */
@Composable
internal fun OfflinePersonalPhraseTurnsDialog(
    initialLanguage: String,
    onManagePhrases: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deck = remember(context) { PrivatePhraseDeck(context) }
    val notes = remember(deck) { deck.entries() }
    var language by remember(initialLanguage) {
        mutableStateOf(initialLanguage.takeIf { it in setOf("kmr", "ckb", "ps") } ?: "kmr")
    }
    var meaningLanguage by remember { mutableStateOf("nl") }
    var fromMeaning by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var alternate by remember { mutableStateOf(true) }
    var turns by remember { mutableStateOf<List<LocalPersonalTurn>>(emptyList()) }
    var shown by remember { mutableStateOf<PersonalPhrase?>(null) }
    var choosingSaved by remember { mutableStateOf(false) }
    var tableTurn by remember { mutableStateOf<Int?>(null) }
    val labels = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val normalized = draft.trim().lowercase(Locale.ROOT)
    val choices = if (normalized.isEmpty()) emptyList() else notes.orEmpty().filter { note ->
        note.language == language && note.meaningLanguage == meaningLanguage &&
            (if (fromMeaning) note.ownMeaning else note.phrase).trim()
                .lowercase(Locale.ROOT) == normalized
    }
    tableTurn?.let { selected ->
        OfflinePersonalTurnTableDialog(
            turns = turns.map { it.note to it.fromMeaning },
            initialIndex = selected,
            onBack = { tableTurn = null }
        )
        return
    }
    shown?.let { note ->
        OfflinePhrasePresenter(notes = listOf(note), initial = note,
            onDismiss = { shown = null })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gesprek met eigen offline zinnen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Typ om de beurt een zin die je zelf eerder hebt opgeslagen. " +
                    "Alleen letterlijke overeenkomsten krijgen jouw oorspronkelijke " +
                    "betekenis terug. Geen vrije vertaling, spraakherkenning of internet. " +
                    "De gekozen beurten verdwijnen bij sluiten.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("kmr", "ckb", "ps").forEach { option ->
                        TextButton(onClick = { language = option; draft = "" }) {
                            Text((if (language == option) "✓ " else "") +
                                (labels[option] ?: option),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("nl", "en").forEach { code ->
                        TextButton(onClick = { meaningLanguage = code; draft = "" }) {
                            Text((if (meaningLanguage == code) "✓ " else "") +
                                (if (code == "nl") "Nederlands" else "English"))
                        }
                    }
                }
                OutlinedButton(
                    onClick = { fromMeaning = !fromMeaning; draft = "" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (fromMeaning) "Ik typ NL/EN → mijn eigen zin ⇄"
                        else "Ik typ eigen zin → mijn eigen NL/EN ⇄")
                }
                OutlinedButton(
                    enabled = notes?.isNotEmpty() == true,
                    onClick = { choosingSaved = !choosingSaved },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (choosingSaved) "Sluit mijn themakiezer"
                        else "Kies een opgeslagen zin uit mijn thema’s of favorieten…")
                }
                if (choosingSaved && notes != null) {
                    OfflinePhraseTurnTopicPicker(
                        notes = notes,
                        language = language,
                        meaningLanguage = meaningLanguage,
                        onPick = { note ->
                            // Only the deliberately tapped original user-authored
                            // pair may become an ephemeral conversation turn.
                            if (note in notes && note.language == language &&
                                note.meaningLanguage == meaningLanguage) {
                                turns = (turns + LocalPersonalTurn(note, fromMeaning))
                                    .takeLast(12)
                                draft = ""
                                choosingSaved = false
                                if (alternate) fromMeaning = !fromMeaning
                            }
                        }
                    )
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(180) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    label = { Text("Typ exact een eerder bewaarde zin") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (!fromMeaning && language in setOf("ckb", "ps"))
                            TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                if (notes == null) {
                    Text("Eigen zinnen konden niet worden gelezen. Er wordt niets gewijzigd.",
                        color = MaterialTheme.colorScheme.error)
                } else if (notes.isEmpty()) {
                    Text("Er zijn nog geen opgeslagen eigen zinnen.")
                } else if (draft.isNotBlank() && choices.isEmpty()) {
                    Text("Geen exact opgeslagen paar. Tuddy verzint geen vertaling.",
                        color = MaterialTheme.colorScheme.error)
                } else if (choices.isNotEmpty()) {
                    Text("Kies jouw originele paar (" + choices.size.toString() + "):",
                        style = MaterialTheme.typography.labelMedium)
                    choices.forEach { note ->
                        val counterpart = if (fromMeaning) note.phrase else note.ownMeaning
                        TextButton(onClick = {
                            turns = (turns + LocalPersonalTurn(note, fromMeaning)).takeLast(12)
                            draft = ""
                            if (alternate) fromMeaning = !fromMeaning
                        }) {
                            Text((labels[note.language] ?: note.language) + " ↔ " + counterpart)
                        }
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { alternate = !alternate }) {
                        Text(if (alternate) "✓ Wissel na beurt" else "☐ Wissel na beurt")
                    }
                    TextButton(onClick = {
                        val removed = turns.lastOrNull()
                        turns = turns.dropLast(1)
                        // Restore the direction used by the removed turn, even if
                        // the user changed the alternating setting afterwards.
                        if (removed != null) fromMeaning = removed.fromMeaning
                    }, enabled = turns.isNotEmpty()) {
                        Text("↶ Laatste beurt terug")
                    }
                    TextButton(onClick = { turns = emptyList() }, enabled = turns.isNotEmpty()) {
                        Text("Wis alle beurten")
                    }
                }
                if (turns.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { tableTurn = turns.lastIndex },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("↕ Speel dit eigen gesprek tegenover elkaar af")
                    }
                    Text("Bekijk de beurten één voor één, met aan beide " +
                        "kanten de juiste originele zin en jouw eigen betekenis. " +
                        "Geen opname, vertaalmodel of automatische opslag.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (turns.isEmpty()) {
                    Text("Nog geen gekozen beurt. Alleen expliciet gekozen " +
                        "paren verschijnen hieronder.", style = MaterialTheme.typography.bodySmall)
                }
                turns.forEachIndexed { index, turn ->
                    HorizontalDivider()
                    val note = turn.note
                    val prompt = if (turn.fromMeaning) note.ownMeaning else note.phrase
                    val result = if (turn.fromMeaning) note.phrase else note.ownMeaning
                    Text("Beurt " + (index + 1) + ": " + prompt,
                        style = MaterialTheme.typography.bodySmall)
                    Text(result, style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (turn.fromMeaning && note.language in setOf("ckb", "ps"))
                            TextDirection.Rtl else TextDirection.Ltr))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { tableTurn = index }) {
                            Text("↕ Toon aan beide kanten")
                        }
                        TextButton(enabled = index > 0, onClick = {
                            val reordered = turns.toMutableList()
                            val moved = reordered.removeAt(index)
                            reordered.add(index - 1, moved)
                            turns = reordered
                        }) {
                            Text("↑")
                        }
                        TextButton(enabled = index < turns.lastIndex, onClick = {
                            val reordered = turns.toMutableList()
                            val moved = reordered.removeAt(index)
                            reordered.add(index + 1, moved)
                            turns = reordered
                        }) {
                            Text("↓")
                        }
                        TextButton(onClick = {
                            turns = turns.filterIndexed { i, _ -> i != index }
                        }) {
                            Text("Verwijder")
                        }
                    }
                    TextButton(onClick = { shown = note }) {
                        Text("Toon deze eigen zin groot")
                    }
                }
                OfflinePersonalTurnScriptExport(
                    turns = turns.map { it.note to it.fromMeaning }
                )
                Text("Deze zinnen zijn niet onafhankelijk beoordeeld. " +
                    "Gebruik ze niet bij urgente, medische of juridische beslissingen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten en wis beurten") } },
        dismissButton = {
            TextButton(onClick = onManagePhrases) { Text("Mijn eigen zinnen beheren…") }
        }
    )
}
