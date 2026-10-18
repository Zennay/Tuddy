package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PersonalPhrasePracticeRound

/**
 * Optional study of notes typed by the user. No translation engine, online
 * dictionary, or claim that a native speaker verified the source material.
 */
@Composable
internal fun PersonalPhrasePracticePanel(
    notes: List<PersonalPhrase>,
    initialLanguage: String,
    onDismiss: () -> Unit
) {
    val selected = remember(notes, initialLanguage) {
        notes.filter { it.language == initialLanguage }.ifEmpty { notes }
    }
    var offset by remember(selected) { mutableIntStateOf(0) }
    var reverse by remember { mutableStateOf(false) }
    var round by remember(selected) {
        mutableStateOf(PersonalPhrasePracticeRound.start(selected))
    }
    val card = round.current
    val names = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val isRtl = card?.language == "ckb" || card?.language == "ps"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oefen mijn eigen zinnen · offline") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 490.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Alle zinnen en betekenissen zijn door jou zelf geschreven; " +
                        "Tuddy controleert ze niet of met een taalmodel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { reverse = !reverse }) {
                    Text(if (reverse) "Oefen: eigen betekenis → zin  ↔"
                        else "Oefen: zin → eigen betekenis  ↔")
                }
                if (round.total == 0) {
                    Text("Voeg eerst een eigen zin toe en bewaar die lokaal.")
                } else if (card != null) {
                    Text(
                        "Afgerond ${round.recalled + round.needsPractice}/${round.total} " +
                            "· nog ${round.remaining.size} kaarten",
                        style = MaterialTheme.typography.labelMedium
                    )
                    HorizontalDivider()
                    Text(
                        if (reverse) card.ownMeaning else card.phrase,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            textDirection = if (!reverse && isRtl) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    Text(
                        if (reverse) "Eigen betekenis (${card.meaningLanguage})"
                            else "Eigen zin · ${names[card.language] ?: card.language}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (round.revealed) {
                        Text(
                            if (reverse) card.phrase else card.ownMeaning,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDirection = if (reverse && isRtl) TextDirection.Rtl
                                    else TextDirection.Ltr
                            )
                        )
                        Text(
                            "Beoordeel zelf. Dit bewijst geen correcte vertaling.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { round = round.grade(false) }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { round = round.grade(true) }
                            ) { Text("Ik wist het") }
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { round = round.reveal() }
                        ) { Text("Toon mijn betekenis") }
                    }
                    Text(
                        "Lastige zinnen verschijnen maximaal één keer opnieuw in deze ronde.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Oefenronde klaar", style = MaterialTheme.typography.headlineSmall)
                    Text("${round.recalled} zelf herkend, ${round.needsPractice} nog lastig.")
                    Text(
                        "Dit is alleen jouw oordeel; resultaten verdwijnen bij sluiten.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            round = PersonalPhrasePracticeRound.start(selected, offset)
                        }
                    ) { Text("Oefen dezelfde set opnieuw") }
                    if (selected.size > PersonalPhrasePracticeRound.BATCH_SIZE) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                offset = (offset + PersonalPhrasePracticeRound.BATCH_SIZE) % selected.size
                                round = PersonalPhrasePracticeRound.start(selected, offset)
                            }
                        ) { Text("Volgende zinnen oefenen") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar eigen zinnen") }
        }
    )
}
