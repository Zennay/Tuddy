package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

/** Read-only, ephemeral card for finished live captions. No network or export. */
private enum class CaptionCardSide { BOTH, ORIGINAL, TRANSLATION }

@Composable
private fun CaptionSide(text: String, code: String, large: Boolean,
    highlightedWord: Int? = null) {
    val fallbackRtl = code.substringBefore('-') in setOf("ar", "fa", "ur", "he", "ps", "ckb")
    val wordRanges = remember(text) { Regex("\\S+").findAll(text).map { it.range }.toList() }
    val selected = highlightedWord?.let { wordRanges.getOrNull(it) }
    val emphasis = MaterialTheme.colorScheme.primary
    val displayed = buildAnnotatedString {
        append(text)
        if (selected != null) {
            addStyle(
                SpanStyle(color = emphasis, background = emphasis.copy(alpha = 0.16f),
                    fontWeight = FontWeight.Bold),
                selected.first, selected.last + 1
            )
        }
    }
    Text(
        displayed,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = if (large) 38.sp else 26.sp,
            lineHeight = if (large) 48.sp else 36.sp,
            fontWeight = FontWeight.Medium,
            textDirection = PrivateTextDirection.forContent(text, fallbackRtl)
        )
    )
}

/**
 * Shows a live, explicitly user-triggered TTS cue. Text ranges are indicative
 * callbacks from the installed Android voice, never claimed as exact timing.
 */
@Composable
private fun CaptionAudioFollowCard(
    text: String,
    label: String,
    languageCode: String,
    range: Pair<Int, Int>?
) {
    val background = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onPrimary
    val displayed = remember(text, range, background, foreground) {
        buildAnnotatedString {
            append(text)
            if (range != null && range.first >= 0 &&
                range.second > range.first && range.second <= text.length
            ) {
                addStyle(
                    SpanStyle(
                        color = foreground,
                        background = background,
                        fontWeight = FontWeight.Bold
                    ),
                    range.first,
                    range.second
                )
            }
        }
    }
    val fallbackRtl = languageCode.substringBefore('-') in
        setOf("ar", "fa", "ur", "he", "ps", "ckb")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Nu voorgelezen · $label",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                displayed,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = PrivateTextDirection.forContent(text, fallbackRtl)
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                if (range != null)
                    "Indicatieve tekstmarkering van de geïnstalleerde stem."
                else "Tekstmarkering verschijnt als de stem tekstbereiken terugmeldt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Shows only captions with locally finished text, as supplied by the caller.
 * A sentence can be shown across a table without Android share/copy or
 * persistence. Translation quality is not verified or implied.
 */
@Composable
internal fun FullscreenCaptionCard(
    captions: List<FocusCaptionLine>,
    initialId: Long,
    onDismiss: () -> Unit,
    onStudyCaption: ((Long) -> Unit)? = null
) {
    val finished = captions.filter {
        (it.status == "Lokaal" || it.status == "Lokaal vertaald") &&
            it.original.isNotBlank()
    }
    var index by remember(initialId) {
        mutableIntStateOf(finished.indexOfFirst { it.id == initialId }.coerceAtLeast(0))
    }
    var side by remember { mutableStateOf(CaptionCardSide.BOTH) }
    var large by remember { mutableStateOf(true) }
    var audioNotice by remember { mutableStateOf("") }
    var activeSpeechText by remember { mutableStateOf<String?>(null) }
    var activeSpeechLabel by remember { mutableStateOf("") }
    var activeSpeechLanguage by remember { mutableStateOf("") }
    var activeSpeechRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val context = LocalContext.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
                activeSpeechText = null
                activeSpeechLabel = ""
                activeSpeechLanguage = ""
                activeSpeechRange = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    val safeIndex = index.coerceIn(0, (finished.size - 1).coerceAtLeast(0))
    val card = finished.getOrNull(safeIndex)
    val translation = card?.translated?.takeIf { it.isNotBlank() && it != card.original }
    // Intentionally manual, never linked to recognizer/audio timing.
    var wordStepper by remember(card?.id) { mutableStateOf(false) }
    var stepTranslation by remember(card?.id) { mutableStateOf(false) }
    var currentWord by remember(card?.id) { mutableIntStateOf(0) }
    val studyText = if (stepTranslation && translation != null) translation
        else card?.original.orEmpty()
    val wordCount = remember(studyText) { Regex("\\S+").findAll(studyText).count() }
    val safeWordIndex = currentWord.coerceIn(0, (wordCount - 1).coerceAtLeast(0))
    val selectedTextWord = remember(studyText, safeWordIndex) {
        Regex("\\S+").findAll(studyText).elementAtOrNull(safeWordIndex)?.value ?: ""
    }

    fun pronounceCaption(text: String, languageTag: String, label: String) {
        val safe = text.replace('\n', ' ').replace('\r', ' ').trim()
        if (safe.isBlank() || safe.length > 400 || safe.any { it.isISOControl() }) {
            speaker.stop()
            activeSpeechText = null
            activeSpeechLabel = ""
            activeSpeechLanguage = ""
            activeSpeechRange = null
            audioNotice = "Alleen geselecteerde tekst tot 400 tekens zonder controletekens kan lokaal worden voorgelezen."
            return
        }
        activeSpeechText = safe
        activeSpeechLabel = label
        activeSpeechLanguage = languageTag
        activeSpeechRange = null
        audioNotice = "Lokale voorlezing voorbereiden…"
        speaker.speakSequence(
            lines = listOf(safe),
            languageTag = languageTag,
            pace = LocalSpeechPace.NORMAL,
            onProgress = { completed, total ->
                audioNotice = "Lokaal voorlezen: $completed/$total regels."
            },
            feedback = { audioNotice = it },
            onFinished = { completed ->
                activeSpeechText = null
                activeSpeechLabel = ""
                activeSpeechLanguage = ""
                activeSpeechRange = null
                if (completed) audioNotice = "Lokaal voorlezen voltooid."
            },
            onLineStarted = {
                activeSpeechRange = null
                audioNotice = "Lokaal voorlezen: $label."
            },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }

    // Manual position and voice controls are independent of the microphone:
    // changing the caption, text side, or selected word cancels old speech.
    LaunchedEffect(card?.id, side, wordStepper, stepTranslation, safeWordIndex) {
        speaker.stop()
        activeSpeechText = null
        activeSpeechLabel = ""
        activeSpeechLanguage = ""
        activeSpeechRange = null
        audioNotice = ""
    }

    Dialog(
        onDismissRequest = { speaker.stop(); onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tuddy · Toon gesprekszin",
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { speaker.stop(); onDismiss() }) { Text("Sluiten") }
                }
                Text(
                    "Alleen deze tijdelijke sessie · geen kopie of verzending. " +
                        "Een lokale vertaling kan fouten bevatten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                if (card == null) {
                    Text("Geen afgeronde gesprekszin beschikbaar.")
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(onClick = { side = CaptionCardSide.BOTH }) {
                            Text(if (side == CaptionCardSide.BOTH) "Beide ✓" else "Beide")
                        }
                        TextButton(onClick = { side = CaptionCardSide.ORIGINAL }) {
                            Text(if (side == CaptionCardSide.ORIGINAL)
                                "Origineel ✓" else "Origineel")
                        }
                        TextButton(
                            enabled = translation != null,
                            onClick = { side = CaptionCardSide.TRANSLATION }
                        ) {
                            Text(if (side == CaptionCardSide.TRANSLATION)
                                "Vertaling ✓" else "Vertaling")
                        }
                    }
                    TextButton(onClick = { large = !large }) {
                        Text(if (large) "Kleinere letters" else "Grotere letters")
                    }
                    if (onStudyCaption != null && card.status == "Lokaal vertaald" &&
                        translation != null && card.original != translation &&
                        card.original.length <= 400 && translation.length <= 400) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                speaker.stop()
                                activeSpeechText = null
                                activeSpeechLabel = ""
                                activeSpeechLanguage = ""
                                activeSpeechRange = null
                                onStudyCaption(card.id)
                            }
                        ) { Text("🧠 Oefen vanaf deze zin · alleen tijdelijk") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            enabled = activeSpeechText == null &&
                                card.original.isNotBlank() && card.original.length <= 400,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pronounceCaption(card.original, card.sourceCode, "Origineel")
                            }
                        ) { Text("🔊 Originele zin") }
                        TextButton(
                            enabled = activeSpeechText == null &&
                                card.status == "Lokaal vertaald" &&
                                translation != null && translation.length <= 400,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pronounceCaption(translation.orEmpty(), card.targetCode, "Vertaling")
                            }
                        ) { Text("🔊 Vertaling") }
                    }
                    activeSpeechText?.let { text ->
                        CaptionAudioFollowCard(
                            text = text,
                            label = activeSpeechLabel,
                            languageCode = activeSpeechLanguage,
                            range = activeSpeechRange
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                wordStepper = !wordStepper
                                currentWord = 0
                                if (wordStepper) {
                                    stepTranslation = false
                                    side = CaptionCardSide.ORIGINAL
                                }
                            }
                        ) {
                            Text(if (wordStepper) "Stop woordstappen" else "▸ Lees woord voor woord")
                        }
                        if (wordStepper) {
                            Text("Handmatig tekstwoord benadrukt, GEEN audio-woordtiming " +
                                "of woord-voor-woord vertaaluitlijning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = {
                                    stepTranslation = false
                                    currentWord = 0
                                    side = CaptionCardSide.ORIGINAL
                                }) { Text(if (!stepTranslation) "Origineel ✓" else "Origineel") }
                                TextButton(
                                    enabled = translation != null,
                                    onClick = {
                                        stepTranslation = true
                                        currentWord = 0
                                        side = CaptionCardSide.TRANSLATION
                                    }
                                ) { Text(if (stepTranslation) "Vertaling ✓" else "Vertaling") }
                            }
                            Text(
                                "Geselecteerd tekstwoord: " +
                                    (selectedTextWord.ifBlank { "geen" }) +
                                    " · handmatige positie, geen audiotiming",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    enabled = activeSpeechText == null && selectedTextWord.isNotBlank() &&
                                        (!stepTranslation ||
                                            card.status == "Lokaal vertaald"),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        speaker.speak(selectedTextWord,
                                            if (stepTranslation) card.targetCode
                                            else card.sourceCode,
                                            LocalSpeechPace.NORMAL) { audioNotice = it }
                                    }
                                ) { Text("🔊 Dit woord") }
                                TextButton(
                                    enabled = activeSpeechText == null && selectedTextWord.isNotBlank() &&
                                        (!stepTranslation ||
                                            card.status == "Lokaal vertaald"),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        speaker.speak(selectedTextWord,
                                            if (stepTranslation) card.targetCode
                                            else card.sourceCode,
                                            LocalSpeechPace.SLOW) { audioNotice = it }
                                    }
                                ) { Text("Rustig woord") }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    enabled = safeWordIndex > 0,
                                    onClick = { currentWord = (safeWordIndex - 1).coerceAtLeast(0) }
                                ) { Text("← Woord") }
                                Text(
                                    "${if (wordCount == 0) 0 else safeWordIndex + 1} / $wordCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    enabled = wordCount > 0 && safeWordIndex < wordCount - 1,
                                    onClick = { currentWord = safeWordIndex + 1 }
                                ) { Text("Woord →") }
                            }
                        }
                    }
                    TextButton(onClick = {
                        speaker.stop()
                        activeSpeechText = null
                        activeSpeechLabel = ""
                        activeSpeechLanguage = ""
                        activeSpeechRange = null
                        audioNotice = "Lokale voorlezing gestopt."
                    }) { Text("■ Stop voorlezen") }
                    if (audioNotice.isNotBlank()) {
                        Text(audioNotice, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Uitspraak alleen na tik met lokaal gemelde geïnstalleerde " +
                            "Android-stem; geen stemdownload of audio-woordtiming.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        if (side != CaptionCardSide.TRANSLATION || translation == null) {
                            Text("Origineel · " + card.sourceCode.uppercase(),
                                style = MaterialTheme.typography.labelLarge)
                            CaptionSide(card.original, card.sourceCode, large,
                                if (wordStepper && !stepTranslation) safeWordIndex else null)
                        }
                        if (side != CaptionCardSide.ORIGINAL) {
                            HorizontalDivider()
                            if (translation != null) {
                                Text("Lokaal vertaald · " + card.targetCode.uppercase(),
                                    style = MaterialTheme.typography.labelLarge)
                                CaptionSide(translation, card.targetCode, large,
                                    if (wordStepper && stepTranslation) safeWordIndex else null)
                            } else {
                                Text("Voor deze regel is geen afzonderlijke vertaling beschikbaar.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        card.personalMeaning?.let { own ->
                            HorizontalDivider()
                            Text("Mijn eigen uitleg · niet geverifieerd, niet gedeeld",
                                style = MaterialTheme.typography.labelLarge)
                            CaptionSide(own, card.targetCode, large)
                        }
                        if (card.speaker == "A" || card.speaker == "B") {
                            Text("Spreker " + card.speaker + " · handmatig gelabeld",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(enabled = safeIndex > 0, onClick = {
                        index = (safeIndex - 1).coerceAtLeast(0)
                        side = CaptionCardSide.BOTH
                    }) { Text("← Vorige") }
                    Text((if (card == null) 0 else safeIndex + 1).toString() +
                        " / " + finished.size,
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        enabled = safeIndex < finished.lastIndex,
                        onClick = {
                            index = safeIndex + 1
                            side = CaptionCardSide.BOTH
                        }
                    ) { Text("Volgende →") }
                }
            }
        }
    }
}
