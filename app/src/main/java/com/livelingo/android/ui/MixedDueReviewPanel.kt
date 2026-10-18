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
import com.livelingo.android.DuePersonalPhrase
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateVocabularyDeck
import com.livelingo.android.ScheduledOwnWord
import com.livelingo.android.ScheduledWordReview
import java.time.LocalDate

/** One temporary round from three independently enabled app-private review schedules. */
private sealed interface MixedCard {
    data class Curated(val row: ScheduledWordReview) : MixedCard
    data class Own(val row: ScheduledOwnWord) : MixedCard
    data class Phrase(val row: DuePersonalPhrase) : MixedCard
}

private data class CardText(
    val label: String, val language: String, val prompt: String,
    val answer: String, val userMeaning: Boolean
)

private fun textFor(card: MixedCard): CardText = when (card) {
    is MixedCard.Curated -> CardText(
        "Basiswoord", card.row.saved.language, card.row.saved.word,
        card.row.saved.entry.definition, false
    )
    is MixedCard.Own -> CardText(
        "Eigen woord", card.row.entry.language, card.row.entry.word,
        card.row.entry.ownMeaning, true
    )
    is MixedCard.Phrase -> CardText(
        "Eigen zin", card.row.phrase.language, card.row.phrase.phrase,
        card.row.phrase.ownMeaning, true
    )
}

/** A fresh local-only snapshot: alternate selected groups, at most 30 cards. */
private fun buildRound(
    words: PrivateVocabularyDeck, own: PrivateOwnWordDeck,
    phrases: PrivatePhraseDeck, selected: Set<String>
): List<MixedCard>? {
    val today = LocalDate.now().toEpochDay()
    val groups = mutableListOf<List<MixedCard>>()
    if ("curated" in selected) {
        if (!words.reviewsEnabled()) return null
        groups.add(words.scheduledReviews().filter { it.dueDay <= today }
            .map { MixedCard.Curated(it) })
    }
    if ("own" in selected) {
        if (!own.reviewsEnabled()) return null
        val saved = own.scheduledReviews() ?: return null
        groups.add(saved.filter { it.dueDay <= today }.map { MixedCard.Own(it) })
    }
    if ("phrases" in selected) {
        if (!phrases.reviewsEnabled()) return null
        val saved = phrases.scheduledReviews() ?: return null
        groups.add(saved.filter { it.dueDay <= today }.map { MixedCard.Phrase(it) })
    }
    val queues = groups.map { ArrayDeque(it) }
    val round = mutableListOf<MixedCard>()
    while (round.size < 30 && queues.any { it.isNotEmpty() }) {
        queues.forEach { if (it.isNotEmpty() && round.size < 30) round.add(it.removeFirst()) }
    }
    return round
}

@Composable
internal fun MixedDueReviewPanel(
    words: PrivateVocabularyDeck,
    ownWords: PrivateOwnWordDeck,
    phrases: PrivatePhraseDeck,
    onWordChanged: () -> Unit,
    onOwnWordChanged: () -> Unit,
    onPhraseChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val available = buildSet {
        if (words.reviewsEnabled()) add("curated")
        if (ownWords.reviewsEnabled()) add("own")
        if (phrases.reviewsEnabled()) add("phrases")
    }
    var chosen by remember { mutableStateOf(available) }
    var round by remember { mutableStateOf<List<MixedCard>?>(null) }
    var position by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var reversed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val current = round?.getOrNull(position)
    val copy = current?.let(::textFor)

    fun start() {
        if (chosen.isEmpty() || chosen.any { it !in available }) {
            error = "Kies minstens één lokaal ingeschakelde planning."
            return
        }
        val fresh = buildRound(words, ownWords, phrases, chosen)
        if (fresh == null) {
            error = "Een eigen lijst is niet leesbaar of de planning is uitgeschakeld."
            return
        }
        round = fresh
        position = 0
        revealed = false
        reversed = false
        error = ""
    }

    fun rate(remembered: Boolean) {
        val card = current ?: return
        val success = when (card) {
            is MixedCard.Curated -> words.rateScheduledWord(
                card.row.saved.language, card.row.saved.word, remembered
            )
            is MixedCard.Own -> ownWords.rateScheduledWord(card.row.entry, remembered)
            is MixedCard.Phrase -> phrases.rateScheduledPhrase(card.row.phrase, remembered)
        }
        if (!success) {
            error = "Beoordeling kon niet worden opgeslagen. De kaart blijft open."
            return
        }
        when (card) {
            is MixedCard.Curated -> onWordChanged()
            is MixedCard.Own -> onOwnWordChanged()
            is MixedCard.Phrase -> onPhraseChanged()
        }
        position++
        revealed = false
        error = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemengd offline oefenen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Oefen maximaal 30 kaarten die vandaag aan de beurt zijn: " +
                        "basiswoorden, eigen woorden en eigen zinnen door elkaar. " +
                        "De drie herhaalplannen blijven volledig gescheiden.",
                    style = MaterialTheme.typography.bodySmall
                )
                when {
                    round == null -> {
                        listOf(
                            "curated" to "Bewaarde basiswoorden",
                            "own" to "Mijn eigen woorden",
                            "phrases" to "Mijn eigen zinnen"
                        ).forEach { (id, label) ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = id in available,
                                onClick = {
                                    chosen = if (id in chosen) chosen - id else chosen + id
                                }
                            ) {
                                Text((if (id in chosen) "☑ " else "☐ ") + label +
                                    if (id !in available) " (planning uit)" else "")
                            }
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = chosen.isNotEmpty(),
                            onClick = ::start
                        ) { Text("Begin gemengde ronde") }
                    }
                    copy == null -> {
                        Text(
                            if (round!!.isEmpty()) "Geen kaarten vandaag aan de beurt."
                            else "Je hebt deze ${round!!.size} kaarten afgehandeld.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Overgeslagen kaarten blijven verschuldigd. Je kunt " +
                            "daarna een nieuwe ronde met actuele kaarten starten.",
                            style = MaterialTheme.typography.bodySmall)
                        Button(modifier = Modifier.fillMaxWidth(), onClick = ::start) {
                            Text("Nieuwe ronde met resterende kaarten")
                        }
                        TextButton(onClick = { round = null; error = "" }) {
                            Text("Andere kaartgroepen kiezen")
                        }
                    }
                    else -> {
                        Text(
                            "Kaart ${position + 1} van ${round!!.size} · ${copy.label} · " +
                                copy.language.uppercase(),
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (copy.userMeaning) {
                            TextButton(onClick = { reversed = !reversed; revealed = false }) {
                                Text(if (reversed) "Betekenis → tekst ⇄" else "Tekst → betekenis ⇄")
                            }
                        }
                        HorizontalDivider()
                        val rtl = copy.language in setOf("ckb", "ps", "ar", "fa", "ur")
                        Text(
                            if (reversed && copy.userMeaning) copy.answer else copy.prompt,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                textDirection = if (rtl && !reversed) TextDirection.Rtl
                                    else TextDirection.Ltr
                            )
                        )
                        if (!revealed) {
                            Button(modifier = Modifier.fillMaxWidth(),
                                onClick = { revealed = true }) {
                                Text("Toon antwoord")
                            }
                        } else {
                            Text(
                                if (reversed && copy.userMeaning) copy.prompt else copy.answer,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    textDirection = if (rtl && reversed) TextDirection.Rtl
                                    else TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (copy.userMeaning) Text(
                                "Je eigen betekenis is niet onafhankelijk gecontroleerd.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(modifier = Modifier.weight(1f),
                                    onClick = { rate(false) }) { Text("Nog oefenen") }
                                Button(modifier = Modifier.weight(1f),
                                    onClick = { rate(true) }) { Text("Ik wist het") }
                            }
                        }
                        TextButton(onClick = {
                            position++
                            revealed = false
                            error = ""
                        }) { Text("Overslaan zonder beoordeling") }
                    }
                }
                if (error.isNotBlank()) Text(
                    error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Niets begint automatisch. Geen microfoon, modeldownload, " +
                        "account, cloud of nieuwe opslag. Zelfbeoordeling is geen taaltoets.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
