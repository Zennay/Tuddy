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
import com.livelingo.android.PrivateSubtitleClip
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
    var rangeToolsOpen by remember { mutableStateOf(false) }
    // Up to eight user-tapped audio moments. No cue text, audio bytes or storage.
    var audioBookmarks by remember { mutableStateOf<List<Long>>(emptyList()) }
    // Four reusable A–B practice cards within this private player session.
    var savedPracticePassages by remember {
        mutableStateOf<List<Pair<Long, Long>>>(emptyList())
    }
    var bookmarkToolsOpen by remember { mutableStateOf(false) }
    var bookmarkStartMs by remember { mutableLongStateOf(-1L) }
    var bookmarkEndMs by remember { mutableLongStateOf(-1L) }
    var bookmarkRepeating by remember { mutableStateOf(false) }
    var lastLoopAtMs by remember { mutableLongStateOf(0L) }
    // Zero preserves the existing open-ended repeat. Finite practice is
    // counted in actually completed, not merely selected, cue/audio passes.
    var repeatPassLimit by remember { mutableIntStateOf(0) }
    var completedPasses by remember { mutableIntStateOf(0) }
    // Guided queue is a frozen copy: later marker edits cannot retarget a
    // playing fragment. Nothing persists beyond this user-opened dialog.
    var guidedActive by remember { mutableStateOf(false) }
    var guidedQueue by remember { mutableStateOf<List<Pair<Long, Long>>>(emptyList()) }
    var guidedIndex by remember { mutableIntStateOf(0) }
    var guidedRounds by remember { mutableIntStateOf(1) }
    var guidedPasses by remember { mutableIntStateOf(0) }
    // Human judgement only: no speech recognition or pronunciation scoring.
    var passageRatings by remember {
        mutableStateOf<Map<Pair<Long, Long>, String>>(emptyMap())
    }
    var guidedNeedsOnly by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("Kies zelf audio bij deze ondertitels.") }
    var cuePickerOpen by remember { mutableStateOf(false) }
    var confirmApplyOpen by remember { mutableStateOf(false) }
    var confirmClipOpen by remember { mutableStateOf(false) }
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
        rangeToolsOpen = false
        audioBookmarks = emptyList()
        savedPracticePassages = emptyList()
        guidedActive = false
        guidedQueue = emptyList()
        guidedIndex = 0
        guidedPasses = 0
        passageRatings = emptyMap()
        guidedNeedsOnly = false
        bookmarkToolsOpen = false
        bookmarkStartMs = -1L
        bookmarkEndMs = -1L
        bookmarkRepeating = false
        subtitleOffsetMs = 0L
        cuePickerOpen = false
        confirmApplyOpen = false
        confirmClipOpen = false
        cueQuery = ""
        lastLoopAtMs = 0L
        completedPasses = 0
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
        // No silently truncated line may be part of an A–B exercise.
        // Bound practice passages to five minutes so accidental far-apart
        // anchors cannot create a huge surprising repeat interval.
        if (start < 0L || start >= durationMs || end > durationMs ||
            end - start !in 750L..300_000L ||
            cues.subList(rangeStartIndex, rangeEndIndex + 1).any { cue ->
                !cuePlayable(cue) || cueStart(cue) < start || cueEnd(cue) > end
            }) return null
        return start to end
    }

    fun bookmarkBounds(): Pair<Long, Long>? {
        val start = bookmarkStartMs
        val end = bookmarkEndMs
        if (start !in 0L until durationMs || end > durationMs ||
            end - start !in 750L..300_000L ||
            start !in audioBookmarks || end !in audioBookmarks) return null
        return start to end
    }


    // Reuse the existing bounded personal audio A/B deck from an exact SRT/VTT
    // cue. Nothing is inferred from speech, written to disk or sent to a server.
    fun saveCueForPractice(index: Int) {
        if (!prepared || guidedActive) {
            notice = "Stop eerst de oefenroute en open zelf een bruikbaar audiobestand."
            return
        }
        val cue = cues.getOrNull(index) ?: return
        val start = cueStart(cue)
        val end = cueEnd(cue)
        val bounds = start to end
        if (start < 0L || end > durationMs || end - start !in 750L..300_000L) {
            notice = "Deze ondertitel past niet volledig in je audio of duurt buiten 0,75 s–5 min."
            return
        }
        if (bounds in savedPracticePassages) {
            bookmarkToolsOpen = true
            notice = "Deze exacte ondertitel staat al in je oefenroute."
            return
        }
        if (savedPracticePassages.size >= 4) {
            bookmarkToolsOpen = true
            notice = "De oefenroute is vol (4/4). Verwijder eerst bewust een fragment."
            return
        }
        val missing = listOf(start, end).distinct().filterNot { it in audioBookmarks }
        if (audioBookmarks.size + missing.size > 8) {
            bookmarkToolsOpen = true
            notice = "Te weinig ruimte voor twee audiomarkeringen (maximaal 8). Wis eerst zelf markeringen."
            return
        }
        if (missing.any { mark ->
                (audioBookmarks + missing).any { other ->
                    other != mark && kotlin.math.abs(other - mark) < 500L
                }
            }) {
            bookmarkToolsOpen = true
            notice = "Een bestaand audiomoment ligt binnen 0,5 s van deze regelgrens. " +
                "Pas de markeringen zelf aan om een onduidelijk fragment te vermijden."
            return
        }
        audioBookmarks = (audioBookmarks + missing).distinct().sorted()
        savedPracticePassages = savedPracticePassages + bounds
        bookmarkToolsOpen = true
        notice = "Ondertitelregel ${index + 1} als eigen oefenfragment " +
            "(${savedPracticePassages.size}/4) opgeslagen; start de oefenroute zelf."
    }

    fun currentRepeatBounds(): Pair<Long, Long>? {
        if (guidedActive) return guidedQueue.getOrNull(guidedIndex)
        if (bookmarkRepeating) return bookmarkBounds()
        if (rangeRepeating) return rangeBounds()
        val single = cues.getOrNull(loopCueIndex) ?: return null
        if (!cuePlayable(single) || single.endMillis - single.startMillis < 750L)
            return null
        return cueStart(single) to cueEnd(single)
    }

    fun advanceGuidedPass(): Pair<Long, Long>? {
        if (!guidedActive) return null
        val current = guidedQueue.getOrNull(guidedIndex)
        if (current == null) {
            guidedActive = false
            pauseAudio()
            notice = "Oefenroute gestopt: fragment niet meer beschikbaar."
            return null
        }
        guidedPasses += 1
        if (guidedPasses < guidedRounds) return current
        if (guidedIndex + 1 < guidedQueue.size) {
            guidedIndex += 1
            guidedPasses = 0
            notice = "Oefenroute: fragment ${guidedIndex + 1} van ${guidedQueue.size}."
            return guidedQueue[guidedIndex]
        }
        guidedActive = false
        pauseAudio()
        notice = "Begeleide oefenroute voltooid: ${guidedQueue.size} fragmenten " +
            "${guidedRounds}× gehoord. Audio is gepauzeerd."
        return null
    }

    fun finishRepeatPass(): Boolean {
        completedPasses += 1
        if (repeatPassLimit == 0 || completedPasses < repeatPassLimit) return false
        loopCueIndex = -1
        rangeRepeating = false
        bookmarkRepeating = false
        pauseAudio()
        notice = "Oefening klaar: ${completedPasses} volledige " +
            (if (completedPasses == 1) "herhaling" else "herhalingen") +
            ". Kies opnieuw een fragment om verder te oefenen."
        return true
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
        confirmClipOpen = false
        lastLoopAtMs = 0L
        completedPasses = 0
        notice = "Verschuiving aangepast; dit wijzigt je SRT/VTT-bestand niet."
    }

    fun seekTo(position: Long) {
        val current = player ?: return
        if (!prepared || durationMs <= 0L) return
        val target = position.coerceIn(0L, durationMs)
        val repeat = currentRepeatBounds()
        if (repeat != null && target !in repeat.first until repeat.second) {
            guidedActive = false
            guidedQueue = emptyList()
            guidedIndex = 0
            guidedPasses = 0
            loopCueIndex = -1
            rangeRepeating = false
            bookmarkRepeating = false
            completedPasses = 0
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
                            val nextPass = if (guidedActive) advanceGuidedPass() else
                                if (repeat != null && !finishRepeatPass()) repeat else null
                            val replayed = if (nextPass != null) runCatching {
                                finished.seekTo(nextPass.first.toInt())
                                finished.start()
                                positionMs = nextPass.first
                                true
                            }.getOrDefault(false) else false
                            if (!replayed) {
                                playing = false
                                if (player === finished && !guidedActive) positionMs = durationMs
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
        rangeStartIndex, rangeEndIndex, bookmarkRepeating, bookmarkStartMs,
        bookmarkEndMs, subtitleOffsetMs, guidedActive, guidedIndex, guidedRounds) {
        val current = player
        while (playing && current != null && player === current) {
            val now = runCatching { current.currentPosition.toLong() }.getOrNull()
            if (now != null) {
                positionMs = now.coerceIn(0L, durationMs)
                val repeat = currentRepeatBounds()
                val elapsed = SystemClock.elapsedRealtime()
                if (repeat != null && now >= repeat.second &&
                    elapsed - lastLoopAtMs >= 450L) {
                    val nextPass = if (guidedActive) advanceGuidedPass() else
                        if (finishRepeatPass()) null else repeat
                    if (nextPass == null) break
                    val rewound = runCatching {
                        current.seekTo(nextPass.first.toInt())
                        true
                    }.getOrDefault(false)
                    if (rewound) {
                        lastLoopAtMs = elapsed
                        positionMs = nextPass.first
                    }
                }
            }
            delay(160L)
        }
    }

    val adjustedDraft = remember(cues, subtitleOffsetMs) {
        PrivateSubtitleAudioAlignment.preview(cues, subtitleOffsetMs)
    }
    // A hand-picked whole A–B section becomes a NEW, zero-based subtitle-only
    // editor draft. This deliberately never creates or copies audio content.
    val passageDraft = remember(cues, subtitleOffsetMs,
        rangeStartIndex, rangeEndIndex, durationMs) {
        val bounds = rangeBounds()
        if (bounds == null) null else {
            val selected = cues.subList(rangeStartIndex, rangeEndIndex + 1).toList()
            val timed = if (subtitleOffsetMs == 0L) selected
                else PrivateSubtitleAudioAlignment.preview(selected, subtitleOffsetMs)
            timed?.let { PrivateSubtitleClip.extract(it, bounds.first, bounds.second)?.cues }
        }
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
                    TextButton(
                        onClick = { bookmarkToolsOpen = !bookmarkToolsOpen },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (bookmarkToolsOpen) "▴ Verberg audio-tijdmarkeringen"
                            else "▾ Markeer audiomomenten en herhaal vrij A–B")
                    }
                    if (bookmarkToolsOpen) {
                        HorizontalDivider()
                        Text("Markeer maximaal 8 zelf gekozen punten in de audio. " +
                            "Spring terug om een uitspraak opnieuw te horen of kies " +
                            "twee markeringen als A en B (0,75 s tot 5 min). " +
                            "De gekozen SRT/VTT hoeft geen ondertitelregel op die tijden te hebben. " +
                            "Alle markeringen verdwijnen bij sluiten of andere audio.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            enabled = !guidedActive &&
                                audioBookmarks.size < 8 && durationMs > 0L,
                            onClick = {
                                val mark = positionMs.coerceIn(0L, durationMs)
                                if (audioBookmarks.any { kotlin.math.abs(it - mark) < 500L }) {
                                    notice = "Hier staat al bijna dezelfde audiomarkering (binnen 0,5 s)."
                                } else {
                                    audioBookmarks = (audioBookmarks + mark).sorted()
                                    notice = "Audiomoment ${clock(mark)} toegevoegd; alleen in dit scherm."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("＋ Markeer huidige audiopositie (${audioBookmarks.size}/8)") }
                        audioBookmarks.forEachIndexed { index, mark ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = {
                                    seekTo(mark)
                                    notice = "Naar eigen audiomarkering ${clock(mark)} gesprongen."
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("▶ Naar markering ${index + 1} · ${clock(mark)}")
                                }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(enabled = !guidedActive, onClick = {
                                        bookmarkStartMs = mark
                                        bookmarkRepeating = false
                                        notice = "Audio A-start: ${clock(mark)}."
                                    }, modifier = Modifier.weight(1f)) {
                                        Text(if (mark == bookmarkStartMs) "A-start ✓" else "Kies A")
                                    }
                                    TextButton(enabled = !guidedActive, onClick = {
                                        bookmarkEndMs = mark
                                        bookmarkRepeating = false
                                        notice = "Audio B-einde: ${clock(mark)}."
                                    }, modifier = Modifier.weight(1f)) {
                                        Text(if (mark == bookmarkEndMs) "B-einde ✓" else "Kies B")
                                    }
                                    TextButton(enabled = !guidedActive, onClick = {
                                        audioBookmarks = audioBookmarks.filterNot { it == mark }
                                        savedPracticePassages = savedPracticePassages.filterNot {
                                            it.first == mark || it.second == mark
                                        }
                                        passageRatings = passageRatings.filterKeys {
                                            it.first != mark && it.second != mark
                                        }
                                        if (bookmarkStartMs == mark || bookmarkEndMs == mark) {
                                            bookmarkStartMs = -1L
                                            bookmarkEndMs = -1L
                                            bookmarkRepeating = false
                                        }
                                        notice = "Eigen audiomarkering verwijderd."
                                    }, modifier = Modifier.weight(1f)) { Text("Wis") }
                                }
                            }
                        }
                        Text("Mijn tijdelijke oefenfragmenten (${savedPracticePassages.size}/4)",
                            style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(
                            enabled = !guidedActive && bookmarkBounds() != null &&
                                savedPracticePassages.size < 4 &&
                                bookmarkBounds() !in savedPracticePassages,
                            onClick = {
                                val bounds = bookmarkBounds()
                                if (bounds != null && bounds !in savedPracticePassages &&
                                    savedPracticePassages.size < 4) {
                                    savedPracticePassages = savedPracticePassages + bounds
                                    notice = "A–B-fragment opgeslagen voor deze audiosessie."
                                }
                            }, modifier = Modifier.fillMaxWidth()
                        ) { Text("＋ Bewaar gekozen A–B als oefenfragment") }
                        if (savedPracticePassages.isEmpty()) {
                            Text("Bewaar A en B om meerdere uitspraken in één sessie terug te vinden.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        savedPracticePassages.forEachIndexed { index, passage ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Fragment ${index + 1} · ${clock(passage.first)} – " +
                                    clock(passage.second),
                                    style = MaterialTheme.typography.bodyMedium)
                                val sourceCue = cues.firstOrNull {
                                    cueStart(it) == passage.first && cueEnd(it) == passage.second
                                }
                                if (sourceCue != null) {
                                    Text("Ondertitel: " + sourceCue.text.take(120),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDirection = TextDirection.ContentOrRtl))
                                }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = !guidedActive && prepared &&
                                            passage.first in audioBookmarks &&
                                            passage.second in audioBookmarks,
                                        onClick = {
                                            // Seek before switching repeat modes so the old
                                            // bounds can never cancel the selected new preset.
                                            seekTo(passage.first)
                                            bookmarkStartMs = passage.first
                                            bookmarkEndMs = passage.second
                                            loopCueIndex = -1
                                            rangeRepeating = false
                                            bookmarkRepeating = true
                                            completedPasses = 0
                                            lastLoopAtMs = 0L
                                            notice = "Fragment ${index + 1} actief. " +
                                                "Druk op Afspelen indien gepauzeerd."
                                        }, modifier = Modifier.weight(2f)
                                    ) { Text("▶ Oefen") }
                                    TextButton(
                                        enabled = !guidedActive && index > 0,
                                        onClick = {
                                            savedPracticePassages =
                                                savedPracticePassages.toMutableList().apply {
                                                    add(index - 1, removeAt(index))
                                                }
                                        }, modifier = Modifier.weight(1f)
                                    ) { Text("↑") }
                                    TextButton(
                                        enabled = !guidedActive && index < savedPracticePassages.lastIndex,
                                        onClick = {
                                            savedPracticePassages =
                                                savedPracticePassages.toMutableList().apply {
                                                    add(index + 1, removeAt(index))
                                                }
                                        }, modifier = Modifier.weight(1f)
                                    ) { Text("↓") }
                                    TextButton(
                                        enabled = !guidedActive,
                                        onClick = {
                                            savedPracticePassages =
                                                savedPracticePassages.filterIndexed { i, _ ->
                                                    i != index
                                                }
                                            passageRatings = passageRatings - passage
                                            if (bookmarkRepeating &&
                                                bookmarkStartMs == passage.first &&
                                                bookmarkEndMs == passage.second) {
                                                bookmarkRepeating = false
                                                completedPasses = 0
                                            }
                                            notice = "Oefenfragment uit deze sessie verwijderd."
                                        }, modifier = Modifier.weight(1f)
                                    ) { Text("Wis") }
                                }
                                Text("Mijn eigen luisterinschatting, geen automatisch taalniveau",
                                    style = MaterialTheme.typography.labelSmall)
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf(
                                        "repeat" to "Nog oefenen",
                                        "okay" to "Gaat goed",
                                        "easy" to "Makkelijk"
                                    ).forEach { (choice, label) ->
                                        TextButton(
                                            enabled = !guidedActive,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                passageRatings = passageRatings +
                                                    (passage to choice)
                                            }
                                        ) { Text((if (passageRatings[passage] == choice)
                                            "✓ " else "") + label,
                                            style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                        Text("Begeleide A–B-oefenroute",
                            style = MaterialTheme.typography.titleSmall)
                        val needsPractice = savedPracticePassages.filter {
                            passageRatings[it] == "repeat"
                        }
                        Text("Zelf beoordeeld: " +
                            "${needsPractice.size} nog oefenen · " +
                            "${savedPracticePassages.count { passageRatings[it] == "okay" }} gaat goed · " +
                            "${savedPracticePassages.count { passageRatings[it] == "easy" }} makkelijk.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                enabled = !guidedActive,
                                onClick = { guidedNeedsOnly = false }
                            ) { Text((if (!guidedNeedsOnly) "✓ " else "") + "Alle fragmenten") }
                            TextButton(
                                modifier = Modifier.weight(1f),
                                enabled = !guidedActive && needsPractice.isNotEmpty(),
                                onClick = { guidedNeedsOnly = true }
                            ) { Text((if (guidedNeedsOnly) "✓ " else "") +
                                "Alleen nog oefenen") }
                        }
                        val practiceCandidates =
                            if (guidedNeedsOnly) needsPractice else savedPracticePassages
                        Text("Speel mijn opgeslagen fragmenten in gekozen volgorde " +
                            "achter elkaar af. Begin bewust met Afspelen als audio gepauzeerd is. " +
                            "Bij voltooiing pauzeert de speler vanzelf; Android-background blijft pauzeren.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 5).forEach { rounds ->
                                TextButton(
                                    enabled = !guidedActive && guidedRounds != rounds,
                                    onClick = { guidedRounds = rounds },
                                    modifier = Modifier.weight(1f)
                                ) { Text((if (guidedRounds == rounds) "✓ " else "") +
                                    "${rounds}× per fragment") }
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = guidedActive || (prepared &&
                                practiceCandidates.isNotEmpty() &&
                                practiceCandidates.size <= 4 &&
                                practiceCandidates.all { (start, end) ->
                                    start in audioBookmarks && end in audioBookmarks &&
                                        start >= 0L && end <= durationMs &&
                                        end - start in 750L..300_000L
                                }),
                            onClick = {
                                if (guidedActive) {
                                    pauseAudio()
                                    guidedActive = false
                                    guidedQueue = emptyList()
                                    guidedIndex = 0
                                    guidedPasses = 0
                                    notice = "Oefenroute gestopt. Audio staat gepauzeerd."
                                } else {
                                    val snapshot = practiceCandidates.toList()
                                    if (prepared && snapshot.isNotEmpty()) {
                                        // Seek before activating the queue to avoid
                                        // accidental cancellation by an old repeat mode.
                                        seekTo(snapshot.first().first)
                                        bookmarkRepeating = false
                                        rangeRepeating = false
                                        loopCueIndex = -1
                                        guidedQueue = snapshot
                                        guidedIndex = 0
                                        guidedPasses = 0
                                        lastLoopAtMs = 0L
                                        guidedActive = true
                                        notice = "Oefenroute klaar: fragment 1 van ${snapshot.size}, " +
                                            "${guidedRounds}× per fragment. Afspelen als nodig."
                                    }
                                }
                            }
                        ) { Text(if (guidedActive) "■ Stop begeleide oefenroute" else
                            "▶ Start mijn begeleide A–B-oefenroute") }
                        if (guidedActive) {
                            Text("Fragment ${guidedIndex + 1}/${guidedQueue.size} · " +
                                "afgeronde herhalingen: ${guidedPasses}/${guidedRounds}. " +
                                "Pauseer met de bestaande afspeelknop of stop de hele route.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        OutlinedButton(
                            enabled = !guidedActive &&
                                (bookmarkRepeating || bookmarkBounds() != null),
                            onClick = {
                                if (bookmarkRepeating) {
                                    bookmarkRepeating = false
                                    notice = "Vrije audio A–B-herhaling gestopt."
                                } else {
                                    val bounds = bookmarkBounds()
                                    if (bounds == null) {
                                        notice = "Kies twee geldige markeringen met 0,75 s tot 5 min afstand."
                                    } else {
                                        loopCueIndex = -1
                                        rangeRepeating = false
                                        bookmarkRepeating = true
                                        lastLoopAtMs = 0L
                                        completedPasses = 0
                                        seekTo(bounds.first)
                                        notice = "Vrije audio A–B ${clock(bounds.first)} tot " +
                                            "${clock(bounds.second)}; druk op Afspelen indien gepauzeerd."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (bookmarkRepeating) "↻ Stop vrije audio A–B-herhaling"
                                else "↻ Herhaal tussen mijn audio A- en B-markeringen")
                        }
                    }
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

                    OutlinedButton(
                        enabled = !guidedActive && activeIndex >= 0,
                        onClick = { saveCueForPractice(activeIndex) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("＋ Huidige ondertitel als A–B-oefenfragment bewaren")
                    }
                    Text("Gebruikt uitsluitend de originele, eventueel tijdelijk verschoven " +
                        "SRT/VTT-begin- en eindtijd. Fragmenten vind je onder " +
                        "audiomarkeringen en begeleide oefenroute (maximaal vier).",
                        style = MaterialTheme.typography.labelSmall)
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
                    listOf(
                        listOf(-5000L to "−5s", -1000L to "−1s", -250L to "−0,25s"),
                        listOf(250L to "+0,25s", 1000L to "+1s", 5000L to "+5s")
                    ).forEach { controls ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            controls.forEach { (delta, label) ->
                                TextButton(onClick = { shiftSubtitles(delta) },
                                    modifier = Modifier.weight(1f)
                                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                            }
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
                        enabled = !guidedActive && (loopCueIndex >= 0 || (active != null &&
                            active.endMillis - active.startMillis >= 750L &&
                            cuePlayable(active))),
                        onClick = {
                            loopCueIndex = if (loopCueIndex >= 0) -1 else activeIndex
                            completedPasses = 0
                            rangeRepeating = false
                            bookmarkRepeating = false
                            lastLoopAtMs = 0L
                        }
                    ) {
                        Text(if (loopCueIndex >= 0)
                            "↻ Herhaal regel ${loopCueIndex + 1} ✓ · tik om te stoppen"
                        else "↻ Herhaal deze ondertitelregel")
                    }
                    Text("Aantal keren beluisteren per herhaalfragment",
                        style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Doorlopend", 1 to "1×", 3 to "3×", 5 to "5×")
                            .forEach { (limit, label) ->
                                TextButton(
                                    onClick = {
                                        repeatPassLimit = limit
                                        completedPasses = 0
                                        lastLoopAtMs = 0L
                                    },
                                    enabled = !guidedActive && repeatPassLimit != limit,
                                    modifier = Modifier.weight(1f)
                                ) { Text((if (repeatPassLimit == limit) "✓ " else "") + label) }
                            }
                    }
                    if (completedPasses > 0) {
                        Text("Afgeronde fragmenten: $completedPasses" +
                            (if (repeatPassLimit == 0) "" else " / $repeatPassLimit"),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Herhalen gebruikt alleen de door jou ingevoerde tijdcodes " +
                        "(minimaal 0,75 s). Geen automatische woord- of audiouitlijning.",
                        style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { rangeToolsOpen = !rangeToolsOpen },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (rangeToolsOpen) "▴ Verberg A–B-passageoefening"
                            else "▾ Oefen een A–B-passage van meerdere regels")
                    }
                    if (rangeToolsOpen) {
                    HorizontalDivider()
                    Text("Oefen meerdere ondertitelregels achter elkaar: markeer eerst " +
                        "A (beginregel), daarna B (eindregel). Start vervolgens A–B-herhaling. " +
                        "Het gehele fragment moet binnen de audio vallen (0,75 s tot 5 min).",
                        style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !guidedActive && activeIndex >= 0 && active != null && cuePlayable(active),
                            onClick = {
                                rangeStartIndex = activeIndex
                                rangeEndIndex = -1
                                rangeRepeating = false
                                bookmarkRepeating = false
                                loopCueIndex = -1
                                confirmClipOpen = false
                                notice = "A ingesteld op regel " + (activeIndex + 1) +
                                    ". Kies nu een latere of dezelfde regel als B."
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (rangeStartIndex >= 0) "A ✓ regel " +
                            (rangeStartIndex + 1) else "Kies A-start") }
                        OutlinedButton(
                            enabled = !guidedActive && rangeStartIndex >= 0 && activeIndex >= rangeStartIndex &&
                                active != null && cuePlayable(active),
                            onClick = {
                                rangeEndIndex = activeIndex
                                rangeRepeating = false
                                bookmarkRepeating = false
                                loopCueIndex = -1
                                confirmClipOpen = false
                                notice = "B ingesteld op regel " + (activeIndex + 1) + "."
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (rangeEndIndex >= rangeStartIndex &&
                            rangeStartIndex >= 0) "B ✓ regel " +
                            (rangeEndIndex + 1) else "Kies B-einde") }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !guidedActive && (rangeRepeating || rangeBounds() != null),
                        onClick = {
                            if (rangeRepeating) {
                                rangeRepeating = false
                                notice = "A–B-herhaling gestopt."
                            } else {
                                val bounds = rangeBounds()
                                if (bounds != null) {
                                    loopCueIndex = -1
                                    bookmarkRepeating = false
                                    lastLoopAtMs = 0L
                                    rangeRepeating = true
                                    completedPasses = 0
                                    seekTo(bounds.first)
                                    notice = "Herhaal A–B vanaf de gekozen startregel; " +
                                        "druk op Afspelen als de audio pauzeert."
                                }
                            }
                        }
                    ) { Text(if (rangeRepeating) "Stop A–B-herhaling ✓"
                        else "↻ Herhaal mijn A–B-passage") }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = passageDraft != null,
                        onClick = {
                            pauseAudio()
                            rangeRepeating = false
                            confirmClipOpen = true
                        }
                    ) { Text("Maak A–B als afzonderlijk ondertitelconcept…") }
                    Text("Je kunt je hele A–B-passage als een nieuw SRT/VTT-concept " +
                        "laten beginnen bij 00:00, maar audio wordt nooit geëxporteerd.",
                        style = MaterialTheme.typography.labelSmall)
                    Text("A–B-herhaling is tijdelijk en bevat alleen de door jou gekozen " +
                        "ondertitelregels, geen automatische audioanalyse.",
                        style = MaterialTheme.typography.labelSmall)
                    }
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
    if (confirmClipOpen && passageDraft != null) {
        AlertDialog(
            onDismissRequest = { confirmClipOpen = false },
            title = { Text("Alleen mijn A–B-ondertitelfragment overnemen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dit maakt een eigen tijdelijk editorconcept met " +
                        passageDraft.size + " van " + cues.size + " ondertitelregels.")
                    Text("Geselecteerde regels: A " + (rangeStartIndex + 1) +
                        " tot en met B " + (rangeEndIndex + 1) + ".")
                    Text("De nieuwe teksttijdlijn begint bij 00:00; " +
                        "de laatste regel eindigt op " + passageDraft.last().endLabel + ".")
                    Text("Je originele document én het audiobestand blijven ongewijzigd. " +
                        "Er wordt GEEN audioclip gemaakt. Pas de tekst zo nodig aan " +
                        "en gebruik daarna alleen bewust 'Bewaar nieuwe kopie' in de editor.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val chosen = passageDraft.toList()
                    confirmClipOpen = false
                    closeAudio()
                    onApplyAdjustedCues(chosen)
                }) { Text("Open alleen mijn geselecteerde ondertitels in de editor") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClipOpen = false }) {
                    Text("Annuleer · niets overnemen")
                }
            }
        )
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


                                OutlinedButton(
                                    enabled = !guidedActive,
                                    onClick = {
                                        saveCueForPractice(index)
                                        // Remain in search after adding: choose more phrases
                                        // without restarting playback or copying audio.
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("＋ Regel ${index + 1} in mijn oefenroute bewaren")
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
