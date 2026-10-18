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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.OwnWord
import com.livelingo.android.SavedWord
import java.text.Normalizer
import java.util.Locale

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
    val spokenLanguage: String
)

private enum class PracticeFilter { ALL, WORDS, OWN_PHRASES }
private enum class PracticeMode { REVEAL, TYPE }

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
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf(PracticeFilter.ALL) }
    var mode by remember { mutableStateOf(PracticeMode.REVEAL) }
    var voiceNotice by remember { mutableStateOf("") }
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    val cards = remember(savedWords, personalPhrases, ownWords, filter, mode) {
        val wordCards = if (filter != PracticeFilter.OWN_PHRASES) savedWords.mapNotNull { saved ->
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
        val phraseCards = if (filter != PracticeFilter.WORDS) personalPhrases.orEmpty().map { phrase ->
            PracticeCard(
                prompt = phrase.ownMeaning,
                answer = phrase.phrase,
                language = phrase.language.uppercase(),
                note = "Jouw eigen betekenis in ${phrase.meaningLanguage.uppercase()} · niet geverifieerd",
                rtlAnswer = phrase.language == "ckb" || phrase.language == "ps",
                spokenText = phrase.phrase,
                spokenLanguage = phrase.language
            )
        } else emptyList()
        val ownWordCards = if (filter != PracticeFilter.OWN_PHRASES) ownWords.orEmpty().map { saved ->
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { mode = PracticeMode.REVEAL }) {
                        Text(if (mode == PracticeMode.REVEAL) "Omdraaien ✓" else "Omdraaien")
                    }
                    TextButton(onClick = { mode = PracticeMode.TYPE }) {
                        Text(if (mode == PracticeMode.TYPE) "Antwoord typen ✓" else "Antwoord typen")
                    }
                }
                if (mode == PracticeMode.TYPE) {
                    Text("Typ het opgeslagen woord of de eigen zin. Tuddy vergelijkt " +
                        "alleen de letterlijke tekst (hoofdletters en spaties tellen niet). " +
                        "Dit is geen controle van uitspraak, grammatica of betekenis.",
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
                    PracticeCardsSession(cards, mode == PracticeMode.TYPE, speaker) {
                        voiceNotice = it
                    }
                    if (voiceNotice.isNotBlank()) {
                        Text(voiceNotice, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
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
    typedMode: Boolean,
    speaker: LocalCaptionSpeaker,
    onVoiceNotice: (String) -> Unit
) {
    val pending = remember(cards, typedMode) {
        mutableStateListOf<PracticeCard>().apply { addAll(cards) }
    }
    var revealed by remember(cards, typedMode) { mutableStateOf(false) }
    var typedAnswer by remember(cards, typedMode) { mutableStateOf("") }
    var mastered by remember(cards, typedMode) { mutableIntStateOf(0) }
    var retries by remember(cards, typedMode) { mutableIntStateOf(0) }
    var skipped by remember(cards, typedMode) { mutableIntStateOf(0) }

    fun nextCard(remembered: Boolean, repeat: Boolean = false) {
        if (pending.isEmpty()) return
        speaker.stop()
        onVoiceNotice("")
        val first = pending.removeAt(0)
        if (repeat) {
            pending.add(first)
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
    // Never continue pronouncing an earlier answer when the learning mode changes.
    DisposableEffect(cards, typedMode) {
        onDispose { speaker.stop() }
    }
    Text(card.language + " · " + card.note,
        style = MaterialTheme.typography.labelSmall)
    Text(card.prompt, style = MaterialTheme.typography.titleLarge)

    if (typedMode && !revealed) {
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = { typedAnswer = it.take(300) },
            label = { Text("Mijn letterlijke antwoord") },
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
        Text(card.answer,
            style = MaterialTheme.typography.titleMedium.copy(
                textDirection = if (card.rtlAnswer) TextDirection.Rtl else TextDirection.Ltr
            ),
            color = MaterialTheme.colorScheme.primary)
        // The same strictly installed, non-network Android TTS voice gate as
        // for a live caption. Never reveal the answer with audio before the
        // learner explicitly opens the answer.
        if (card.spokenText.isNotBlank() && card.spokenText.length <= 400) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    speaker.speak(card.spokenText, card.spokenLanguage, LocalSpeechPace.NORMAL) {
                        onVoiceNotice(it)
                    }
                }) { Text("🔊 Normaal") }
                TextButton(onClick = {
                    speaker.speak(card.spokenText, card.spokenLanguage, LocalSpeechPace.SLOW) {
                        onVoiceNotice(it)
                    }
                }) { Text("🔊 Rustig") }
                TextButton(onClick = {
                    speaker.stop()
                    onVoiceNotice("Voorlezen gestopt.")
                }) { Text("Stop stem") }
            }
        }
        if (typedMode) {
            val matches = literalAnswerMatches(typedAnswer, card.answer)
            Text(
                if (matches) "De tekst komt overeen met je bewaarde antwoord."
                else "De tekst verschilt van je bewaarde antwoord. " +
                    "Synoniemen en inhoudelijke juistheid worden niet beoordeeld.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!matches) {
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
                OutlinedButton(onClick = { nextCard(false, repeat = true) }) {
                    Text("Nog eens")
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
