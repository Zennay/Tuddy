package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.OfflineTypedCaptionPolicy
import com.livelingo.android.TargetCatalog

/**
 * Highlights only text ranges reported by the installed local Android TTS voice.
 * The spoken string is shown verbatim after the same whitespace normalization
 * used for speech, so offsets never target the unnormalized source text.
 */
@Composable
private fun FaceToFaceSpeechText(
    text: String,
    sideLabel: String,
    activeSpeechText: String?,
    activeSpeechSide: String?,
    activeSpeechRange: Pair<Int, Int>?
): AnnotatedString {
    val displayedText = if (activeSpeechSide == sideLabel && activeSpeechText != null) {
        activeSpeechText
    } else text
    val range = activeSpeechRange.takeIf {
        activeSpeechSide == sideLabel && activeSpeechText != null
    }
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
 * One device on the table, with the far-side line rotated for the person
 * opposite the owner. The user opts in: never start the microphone, speech,
 * model installation, recording, storage, file provider or clipboard.
 *
 * Crucially, only lines with a completed local translation are admitted:
 * partial text, unavailable models and failed translations are NOT advice to
 * the other reader. History is bounded by the existing RAM conversation.
 */
@Composable
internal fun FaceToFaceConversationDialog(
    lines: List<TypedConversationLine>,
    onBack: () -> Unit,
    onCover: (() -> Unit)? = null,
    onSubmitTurn: ((String, String, String) -> Boolean)? = null,
    initialSource: String = "nl",
    initialTarget: String = "en"
) {
    val completed = lines.takeLast(80).filter {
        it.status == "Lokaal vertaald" &&
            it.original.isNotBlank() && it.translated.isNotBlank()
    }
    var followNewest by remember { mutableStateOf(true) }
    var chosenIndex by remember { mutableStateOf(0) }
    // An index is not stable: older RAM captions fall off the 80-line window.
    var chosenLineId by remember { mutableStateOf<Long?>(null) }
    var translatedOnFarSide by remember { mutableStateOf(true) }
    var largeText by remember { mutableStateOf(true) }
    // Only the deliberate two-way typed caller offers input; live is read-only.
    var input by remember { mutableStateOf("") }
    var turnFrom by remember(initialSource) { mutableStateOf(initialSource) }
    var turnTo by remember(initialTarget) { mutableStateOf(initialTarget) }
    var turnNotice by remember { mutableStateOf("") }
    // Resolve an older selection by its original session ID, not by its
    // former list position. Never silently substitute another person's text
    // when the chosen caption is evicted from the temporary RAM window.
    val anchoredIndex = chosenLineId?.takeIf { it != 0L }?.let { chosen ->
        completed.indexOfFirst { it.id == chosen }
    }
    val index = if (followNewest) completed.lastIndex
        else anchoredIndex ?: chosenIndex.coerceIn(0, completed.lastIndex.coerceAtLeast(0))
    val selectionExpired = !followNewest && anchoredIndex == -1
    val selected = completed.getOrNull(index)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    var audioNotice by remember { mutableStateOf("") }
    var activeSpeechText by remember { mutableStateOf<String?>(null) }
    var activeSpeechSide by remember { mutableStateOf<String?>(null) }
    var activeSpeechRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var slowVoice by remember { mutableStateOf(false) }
    // Hide BOTH viewers immediately without deleting the in-memory transcript.
    // Revealing requires an explicit tap; incoming lines never uncover it.
    var covered by remember { mutableStateOf(false) }
    DisposableEffect(speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                speaker.stop()
                activeSpeechText = null
                activeSpeechSide = null
                activeSpeechRange = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); speaker.close() }
    }
    LaunchedEffect(selected?.id, selected?.original, selected?.translated,
        selected?.source, selected?.target) {
        speaker.stop()
        activeSpeechText = null
        activeSpeechSide = null
        activeSpeechRange = null
        audioNotice = ""
    }
    fun pronounce(text: String?, code: String?, sideLabel: String) {
        val line = text?.replace('\n', ' ')?.replace('\r', ' ')?.trim().orEmpty()
        val languageTag = code?.takeIf { it.isNotBlank() }
        if (line.isBlank() || line.length > 400 ||
            line.any { it.isISOControl() } || languageTag == null
        ) {
            speaker.stop()
            activeSpeechText = null
            activeSpeechSide = null
            activeSpeechRange = null
            audioNotice = "Deze zin kan niet veilig met een lokale stem worden voorgelezen."
            return
        }
        activeSpeechText = line
        activeSpeechSide = sideLabel
        activeSpeechRange = null
        audioNotice = "Lokale voorlezing voorbereiden…"
        speaker.speakSequence(
            lines = listOf(line),
            languageTag = languageTag,
            pace = if (slowVoice) LocalSpeechPace.SLOW else LocalSpeechPace.NORMAL,
            onProgress = { completed, total ->
                audioNotice = "Lokaal voorlezen: $completed/$total regels."
            },
            feedback = { audioNotice = it },
            onFinished = { completed ->
                activeSpeechText = null
                activeSpeechSide = null
                activeSpeechRange = null
                if (completed) audioNotice = "Voorlezen voltooid."
            },
            onLineStarted = {
                activeSpeechRange = null
                audioNotice = "Lokaal voorlezen: $sideLabel."
            },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }
    val rtlLanguages = setOf("ar", "fa", "ur", "he", "ps", "ckb")
    fun direction(code: String): TextDirection =
        if (code.substringBefore('-') in rtlLanguages) TextDirection.Rtl
        else TextDirection.Ltr

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface) {
            if (covered) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🔒 Gesprek afgedekt",
                        style = MaterialTheme.typography.headlineMedium)
                    Text("Beide kanten zijn verborgen. Nieuwe gespreksregels " +
                        "blijven ook afgedekt. De lokale stem is gestopt.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp))
                    Button(onClick = { covered = false }) {
                        Text("Toon gesprek opnieuw")
                    }
                    TextButton(onClick = onBack) { Text("Terug") }
                }
            } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Rotate the ENTIRE far-side text block, not individual letters.
                // The phone is placed flat between two people, without rotating
                // the device or changing Android's orientation settings.
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer { rotationZ = 180f }
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val lang = if (translatedOnFarSide) selected?.target
                            else selected?.source
                        val value = if (translatedOnFarSide) selected?.translated
                            else selected?.original
                        Text("TEGENOVER · ${lang?.uppercase() ?: "—"}",
                            style = MaterialTheme.typography.labelLarge)
                        val farValue = FaceToFaceSpeechText(
                            text = value ?: if (selectionExpired)
                                "Deze oude regel is niet meer in het tijdelijke geheugen."
                            else "Nog geen afgeronde lokale vertaling.",
                            sideLabel = "Tegenover",
                            activeSpeechText = activeSpeechText,
                            activeSpeechSide = activeSpeechSide,
                            activeSpeechRange = activeSpeechRange
                        )
                        Text(farValue,
                            fontSize = if (largeText) 32.sp else 24.sp,
                            lineHeight = if (largeText) 41.sp else 32.sp,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = direction(lang ?: "nl")
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = 12.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Cover both readers before any optional external handler.
                            speaker.stop()
                            activeSpeechText = null
                            activeSpeechSide = null
                            activeSpeechRange = null
                            audioNotice = ""
                            covered = true
                            onCover?.invoke()
                        }
                    ) { Text("🔒 Beide kanten direct afdekken") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            val previous = (index - 1).coerceAtLeast(0)
                            followNewest = false
                            chosenIndex = previous
                            chosenLineId = completed.getOrNull(previous)?.id
                                ?.takeIf { it != 0L }
                        }, enabled = selected != null && index > 0) { Text("← Vorige") }
                        Text("${if (selected == null) 0 else index + 1}/${completed.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 14.dp))
                        TextButton(onClick = {
                            if (index >= completed.lastIndex - 1) {
                                followNewest = true
                                chosenLineId = null
                            } else {
                                val next = index + 1
                                followNewest = false
                                chosenIndex = next
                                chosenLineId = completed.getOrNull(next)?.id
                                    ?.takeIf { it != 0L }
                            }
                        }, enabled = selected != null && index < completed.lastIndex
                        ) { Text("Volgende →") }
                    }
                    if (selectionExpired) {
                        Text(
                            "De gekozen oudere regel is uit het tijdelijke geheugen verdwenen. " +
                                "Tik op ‘↓ Nieuwste’ om verder te lezen.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            speaker.stop()
                            activeSpeechText = null
                            activeSpeechSide = null
                            activeSpeechRange = null
                            translatedOnFarSide = !translatedOnFarSide
                        }) { Text("↕ Wissel kanten") }
                        TextButton(onClick = { largeText = !largeText }) {
                            Text(if (largeText) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(onClick = {
                            followNewest = true
                            chosenLineId = null
                        }) { Text("↓ Nieuwste") }
                        TextButton(onClick = onBack) { Text("Terug") }
                    }
                    if (selected != null) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                enabled = activeSpeechText == null &&
                                    selected.original.length <= 400 &&
                                    selected.translated.length <= 400,
                                onClick = {
                                    val code = if (translatedOnFarSide) selected.target
                                        else selected.source
                                    val value = if (translatedOnFarSide) selected.translated
                                        else selected.original
                                    pronounce(value, code, "Tegenover")
                                }
                            ) { Text("🔊 Tegenover") }
                            TextButton(
                                enabled = activeSpeechText == null &&
                                    selected.original.length <= 400 &&
                                    selected.translated.length <= 400,
                                onClick = {
                                    val code = if (translatedOnFarSide) selected.source
                                        else selected.target
                                    val value = if (translatedOnFarSide) selected.original
                                        else selected.translated
                                    pronounce(value, code, "Hier")
                                }
                            ) { Text("🔊 Hier") }
                            TextButton(onClick = {
                                speaker.stop()
                                activeSpeechText = null
                                activeSpeechSide = null
                                activeSpeechRange = null
                                slowVoice = !slowVoice
                            }) { Text(if (slowVoice) "Langzaam ✓" else "Rustig") }
                            TextButton(onClick = {
                                speaker.stop()
                                activeSpeechText = null
                                activeSpeechSide = null
                                activeSpeechRange = null
                                audioNotice = "Voorlezen gestopt."
                            }) { Text("■ Stop") }
                        }
                        if (audioNotice.isNotBlank()) {
                            Text(audioNotice, style = MaterialTheme.typography.labelSmall)
                        }
                        if (activeSpeechText != null) {
                            Text(
                                if (activeSpeechRange != null)
                                    "Indicatieve tekstmarkering van de geïnstalleerde stem."
                                else "Tekstmarkering verschijnt als de stem tekstbereiken terugmeldt.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (onSubmitTurn != null) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Typ: ${turnFrom.uppercase()} → ${turnTo.uppercase()}",
                                style = MaterialTheme.typography.labelMedium)
                            TextButton(onClick = {
                                val prior = turnFrom
                                turnFrom = turnTo
                                turnTo = prior
                                turnNotice = "Handmatig van richting gewisseld."
                            }) { Text("↔ Richting") }
                        }
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                if (it.length <= OfflineTypedCaptionPolicy.MAX_INPUT_UNITS)
                                    input = it
                            },
                            label = { Text("Typ één zin · pas na afgeronde vertaling zichtbaar") },
                            minLines = 1, maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = direction(turnFrom))
                        )
                        Button(
                            enabled = turnFrom != turnTo &&
                                turnFrom in TargetCatalog.codes &&
                                turnTo in TargetCatalog.codes &&
                                OfflineTypedCaptionPolicy.normalize(input) != null,
                            onClick = {
                                val normalized = OfflineTypedCaptionPolicy.normalize(input)
                                if (normalized != null &&
                                    onSubmitTurn(normalized, turnFrom, turnTo)) {
                                    speaker.stop()
                                    activeSpeechText = null
                                    activeSpeechSide = null
                                    activeSpeechRange = null
                                    input = ""
                                    val previous = turnFrom
                                    turnFrom = turnTo
                                    turnTo = previous
                                    followNewest = true
                                    chosenLineId = null
                                    turnNotice = "Beurt toegevoegd. Nog geen resultaat " +
                                        "tot de lokale vertaling voltooid is."
                                } else {
                                    turnNotice = "Niet toegevoegd; controleer dit via de typmodus."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Vertaal beurt lokaal · wissel taal") }
                        if (turnNotice.isNotBlank()) {
                            Text(turnNotice, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("Alleen afgeronde lokale vertalingen · controleer de betekenis. " +
                        "Geen live audio, opslag of automatische synchronisatie.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState()).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val lang = if (translatedOnFarSide) selected?.source
                            else selected?.target
                        val value = if (translatedOnFarSide) selected?.original
                            else selected?.translated
                        Text("HIER · ${lang?.uppercase() ?: "—"}",
                            style = MaterialTheme.typography.labelLarge)
                        val nearValue = FaceToFaceSpeechText(
                            text = value ?: if (selectionExpired)
                                "Tik op ‘↓ Nieuwste’ om recente regels te zien."
                            else "Kies in de typmodus twee talen en vertaal een zin " +
                                "met reeds aanwezige lokale modellen.",
                            sideLabel = "Hier",
                            activeSpeechText = activeSpeechText,
                            activeSpeechSide = activeSpeechSide,
                            activeSpeechRange = activeSpeechRange
                        )
                        Text(nearValue,
                            fontSize = if (largeText) 32.sp else 24.sp,
                            lineHeight = if (largeText) 41.sp else 32.sp,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = direction(lang ?: "nl")
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    }
                }
            }
            }
        }
    }
}
