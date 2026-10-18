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
import com.livelingo.android.PrivateOwnWordDeck
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
    var notice by remember { mutableStateOf("") }
    val card = due.getOrNull(index)
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
                        else "Alle ${due.size} eigen woorden van vandaag zijn beoordeeld.",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Open Tuddy later zelf opnieuw. Geen pushmeldingen, " +
                        "account of automatische vertaling.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    val note = card.entry
                    val rtl = note.language in rtlCodes
                    Text("Kaart ${index + 1} van ${due.size} · ${note.language.uppercase()}",
                        style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { reverse = !reverse; reveal = false }) {
                        Text(if (reverse) "Eigen betekenis → woord ⇄" else "Woord → eigen betekenis ⇄")
                    }
                    HorizontalDivider()
                    Text(
                        if (reverse) note.ownMeaning else note.word,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = if (!reverse && rtl) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    if (!reveal) {
                        Button(modifier = Modifier.fillMaxWidth(),
                            onClick = { reveal = true }) {
                            Text("Toon mijn eigen antwoord")
                        }
                    } else {
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
                                        index++
                                        reveal = false
                                        notice = ""
                                    } else notice = "Opslaan mislukt; kaart niet beoordeeld."
                                }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledWord(note, true)) {
                                        onChanged()
                                        index++
                                        reveal = false
                                        notice = ""
                                    } else notice = "Opslaan mislukt; kaart niet beoordeeld."
                                }
                            ) { Text("Ik wist het") }
                        }
                        Text("Moeilijk: morgen. Zelf onthouden: na 2/4/7/14/30 dagen opnieuw.",
                            style = MaterialTheme.typography.bodySmall)
                    }
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
