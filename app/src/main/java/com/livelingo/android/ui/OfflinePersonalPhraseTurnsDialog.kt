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
import com.livelingo.android.PrivatePhraseTextPolicy
import java.util.Locale

private data class LocalPersonalTurn(
    val note: PersonalPhrase,
    val fromMeaning: Boolean,
    val temporary: Boolean = false
)

/**
 * Finite user-authored conversation, never machine translation: a turn is
 * available from an exact saved personal phrase OR when both sides are manually
 * provided in full for a temporary pair. All turns live in Compose RAM.
 * Only a separately previewed, user-approved Android SAF export can persist them.
 */
@Composable
internal fun OfflinePersonalPhraseTurnsDialog(
    initialLanguage: String,
    onManagePhrases: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deck = remember(context) { PrivatePhraseDeck(context) }
    var storedNotes by remember(deck) { mutableStateOf(deck.entries()) }
    // Immutable rendering snapshot enables safe nullable smart casts.
    val notes = storedNotes
    var language by remember(initialLanguage) {
        mutableStateOf(initialLanguage.takeIf { it in setOf("kmr", "ckb", "ps") } ?: "kmr")
    }
    var meaningLanguage by remember { mutableStateOf("nl") }
    var fromMeaning by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var manualOriginal by remember { mutableStateOf("") }
    var manualMeaning by remember { mutableStateOf("") }
    var manualNotice by remember { mutableStateOf("") }
    var saveNotice by remember { mutableStateOf("") }
    var pendingSave by remember { mutableStateOf<Pair<Int, PersonalPhrase>?>(null) }
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
                Text("Kies om de beurt een exact bewaard eigen zinnenpaar, " +
                    "of typ zelf beide kanten van een nieuw tijdelijk paar. " +
                    "Tuddy genereert geen betekenis: geen vrije vertaling, " +
                    "spraakherkenning of internet. De gekozen beurten " +
                    "verdwijnen bij sluiten, tenzij je zelf expliciet exporteert.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("kmr", "ckb", "ps").forEach { option ->
                        TextButton(onClick = {
                            language = option
                            draft = ""; manualOriginal = ""; manualMeaning = ""
                            manualNotice = ""
                        }) {
                            Text((if (language == option) "✓ " else "") +
                                (labels[option] ?: option),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("nl", "en").forEach { code ->
                        TextButton(onClick = {
                            meaningLanguage = code
                            draft = ""; manualOriginal = ""; manualMeaning = ""
                            manualNotice = ""
                        }) {
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
                Text("Nieuw: maak één tijdelijke beurt met twee zelfgetypte zinnen.",
                    style = MaterialTheme.typography.labelMedium)
                Text("Dit is géén automatische vertaling: beide teksten moet jij zelf " +
                    "schrijven of met je gesprekspartner afspreken. Deze invoer wordt " +
                    "niet toegevoegd aan Mijn eigen zinnen en verdwijnt bij sluiten.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = manualOriginal,
                    onValueChange = { manualOriginal = it.take(120); manualNotice = "" },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    label = { Text("Zelfgeschreven " + (labels[language] ?: language) + "-zin") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (language in setOf("ckb", "ps"))
                            TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                OutlinedTextField(
                    value = manualMeaning,
                    onValueChange = { manualMeaning = it.take(180); manualNotice = "" },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    label = { Text("Mijn eigen betekenis (" + meaningLanguage.uppercase() + ")") }
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualOriginal.isNotBlank() && manualMeaning.isNotBlank(),
                    onClick = {
                        val original = manualOriginal.trim()
                        val meaning = manualMeaning.trim()
                        if (!PrivatePhraseTextPolicy.valid(original, 120) ||
                            !PrivatePhraseTextPolicy.valid(meaning, 180)) {
                            manualNotice = "Tekst bevat ongeldige tekens of is te lang."
                        } else {
                            val own = PersonalPhrase(language, original, meaningLanguage, meaning)
                            turns = (turns + LocalPersonalTurn(own, fromMeaning, temporary = true))
                                .takeLast(12)
                            manualOriginal = ""; manualMeaning = ""
                            manualNotice = "Tijdelijk paar toegevoegd, nergens opgeslagen."
                            if (alternate) fromMeaning = !fromMeaning
                        }
                    }
                ) { Text("Voeg mijn twee tijdelijke zinnen als beurt toe") }
                if (manualNotice.isNotBlank()) {
                    Text(manualNotice, style = MaterialTheme.typography.bodySmall)
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
                if (saveNotice.isNotBlank()) {
                    Text(saveNotice, style = MaterialTheme.typography.bodySmall)
                }
                turns.forEachIndexed { index, turn ->
                    HorizontalDivider()
                    val note = turn.note
                    val prompt = if (turn.fromMeaning) note.ownMeaning else note.phrase
                    val result = if (turn.fromMeaning) note.phrase else note.ownMeaning
                    Text("Beurt " + (index + 1) +
                        (if (turn.temporary) " · alleen tijdelijk" else " · opgeslagen paar") +
                        ": " + prompt,
                        style = MaterialTheme.typography.bodySmall)
                    Text(result, style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (turn.fromMeaning && note.language in setOf("ckb", "ps"))
                            TextDirection.Rtl else TextDirection.Ltr))
                    OutlinedButton(
                        onClick = { tableTurn = index },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("↕ Toon deze beurt aan beide kanten")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(enabled = index > 0, onClick = {
                            val reordered = turns.toMutableList()
                            val moved = reordered.removeAt(index)
                            reordered.add(index - 1, moved)
                            turns = reordered
                        }) {
                            Text("↑ Eerder")
                        }
                        TextButton(enabled = index < turns.lastIndex, onClick = {
                            val reordered = turns.toMutableList()
                            val moved = reordered.removeAt(index)
                            reordered.add(index + 1, moved)
                            turns = reordered
                        }) {
                            Text("↓ Later")
                        }
                        TextButton(onClick = {
                            turns = turns.filterIndexed { i, _ -> i != index }
                        }) {
                            Text("Verwijder")
                        }
                    }
                    if (turn.temporary) {
                        TextButton(onClick = {
                            pendingSave = index to note
                            saveNotice = ""
                        }) {
                            Text("Bewaar dit eigen paar in mijn privéwoordenlijst…")
                        }
                    } else {
                        TextButton(onClick = { shown = note }) {
                            Text("Toon deze opgeslagen zin groot")
                        }
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
    val request = pendingSave
    if (request != null) {
        val (position, proposed) = request
        AlertDialog(
            onDismissRequest = { pendingSave = null },
            title = { Text("Dit paar voortaan op dit toestel bewaren?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Je verlaat de tijdelijke gespreksmodus voor uitsluitend " +
                        "dit zelfgeschreven paar. Het blijft dan lokaal in je " +
                        "persoonlijke zinnenlijst totdat je het zelf verwijdert. " +
                        "Er wordt niets gedeeld of automatisch vertaald.",
                        style = MaterialTheme.typography.bodySmall)
                    Text(proposed.language.uppercase() + ": " + proposed.phrase,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (proposed.language in setOf("ckb", "ps"))
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                    Text(proposed.meaningLanguage.uppercase() + ": " + proposed.ownMeaning,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Een bestaande zin wordt nooit overschreven. " +
                        "Deze eigen betekenis is niet gecontroleerd.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val current = turns.getOrNull(position)
                    if (current == null || !current.temporary || current.note != proposed) {
                        saveNotice = "De gekozen tijdelijke beurt is gewijzigd. " +
                            "Kies opnieuw bewaren."
                    } else {
                        val saved = deck.saveNew(
                            proposed.language, proposed.phrase,
                            proposed.meaningLanguage, proposed.ownMeaning
                        )
                        if (saved) {
                            storedNotes = deck.entries()
                            turns = turns.mapIndexed { index, item ->
                                if (index == position) item.copy(temporary = false) else item
                            }
                            saveNotice = "Alleen dit eigen zinnenpaar is lokaal bewaard."
                        } else {
                            saveNotice = "Niet opgeslagen. Deze bronzin bestaat al, " +
                                "de privéwoordenlijst is vol of opslag is niet beschikbaar. " +
                                "Bestaande betekenissen zijn niet gewijzigd."
                        }
                    }
                    pendingSave = null
                }) { Text("Bewaar alleen dit zelfgeschreven paar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSave = null }) { Text("Tijdelijk houden") }
            }
        )
    }
}
