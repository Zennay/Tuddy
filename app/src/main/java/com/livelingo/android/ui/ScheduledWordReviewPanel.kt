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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateVocabularyDeck
import java.time.LocalDate

/**
 * Manual, opt-in on-device review of existing curated words only.
 * No microphone, language model, notification, login or remote progress sync.
 * A word is never automatically rated as learned.
 */
@Composable
internal fun ScheduledWordReviewPanel(
    deck: PrivateVocabularyDeck,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val cards = remember { deck.scheduledReviews().filter { it.dueDay <= today } }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var writeFirst by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val current = cards.getOrNull(index)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn herhalingen · offline") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Vrijwillige herhaalplanning. Alleen woordidentiteiten, een herhaalniveau " +
                        "en een volgende datum blijven privé op dit toestel. " +
                        "Geen gesprekken, leerdiagnose, account of meldingen.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (current == null) {
                    Text(
                        if (cards.isEmpty()) "Vandaag zijn er geen woorden aan de beurt."
                        else "Alle ${cards.size} woorden van vandaag zijn beoordeeld.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Kom morgen of later terug via Mijn opgeslagen woorden. " +
                            "Zelfbeoordelingen zijn geen officiële taaltoets; " +
                            "een back-up van woorden bevat geen herhaalschema.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "Kaart ${index + 1} van ${cards.size} · " +
                            "${cards.size - index - 1} hierna",
                        style = MaterialTheme.typography.labelMedium
                    )
                    TextButton(onClick = {
                        writeFirst = !writeFirst
                        revealed = false
                        draft = ""
                    }) { Text(if (writeFirst) "✓ Eerst zelf betekenis schrijven" else
                        "✍ Eerst zelf betekenis schrijven") }
                    HorizontalDivider()
                    Text(
                        "${current.saved.entry.illustration}  ${current.saved.word}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Taal: ${current.saved.language} · Bedenk de betekenis.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (writeFirst && !revealed) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(220) },
                            label = { Text("Mijn bedachte betekenis") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Text("Alleen tijdelijk; geen automatische toets of opslag van je poging.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (revealed) {
                        if (writeFirst) {
                            Text("Mijn getypte poging:",
                                style = MaterialTheme.typography.labelMedium)
                            Text(draft.ifBlank { "(niet ingevuld)" },
                                style = MaterialTheme.typography.bodyLarge)
                            Text("Vergelijk met de ingebouwde definitie hieronder.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(current.saved.entry.definition,
                            style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Alleen je eigen inschatting. ‘Ik wist het’ schuift de volgende " +
                                "herhaling op; ‘Nog oefenen’ brengt het woord morgen terug.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledWord(current.saved.language,
                                            current.saved.word, false)) {
                                        onChanged()
                                        index++
                                        revealed = false
                                        draft = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt. Deze kaart is niet beoordeeld."
                                }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledWord(current.saved.language,
                                            current.saved.word, true)) {
                                        onChanged()
                                        index++
                                        revealed = false
                                        draft = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt. Deze kaart is niet beoordeeld."
                                }
                            ) { Text("Ik wist het") }
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { revealed = true }
                        ) { Text("Toon betekenis") }
                    }
                }
                if (notice.isNotEmpty()) {
                    Text(notice, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Deze modus gebruikt uitsluitend de bestaande kleine lokale " +
                        "woordenlijst. Je oude vrije oefenronde blijft beschikbaar.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar mijn woorden") }
        }
    )
}
