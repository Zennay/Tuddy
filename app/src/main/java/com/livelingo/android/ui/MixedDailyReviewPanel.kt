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
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateVocabularyDeck
import com.livelingo.android.ScheduledOwnWord
import com.livelingo.android.DuePersonalPhrase
import com.livelingo.android.ScheduledWordReview
import java.time.LocalDate

private sealed class MixedStudyCard {
    data class Basis(val row: ScheduledWordReview) : MixedStudyCard()
    data class Own(val row: ScheduledOwnWord) : MixedStudyCard()
    data class Phrase(val row: DuePersonalPhrase) : MixedStudyCard()
}

/**
 * RAM-only interleaving of three independently consented plans. Only an
 * explicit self-rating updates that card's original app-private store.
 * Closing midway never rates remaining cards. No transcription or model use.
 */
@Composable
internal fun MixedDailyReviewPanel(
    words: PrivateVocabularyDeck,
    ownWords: PrivateOwnWordDeck,
    phrases: PrivatePhraseDeck,
    onWordChanged: () -> Unit,
    onOwnWordChanged: () -> Unit,
    onPhraseChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val mixed: List<MixedStudyCard>? = remember(words, ownWords, phrases) {
        val today = LocalDate.now().toEpochDay()
        val basics: List<MixedStudyCard> = if (words.reviewsEnabled())
            words.scheduledReviews().filter { it.dueDay <= today }
                .map { MixedStudyCard.Basis(it) }
        else emptyList()
        val own: List<MixedStudyCard>? = if (ownWords.reviewsEnabled())
            ownWords.scheduledReviews()?.filter { it.dueDay <= today }
                ?.map { MixedStudyCard.Own(it) }
        else emptyList()
        val privatePhrases: List<MixedStudyCard>? = if (phrases.reviewsEnabled())
            phrases.scheduledReviews()?.filter { it.dueDay <= today }
                ?.map { MixedStudyCard.Phrase(it) }
        else emptyList()
        if (own == null || privatePhrases == null) null else {
            // Rotate sources instead of exhausting one language group first.
            val lanes: List<List<MixedStudyCard>> = listOf(basics, privatePhrases, own)
            buildList<MixedStudyCard> {
                val longest = lanes.maxOfOrNull { lane -> lane.size } ?: 0
                for (i in 0 until longest) {
                    for (lane in lanes) if (i < lane.size) add(lane[i])
                }
            }
        }
    }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var remembered by remember { mutableIntStateOf(0) }
    var hard by remember { mutableIntStateOf(0) }
    var skipped by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }
    val card = mixed?.getOrNull(index)
    val rtlLanguages = setOf("ckb", "ps", "ar", "fa", "ur", "he", "sd", "ug")

    fun rateCurrent(known: Boolean) {
        val current = card ?: return
        val persisted = when (current) {
            is MixedStudyCard.Basis ->
                words.rateScheduledWord(current.row.saved.language,
                    current.row.saved.word, known)
            is MixedStudyCard.Own ->
                ownWords.rateScheduledWord(current.row.entry, known)
            is MixedStudyCard.Phrase ->
                phrases.rateScheduledPhrase(current.row.phrase, known)
        }
        if (!persisted) {
            notice = "Deze beoordeling kon niet veilig worden opgeslagen. " +
                "Je bent nog op dezelfde kaart."
            return
        }
        when (current) {
            is MixedStudyCard.Basis -> onWordChanged()
            is MixedStudyCard.Own -> onOwnWordChanged()
            is MixedStudyCard.Phrase -> onPhraseChanged()
        }
        if (known) remembered++ else hard++
        index++
        revealed = false
        notice = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn gemengde oefenronde · offline") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 530.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Eén tijdelijke ronde van drie aparte privé-herhaalplanningen. " +
                        "Alleen jouw bewuste zelfbeoordeling wordt per bestaande planning " +
                        "opgeslagen. Geen account, microfoon, online model of automatische scores.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (mixed == null) {
                    Text(
                        "Eén van de lokale woorden- of zinnenlijsten kon niet veilig " +
                            "worden gelezen. Er is niets gewijzigd.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (card == null) {
                    Text(
                        if (mixed.isEmpty()) "Geen kaarten aan de beurt."
                        else "Gemengde ronde afgerond",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "${remembered} zelf onthouden · ${hard} nog oefenen · " +
                            "${skipped} zonder beoordeling overgeslagen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Overgeslagen kaarten blijven volgens hun bestaande " +
                            "herhaalplanning aan de beurt. Je kunt deze ronde sluiten.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val (language, prompt, answer, sourceLabel) = when (card) {
                        is MixedStudyCard.Basis -> listOf(
                            card.row.saved.language, card.row.saved.word,
                            card.row.saved.entry.definition, "Ingebouwd basiswoord")
                        is MixedStudyCard.Own -> listOf(
                            card.row.entry.language, card.row.entry.word,
                            card.row.entry.ownMeaning,
                            "Zelfgeschreven woordbetekenis · niet geverifieerd")
                        is MixedStudyCard.Phrase -> listOf(
                            card.row.phrase.language, card.row.phrase.phrase,
                            card.row.phrase.ownMeaning,
                            "Zelfgeschreven zin · niet geverifieerd")
                    }
                    Text("Kaart ${index + 1} / ${mixed.size} · ${language.uppercase()}",
                        style = MaterialTheme.typography.labelMedium)
                    Text(sourceLabel, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { reverse = !reverse; revealed = false }) {
                        Text(if (reverse) "Betekenis → woord/zin ⇄"
                            else "Woord/zin → betekenis ⇄")
                    }
                    HorizontalDivider()
                    Text(
                        if (reverse) answer else prompt,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            textDirection = if (!reverse && language in rtlLanguages)
                                TextDirection.Rtl else TextDirection.Ltr
                        )
                    )
                    if (!revealed) {
                        Button(modifier = Modifier.fillMaxWidth(),
                            onClick = { revealed = true }) {
                            Text("Toon antwoord")
                        }
                    } else {
                        Text(
                            if (reverse) prompt else answer,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = if (reverse && language in rtlLanguages)
                                    TextDirection.Rtl else TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { rateCurrent(false) }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { rateCurrent(true) }
                            ) { Text("Ik wist het") }
                        }
                    }
                    TextButton(onClick = {
                        skipped++
                        index++
                        revealed = false
                        notice = ""
                    }) { Text("Sla zonder beoordeling over") }
                    Text(
                        "Een onbekende kaart hoef je niet te beoordelen. " +
                            "De oorspronkelijke planning blijft voor overslaan intact.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (notice.isNotBlank()) {
                    Text(notice, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar studieoverzicht") }
        }
    )
}
