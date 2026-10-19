package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.OwnWord
import kotlin.random.Random

/**
 * Completely ephemeral flashcards over explicitly user-authored word meanings.
 * Neither the cards' answers nor scores are sent to any service or persisted.
 * This deliberately makes no claim of verified, native-speaker-reviewed meanings.
 */
@Composable
internal fun OwnWordPracticePanel(
    notes: List<OwnWord>,
    initialLanguage: String,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var shuffledVersion by remember { mutableIntStateOf(0) }
    var reverse by remember { mutableStateOf(false) }
    val availableLanguages = remember(notes) { notes.map { it.language }.distinct().sorted() }
    val selectedNotes = remember(notes, selectedLanguage, shuffledVersion) {
        val filtered = if (selectedLanguage == "all") notes
            else notes.filter { it.language == selectedLanguage }
        if (shuffledVersion == 0) filtered else filtered.shuffled(Random(shuffledVersion))
    }
    // Ephemeral user-selected focus list, reset if language/shuffle source changes.
    var focusNotes by remember(selectedNotes) { mutableStateOf<List<OwnWord>?>(null) }
    var focusRoundVersion by remember(selectedNotes) { mutableIntStateOf(0) }
    val studyNotes = focusNotes ?: selectedNotes
    var offset by remember(studyNotes, focusNotes != null, focusRoundVersion) { mutableIntStateOf(0) }
    var index by remember(studyNotes, focusNotes != null, focusRoundVersion, offset) { mutableIntStateOf(0) }
    var revealed by remember(studyNotes, focusNotes != null, focusRoundVersion, offset) { mutableStateOf(false) }
    var known by remember(studyNotes, focusNotes != null, focusRoundVersion, offset) { mutableIntStateOf(0) }
    var hard by remember(studyNotes, focusNotes != null, focusRoundVersion, offset) { mutableIntStateOf(0) }
    var missed by remember(studyNotes, focusNotes != null, focusRoundVersion, offset) { mutableStateOf<List<OwnWord>>(emptyList()) }
    val group = studyNotes.drop(offset).take(20)
    val current = group.getOrNull(index)
    val rtlCodes = setOf("ckb", "ps", "ar", "fa", "ur", "sd", "ug")
    val names = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val scoreLine = "Beoordeeld: ${known + hard} / ${group.size} · ${known} onthouden · ${hard} lastig"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oefen mijn eigen woordbetekenissen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Dit zijn alleen de woorden en betekenissen die jij eerder bewust " +
                        "zelf hebt bewaard. Niet gecontroleerd door Tuddy of een " +
                        "native speaker. Geen taalmodel, microfoon, internet of opgeslagen score.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Kies taal", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { selectedLanguage = "all" }) {
                    Text(if (selectedLanguage == "all") "Alle talen ✓" else "Alle talen")
                }
                availableLanguages.forEach { lang ->
                    TextButton(onClick = { selectedLanguage = lang }) {
                        val name = names[lang] ?: lang.uppercase()
                        Text(if (selectedLanguage == lang) "$name ✓" else name)
                    }
                }
                if (focusNotes != null) {
                    Text("Focusronde: alleen ${focusNotes?.size ?: 0} woorden die jij " +
                        "eerder lastig vond. Dit blijft uitsluitend tijdens deze sessie.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { focusNotes = null; focusRoundVersion++ }
                    ) { Text("Terug naar de hele woordenlijst") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = selectedNotes.size > 1,
                        onClick = { shuffledVersion++ }
                    ) { Text("↻ Schud kaarten") }
                    TextButton(onClick = { reverse = !reverse; revealed = false }) {
                        Text(if (reverse) "Betekenis → woord ⇄" else "Woord → betekenis ⇄")
                    }
                }
                if (current == null) {
                    Text(
                        if (group.isEmpty()) "Voor deze taal zijn nog geen eigen woorden opgeslagen."
                        else "Ronde afgerond",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (group.isNotEmpty()) {
                        Text(scoreLine, style = MaterialTheme.typography.bodyMedium)
                        if (missed.isNotEmpty()) {
                            Text("Door jou lastig gevonden: ${missed.size} unieke woorden. " +
                                "Je kunt deze apart oefenen, zonder een automatische taalcontrole.",
                                style = MaterialTheme.typography.bodySmall)
                            missed.take(8).forEach { note ->
                                Text("• ${note.word}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDirection = if (note.language in rtlCodes)
                                            TextDirection.Rtl else TextDirection.Ltr
                                    ))
                            }
                            if (missed.size > 8) Text("En ${missed.size - 8} andere woorden.",
                                style = MaterialTheme.typography.bodySmall)
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { focusNotes = missed.toList(); focusRoundVersion++ }
                            ) { Text("Oefen mijn ${missed.size} lastige woorden") }
                        }
                        Text("De beoordeling blijft alleen bestaan zolang je deze ronde opent.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                offset = 0
                                index = 0
                                known = 0
                                hard = 0
                                revealed = false
                            }
                        ) { Text("Begin deze woordselectie opnieuw") }
                        if (focusNotes == null && offset + group.size < selectedNotes.size) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    offset += group.size
                                    index = 0
                                    known = 0
                                    hard = 0
                                    revealed = false
                                }
                            ) { Text("Volgende 20 eigen woorden oefenen") }
                        }
                    }
                } else {
                    val isRtlWord = current.language in rtlCodes
                    val prompt = if (reverse) current.ownMeaning else current.word
                    val answer = if (reverse) current.word else current.ownMeaning
                    Text(
                        "${index + 1} / ${group.size} · ${names[current.language] ?: current.language.uppercase()}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    HorizontalDivider()
                    Text(
                        prompt,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            textDirection = if (!reverse && isRtlWord) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    if (!revealed) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { revealed = true }
                        ) { Text("Toon mijn eigen betekenis") }
                    } else {
                        Text(
                            answer,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = if (reverse && isRtlWord) TextDirection.Rtl
                                    else TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Zelfgeschreven en niet geverifieerd.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (current !in missed) missed = missed + current
                                    hard++
                                    index++
                                    revealed = false
                                }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { known++; index++; revealed = false }
                            ) { Text("Ik wist het") }
                        }
                    }
                    Text(scoreLine, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Maximaal 20 kaarten per ronde. Je kunt zelf aangewezen " +
                        "lastige woorden direct opnieuw oefenen. Geen automatische " +
                        "taalcontrole, voortgangsopslag of pushmelding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar eigen woorden") }
        }
    )
}
