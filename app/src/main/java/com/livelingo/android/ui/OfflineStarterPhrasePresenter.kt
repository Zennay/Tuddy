package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
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
import com.livelingo.android.model.SafeCommunityPhrasebook

/**
 * Follow the visible built-in phrase only when local Android TTS reports
 * a text range. No word timing is inferred from audio duration.
 */
@Composable
private fun StarterPhraseFollowText(
    text: String,
    side: String,
    activeSpeechSide: String?,
    activeSpeechText: String?,
    activeSpeechRange: Pair<Int, Int>?
): AnnotatedString {
    val normalized = text.replace('\n', ' ').replace('\r', ' ').trim()
    val followsSpeech = activeSpeechSide == side &&
        activeSpeechText != null && normalized == activeSpeechText
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
 * Large, explicitly labelled offline display of the FIVE bundled beginner
 * entries, intended for showing a phrase to someone next to the phone.
 *
 * Uses only the already bundled exact-match phrasebook. No ASR, ML Kit,
 * microphone, network, clipboard, share intent or storage.
 * Optional pronunciation reuses only an already installed non-network voice.
 */
@Composable
internal fun OfflineStarterPhrasePresenter(
    phrases: List<Pair<String, String>>,
    initialIndex: Int,
    initialSource: String,
    target: String,
    onDismiss: () -> Unit
) {
    val label = when (target) {
        "kmr" -> "Kurmanji"
        "ckb" -> "Sorani"
        "ps" -> "Pasjtoe"
        else -> target.uppercase()
    }
    var index by remember(initialIndex, phrases) {
        mutableIntStateOf(initialIndex.coerceIn(0, (phrases.size - 1).coerceAtLeast(0)))
    }
    var source by remember(initialSource) {
        mutableStateOf(if (initialSource == "en") "en" else "nl")
    }
    var targetFirst by remember { mutableStateOf(true) }
    var larger by remember { mutableStateOf(true) }
    var audioNotice by remember { mutableStateOf("") }
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
    LaunchedEffect(index, source, target) {
        // Never pronounce a word from a previous card or language selection.
        speaker.stop()
        activeSpeechSide = null
        activeSpeechText = null
        activeSpeechRange = null
        audioNotice = ""
    }
    fun pronounce(text: String, language: String, side: String, pace: LocalSpeechPace) {
        val safe = text.replace('\n', ' ').replace('\r', ' ').trim()
        if (safe.isBlank() || safe.length > 400 ||
            safe.any { it.isISOControl() } || language.isBlank()
        ) {
            speaker.stop()
            activeSpeechSide = null
            activeSpeechText = null
            activeSpeechRange = null
            audioNotice = "Deze voorbeeldtekst kan niet veilig met een lokale stem worden voorgelezen."
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
                audioNotice = if (side == "source")
                    "Nu voorgelezen: bronwoord." else "Nu voorgelezen: doelwoord."
            },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }
    fun clearSpeech() {
        speaker.stop()
        activeSpeechSide = null
        activeSpeechText = null
        activeSpeechRange = null
    }
    val item = phrases.getOrNull(index)
    val original = item?.let { if (source == "nl") it.first else it.second }.orEmpty()
    val result = if (item == null) null else
        SafeCommunityPhrasebook.translate(source, target, original).getOrNull()
    val isRtl = target == "ckb" || target == "ps"

    Dialog(onDismissRequest = { clearSpeech(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.86f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("5 basiswoorden · $label",
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { clearSpeech(); onDismiss() }) { Text("Sluiten") }
                }
                Text(
                    "Vijf ingebouwde voorbeeldwoorden, niet onafhankelijk door " +
                        "moedertaalsprekers geverifieerd. Geen vrije vertaling, " +
                        "automatische spraak of audio-synchronisatie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { source = if (source == "nl") "en" else "nl" }) {
                        Text(if (source == "nl") "Nederlands ⇄" else "English ⇄")
                    }
                    TextButton(onClick = { targetFirst = !targetFirst }) {
                        Text("Wissel volgorde")
                    }
                    TextButton(onClick = { larger = !larger }) {
                        Text(if (larger) "Kleiner" else "Groter")
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (item == null) {
                        Text("Geen voorbeeldkaart beschikbaar.")
                    } else {
                        @Composable
                        fun sourceSide() {
                            Text(
                                if (source == "nl") "Bronwoord · Nederlands" else "Source word · English",
                                style = MaterialTheme.typography.labelLarge
                            )
                            val active = activeSpeechSide == "source" &&
                                activeSpeechText != null &&
                                original.replace('\n', ' ').replace('\r', ' ').trim() == activeSpeechText
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (active) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        StarterPhraseFollowText(
                                            original, "source", activeSpeechSide,
                                            activeSpeechText, activeSpeechRange
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = if (larger) 35.sp else 25.sp,
                                            lineHeight = if (larger) 44.sp else 34.sp,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        color = if (active)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (active) Text(
                                        if (activeSpeechRange != null)
                                            "Nu voorgelezen · indicatieve tekstmarkering van de stem."
                                        else "Nu voorgelezen · deze stem geeft geen woordbereik terug.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        @Composable
                        fun targetSide() {
                            Text("Ingebouwd voorbeeld · $label",
                                style = MaterialTheme.typography.labelLarge)
                            val targetText = result ?: "Geen bekende lokale invoer; niets geraden."
                            val active = activeSpeechSide == "target" &&
                                activeSpeechText != null &&
                                targetText.replace('\n', ' ').replace('\r', ' ').trim() == activeSpeechText
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (active) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        StarterPhraseFollowText(
                                            targetText, "target", activeSpeechSide,
                                            activeSpeechText, activeSpeechRange
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = if (larger) 47.sp else 32.sp,
                                            lineHeight = if (larger) 58.sp else 41.sp,
                                            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.primary
                                    )
                                    if (active) Text(
                                        if (activeSpeechRange != null)
                                            "Nu voorgelezen · indicatieve tekstmarkering van de stem."
                                        else "Nu voorgelezen · deze stem geeft geen woordbereik terug.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        if (targetFirst) {
                            targetSide()
                            HorizontalDivider()
                            sourceSide()
                        } else {
                            sourceSide()
                            HorizontalDivider()
                            targetSide()
                        }
                        Text(
                            "Controleer belangrijke communicatie met een menselijke tolk. " +
                                "Dit voorbeeld is geen volledig offline taalmodel.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (item != null && result != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                enabled = activeSpeechText == null && original.isNotBlank() &&
                                    original.length <= 400,
                                modifier = Modifier.weight(1f),
                                onClick = { pronounce(original, source, "source", LocalSpeechPace.NORMAL) }
                            ) { Text("🔊 Bronwoord") }
                            TextButton(
                                enabled = activeSpeechText == null && result.isNotBlank() &&
                                    result.length <= 400,
                                modifier = Modifier.weight(1f),
                                onClick = { pronounce(result, target, "target", LocalSpeechPace.NORMAL) }
                            ) { Text("🔊 Doelwoord") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                enabled = activeSpeechText == null && result.isNotBlank() &&
                                    result.length <= 400,
                                modifier = Modifier.weight(1f),
                                onClick = { pronounce(result, target, "target", LocalSpeechPace.SLOW) }
                            ) { Text("Rustig doelwoord") }
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
                Text("Voorlezen alleen na jouw tik met een reeds geïnstalleerde " +
                    "als lokaal gemelde Android-stem. Er wordt geen stem gedownload.",
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        enabled = index > 0,
                        onClick = { index-- }
                    ) { Text("← Vorige") }
                    Text("${if (item == null) 0 else index + 1} / ${phrases.size}",
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        enabled = index < phrases.lastIndex,
                        onClick = { index++ }
                    ) { Text("Volgende →") }
                }
            }
        }
    }
}
