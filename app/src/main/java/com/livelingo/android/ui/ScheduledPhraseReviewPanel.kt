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
import com.livelingo.android.PrivatePhraseDeck
import java.time.LocalDate

/** One day's self-directed, local-only Kurmanji/Sorani/Pashto phrase review. */
@Composable
internal fun ScheduledPhraseReviewPanel(
    deck: PrivatePhraseDeck,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val due = remember { deck.scheduledReviews().orEmpty().filter { it.dueDay <= today } }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var writeFirst by remember { mutableStateOf(false) }
    var typedAnswer by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val current = due.getOrNull(index)
    val langNames = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen zinnen · herhalen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Dit zijn alleen jouw eigen, niet onafhankelijk gecontroleerde " +
                        "zinnen en betekenissen. Zelf beoordelen is geen taaltoets. " +
                        "Geen microfoon, model, account of internet.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (current == null) {
                    Text(
                        if (due.isEmpty()) "Vandaag zijn er geen eigen zinnen aan de beurt."
                        else "Je hebt alle ${due.size} zinnen van vandaag bekeken.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tuddy bewaart uitsluitend een lokale hash, niveau en herhaaldatum. " +
                            "Open de app later zelf opnieuw: er zijn geen meldingen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val phrase = current.phrase
                    val rtl = phrase.language == "ckb" || phrase.language == "ps"
                    val prompt = if (reverse) phrase.ownMeaning else phrase.phrase
                    val answer = if (reverse) phrase.phrase else phrase.ownMeaning
                    Text("Zin ${index + 1} van ${due.size} · ${langNames[phrase.language]}",
                        style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = {
                        reverse = !reverse
                        revealed = false
                        typedAnswer = ""
                    }) {
                        Text(if (reverse) "Betekenis → eigen zin ⇄" else "Eigen zin → betekenis ⇄")
                    }
                    TextButton(onClick = {
                        writeFirst = !writeFirst
                        typedAnswer = ""
                        revealed = false
                    }) { Text(if (writeFirst) "✓ Zelf antwoord schrijven" else
                        "✍ Zelf antwoord schrijven") }
                    HorizontalDivider()
                    Text(
                        prompt,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = if ((!reverse && rtl)) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    if (writeFirst && !revealed) {
                        OutlinedTextField(
                            value = typedAnswer,
                            onValueChange = { typedAnswer = it.take(220) },
                            label = { Text(if (reverse) "Mijn eigen zin" else "Mijn eigen betekenis") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (reverse && rtl) TextDirection.Rtl
                                    else TextDirection.Ltr
                            )
                        )
                        Text("Alleen tijdelijk in dit scherm; geen automatische taalcontrole.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (revealed) {
                        if (writeFirst) {
                            Text("Mijn getypte poging:", style = MaterialTheme.typography.labelMedium)
                            Text(typedAnswer.ifBlank { "(niet ingevuld)" },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (reverse && rtl) TextDirection.Rtl
                                        else TextDirection.Ltr
                                ))
                            Text("Vergelijk zelf met je oorspronkelijke antwoord hieronder.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            answer,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = if (reverse && rtl) TextDirection.Rtl
                                    else TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Je eigen betekenis, niet door Tuddy geverifieerd.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledPhrase(phrase, false)) {
                                        onChanged()
                                        index++
                                        revealed = false
                                        typedAnswer = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt; je beoordeling is niet onthouden."
                                }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledPhrase(phrase, true)) {
                                        onChanged()
                                        index++
                                        revealed = false
                                        typedAnswer = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt; je beoordeling is niet onthouden."
                                }
                            ) { Text("Ik wist het") }
                        }
                        Text(
                            "Nog oefenen: morgen. Ik wist het: later na 2, 4, 7, 14 of 30 dagen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { revealed = true }
                        ) { Text("Toon mijn betekenis") }
                    }
                }
                if (notice.isNotBlank()) Text(
                    notice,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar eigen zinnen") }
        }
    )
}
