package com.livelingo.android.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleAudioAlignment
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * User-selected audio + user-selected SRT/VTT timecodes. This is a private
 * playback aid, NOT ASR, word-level timing or a verified synchronized track.
 *
 * No audio bytes are persisted or transmitted by Tuddy. A user-selected SAF
 * document provider may itself download/synchronize the media. Playback is
 * paused when Android backgrounds the activity and released on dialog exit.
 */
@Composable
internal fun LocalSubtitleAudioPreviewDialog(
    cues: List<PrivateSubtitleCue>,
    audioUri: Uri,
    onChangeAudio: () -> Unit,
    onApplyAdjustedCues: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var dragPositionMs by remember { mutableLongStateOf(-1L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    // Playback-only calibration. A positive offset shows subtitles later than source timecodes.
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var loopCueIndex by remember { mutableIntStateOf(-1) }
    var rangeStartIndex by remember { mutableIntStateOf(-1) }
    var rangeEndIndex by remember { mutableIntStateOf(-1) }
    var rangeRepeating by remember { mutableStateOf(false) }
    var lastLoopAtMs by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf("Kies zelf audio bij deze ondertitels.") }
    var cuePickerOpen by remember { mutableStateOf(false) }
    var confirmApplyOpen by remember { mutableStateOf(false) }
    var cueQuery by remember { mutableStateOf("") }

    fun closeAudio() {
        val old = player
        player = null
        prepared = false
        playing = false
        positionMs = 0L
        dragPositionMs = -1L
        durationMs = 0L
        playbackSpeed = 1f
        loopCueIndex = -1
        rangeStartIndex = -1
        rangeEndIndex = -1
        rangeRepeating = false
        subtitleOffsetMs = 0L
        cuePickerOpen = false
        confirmApplyOpen = false
        cueQuery = ""
        lastLoopAtMs = 0L
        if (old != null) runCatching {
            old.setOnPreparedListener(null)
            old.setOnErrorListener(null)
            old.setOnCompletionListener(null)
            old.release()
        }
    }

    fun pauseAudio() {
        player?.let { current ->
            runCatching {
                if (current.isPlaying) current.pause()
                positionMs = current.currentPosition.toLong().coerceAtLeast(0L)
            }
        }
        playing = false
    }

    fun cueStart(cue: PrivateSubtitleCue) = cue.startMillis + subtitleOffsetMs
    fun cueEnd(cue: PrivateSubtitleCue) = cue.endMillis + subtitleOffsetMs
    fun cuePlayable(cue: PrivateSubtitleCue) =
        cueStart(cue) >= 0L && cueStart(cue) < durationMs && cueEnd(cue) <= durationMs

    // A/B repetition uses existing user-authored cue intervals, never inferred speech beats.
    fun rangeBounds(): Pair<Long, Long>? {
        if (rangeStartIndex < 0 || rangeEndIndex < rangeStartIndex) return null
        val first = cues.getOrNull(rangeStartIndex) ?: return null
        val last = cues.getOrNull(rangeEndIndex) ?: return null
        val start = cueStart(first)
        val end = cueEnd(last)
        if (start < 0L || start >= durationMs || end > durationMs ||
            end - start < 750L) return null
        return start to end
    }

    fun currentRepeatBounds(): Pair<Long, Long>? {
        if (rangeRepeating) return rangeBounds()
        val single = cues.getOrNull(loopCueIndex) ?: return null
        if (!cuePlayable(single) || single.endMillis - single.startMillis < 750L)
            return null
        return cueStart(single) to cueEnd(single)
    }

    fun shiftSubtitles(deltaMs: Long) {
        val shifted = (subtitleOffsetMs + deltaMs).coerceIn(-30_000L, 30_000L)
        if (shifted == subtitleOffsetMs) return
        subtitleOffsetMs = shifted
        loopCueIndex = -1
        rangeStartIndex = -1
        rangeEndIndex = -1
        rangeRepeating = false
        confirmApplyOpen = false
        lastLoopAtMs = 0L
        notice = "Verschuiving aangepast; dit wijzigt je SRT/VTT-bestand niet."
    }

    fun seekTo(position: Long) {
        val current = player ?: return
        if (!prepared || durationMs <= 0L) return
        val target = position.coerceIn(0L, durationMs)
        val repeat = currentRepeatBounds()
        if (repeat != null && target !in repeat.first until repeat.second) {
            loopCueIndex = -1
            rangeRepeating = false
        }
        runCatching { current.seekTo(target.toInt()) }
            .onSuccess { positionMs = target }
            .onFailure { notice = "Naar deze tijd springen is niet gelukt." }
    }

    fun changeSpeed(value: Float) {
        if (!prepared || value !in listOf(0.75f, 1f, 1.25f)) return
        val current = player ?: return
        if (playing) {
            val applied = runCatching {
                current.playbackParams = PlaybackParams().setSpeed(value).setPitch(1f)
            }.isSuccess
            if (!applied) {
                notice = "Snelheid wijzigen wordt niet ondersteund door deze audiospeler."
                return
            }
        }
        playbackSpeed = value
    }

    // MainActivity owns the SAF picker. Android may stop the Activity while
    // DocumentsUI is open; this dialog is created only AFTER picker returns.
    LaunchedEffect(audioUri) {
        closeAudio()
        if (audioUri.scheme != "content") {
            notice = "Alleen een zelfgekozen Android-document is toegestaan."
        } else {
                val next = MediaPlayer()
                runCatching {
                    next.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    next.setOnPreparedListener { ready ->
                        if (player === ready) {
                            // The scrubber uses Int milliseconds. Reject excessively
                            // long or unknown tracks rather than overflowing/guessing.
                            val duration = ready.duration.toLong()
                            if (duration !in 1L..21_600_000L) {
                                notice = "Audio heeft geen bruikbare duur (maximaal 6 uur)."
                                closeAudio()
                            } else {
                                durationMs = duration
                                prepared = true
                                notice = "Audio geladen. Tik op Afspelen; tijdcodes zijn van je eigen SRT/VTT."
                            }
                        }
                    }
                    next.setOnCompletionListener { finished ->
                        if (player === finished) {
                            val repeat = currentRepeatBounds()
                            val replayed = if (repeat != null) runCatching {
                                finished.seekTo(repeat.first.toInt())
                                finished.start()
                                positionMs = repeat.first
                                true
                            }.getOrDefault(false) else false
                            if (!replayed) {
                                playing = false
                                positionMs = durationMs
                            }
                        }
                    }
                    next.setOnErrorListener { failed, _, _ ->
                        if (player === failed) {
                            notice = "Dit audiobestand kan hier niet lokaal worden afgespeeld."
                            closeAudio()
                        }
                        true
                    }
                    next.setDataSource(context, audioUri)
                    player = next
                    notice = "Audio wordt geopend; afspelen start niet automatisch."
                    next.prepareAsync()
                }.onFailure {
                    if (player === next) closeAudio() else runCatching { next.release() }
                    notice = "Audio openen mislukt. Kies een ondersteund audiobestand."
                }

        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)
                pauseAudio()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            closeAudio()
        }
    }

    LaunchedEffect(player, playing, loopCueIndex, rangeRepeating,
        rangeStartIndex, rangeEndIndex, subtitleOffsetMs) {
        val current = player
        while (playing && current != null && player === current) {
            val now = runCatching { current.currentPosition.toLong() }.getOrNull()
            if (now != null) {
                positionMs = now.coerceIn(0L, durationMs)
                val repeat = currentRepeatBounds()
                val elapsed = SystemClock.elapsedRealtime()
                if (repeat != null && now >= repeat.second &&
                    elapsed - lastLoopAtMs >= 450L) {
                    val rewound = runCatching {
                        current.seekTo(repeat.first.toInt())
                        true
                    }.getOrDefault(false)
                    if (rewound) {
                        lastLoopAtMs = elapsed
                        positionMs = repeat.first
                    }
                }
            }
            delay(160L)
        }
    }

    val adjustedDraft = remember(cues, subtitleOffsetMs) {
        PrivateSubtitleAudioAlignment.preview(cues, subtitleOffsetMs)
    }
    val displayTimeMs = if (dragPositionMs >= 0L) dragPositionMs else positionMs
    val activeIndex = if (prepared) cues.indexOfLast {
        displayTimeMs >= cueStart(it) && displayTimeMs < cueEnd(it)
    } else -1
    val active = cues.getOrNull(activeIndex)
    val nextIndex = cues.indexOfFirst {
        val start = cueStart(it)
        start > positionMs + 250L && start in 0L until durationMs
    }
    val previousIndex = cues.indexOfLast {
        val start = cueStart(it)
        start < positionMs - 500L && start in 0L until durationMs
    }

    fun clock(millis: Long): String {
        val wholeSeconds = millis.coerceAtLeast(0L) / 1000L
        return String.format(Locale.ROOT, "%02d:%02d:%02d",
            wholeSeconds / 3600L, (wholeSeconds / 60L) % 60L, wholeSeconds % 60L)
    }

    Dialog(
        onDismissRequest = {
            closeAudio()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ondertitels bij eigen audio",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        closeAudio()
                        onDismiss()
                    }) { Text("Sluiten") }
                }
                Text("Kies zelf een audio-opname en een passend SRT/VTT-bestand. " +
                    "Tuddy volgt alleen de ingevulde cue-tijdcodes; geen " +
                    "spraakherkenning, automatische synchronisatie of woordtiming.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    pauseAudio()
                    closeAudio()
                    onChangeAudio()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (prepared) "Andere audio kiezen…" else "Audio openen…")
                }
                Text(notice, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (prepared) {
                    Slider(
                        value = displayTimeMs.coerceIn(0L, durationMs).toFloat(),
                        onValueChange = { dragPositionMs = it.toLong() },
                        onValueChangeFinished = {
                            if (dragPositionMs >= 0L) seekTo(dragPositionMs)
                            dragPositionMs = -1L
                        },
                        valueRange = 0f..durationMs.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${clock(displayTimeMs)} / ${clock(durationMs)}",
                        style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        OutlinedButton(onClick = { seekTo(cueStart(cues[previousIndex])) },
                            enabled = previousIndex >= 0) { Text("◀ Vorige") }
                        OutlinedButton(onClick = {
                            val current = player ?: return@OutlinedButton
                            if (playing) pauseAudio() else {
                                runCatching {
                                    current.start()
                                    playing = true
                                    if (playbackSpeed != 1f) {
                                        current.playbackParams = PlaybackParams()
                                            .setSpeed(playbackSpeed).setPitch(1f)
                                    }
                                }.onFailure {
                                    playing = runCatching { current.isPlaying }.getOrDefault(false)
                                    playbackSpeed = 1f
                                    notice = "Aangepast afspelen niet mogelijk; probeer normale snelheid."
                                }
                            }
                        }) { Text(if (playing) "Ⅱ Pauze" else "▶ Afspelen") }
                        OutlinedButton(onClick = { seekTo(cueStart(cues[nextIndex])) },
                            enabled = nextIndex >= 0) { Text("Volgende ▶") }
                    }
                    OutlinedButton(
                        onClick = {
                            pauseAudio()
                            cueQuery = ""
                            cuePickerOpen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("⌕ Zoek passage en spring naar ondertitel…") }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.75f to "Rustig 0,75×", 1f to "1×",
                            1.25f to "Vlot 1,25×").forEach { (value, label) ->
                            TextButton(onClick = { changeSpeed(value) },
                                modifier = Modifier.weight(1f)) {
                                Text((if (playbackSpeed == value) "✓ " else "") + label)
                            }
                        }
                    }
                    Text("Tijdcorrectie: " + (if (subtitleOffsetMs > 0L) "+" else "") +
                        String.format(Locale.ROOT, "%.2f", subtitleOffsetMs / 1000.0) +
                        " s · positief = ondertitels later",
                        style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(-5000L to "−5s", -1000L to "−1s", -250L to "−0,25s",
                            250L to "+0,25s", 1000L to "+1s", 5000L to "+5s")
                            .forEach { (delta, label) ->
                                TextButton(onClick = { shiftSubtitles(delta) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                            }
                    }
                    OutlinedButton(
                        onClick = { shiftSubtitles(-subtitleOffsetMs) },
                        enabled = subtitleOffsetMs != 0L,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Herstel de oorspronkelijke tijdcodes (0 s)") }
                    OutlinedButton(
                        onClick = {
                            pauseAudio()
                            confirmApplyOpen = true
                        },
                        enabled = adjustedDraft != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Neem deze correctie over in mijn tijdelijke ondertiteleditor…") }
                    if (subtitleOffsetMs != 0L && adjustedDraft == null) {
                        Text("Deze verschuiving zou een ondertitel buiten het toegestane " +
                            "tijdvak brengen. Stel eerst een kleinere correctie in.",
                            style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = loopCueIndex >= 0 || (active != null &&
                            active.endMillis - active.startMillis >= 750L &&
                            cuePlayable(active)),
                        onClick = {
                            loopCueIndex = if (loopCueIndex >= 0) -1 else activeIndex
                            rangeRepeating = false
                            lastLoopAtMs = 0L
                        }
                    ) {
                        Text(if (loopCueIndex >= 0)
                            "↻ Herhaal regel ${loopCueIndex + 1} ✓ · tik om te stoppen"
                        else "↻ Herhaal deze ondertitelregel")
                    }
                    Text("Herhalen gebruikt alleen de door jou ingevoerde tijdcodes " +
                        "(minimaal 0,75 s). Geen automatische woord- of audiouitlijning.",
                        style = MaterialTheme.typography.labelSmall)
                    HorizontalDivider()
                    Text("Oefen meerdere ondertitelregels achter elkaar: markeer eerst " +
                        "A (beginregel), daarna B (eindregel). Start vervolgens A–B-herhaling.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = activeIndex >= 0 && active != null && cuePlayable(active),
                            onClick = {
                                rangeStartIndex = activeIndex
                                rangeEndIndex = -1
                                rangeRepeating = false
                                loopCueIndex = -1
                                notice = "A ingesteld op regel " + (activeIndex + 1) +
                                    ". Kies nu een latere of dezelfde regel als B."
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (rangeStartIndex >= 0) "A ✓ regel " +
                            (rangeStartIndex + 1) else "Kies A-start") }
                        OutlinedButton(
                            enabled = rangeStartIndex >= 0 && activeIndex >= rangeStartIndex &&
                                active != null && cuePlayable(active),
                            onClick = {
                                rangeEndIndex = activeIndex
                                rangeRepeating = false
                                loopCueIndex = -1
                                notice = "B ingesteld op regel " + (activeIndex + 1) + "."
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (rangeEndIndex >= rangeStartIndex &&
                            rangeStartIndex >= 0) "B ✓ regel " +
                            (rangeEndIndex + 1) else "Kies B-einde") }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rangeRepeating || rangeBounds() != null,
                        onClick = {
                            if (rangeRepeating) {
                                rangeRepeating = false
                                notice = "A–B-herhaling gestopt."
                            } else {
                                val bounds = rangeBounds()
                                if (bounds != null) {
                                    loopCueIndex = -1
                                    lastLoopAtMs = 0L
                                    rangeRepeating = true
                                    seekTo(bounds.first)
                                    notice = "Herhaal A–B vanaf de gekozen startregel; " +
                                        "druk op Afspelen als de audio pauzeert."
                                }
                            }
                        }
                    ) { Text(if (rangeRepeating) "Stop A–B-herhaling ✓"
                        else "↻ Herhaal mijn A–B-passage") }
                    Text("A–B-herhaling is tijdelijk en bevat alleen de door jou gekozen " +
                        "ondertitelregels, geen automatische audioanalyse.",

                        style = MaterialTheme.typography.labelSmall)
                    Text("Tijdcorrectie geldt alleen voor deze audiosessie; " +
                        "de originele SRT/VTT wordt niet overschreven.",
                        style = MaterialTheme.typography.labelSmall)
                    HorizontalDivider()
                    Text(if (activeIndex >= 0)
                        "Regel ${activeIndex + 1} van ${cues.size} · " +
                            "${active?.startLabel} → ${active?.endLabel}" +
                            " · in audio ${clock(cueStart(active!!))}"
                    else "Geen ondertitel actief op deze audiopositie.",
                        style = MaterialTheme.typography.labelLarge)
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(active?.text.orEmpty().ifBlank {
                            "Gebruik Vorige/Volgende om naar een ondertitel te springen."
                        }, style = MaterialTheme.typography.headlineMedium.copy(
                            textDirection = TextDirection.ContentOrRtl
                        ))
                    }
                } else {
                    Text("Geen audio geladen. Je ondertitels blijven in de " +
                        "tijdelijke editor; niets wordt automatisch bewaard.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                Text("Privacy: Tuddy bewaart geen audiokopie en verstuurt geen " +
                    "audiobestand. De gekozen Android-bestandsprovider kan " +
                    "zelf bestanden downloaden of synchroniseren.",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (confirmApplyOpen && adjustedDraft != null && subtitleOffsetMs != 0L) {
        AlertDialog(
            onDismissRequest = { confirmApplyOpen = false },
            title = { Text("Gecorrigeerde tijdcodes als eigen concept?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alle " + adjustedDraft.size +
                        " ondertitelregels worden in een NIEUW tijdelijk editorconcept " +
                        "naar deze handmatig gekozen timing verschoven.")
                    Text("Correctie: " + (if (subtitleOffsetMs > 0L) "+" else "") +
                        String.format(Locale.ROOT, "%.2f", subtitleOffsetMs / 1000.0) +
                        " seconden.")
                    Text("Eerste cue: " + cues.first().startLabel +
                        " → " + adjustedDraft.first().startLabel)
                    Text("Laatste cue: " + cues.last().endLabel +
                        " → " + adjustedDraft.last().endLabel)
                    Text("Je originele SRT/VTT blijft ongewijzigd. Er is geen " +
                        "automatische uitlijning gemeten; pas in de editor aan en " +
                        "kies alleen zelf of je een nieuwe kopie bewaart.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val proposed = adjustedDraft
                    confirmApplyOpen = false
                    closeAudio()
                    onApplyAdjustedCues(proposed)
                }) { Text("Open tijdelijk gecorrigeerd concept in editor") }
            },
            dismissButton = {
                TextButton(onClick = { confirmApplyOpen = false }) {
                    Text("Blijf luisteren zonder aanpassing")
                }
            }
        )
    }
    if (cuePickerOpen && prepared) {
        val matchingCues = remember(cues, cueQuery) {
            val term = cueQuery.trim()
            cues.mapIndexed { index, cue -> index to cue }
                .filter { term.isBlank() ||
                    it.second.text.contains(term, ignoreCase = true) ||
                    it.second.startLabel.contains(term, ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = { cuePickerOpen = false },
            title = { Text("Zoek in mijn ondertitels") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Zoek alleen in het zelf geopende SRT/VTT-bestand. " +
                            "Spring naar een regel óf kies één regel als tijdanker: " +
                            "de geselecteerde cue begint dan op je huidige audiopositie " +
                            "(maximaal ±30 seconden verschil). Dit is handmatige correctie.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = cueQuery,
                        onValueChange = { cueQuery = it.take(100) },
                        label = { Text("Tekst of begintijd zoeken") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${matchingCues.size} van ${cues.size} regels",
                        style = MaterialTheme.typography.labelMedium)
                    if (matchingCues.isEmpty()) {
                        Text("Geen ondertitelregels gevonden; probeer een ander woord.",
                            style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(matchingCues, key = { it.first }) { (index, cue) ->
                                val start = cueStart(cue)
                                val isInAudio = start >= 0L && start < durationMs
                                TextButton(
                                    enabled = isInAudio,
                                    onClick = {
                                        loopCueIndex = -1
                                        seekTo(start)
                                        cuePickerOpen = false
                                        notice = "Naar ondertitelregel ${index + 1} gesprongen."
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("Regel ${index + 1} · ${cue.startLabel}" +
                                            if (isInAudio) " · audio ${clock(start)}"
                                            else " · buiten audio",
                                            style = MaterialTheme.typography.labelMedium)
                                        Text(cue.text, maxLines = 2,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDirection = TextDirection.ContentOrRtl
                                            ))
                                    }
                                }

                                val anchorOffset = positionMs - cue.startMillis
                                val canAnchor = anchorOffset in -30_000L..30_000L
                                OutlinedButton(
                                    enabled = canAnchor,
                                    onClick = {
                                        pauseAudio()
                                        // Neither seek the player nor mutate the original subtitle cues.
                                        shiftSubtitles(anchorOffset - subtitleOffsetMs)
                                        loopCueIndex = -1
                                        cuePickerOpen = false
                                        notice = "Ondertitelregel " + (index + 1) +
                                            " gekoppeld aan de huidige audiopositie (" +
                                            clock(positionMs) + "); alleen tijdelijke preview."
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (canAnchor) "Gebruik als tijdanker op huidige audiopositie"
                                        else "Buiten bereik van ±30 s: geen tijdanker mogelijk")
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { cuePickerOpen = false }) { Text("Terug naar audio") }
            }
        )
    }
}
