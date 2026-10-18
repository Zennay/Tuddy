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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.SavedWord

/**
 * Fully on-device practice of already saved information. User-provided
 * meanings are never mistaken for verified translations.
 * Session attempts and results stay only in Compose memory.
 */
private data class PracticeCard(
    val prompt: String,
    val answer: String,
    val language: String,
    val note: String,
    val rtlAnswer: Boolean
)

private enum class PracticeFilter { ALL, WORDS, OWN_PHRASES }

@Composable
internal fun OfflineStudyFlashcards(
    savedWords: List<SavedWord>,
    personalPhrases: List<PersonalPhrase>?,
    onManageWords: () -> Unit,
    onManagePhrases: () -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf(PracticeFilter.ALL) }
    val cards = remember(savedWords, personalPhrases, filter) {
        val wordCards = if (filter != PracticeFilter.OWN_PHRASES) savedWords.mapNotNull { saved ->
            val definition = saved.entry.definition.trim()
            if (definition.isEmpty() || definition.length > 250) null
            else PracticeCard(
                prompt = saved.word,
                answer = definition,
                language = saved.language.uppercase(),
                note = "Betekenis uit kleine ingebouwde woordenlijst · niet uit gesprek",
                rtlAnswer = false
            )
        } else emptyList()
        val phraseCards = if (filter != PracticeFilter.WORDS) personalPhrases.orEmpty().map { phrase ->
            PracticeCard(
                prompt = phrase.ownMeaning,
                answer = phrase.phrase,
                language = phrase.language.uppercase(),
                note = "Jouw eigen betekenis in ${phrase.meaningLanguage.uppercase()} · niet geverifieerd",
                rtlAnswer = phrase.language == "ckb" || phrase.language == "ps"
            )
        } else emptyList()
        (wordCards + phraseCards).take(200)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline oefenkaarten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 515.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Oefen met alleen de woorden en zinnen die je zelf hebt bewaard. " +
                    "Je antwoorden en voortgang blijven tijdelijk in deze oefensessie.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    TextButton(onClick = { filter = PracticeFilter.ALL }) {
                        Text(if (filter == PracticeFilter.ALL) "Alles ✓" else "Alles")
                    }
                    TextButton(onClick = { filter = PracticeFilter.WORDS }) {
                        Text(if (filter == PracticeFilter.WORDS) "Woorden ✓" else "Woorden")
                    }
                    TextButton(onClick = { filter = PracticeFilter.OWN_PHRASES }) {
                        Text(if (filter == PracticeFilter.OWN_PHRASES) "Zinnen ✓" else "Zinnen")
                    }
                }
                if (personalPhrases == null && filter != PracticeFilter.WORDS) {
                    Text("Eigen zinnen konden niet veilig worden ingelezen. " +
                        "Er zijn geen onzekere notities als oefenkaarten toegevoegd.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (cards.isEmpty()) {
                    Text("Nog geen kaarten in deze selectie. Bewaar eerst privéwoorden " +
                        "of eigen oefenzinnen.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    PracticeCardsSession(cards)
                }
                HorizontalDivider()
                TextButton(onClick = onManageWords, modifier = Modifier.fillMaxWidth()) {
                    Text("Beheer mijn opgeslagen woorden")
                }
                TextButton(onClick = onManagePhrases, modifier = Modifier.fillMaxWidth()) {
                    Text("Beheer mijn eigen zinnen")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}

@Composable
private fun PracticeCardsSession(cards: List<PracticeCard>) {
    val pending = remember(cards) {
        mutableStateListOf<PracticeCard>().apply { addAll(cards) }
    }
    var revealed by remember(cards) { mutableStateOf(false) }
    var mastered by remember(cards) { mutableIntStateOf(0) }
    var retries by remember(cards) { mutableIntStateOf(0) }

    Text("Nog te oefenen: ${pending.size}  ·  Gekend: $mastered  ·  Herhaald: $retries",
        style = MaterialTheme.typography.labelMedium)
    if (pending.isEmpty()) {
        Text("Alle kaarten zijn voor deze sessie afgevinkt. " +
            "Sluit en open opnieuw om vanaf het begin te oefenen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary)
        return
    }
    val card = pending.first()
    Text(card.language + " · " + card.note,
        style = MaterialTheme.typography.labelSmall)
    Text(card.prompt, style = MaterialTheme.typography.titleLarge)
    if (revealed) {
        HorizontalDivider()
        Text("Antwoord", style = MaterialTheme.typography.labelMedium)
        Text(card.answer,
            style = MaterialTheme.typography.titleMedium.copy(
                textDirection = if (card.rtlAnswer) TextDirection.Rtl else TextDirection.Ltr
            ),
            color = MaterialTheme.colorScheme.primary)
        Text("Beoordeel zelf of je dit wist. Tuddy corrigeert geen eigen zinnen.",
            style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val first = pending.removeAt(0)
                pending.add(first)
                retries++
                revealed = false
            }) { Text("Nog eens") }
            Button(onClick = {
                pending.removeAt(0)
                mastered++
                revealed = false
            }) { Text("Ken ik ✓") }
        }
    } else {
        Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Toon antwoord")
        }
    }
}
