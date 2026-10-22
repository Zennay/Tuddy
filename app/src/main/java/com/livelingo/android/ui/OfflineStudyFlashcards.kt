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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseGap
import com.livelingo.android.OwnWord
import com.livelingo.android.SavedWord
import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/** Highlight only ranges reported by the installed local TTS voice. */
@Composable
private fun PracticeSpeechText(
    text: String,
    activeSpeechText: String?,
    activeSpeechRange: Pair<Int, Int>?
): AnnotatedString {
    val normalized = text.replace('\n', ' ').replace('\r', ' ').trim()
    val followsSpeech = activeSpeechText != null && normalized == activeSpeechText
    val displayedText = if (followsSpeech) activeSpeechText.orEmpty() else text
    val range = activeSpeechRange.takeIf { followsSpeech }
    val background = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onPrimary
    return remember(displayedText, range, background, foreground) {
        buildAnnotatedString {
            append(displayedText)
            if (range != null && range.first >= 0 &&
                range.second > range.first && range.second <= displayedText.length
            ) {
                addStyle(
                    SpanStyle(
                        background = background,
                        color = foreground,
                        fontWeight = FontWeight.Bold
                    ),
                    range.first,
                    range.second
                )
            }
        }
    }
}

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
    val rtlAnswer: Boolean,
    val spokenText: String,
    val spokenLanguage: String,
    val selfAuthoredPhrase: PersonalPhrase? = null
)

private enum class PracticeFilter { ALL, WORDS, OWN_PHRASES }
private enum class PracticeMode { REVEAL, TYPE, GAP }
private enum class PracticeLength(val limit: Int) { QUICK(12), FOCUSED(30), ALL(200) }

/** Case/spacing/width normalization only: exact typed text, NOT meaning verification. */
private fun literalAnswerMatches(typed: String, expected: String): Boolean {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    return typed.isNotBlank() && typed.length <= 300 && normalize(typed) == normalize(expected)
}

@Composable
internal fun OfflineStudyFlashcards(
    savedWords: List<SavedWord>,
    personalPhrases: List<PersonalPhrase>?,
    ownWords: List<OwnWord>?,
    speaker: LocalCaptionSpeaker,
    onManageWords: () -> Unit,
    onManageOwnWords: () -> Unit,
    onManagePhrases: () -> Unit,
    onSaveStudyBook: () -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf(PracticeFilter.ALL) }
    var mode by remember { mutableStateOf(PracticeMode.REVEAL) }
    var length by remember { mutableStateOf(PracticeLength.QUICK) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var voiceNotice by remember { mutableStateOf("") }
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    val allCards = remember(savedWords, personalPhrases, ownWords, filter, mode) {
        val wordCards = if (filter != PracticeFilter.OWN_PHRASES && mode != PracticeMode.GAP) savedWords.mapNotNull { saved ->
            val definition = saved.entry.definition.trim()
            if (definition.isEmpty() || definition.length > 250) null
            else PracticeCard(
                prompt = if (mode == PracticeMode.TYPE) definition else saved.word,
                answer = if (mode == PracticeMode.TYPE) saved.word else definition,
                language = saved.language.uppercase(),
                note = "Betekenis uit kleine ingebouwde woordenlijst · niet uit gesprek",
                rtlAnswer = false,
                spokenText = saved.word,
                spokenLanguage = saved.language
            )
        } else emptyList()
        val phraseCards = if (filter != PracticeFilter.WORDS || mode == PracticeMode.GAP)
            personalPhrases.orEmpty().mapNotNull { phrase ->
            val gap = if (mode == PracticeMode.GAP) PrivatePhraseGap.from(phrase) else null
            if (mode == PracticeMode.GAP && gap == null) return@mapNotNull null
            PracticeCard(
                prompt = gap?.hiddenPhrase ?: phrase.ownMeaning,
                answer = gap?.missingToken ?: phrase.phrase,
                language = phrase.language.uppercase(),
                note = "Jouw eigen betekenis in ${phrase.meaningLanguage.uppercase()} · niet geverifieerd",
                rtlAnswer = phrase.language == "ckb" || phrase.language == "ps",
                spokenText = phrase.phrase,
                spokenLanguage = phrase.language,
                selfAuthoredPhrase = phrase
            )
        } else emptyList()
        val ownWordCards = if (filter != PracticeFilter.OWN_PHRASES && mode != PracticeMode.GAP) ownWords.orEmpty().map { saved ->
            PracticeCard(
                prompt = if (mode == PracticeMode.TYPE) saved.ownMeaning else saved.word,
                answer = if (mode == PracticeMode.TYPE) saved.word else saved.ownMeaning,
                language = saved.language.uppercase(),
                note = "Jouw eigen woordbetekenis · niet gecontroleerd",
                rtlAnswer = mode == PracticeMode.TYPE &&
                    saved.language in setOf("ar", "fa", "ur", "ckb", "ps"),
                spokenText = saved.word,
                spokenLanguage = saved.language
            )
        } else emptyList()
        (wordCards + ownWordCards + phraseCards).take(200)
    }
    val cards = remember(allCards, length, shuffleSeed) {
        val ordered = if (shuffleSeed == 0) allCards
            else allCards.shuffled(Random(shuffleSeed))
        ordered.take(length.limit)
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
                Text("Oefen met alleen bewust bewaarde woorden en zinnen. " +
                    "Antwoorden en voortgang blijven tijdelijk in deze oefensessie.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    TextButton(onClick = {
                        filter = PracticeFilter.ALL
                        if (mode == PracticeMode.GAP) mode = PracticeMode.REVEAL
                    }) {
                        Text(if (filter == PracticeFilter.ALL) "Alles ✓" else "Alles")
                    }
                    TextButton(onClick = {
                        filter = PracticeFilter.WORDS
                        if (mode == PracticeMode.GAP) mode = PracticeMode.REVEAL
                    }) {
                        Text(if (filter == PracticeFilter.WORDS) "Woorden ✓" else "Woorden")
                    }
                    TextButton(onClick = { filter = PracticeFilter.OWN_PHRASES }) {
                        Text(if (filter == PracticeFilter.OWN_PHRASES) "Zinnen ✓" else "Zinnen")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { mode = PracticeMode.REVEAL }) {
                        Text(if (mode == PracticeMode.REVEAL) "Omdraaien ✓" else "Omdraaien")
                    }
                    TextButton(onClick = { mode = PracticeMode.TYPE }) {
                        Text(if (mode == PracticeMode.TYPE) "Antwoord typen ✓" else "Antwoord typen")
                    }

                }
                OutlinedButton(
                    enabled = personalPhrases.orEmpty().any {
                        PrivatePhraseGap.from(it) != null
                    },
                    onClick = {
                        filter = PracticeFilter.OWN_PHRASES
                        mode = PracticeMode.GAP
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (mode == PracticeMode.GAP)
                        "✓ Mijn eigen zinnen · ontbrekend woord"
                        else "Oefen ontbrekende woorden uit eigen zinnen")
                }
                Text("Oefenronde · kies hoeveel kaarten je nu wilt oefenen",
                    style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(
                        PracticeLength.QUICK to "12 kaarten",
                        PracticeLength.FOCUSED to "30 kaarten",
                        PracticeLength.ALL to "Alle"
                    ).forEach { (choice, label) ->
                        TextButton(onClick = { length = choice }) {
                            Text(if (length == choice) label + " ✓" else label)
                        }
                    }
                }
                TextButton(
                    enabled = allCards.size > 1,
                    onClick = { shuffleSeed++ },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("↻ Nieuwe willekeurige oefenset") }
                Text("In deze ronde: ${cards.size} van ${allCards.size} beschikbare kaarten. " +
                    "Een lastige kaart keert maximaal twee keer terug. " +
                    "Na sluiten verdwijnen je antwoorden en tellers.",
                    style = MaterialTheme.typography.bodySmall)
                if (mode == PracticeMode.TYPE) {
                    Text("Typ het opgeslagen woord of de eigen zin. Tuddy vergelijkt " +
                        "alleen de letterlijke tekst (hoofdletters en spaties tellen niet). " +
                        "Dit is geen controle van uitspraak, grammatica of betekenis.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (mode == PracticeMode.GAP) {
                    Text("Uit je eigen zinnen wordt één woord verborgen. " +
                        "Typ je poging, onthul het woord en beoordeel zelf. " +
                        "De app beoordeelt je taal niet en bewaart geen antwoorden; " +
                        "er is geen netwerkverzoek of nieuw audiobestand.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (ownWords == null && filter != PracticeFilter.OWN_PHRASES) {
                    Text("Eigen woordnotities konden niet veilig worden geladen.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
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
                    PracticeCardsSession(cards, mode, speaker) {
                        voiceNotice = it
                    }
                    if (voiceNotice.isNotBlank()) {
                        Text(voiceNotice, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
                OutlinedButton(
                    enabled = ownWords != null && personalPhrases != null &&
                        (savedWords.isNotEmpty() || ownWords.isNotEmpty() ||
                            personalPhrases.isNotEmpty()),
                    onClick = onSaveStudyBook,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("▤ Maak één afdrukbaar offline studieboek…") }
                Text("Combineert je bewaarde basiswoorden, eigen woordbetekenissen " +
                    "en zinnen in één HTML-bestand na aparte toestemming. " +
                    "Zelfgeschreven inhoud blijft als ongecontroleerd gemarkeerd.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onManageWords, modifier = Modifier.fillMaxWidth()) {
                    Text("Beheer mijn opgeslagen woorden")
                }
                TextButton(onClick = onManageOwnWords, modifier = Modifier.fillMaxWidth()) {
                    Text("Beheer mijn eigen woordbetekenissen")
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
private fun PracticeCardsSession(
    cards: List<PracticeCard>,
    mode: PracticeMode,
    speaker: LocalCaptionSpeaker,
    onVoiceNotice: (String) -> Unit
) {
    val typedMode = mode != PracticeMode.REVEAL
    val pending = remember(cards, mode) {
        mutableStateListOf<PracticeCard>().apply { addAll(cards) }
    }
    var revealed by remember(cards, mode) { mutableStateOf(false) }
    var typedAnswer by remember(cards, mode) { mutableStateOf("") }
    var activeSpeechText by remember(cards, mode) { mutableStateOf<String?>(null) }
    var activeSpeechRange by remember(cards, mode) { mutableStateOf<Pair<Int, Int>?>(null) }
    var mastered by remember(cards, mode) { mutableIntStateOf(0) }
    var retries by remember(cards, mode) { mutableIntStateOf(0) }
    var skipped by remember(cards, mode) { mutableIntStateOf(0) }
    val repeatsByCard = remember(cards, mode) { mutableStateMapOf<PracticeCard, Int>() }

    fun nextCard(remembered: Boolean, repeat: Boolean = false) {
        if (pending.isEmpty()) return
        speaker.stop()
        activeSpeechText = null
        activeSpeechRange = null
        onVoiceNotice("")
        val first = pending.removeAt(0)
        if (repeat && (repeatsByCard[first] ?: 0) < 2) {
            pending.add(first)
            repeatsByCard[first] = (repeatsByCard[first] ?: 0) + 1
            retries++
        } else if (remembered) {
            mastered++
        } else {
            skipped++
        }
        typedAnswer = ""
        revealed = false
    }

    Text("Nog te oefenen: " + pending.size +
        "  ·  Gekend: " + mastered +
        "  ·  Herhaald: " + retries +
        "  ·  Overgeslagen: " + skipped,
        style = MaterialTheme.typography.labelMedium)
    if (pending.isEmpty()) {
        Text("Alle kaarten zijn voor deze sessie afgehandeld. " +
            "Sluit en open opnieuw om vanaf het begin te oefenen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary)
        return
    }
    val card = pending.first()
    // Stop and clear the temporary highlight when the card changes or the app leaves.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(cards, mode, speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
                activeSpeechText = null
                activeSpeechRange = null
                onVoiceNotice("Voorlezen gestopt bij verlaten van het scherm.")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.stop()
        }
    }
    fun pronounceCard(pace: LocalSpeechPace) {
        val safe = card.spokenText.replace('\n', ' ').replace('\r', ' ').trim()
        if (safe.isBlank() || safe.length > 400 || safe.any { it.isISOControl() } ||
            card.spokenLanguage.isBlank()
        ) {
            speaker.stop()
            activeSpeechText = null
            activeSpeechRange = null
            onVoiceNotice("Deze kaart kan niet veilig met een lokale stem worden voorgelezen.")
            return
        }
        activeSpeechText = safe
        activeSpeechRange = null
        speaker.speakSequence(
            lines = listOf(safe),
            languageTag = card.spokenLanguage,
            pace = pace,
            onProgress = { completed, total ->
                onVoiceNotice("Lokaal voorlezen: $completed/$total regels.")
            },
            feedback = onVoiceNotice,
            onFinished = { completed ->
                activeSpeechText = null
                activeSpeechRange = null
                if (completed) onVoiceNotice("Voorlezen voltooid.")
            },
            onLineStarted = { activeSpeechRange = null },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }
    Text(card.language + " · " + card.note,
        style = MaterialTheme.typography.labelSmall)
    // A repeated card hides a different interior word, with unchanged original text.
    val gap = if (mode == PracticeMode.GAP) card.selfAuthoredPhrase?.let {
        PrivatePhraseGap.from(it, repeatsByCard[card] ?: 0)
    } else null
    Text(
        PracticeSpeechText(gap?.hiddenPhrase ?: card.prompt, activeSpeechText, activeSpeechRange),
        style = MaterialTheme.typography.titleLarge.copy(
            textDirection = if (gap != null && card.rtlAnswer) TextDirection.Rtl
                else TextDirection.Ltr
        ))

    if (typedMode && !revealed) {
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = { typedAnswer = it.take(300) },
            label = { Text(if (gap != null) "Mijn ontbrekende woord" else
                "Mijn letterlijke antwoord") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDirection = if (card.rtlAnswer) TextDirection.Rtl else TextDirection.Ltr
            ),
            minLines = 1,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = typedAnswer.isNotBlank(),
            onClick = { revealed = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Vergelijk antwoord") }
    } else if (revealed) {
        HorizontalDivider()
        Text("Opgeslagen antwoord", style = MaterialTheme.typography.labelMedium)
        Text(
            PracticeSpeechText(gap?.missingToken ?: card.answer, activeSpeechText, activeSpeechRange),
            style = MaterialTheme.typography.titleMedium.copy(
                textDirection = if (card.rtlAnswer) TextDirection.Rtl else TextDirection.Ltr
            ),
            color = MaterialTheme.colorScheme.primary)
        // The same strictly installed, non-network Android TTS voice gate as
        // for a live caption. Never reveal the answer with audio before the
        // learner explicitly opens the answer.
        if (card.spokenText.isNotBlank() && card.spokenText.length <= 400) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(enabled = activeSpeechText == null, onClick = {
                    pronounceCard(LocalSpeechPace.NORMAL)
                }) { Text("🔊 Normaal") }
                TextButton(enabled = activeSpeechText == null, onClick = {
                    pronounceCard(LocalSpeechPace.SLOW)
                }) { Text("🔊 Rustig") }
                TextButton(onClick = {
                    speaker.stop()
                    activeSpeechText = null
                    activeSpeechRange = null
                    onVoiceNotice("Voorlezen gestopt.")
                }) { Text("Stop stem") }
            }
            if (activeSpeechText != null) {
                Text(
                    if (activeSpeechRange != null)
                        "Indicatieve tekstmarkering van de geïnstalleerde stem."
                    else "Markering verschijnt als de stem tekstbereiken terugmeldt.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (gap != null) {
            Text("Jouw poging: " + (typedAnswer.ifBlank { "(niet ingevuld)" }),
                style = MaterialTheme.typography.bodyMedium)
            Text("Hele eigen zin:", style = MaterialTheme.typography.bodyMedium)
            Text(
                PracticeSpeechText(
                    card.selfAuthoredPhrase?.phrase.orEmpty(),
                    activeSpeechText,
                    activeSpeechRange
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = if (card.rtlAnswer) TextDirection.Rtl
                        else TextDirection.Ltr
                )
            )
            Text("Zelfgeschreven betekenis: " +
                (card.selfAuthoredPhrase?.ownMeaning ?: ""),
                style = MaterialTheme.typography.bodySmall)
            Text("Vergelijk zelf: de app controleert geen grammatica of vertaling.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if ((repeatsByCard[card] ?: 0) < 2) {
                    OutlinedButton(onClick = { nextCard(false, repeat = true) }) {
                        Text("Nog eens")
                    }
                }
                Button(onClick = { nextCard(true) }) { Text("Ik wist het ✓") }
            }
        } else if (typedMode) {
            val matches = literalAnswerMatches(typedAnswer, card.answer)
            Text(
                if (matches) "De tekst komt overeen met je bewaarde antwoord."
                else "De tekst verschilt van je bewaarde antwoord. " +
                    "Synoniemen en inhoudelijke juistheid worden niet beoordeeld.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!matches && (repeatsByCard[card] ?: 0) < 2) {
                    OutlinedButton(onClick = { nextCard(false, repeat = true) }) {
                        Text("Nog eens")
                    }
                }
                Button(onClick = { nextCard(remembered = matches) }) {
                    Text(if (matches) "Goed · volgende ✓" else "Overslaan")
                }
            }
        } else {
            Text("Beoordeel zelf of je dit wist. Tuddy corrigeert geen eigen zinnen.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if ((repeatsByCard[card] ?: 0) < 2) {
                    OutlinedButton(onClick = { nextCard(false, repeat = true) }) {
                        Text("Nog eens")
                    }
                }
                Button(onClick = { nextCard(true) }) { Text("Ken ik ✓") }
            }
        }
    } else {
        Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Toon antwoord")
        }
    }
}
