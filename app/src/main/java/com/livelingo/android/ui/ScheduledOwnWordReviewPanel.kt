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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.ScheduledOwnWord
import java.time.LocalDate

/** Deliberate, private review of only self-written, explicitly saved notes. */
@Composable
internal fun ScheduledOwnWordReviewPanel(
    deck: PrivateOwnWordDeck,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val planned = remember { deck.scheduledReviews() }
    val due = remember(planned) { planned.orEmpty().filter { it.dueDay <= today } }
    var index by remember { mutableIntStateOf(0) }
    var reveal by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var writeFirst by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var reviewed by remember { mutableIntStateOf(0) }
    var skippedCards by remember { mutableStateOf<List<ScheduledOwnWord>>(emptyList()) }
    var replayCards by remember { mutableStateOf<List<ScheduledOwnWord>?>(null) }
    val activeCards = replayCards ?: due
    val card = activeCards.getOrNull(index)
    val rtlCodes = setOf("ckb", "ps", "ar", "fa", "ur", "sd", "ug")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen woorden · herhalen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Alle antwoorden zijn door jou zelf geschreven, niet door " +
                    "Tuddy of een taalexpert gecontroleerd. Dit is geen taaltoets.",
                    style = MaterialTheme.typography.bodySmall)
                if (planned == null) {
                    Text("Je lokale eigen woordenlijst kon niet veilig worden gelezen. " +
                        "Er is niets veranderd.", color = MaterialTheme.colorScheme.error)
                } else if (card == null) {
                    Text(if (due.isEmpty()) "Vandaag zijn er geen eigen woorden aan de beurt."
                        else "Ronde afgerond: $reviewed beoordeeld, ${skippedCards.size} nog overgeslagen.",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Overgeslagen woorden behouden hun oorspronkelijke herhaaldatum. " +
                        "Er zijn geen pushmeldingen of automatische beoordelingen.",
                        style = MaterialTheme.typography.bodySmall)
                    if (skippedCards.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            replayCards = skippedCards.toList()
                            skippedCards = emptyList()
                            index = 0
                            reveal = false
                            draft = ""
                            notice = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Herhaal ${skippedCards.size} overgeslagen woorden")
                        }
                    }
                } else {
                    val note = card.entry
                    val rtl = note.language in rtlCodes
                    Text("Kaart ${index + 1} van ${activeCards.size} · ${note.language.uppercase()}",
                        style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { reverse = !reverse; reveal = false; draft = "" }) {
                        Text(if (reverse) "Eigen betekenis → woord ⇄" else "Woord → eigen betekenis ⇄")
                    }
                    TextButton(onClick = { writeFirst = !writeFirst; reveal = false; draft = "" }) {
                        Text(if (writeFirst) "✓ Typ eerst zelf" else "✍ Typ eerst zelf")
                    }
                    HorizontalDivider()
                    Text(
                        if (reverse) note.ownMeaning else note.word,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = if (!reverse && rtl) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    if (writeFirst && !reveal) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(220) },
                            label = { Text(if (reverse) "Mijn woord" else "Mijn eigen betekenis") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (reverse && rtl) TextDirection.Rtl else TextDirection.Ltr
                            )
                        )
                        Text("Alleen tijdelijk, niet opgeslagen of automatisch beoordeeld.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (!reveal) {
                        Button(modifier = Modifier.fillMaxWidth(),
                            onClick = { reveal = true }) {
                            Text("Toon mijn eigen antwoord")
                        }
                    } else {
                        if (writeFirst) {
                            Text("Mijn poging: " + draft.ifBlank { "(niet ingevuld)" },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (reverse && rtl) TextDirection.Rtl else TextDirection.Ltr
                                ))
                        }
                        Text(
                            if (reverse) note.word else note.ownMeaning,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = if (reverse && rtl) TextDirection.Rtl
                                    else TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledWord(note, false)) {
                                        onChanged()
                                        reviewed++
                                        index++
                                        reveal = false
                                        draft = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt; kaart niet beoordeeld."
                                }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledWord(note, true)) {
                                        onChanged()
                                        reviewed++
                                        index++
                                        reveal = false
                                        draft = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt; kaart niet beoordeeld."
                                }
                            ) { Text("Ik wist het") }
                        }
                        Text("Moeilijk: morgen. Zelf onthouden: na 2/4/7/14/30 dagen opnieuw.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        skippedCards = skippedCards + card
                        index++
                        reveal = false
                        draft = ""
                        notice = ""
                    }) { Text("Sla over zonder beoordeling") }
                }
                if (notice.isNotBlank()) Text(notice,
                    color = MaterialTheme.colorScheme.error)
                Text("Alleen de lokale vingerafdruk, niveau en volgende dag blijven " +
                    "bewaard als je deze planning bewust hebt aangezet.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Terug naar mijn woorden") } }
    )
}
