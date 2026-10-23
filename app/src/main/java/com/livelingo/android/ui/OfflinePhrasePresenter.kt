package com.livelingo.android.ui

import android.content.Intent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.PersonalPhrase

@Composable
private fun PhraseSide(
    text: String,
    code: String,
    label: String,
    large: Boolean,
    contentKind: String,
    activeSpeechSide: String?,
    activeSpeechText: String?,
    activeSpeechRange: Pair<Int, Int>?
) {
    val rtl = code in setOf("ckb", "ps", "ar", "fa", "ur")
    val normalized = text.replace('\n', ' ').replace('\r', ' ').trim()
    val followsSpeech = activeSpeechSide == contentKind &&
        activeSpeechText != null && normalized == activeSpeechText
    val displayedText = if (followsSpeech) activeSpeechText.orEmpty() else text
    val range = activeSpeechRange.takeIf { followsSpeech }
    val active = followsSpeech
    val background = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onPrimary
    val annotatedText = remember(displayedText, range, background, foreground) {
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (active) {
            Text(
                if (range != null)
                    "Nu voorgelezen · indicatieve tekstmarkering van de stem."
                else "Nu voorgelezen · deze stem geeft geen woordbereik terug.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (active) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                annotatedText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (large) 34.sp else 25.sp,
                    lineHeight = if (large) 44.sp else 34.sp,
                    textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr,
                    fontWeight = FontWeight.Medium
                ),
                color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            )
        }
    }
}

/**
 * Deliberately visible presentation of the user's OWN notes.
 * Optional speech is user-triggered local TTS only; no ASR/MT. Android sharing is opt-in twice.
 */
@Composable
internal fun OfflinePhrasePresenter(
    notes: List<PersonalPhrase>,
    initial: PersonalPhrase,
    onDismiss: () -> Unit,
    mixedLanguageDeck: Boolean = false
) {
    // A quick-picker deck preserves the exact visible filtered order, even
    // when several languages are selected. Regular single-note callers keep
    // their original same-language alphabetical navigation.
    val cards = remember(notes, initial, mixedLanguageDeck) {
        if (mixedLanguageDeck) notes.toList() else
            notes.filter { it.language == initial.language }
                .sortedWith(compareBy(PersonalPhrase::phrase))
    }
    var selectedIndex by remember(cards, initial) {
        mutableIntStateOf(cards.indexOfFirst { it == initial }.coerceAtLeast(0))
    }
    var large by remember { mutableStateOf(true) }
    var tableMode by remember { mutableStateOf(false) }
    var showMeaningFirst by remember { mutableStateOf(false) }
    var pendingShare by remember { mutableStateOf<PersonalPhrase?>(null) }
    var shareNotice by remember { mutableStateOf("") }
    var audioNotice by remember { mutableStateOf("") }
    var pairPace by remember { mutableStateOf(LocalSpeechPace.NORMAL) }
    var activeSpeechSide by remember { mutableStateOf<String?>(null) }
    var activeSpeechText by remember { mutableStateOf<String?>(null) }
    var activeSpeechRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val context = LocalContext.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
                activeSpeechSide = null
                activeSpeechText = null
                activeSpeechRange = null
                audioNotice = "Voorlezen gestopt bij verlaten van het scherm."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    val selected = cards.getOrNull(selectedIndex)
    LaunchedEffect(
        selected?.phrase, selected?.ownMeaning, selected?.language, selected?.meaningLanguage
    ) {
        // Navigating to another private card must never keep reading the last.
        speaker.stop()
        activeSpeechSide = null
        activeSpeechText = null
        activeSpeechRange = null
        audioNotice = ""
    }
    fun pronouncePhrase(text: String, language: String, side: String, pace: LocalSpeechPace) {
        val safe = text.replace('\n', ' ').replace('\r', ' ').trim()
        if (safe.isBlank() || safe.length > 400 ||
            safe.any { it.isISOControl() } || language.isBlank()
        ) {
            speaker.stop()
            activeSpeechSide = null
            activeSpeechText = null
            activeSpeechRange = null
            audioNotice = "Deze eigen tekst kan niet veilig met een lokale stem worden voorgelezen."
            return
        }
        activeSpeechSide = side
        activeSpeechText = safe
        activeSpeechRange = null
        audioNotice = "Lokale voorlezing voorbereiden…"
        speaker.speakSequence(
            lines = listOf(safe),
            languageTag = language,
            pace = pace,
            onProgress = { completed, total ->
                audioNotice = "Lokaal voorlezen: $completed/$total regels."
            },
            feedback = { audioNotice = it },
            onFinished = { completed ->
                activeSpeechSide = null
                activeSpeechText = null
                activeSpeechRange = null
                if (completed) audioNotice = "Voorlezen voltooid."
            },
            onLineStarted = {
                activeSpeechRange = null
                audioNotice = if (side == "phrase")
                    "Nu voorgelezen: eigen zin." else "Nu voorgelezen: eigen betekenis."
            },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }
    /** Sequentially read user-authored notes with two separate installed local voices. */
    fun pronounceOwnPair(note: PersonalPhrase, meaningFirst: Boolean) {
        val phrase = note.phrase.replace('\n', ' ').replace('\r', ' ').trim()
        val meaning = note.ownMeaning.replace('\n', ' ').replace('\r', ' ').trim()
        if (listOf(phrase, meaning).any { value ->
                value.isBlank() || value.length > 400 ||
                    value.any { ch -> ch.isISOControl() }
            }) {
            speaker.stop()
            activeSpeechSide = null
            activeSpeechText = null
            activeSpeechRange = null
            audioNotice = "Twee veilige afgeronde eigen tekstvelden zijn vereist."
            return
        }
        val stages = if (meaningFirst)
            listOf(Triple(meaning, note.meaningLanguage, "meaning"),
                Triple(phrase, note.language, "phrase"))
        else listOf(Triple(phrase, note.language, "phrase"),
            Triple(meaning, note.meaningLanguage, "meaning"))
        fun playStage(stage: Int) {
            val (spokenText, language, kind) = stages[stage]
            activeSpeechSide = kind
            activeSpeechText = spokenText
            activeSpeechRange = null
            audioNotice = "Geïnstalleerde lokale stem voor " +
                (if (kind == "phrase") "eigen zin" else "eigen betekenis") +
                " controleren…"
            speaker.speakSequence(
                lines = listOf(spokenText),
                languageTag = language,
                pace = pairPace,
                onProgress = { _, _ -> Unit },
                feedback = { audioNotice = it },
                onFinished = { completed ->
                    if (completed && stage == 0) {
                        playStage(1)
                    } else {
                        activeSpeechSide = null
                        activeSpeechText = null
                        activeSpeechRange = null
                        if (completed) audioNotice = "Beide eigen teksten lokaal voorgelezen."
                    }
                },
                onLineStarted = {
                    activeSpeechRange = null
                    audioNotice = "Nu voorgelezen: " +
                        (if (kind == "phrase") "eigen zin." else "eigen betekenis.")
                },
                onWordRange = { _, start, endExclusive ->
                    activeSpeechRange = start to endExclusive
                }
            )
        }
        playStage(0)
    }

    @Composable
    fun RenderPhraseSide(text: String, code: String, label: String, kind: String) {
        PhraseSide(
            text = text,
            code = code,
            label = label,
            large = large,
            contentKind = kind,
            activeSpeechSide = activeSpeechSide,
            activeSpeechText = activeSpeechText,
            activeSpeechRange = activeSpeechRange
        )
    }
    fun clearSpeech() {
        speaker.stop()
        activeSpeechSide = null
        activeSpeechText = null
        activeSpeechRange = null
    }
    fun confirmShare(includeOwnMeaning: Boolean) {
        val chosen = pendingShare
        pendingShare = null
        if (chosen == null || chosen != selected || chosen !in notes) {
            shareNotice = "De kaart is gewijzigd. Kies opnieuw delen."
            return
        }
        val content = if (includeOwnMeaning) {
            chosen.language.uppercase() + ": " + chosen.phrase + "\n" +
                "Mijn eigen betekenis (" + chosen.meaningLanguage.uppercase() + "): " +
                chosen.ownMeaning + "\n" +
                "(Door mij geschreven; niet gecontroleerd door Tuddy)"
        } else chosen.phrase
        if (content.isBlank() || content.length > 400) {
            shareNotice = "Deze eigen zin kan niet veilig worden gedeeld."
            return
        }
        val started = runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(
                send, "Kies waar je jouw eigen zin deelt"
            ))
            true
        }.getOrDefault(false)
        shareNotice = if (started)
            "Android-deelvenster geopend. Jij kiest of en waar je de zin verstuurt."
        else "Delen niet beschikbaar. Er is geen zin verzonden."
    }
    val label = when (selected?.language) {
        "kmr" -> "Kurmanji"
        "ckb" -> "Sorani"
        "ps" -> "Pasjtoe"
        else -> selected?.language?.uppercase() ?: "–"
    }

    if (tableMode) {
        FaceToFacePersonalPhraseDialog(
            cards = cards,
            initialIndex = selectedIndex,
            onBack = { returnedIndex ->
                selectedIndex = returnedIndex
                tableMode = false
            }
        )
        return
    }

    Dialog(
        onDismissRequest = { clearSpeech(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.86f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mijn gesprekskaart · " + label,
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { clearSpeech(); onDismiss() }) { Text("Sluiten") }
                }
                Text(
                    "Door jou geschreven · geen geverifieerde vertaling. " +
                        "Toon alleen zinnen die je met iemand wilt delen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (shareNotice.isNotBlank()) {
                    Text(shareNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (selected != null) {
                    OutlinedButton(
                        onClick = {
                            clearSpeech()
                            tableMode = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("↕ Tegenover elkaar · toon mijn eigen zinnen") }
                    Text("Leg je telefoon plat tussen twee mensen. Beide eigen teksten " +
                        "staan tegenover elkaar; ze zijn niet geverifieerd.",
                        style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                if (selected == null) {
                    Text("Deze opgeslagen zin is niet meer beschikbaar.")
                    Spacer(Modifier.weight(1f))
                } else {
                    Column(
                        modifier = Modifier.weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        if (showMeaningFirst) {
                            RenderPhraseSide(selected.ownMeaning, selected.meaningLanguage,
                                "Eigen betekenis (" + selected.meaningLanguage.uppercase() + ")", "meaning")
                            HorizontalDivider()
                            RenderPhraseSide(selected.phrase, selected.language,
                                "Eigen zin (" + label + ")", "phrase")
                        } else {
                            RenderPhraseSide(selected.phrase, selected.language,
                                "Eigen zin (" + label + ")", "phrase")
                            HorizontalDivider()
                            RenderPhraseSide(selected.ownMeaning, selected.meaningLanguage,
                                "Eigen betekenis (" + selected.meaningLanguage.uppercase() + ")", "meaning")
                        }
                    }
                }
                if (selected != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                enabled = activeSpeechText == null &&
                                    selected.phrase.isNotBlank() && selected.phrase.length <= 400,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pronouncePhrase(
                                        selected.phrase, selected.language, "phrase",
                                        LocalSpeechPace.NORMAL
                                    )
                                }
                            ) { Text("🔊 Eigen zin") }
                            TextButton(
                                enabled = activeSpeechText == null &&
                                    selected.ownMeaning.isNotBlank() &&
                                    selected.ownMeaning.length <= 400,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pronouncePhrase(
                                        selected.ownMeaning, selected.meaningLanguage, "meaning",
                                        LocalSpeechPace.NORMAL
                                    )
                                }
                            ) { Text("🔊 Mijn betekenis") }
                        }
                        Text("Tweetalige eigen zinnen na elkaar · geen modelvertaling",
                            style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                enabled = activeSpeechText == null,
                                modifier = Modifier.weight(1f),
                                onClick = { pairPace = LocalSpeechPace.NORMAL }
                            ) { Text(if (pairPace == LocalSpeechPace.NORMAL)
                                "Normaal ✓" else "Normaal") }
                            TextButton(
                                enabled = activeSpeechText == null,
                                modifier = Modifier.weight(1f),
                                onClick = { pairPace = LocalSpeechPace.SLOW }
                            ) { Text(if (pairPace == LocalSpeechPace.SLOW)
                                "Rustig ✓" else "Rustig") }
                        }
                        OutlinedButton(
                            enabled = activeSpeechText == null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { pronounceOwnPair(selected, meaningFirst = false) }
                        ) { Text("🔊 Mijn zin → eigen betekenis") }
                        OutlinedButton(
                            enabled = activeSpeechText == null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { pronounceOwnPair(selected, meaningFirst = true) }
                        ) { Text("🔊 Eigen betekenis → mijn zin") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                enabled = activeSpeechText == null &&
                                    selected.ownMeaning.isNotBlank() &&
                                    selected.ownMeaning.length <= 400,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pronouncePhrase(
                                        selected.ownMeaning, selected.meaningLanguage, "meaning",
                                        LocalSpeechPace.SLOW
                                    )
                                }
                            ) { Text("Rustig mijn betekenis") }
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    clearSpeech()
                                    audioNotice = "Lokale voorlezing gestopt."
                                }
                            ) { Text("■ Stop") }
                        }
                    }
                }
                if (audioNotice.isNotBlank()) {
                    Text(audioNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = { showMeaningFirst = !showMeaningFirst }) {
                        Text("Wissel volgorde")
                    }
                    TextButton(onClick = { large = !large }) {
                        Text(if (large) "Kleiner" else "Groter")
                    }
                    TextButton(enabled = selected != null,
                        onClick = {
                            clearSpeech()
                            pendingShare = selected
                            shareNotice = ""
                        }) { Text("Deel…") }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        enabled = selectedIndex > 0,
                        onClick = { selectedIndex-- }
                    ) { Text("← Vorige") }
                    Text((if (selected == null) 0 else selectedIndex + 1).toString() +
                        " / " + cards.size,
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        enabled = selectedIndex < cards.lastIndex,
                        onClick = { selectedIndex++ }
                    ) { Text("Volgende →") }
                }
            }
        }
    }
    val request = pendingShare
    if (request != null) {
        AlertDialog(
            onDismissRequest = { pendingShare = null },
            title = { Text("Mijn eigen zin extern delen?") },
            text = {
                Text("Je deelt een door jou geschreven zin via een externe " +
                    "Android-app. Die app kan de tekst bewaren, uploaden of met " +
                    "anderen delen. Tuddy kan dit daarna niet terugdraaien. " +
                    "De eigen betekenis is NIET door Tuddy geverifieerd. " +
                    "Kies bewust wat je wilt versturen.")
            },
            confirmButton = {
                Column {
                    TextButton(onClick = { confirmShare(false) }) {
                        Text("Deel alleen mijn zin")
                    }
                    TextButton(onClick = { confirmShare(true) }) {
                        Text("Deel zin en eigen betekenis")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingShare = null }) {
                    Text("Annuleren")
                }
            }
        )
    }
}
