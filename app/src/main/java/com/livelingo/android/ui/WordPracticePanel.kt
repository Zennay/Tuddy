package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.livelingo.android.SavedWord
import com.livelingo.android.WordPracticeRound

/**
 * Optional offline practice. Scores are self-reported and exist only while
 * this dialog stays open; glossary entries are never modified or inferred.
 */
@Composable
internal fun WordPracticePanel(
    savedWords: List<SavedWord>,
    onDismiss: () -> Unit
) {
    var startIndex by remember(savedWords) { mutableIntStateOf(0) }
    var round by remember(savedWords) {
        mutableStateOf(WordPracticeRound.start(savedWords))
    }
    val current = round.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oefen mijn woorden · offline") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 490.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Kleine gecontroleerde woordenlijst. Geen netwerk, opname, " +
                        "automatische vertaling of opgeslagen leerprofiel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (round.initialCount == 0) {
                    Text("Bewaar eerst een gecontroleerd woord vanuit een ondertitel.")
                } else if (current != null) {
                    Text(
                        "Afgerond ${round.finishedCards} / ${round.initialCount} · " +
                            "Nog ${round.pending.size} kaarten in deze ronde",
                        style = MaterialTheme.typography.labelMedium
                    )
                    HorizontalDivider()
                    Text(
                        "${current.illustration}  ${current.word}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Taal: ${current.language} · Bedenk de betekenis voordat je kijkt.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (round.revealed) {
                        Text(
                            current.definition,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Beoordeel zelf of je dit wist; dit is geen officiële taaltoets.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { round = round.rate(false) }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { round = round.rate(true) }
                            ) { Text("Ik wist het") }
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { round = round.reveal() }
                        ) { Text("Toon betekenis") }
                    }
                    Text(
                        "Lastige woorden komen maximaal twee keer terug in dezelfde ronde.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Ronde afgerond", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${round.recalled} van ${round.initialCount} woorden uiteindelijk zelf " +
                            "herkend. ${round.needsReview} nog lastig; " +
                            "${round.repeatAttempts} extra oefenpogingen."
                    )
                    Text(
                        "Dit is je eigen inschatting, alleen voor deze sessie. " +
                            "Na sluiten onthoudt Tuddy geen score.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            round = WordPracticeRound.start(savedWords, startIndex)
                        }
                    ) { Text("Oefen deze set opnieuw") }
                    if (savedWords.size > WordPracticeRound.MAX_CARDS) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startIndex = (startIndex + WordPracticeRound.MAX_CARDS) % savedWords.size
                                round = WordPracticeRound.start(savedWords, startIndex)
                            }
                        ) { Text("Volgende woorden oefenen") }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar mijn woorden") }
        }
    )
}
