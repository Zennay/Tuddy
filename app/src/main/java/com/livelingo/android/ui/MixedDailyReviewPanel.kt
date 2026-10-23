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
import androidx.compose.material3.FilterChip
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
    var writeFirst by remember { mutableStateOf(false) }
    var typedAnswer by remember { mutableStateOf("") }
    var remembered by remember { mutableIntStateOf(0) }
    var hard by remember { mutableIntStateOf(0) }
    var skipped by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }
    var skippedCards by remember { mutableStateOf<List<MixedStudyCard>>(emptyList()) }
    var replayCards by remember { mutableStateOf<List<MixedStudyCard>?>(null) }
    // Pick once before the round; later filtering would allow previously
    // rated cards to reappear in this still-open session.
    var selectedLanes by remember(mixed) {
        mutableStateOf(buildSet {
            if (mixed.orEmpty().any { it is MixedStudyCard.Basis }) add("basis")
            if (mixed.orEmpty().any { it is MixedStudyCard.Phrase }) add("phrase")
            if (mixed.orEmpty().any { it is MixedStudyCard.Own }) add("own")
        })
    }
    var started by remember { mutableStateOf(false) }
    var cardLimit by remember { mutableStateOf<Int?>(null) }
    val matchingCards = mixed?.filter { card ->
        when (card) {
            is MixedStudyCard.Basis -> "basis" in selectedLanes
            is MixedStudyCard.Phrase -> "phrase" in selectedLanes
            is MixedStudyCard.Own -> "own" in selectedLanes
        }
    }
    val selectedCards = if (cardLimit == null) matchingCards
        else matchingCards?.take(cardLimit!!)
    val notInThisRound = (matchingCards?.size ?: 0) - (selectedCards?.size ?: 0)
    val activeCards = replayCards ?: selectedCards
    val card = if (started) activeCards?.getOrNull(index) else null
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
        typedAnswer = ""
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
                } else if (!started) {
                    Text("Kies de leergroepen voor deze ronde",
                        style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Alleen ingeschakelde privé-herhaalplanningen leveren kaarten. " +
                            "Je keuze blijft voor deze ronde vast staan. " +
                            "Er wordt niets automatisch beoordeeld of opgeslagen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val lanes = listOf(
                        Triple("basis", "Bewaarde basiswoorden",
                            mixed.count { it is MixedStudyCard.Basis }),
                        Triple("phrase", "Zelfgeschreven zinnen",
                            mixed.count { it is MixedStudyCard.Phrase }),
                        Triple("own", "Eigen woordbetekenissen",
                            mixed.count { it is MixedStudyCard.Own })
                    )
                    lanes.forEach { (key, label, count) ->
                        FilterChip(
                            selected = key in selectedLanes,
                            onClick = {
                                selectedLanes = if (key in selectedLanes)
                                    selectedLanes - key else selectedLanes + key
                            },
                            enabled = count > 0,
                            label = { Text("$label ($count)") }
                        )
                    }
                    Text("Kies hoeveel kaarten je nu wilt doen",
                        style = MaterialTheme.typography.titleSmall)
                    listOf<Pair<Int?, String>>(
                        null to "Alle kaarten", 5 to "5 kaarten", 10 to "10 kaarten"
                    ).forEach { (count, label) ->
                        FilterChip(
                            selected = cardLimit == count,
                            onClick = { cardLimit = count },
                            label = { Text(label) }
                        )
                    }
                    Text("${selectedCards?.size ?: 0} kaarten in deze ronde" +
                        if (notInThisRound > 0)
                            " · $notInThisRound andere vandaag nog aan de beurt"
                        else "",
                        style = MaterialTheme.typography.bodyMedium)
                    if (selectedCards.isNullOrEmpty()) {
                        Text("Kies minimaal één leergroep met kaarten aan de beurt. " +
                            "Er wordt geen lege ronde gestart.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { started = true },
                        enabled = !selectedCards.isNullOrEmpty(),
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Begin mijn gekozen oefenronde")
                    }
                } else if (card == null) {
                    Text(
                        if (mixed.isEmpty()) "Geen kaarten aan de beurt."
                        else "Gemengde ronde afgerond",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "${remembered} zelf onthouden · ${hard} nog oefenen · " +
                            "${skippedCards.size} kaarten nog overgeslagen " +
                            "(${skipped} overslaacties).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Overgeslagen kaarten behouden hun oorspronkelijke herhaaldatum. " +
                            "Je kunt alleen die kaarten nogmaals bekijken of de ronde sluiten.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (notInThisRound > 0) {
                        Text("$notInThisRound andere kaarten uit je gekozen groepen " +
                            "zijn buiten deze korte ronde gebleven en behouden hun planning.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (skippedCards.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            replayCards = skippedCards.toList()
                            skippedCards = emptyList()
                            index = 0
                            revealed = false
                            typedAnswer = ""
                            notice = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Herhaal ${skippedCards.size} overgeslagen kaarten")
                        }
                    }
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
                    Text("Kaart ${index + 1} / ${activeCards?.size ?: 0} · ${language.uppercase()}",
                        style = MaterialTheme.typography.labelMedium)
                    Text(sourceLabel, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        reverse = !reverse
                        revealed = false
                        typedAnswer = ""
                    }) {
                        Text(if (reverse) "Betekenis → woord/zin ⇄"
                            else "Woord/zin → betekenis ⇄")
                    }
                    TextButton(onClick = {
                        writeFirst = !writeFirst
                        typedAnswer = ""
                        revealed = false
                    }) { Text(if (writeFirst) "✓ Zelf antwoord schrijven" else
                        "✍ Zelf antwoord schrijven") }
                    HorizontalDivider()
                    Text(
                        if (reverse) answer else prompt,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            textDirection = if (!reverse && language in rtlLanguages)
                                TextDirection.Rtl else TextDirection.Ltr
                        )
                    )
                    if (writeFirst && !revealed) {
                        OutlinedTextField(
                            value = typedAnswer,
                            onValueChange = { typedAnswer = it.take(220) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (reverse) "Schrijf mijn woord of zin" else
                                "Schrijf mijn betekenis") },
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (reverse && language in rtlLanguages)
                                    TextDirection.Rtl else TextDirection.Ltr
                            )
                        )
                        Text("Jouw poging wordt niet opgeslagen, verstuurd of automatisch beoordeeld.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (!revealed) {
                        Button(modifier = Modifier.fillMaxWidth(),
                            onClick = { revealed = true }) {
                            Text("Toon antwoord")
                        }
                    } else {
                        if (writeFirst) {
                            Text("Jouw getypte poging:",
                                style = MaterialTheme.typography.labelMedium)
                            Text(typedAnswer.ifBlank { "(niet ingevuld)" },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (reverse && language in rtlLanguages)
                                        TextDirection.Rtl else TextDirection.Ltr
                                ))
                            Text("Vergelijk zelf met het oorspronkelijke antwoord.",
                                style = MaterialTheme.typography.bodySmall)
                        }
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
                        skippedCards = skippedCards + card
                        skipped++
                        index++
                        revealed = false
                        typedAnswer = ""
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
