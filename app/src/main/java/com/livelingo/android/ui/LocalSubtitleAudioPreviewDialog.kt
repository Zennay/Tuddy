package com.livelingo.android.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.livelingo.android.TargetCatalog
import com.livelingo.android.TextTarget
import kotlinx.coroutines.delay
import java.util.Locale

// Manual-only note languages intentionally absent from the ML Kit text targets.
// Never present these as supported offline translation or ASR engines.
private val manualSubtitleNoteLanguages = listOf(
    TextTarget("kmr", "Kurmancî · Kurmanji (eigen notities)"),
    TextTarget("ckb", "سۆرانی · Sorani (eigen notities)"),
    TextTarget("ps", "پښتو · Pasjtoe (eigen notities)")
)

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
    readOwnMeaning: (String, String) -> String?,
    saveOwnMeaning: (String, String, String) -> Boolean,
    savePersonalPhrase: (String, String, String, String) -> Boolean,
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
    var completedGuidedRoute by remember { mutableStateOf<List<Pair<Long, Long>>>(emptyList()) }
    var completedGuidedRounds by remember { mutableIntStateOf(0) }
    var practiceReportOpen by remember { mutableStateOf(false) }
    // The saved worksheet choice is independent of the guided playback queue.
    var worksheetScope by remember { mutableStateOf(PrivatePracticeRouteScope.ALL) }
    var worksheetAudience by remember { mutableStateOf(PrivateListeningWorksheetAudience.SELF_STUDY) }
    var practiceCsvPreviewOpen by remember { mutableStateOf(false) }
    // An immutable user-approved document lives only while the Android
    // destination chooser is open; never retain the audio URI or audio bytes.
    var pendingPracticeReport by remember {
        mutableStateOf<PendingPrivatePracticeReport?>(null)
    }
    // Human judgement only: no speech recognition or pronunciation scoring.
    var passageRatings by remember {
        mutableStateOf<Map<Pair<Long, Long>, String>>(emptyMap())
    }
    // User-written next-step notes, bounded and only for the current audio session.
    var passageNotes by remember {
        mutableStateOf<Map<Pair<Long, Long>, String>>(emptyMap())
    }
    var guidedScope by remember { mutableStateOf(PrivatePracticeRouteScope.ALL) }
    var notice by remember { mutableStateOf("Kies zelf audio bij deze ondertitels.") }
    var cuePickerOpen by remember { mutableStateOf(false) }
    var confirmApplyOpen by remember { mutableStateOf(false) }
    var confirmClipOpen by remember { mutableStateOf(false) }
    var cueQuery by remember { mutableStateOf("") }
    // Imported subtitle files have no trusted language marker: user picks it.
    var chosenSubtitleLanguage by remember(audioUri) { mutableStateOf<String?>(null) }
    var wordLanguageMenuOpen by remember { mutableStateOf(false) }
    var phraseDraftLanguage by remember { mutableStateOf<String?>(null) }
    var phraseDraftText by remember { mutableStateOf("") }
    var phraseDraftMeaning by remember { mutableStateOf("") }
    var phraseDraftMeaningLanguage by remember { mutableStateOf("nl") }
    var phraseDraftApproved by remember { mutableStateOf(false) }
    var phraseDraftNotice by remember { mutableStateOf("") }

    fun exportablePracticeSummary(): String = privatePracticeSummary(
        cues, subtitleOffsetMs, savedPracticePassages, passageRatings,
        completedGuidedRoute, completedGuidedRounds, guidedActive,
        forExport = true, notes = passageNotes
    )

    val savePracticeReport = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pending = pendingPracticeReport
        pendingPracticeReport = null
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null && pending != null) {
            val updatedSummary = exportablePracticeSummary()
            val updatedBytes = when (pending.format) {
                PrivatePracticeReportFormat.HTML ->
                    PrivatePrintablePracticeSummary.snapshot(updatedSummary)
                PrivatePracticeReportFormat.WORKSHEET_HTML ->
                    PrivateListeningWorksheet.snapshot(
                        cues, subtitleOffsetMs, savedPracticePassages,
                        passageRatings, passageNotes, worksheetScope, worksheetAudience
                    )
                PrivatePracticeReportFormat.TXT ->
                    PrivatePrintablePracticeSummary.snapshotText(updatedSummary)
                PrivatePracticeReportFormat.CSV ->
                    PrivatePrintablePracticeSummary.snapshotCsv(
                        cues, subtitleOffsetMs, savedPracticePassages,
                        passageRatings, passageNotes, worksheetScope
                    )
            }
            if (!prepared || savedPracticePassages.isEmpty() ||
                updatedSummary != pending.summary ||
                pending.selectionScope != worksheetScope ||
                pending.worksheetAudience != worksheetAudience ||
                updatedBytes == null ||
                !updatedBytes.contentEquals(pending.bytes)
            ) {
                notice = "Mijn luisteroefening is gewijzigd. Bekijk en bewaar opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(pending.bytes)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                notice = if (saved)
                    "Privé luisteroverzicht als ${pending.format.title} opgeslagen. Controleer opslag."
                else "Luisteroverzicht niet opgeslagen. Kies een andere opslaglocatie."
            }
        }
    }

    fun choosePracticeReport(format: PrivatePracticeReportFormat) {
        if (!prepared || guidedActive || savedPracticePassages.isEmpty()) return
        val report = exportablePracticeSummary()
        val payload = when (format) {
            PrivatePracticeReportFormat.HTML -> PrivatePrintablePracticeSummary.snapshot(report)
            PrivatePracticeReportFormat.WORKSHEET_HTML ->
                PrivateListeningWorksheet.snapshot(
                    cues, subtitleOffsetMs, savedPracticePassages,
                    passageRatings, passageNotes, worksheetScope, worksheetAudience
                )
            PrivatePracticeReportFormat.TXT -> PrivatePrintablePracticeSummary.snapshotText(report)
            PrivatePracticeReportFormat.CSV ->
                PrivatePrintablePracticeSummary.snapshotCsv(
                    cues, subtitleOffsetMs, savedPracticePassages,
                    passageRatings, passageNotes, worksheetScope
                )
        }
        if (payload == null) {
            notice = "Geen bruikbaar privé luisteroverzicht om te bewaren."
        } else {
            pendingPracticeReport = PendingPrivatePracticeReport(
                report, format, worksheetScope, worksheetAudience, payload)
            practiceReportOpen = false
            savePracticeReport.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = format.mime
                putExtra(Intent.EXTRA_TITLE,
                    if (format == PrivatePracticeReportFormat.WORKSHEET_HTML &&
                        worksheetAudience == PrivateListeningWorksheetAudience.STUDENT_HANDOUT)
                        "Tuddy-luister-leerlingenblad-zonder-bronregels.html"
                    else format.filename)
            })
        }
    }

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
        completedGuidedRoute = emptyList()
        completedGuidedRounds = 0
        practiceReportOpen = false
        practiceCsvPreviewOpen = false
        pendingPracticeReport = null
        worksheetAudience = PrivateListeningWorksheetAudience.SELF_STUDY
        guidedActive = false
        guidedQueue = emptyList()
        guidedIndex = 0
        guidedPasses = 0
        passageRatings = emptyMap()
        passageNotes = emptyMap()
        guidedScope = PrivatePracticeRouteScope.ALL
        bookmarkToolsOpen = false
        bookmarkStartMs = -1L
        bookmarkEndMs = -1L
        bookmarkRepeating = false
        subtitleOffsetMs = 0L
        cuePickerOpen = false
        confirmApplyOpen = false
        confirmClipOpen = false
        cueQuery = ""
        chosenSubtitleLanguage = null
        wordLanguageMenuOpen = false
        phraseDraftLanguage = null
        phraseDraftText = ""
        phraseDraftMeaning = ""
        phraseDraftApproved = false
        phraseDraftNotice = ""
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

    // Only user-selected Kurmanji, Sorani and Pashto text can become an
    // authored personal phrase. Never infer the language or translation.
    fun openPersonalSubtitlePhrase(cue: PrivateSubtitleCue) {
        val manualLanguage = chosenSubtitleLanguage
        if (manualLanguage !in setOf("kmr", "ckb", "ps") || cue.text.isBlank()) {
            notice = "Kies eerst zelf Kurmanji, Sorani of Pasjtoe als ondertiteltaal."
            return
        }
        pauseAudio()
        phraseDraftLanguage = manualLanguage
        phraseDraftText = cue.text.trim()
        phraseDraftMeaning = ""
        phraseDraftMeaningLanguage = "nl"
        phraseDraftApproved = false
        phraseDraftNotice = ""
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


    // Source-cue and selected multi-cue ranges use exactly one bounded
    // ephemeral deck. No additional playback state or storage route.
    fun saveBoundsForPractice(bounds: Pair<Long, Long>, label: String) {
        if (!prepared || guidedActive) {
            notice = "Stop eerst de oefenroute en open zelf een bruikbaar audiobestand."
            return
        }
        val (start, end) = bounds
        if (start < 0L || start >= durationMs || end > durationMs ||
            end - start !in 750L..300_000L) {
            notice = "Dit fragment valt buiten de audio of duurt niet 0,75 s–5 min."
            return
        }
        if (bounds in savedPracticePassages) {
            bookmarkToolsOpen = true
            notice = "Dit exacte fragment staat al in je oefenroute."
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
            notice = "Te weinig ruimte voor audiomarkeringen (maximaal 8). Wis eerst zelf markeringen."
            return
        }
        if (missing.any { mark ->
                (audioBookmarks + missing).any { other ->
                    other != mark && kotlin.math.abs(other - mark) < 500L
                }
            }) {
            bookmarkToolsOpen = true
            notice = "Een bestaand audiomoment ligt binnen 0,5 s van deze passagegrens. " +
                "Pas de markeringen zelf aan om een onduidelijk fragment te vermijden."
            return
        }
        audioBookmarks = (audioBookmarks + missing).distinct().sorted()
        savedPracticePassages = savedPracticePassages + bounds
        bookmarkToolsOpen = true
        notice = label + " toegevoegd als oefenfragment " +
            "(" + savedPracticePassages.size + "/4). Start de begeleide route zelf."
    }

    fun saveCueForPractice(index: Int) {
        val cue = cues.getOrNull(index) ?: return
        saveBoundsForPractice(cueStart(cue) to cueEnd(cue), "Ondertitelregel ${index + 1}")
    }

    fun saveCueRangeForPractice() {
        val bounds = rangeBounds() ?: run {
            notice = "Kies eerst een volledig geldige A–B-passage in je eigen ondertitels."
            return
        }
        saveBoundsForPractice(bounds, "Ondertitelpassage regel ${rangeStartIndex + 1} t/m ${rangeEndIndex + 1}")
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
        completedGuidedRoute = guidedQueue.toList()
        completedGuidedRounds = guidedRounds
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

    val practiceSummaryText = remember(
        cues, subtitleOffsetMs, savedPracticePassages, passageRatings,
        passageNotes, completedGuidedRoute, completedGuidedRounds, guidedActive
    ) {
        privatePracticeSummary(
            cues, subtitleOffsetMs, savedPracticePassages, passageRatings,
            completedGuidedRoute, completedGuidedRounds, guidedActive,
            notes = passageNotes
        )
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
        // Keep the exact user-picked A/B boundary legible even when the
        // passages begin or end within the same wall-clock second.
        val safeMs = millis.coerceAtLeast(0L)
        val wholeSeconds = safeMs / 1000L
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d",
            wholeSeconds / 3600L, (wholeSeconds / 60L) % 60L,
            wholeSeconds % 60L, safeMs % 1000L)
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
                                        passageNotes = passageNotes.filterKeys {
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
                                val fromCue = cues.indexOfFirst {
                                    cueStart(it) == passage.first
                                }
                                val toCue = cues.indexOfLast {
                                    cueEnd(it) == passage.second
                                }
                                if (fromCue >= 0 && toCue >= fromCue) {
                                    val summary = if (fromCue == toCue)
                                        "Ondertitel: " + cues[fromCue].text.take(120)
                                    else "Ondertitelregels " + (fromCue + 1) + "–" +
                                        (toCue + 1) + ": " +
                                        cues[fromCue].text.take(55) + " … " +
                                        cues[toCue].text.take(55)
                                    Text(summary,
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
                                            passageNotes = passageNotes - passage
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
                                OutlinedTextField(
                                    value = passageNotes[passage].orEmpty(),
                                    onValueChange = { value ->
                                        if (!guidedActive) {
                                            val end = value.offsetByCodePoints(
                                                0, minOf(240, value.codePointCount(0, value.length))
                                            )
                                            val bounded = value.substring(0, end)
                                                .replace('\r', ' ').replace('\n', ' ')
                                            passageNotes = if (bounded.isBlank())
                                                passageNotes - passage
                                            else passageNotes + (passage to bounded)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !guidedActive,
                                    label = { Text("Mijn eigen volgende oefenstap (optioneel)") },
                                    supportingText = {
                                        val note = passageNotes[passage].orEmpty()
                                        Text("${note.codePointCount(0, note.length)}/240 · " +
                                            "alleen deze tijdelijke audiosessie")
                                    },
                                    maxLines = 3
                                )
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
                        OutlinedButton(
                            enabled = prepared,
                            onClick = {
                                pauseAudio()
                                practiceReportOpen = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Mijn privé oefenoverzicht bekijken · tekst zelf selecteren") }
                        val needsPractice = savedPracticePassages.filter {
                            passageRatings[it] == "repeat"
                        }
                        val unratedCount = savedPracticePassages.count {
                            passageRatings[it] !in listOf("repeat", "okay", "easy")
                        }
                        Text("Zelf beoordeeld: " +
                            "${needsPractice.size} nog oefenen · " +
                            "${savedPracticePassages.count { passageRatings[it] == "okay" }} gaat goed · " +
                            "${savedPracticePassages.count { passageRatings[it] == "easy" }} makkelijk · " +
                            unratedCount + " nog niet beoordeeld.",
                            style = MaterialTheme.typography.bodySmall)
                        PrivatePracticeRouteScope.entries.forEach { scope ->
                            val candidateCount = privatePracticeRouteCandidates(
                                savedPracticePassages, passageRatings, scope
                            ).size
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !guidedActive && candidateCount > 0,
                                onClick = { guidedScope = scope }
                            ) {
                                Text((if (guidedScope == scope) "✓ " else "○ ") +
                                    scope.title + " (" + candidateCount + ")")
                            }
                        }
                        val practiceCandidates = privatePracticeRouteCandidates(
                            savedPracticePassages, passageRatings, guidedScope
                        )
                        Text("Voor deze route gekozen: " + practiceCandidates.size + " fragmenten.",
                            style = MaterialTheme.typography.labelMedium)
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
                                        completedGuidedRoute = emptyList()
                                        completedGuidedRounds = 0
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
                    if (rangeBounds() != null) {
                        Text("Gekozen bronpassage: regel ${rangeStartIndex + 1} tot en met " +
                            "${rangeEndIndex + 1} · ${clock(rangeBounds()!!.first)} – " +
                            clock(rangeBounds()!!.second) +
                            ". Alleen jouw SRT/VTT-tijdcodes, geen automatische audio-uitlijning.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !guidedActive && rangeBounds() != null,
                        onClick = { saveCueRangeForPractice() }
                    ) {
                        Text("＋ Bewaar deze complete A–B-passage in mijn begeleide oefenroute")
                    }
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
                    Text("Woordkaarten bij je eigen SRT/VTT: kies zelf de teksttaal. " +
                        "Onbekende talen krijgen geen verzonnen vertaling of woordtiming.",
                        style = MaterialTheme.typography.labelSmall)
                    Box {
                        TextButton(onClick = { wordLanguageMenuOpen = true }) {
                            val languageLabel = (manualSubtitleNoteLanguages + TargetCatalog.targets).firstOrNull {
                                it.code == chosenSubtitleLanguage
                            }?.title ?: "Kies de taal van mijn ondertiteltekst…"
                            Text(languageLabel)
                        }
                        DropdownMenu(
                            expanded = wordLanguageMenuOpen,
                            onDismissRequest = { wordLanguageMenuOpen = false }
                        ) {
                            // These three languages support *manual* word/phrase
                            // notes, not ML Kit text translation or verified ASR.
                            (manualSubtitleNoteLanguages + TargetCatalog.targets).forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.title) },
                                    onClick = {
                                        chosenSubtitleLanguage = language.code
                                        wordLanguageMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (active == null) {
                            Text("Gebruik Vorige/Volgende om naar een ondertitel te springen.",
                                style = MaterialTheme.typography.headlineMedium)
                        } else {
                            OfflineWordText(
                                text = active.text,
                                language = chosenSubtitleLanguage,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    textDirection = TextDirection.ContentOrRtl),
                                readOwnMeaning = readOwnMeaning,
                                saveOwnMeaning = saveOwnMeaning,
                                allowSpeech = false
                            )
                        }
                    }
                    Text("Tik op een woord voor de bestaande lokale woordkaart. " +
                        "Je eigen betekenis wordt uitsluitend na afzonderlijk Bewaar " +
                        "opgeslagen in Mijn eigen woorden; het audiobestand blijft ongewijzigd.",
                        style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = active != null && active.text.isNotBlank() &&
                            chosenSubtitleLanguage in setOf("kmr", "ckb", "ps"),
                        onClick = { active?.let(::openPersonalSubtitlePhrase) }
                    ) { Text("＋ Bewaar deze zin met mijn eigen betekenis…") }
                    Text("Mijn eigen zinnen ondersteunt alleen zelf gekozen Kurmanji, " +
                        "Sorani of Pasjtoe, met een door jou geschreven betekenis in NL of EN. " +
                        "Niets wordt automatisch vertaald of opgeslagen.",
                        style = MaterialTheme.typography.labelSmall)
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

    val activePhraseLanguage = phraseDraftLanguage
    if (activePhraseLanguage != null) {
        val validPhraseText = phraseDraftText.isNotBlank() &&
            phraseDraftText.length <= 120 && phraseDraftText == phraseDraftText.trim() &&
            phraseDraftText.none { char ->
                Character.isISOControl(char) || Character.isSurrogate(char) ||
                    (Character.getType(char) == Character.FORMAT.toInt() && char != '\u200C') ||
                    char == '\u2028' || char == '\u2029'
            }
        val validPhraseMeaning = phraseDraftMeaning.isNotBlank() &&
            phraseDraftMeaning.length <= 180 &&
            phraseDraftMeaning == phraseDraftMeaning.trim() &&
            phraseDraftMeaning.none { char ->
                Character.isISOControl(char) || Character.isSurrogate(char) ||
                    (Character.getType(char) == Character.FORMAT.toInt() && char != '\u200C') ||
                    char == '\u2028' || char == '\u2029'
            }
        AlertDialog(
            onDismissRequest = { phraseDraftLanguage = null },
            title = { Text("Mijn eigen zin bewust bewaren?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("Deze tekst komt uitsluitend uit jouw zelf gekozen SRT/VTT " +
                        "en is niet op uitspraak of betekenis gecontroleerd. " +
                        "Je schrijft de betekenis zelf. Een opslag in Mijn eigen zinnen " +
                        "blijft app-privé bestaan na het sluiten van deze audiosessie.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Eigen gekozen zinta(a)l: " + activePhraseLanguage.uppercase(Locale.ROOT))
                    OutlinedTextField(
                        value = phraseDraftText,
                        onValueChange = {
                            phraseDraftText = it.take(400)
                            phraseDraftApproved = false
                            phraseDraftNotice = ""
                        },
                        label = { Text("Mijn eigen zin (maximaal 120 tekens)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!validPhraseText) {
                        Text("Maak de zin zelf maximaal 120 tekens zonder " +
                            "besturingstekens of onzichtbare tekens.",
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("nl" to "Nederlands", "en" to "Engels").forEach { (code, label) ->
                            TextButton(
                                enabled = phraseDraftMeaningLanguage != code,
                                onClick = {
                                    phraseDraftMeaningLanguage = code
                                    phraseDraftApproved = false
                                }
                            ) {
                                Text((if (phraseDraftMeaningLanguage == code) "✓ " else "") + label)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = phraseDraftMeaning,
                        onValueChange = {
                            phraseDraftMeaning = it.take(180)
                            phraseDraftApproved = false
                            phraseDraftNotice = ""
                        },
                        label = { Text("Mijn eigen betekenis (maximaal 180 tekens)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Een identieke zin die al in Mijn eigen zinnen staat wordt " +
                        "hier bewust niet overschreven. Bewerk die in de bestaande woordenlijst.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Checkbox(
                            checked = phraseDraftApproved,
                            enabled = validPhraseText && validPhraseMeaning,
                            onCheckedChange = { phraseDraftApproved = it }
                        )
                        Text("Ik heb de zin en mijn zelf geschreven betekenis nagelezen " +
                            "en wil deze privé opslaan.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (phraseDraftNotice.isNotBlank()) {
                        Text(phraseDraftNotice, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = validPhraseText && validPhraseMeaning && phraseDraftApproved,
                    onClick = {
                        val saved = savePersonalPhrase(
                            activePhraseLanguage, phraseDraftText,
                            phraseDraftMeaningLanguage, phraseDraftMeaning
                        )
                        if (saved) {
                            phraseDraftLanguage = null
                            notice = "Mijn eigen zin bewust privé bewaard; geen automatische vertaling."
                        } else {
                            phraseDraftApproved = false
                            phraseDraftNotice = "Zin niet opgeslagen: bestaat mogelijk al, " +
                                "de woordenlijst is vol, of privéopslag is niet beschikbaar. " +
                                "Je bestaande zinnen zijn ongemoeid."
                        }
                    }
                ) { Text("Bewaar alleen deze zelf gecontroleerde zin") }
            },
            dismissButton = {
                TextButton(onClick = { phraseDraftLanguage = null }) {
                    Text("Annuleer · niets opslaan")
                }
            }
        )
    }

    if (practiceReportOpen && prepared) {
        AlertDialog(
            onDismissRequest = { practiceReportOpen = false },
            title = { Text("Mijn privé luisteroverzicht") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Hier staan je eigen ondertitels, tijdvakken en beoordelingen. " +
                        "Selecteer alleen zelf tekst als je die via Android wilt kopiëren; " +
                        "je klembord- of toetsenbord-app kan een kopie dan lezen.",
                        style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Je kunt een afdrukbare HTML-rapportage, een zelfstandig " +
                            "luisterwerkblad, een TXT-notitie of CSV met eigen beoordelingen " +
                            "bewaren. Op het luisterwerkblad blijven bronregels tijdens " +
                            "het oefenen ingeklapt en worden ze op papier alleen " +
                            "met een expliciet aangevinkt afdrukvakje toegevoegd. " +
                            "Externe Android-opslag kan synchroniseren; audio of audiobestandsnamen " +
                            "gaan niet mee.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Mijn luisterwerkblad bevat alleen de fragmenten in " +
                            "de hieronder gekozen persoonlijke categorie. " +
                            "Dit verandert de begeleide afspeelroute niet.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    PrivatePracticeRouteScope.entries.forEach { candidate ->
                        val candidateCount = privatePracticeRouteCandidates(
                            savedPracticePassages, passageRatings, candidate
                        ).size
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !guidedActive && candidateCount > 0,
                            onClick = { worksheetScope = candidate }
                        ) {
                            Text((if (worksheetScope == candidate) "● " else "○ ") +
                                candidate.title + " (" + candidateCount + ")")
                        }
                    }
                    HorizontalDivider()
                    Text("Voor wie is dit luisterwerkblad?",
                        style = MaterialTheme.typography.titleSmall)
                    PrivateListeningWorksheetAudience.entries.forEach { audience ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !guidedActive,
                            onClick = { worksheetAudience = audience }
                        ) {
                            Text((if (worksheetAudience == audience) "● " else "○ ") +
                                audience.title)
                        }
                    }
                    Text(
                        if (worksheetAudience == PrivateListeningWorksheetAudience.STUDENT_HANDOUT)
                            "Leerlingenbestand: bevat alleen de eigen A–B-tijdvakken " +
                                "en invulvakken, geen bronregels of persoonlijke " +
                                "notities/beoordelingen. Controleer de fragmenttijden " +
                                "voordat je het bestand deelt."
                        else "Zelfstudie: tik expliciet om bronregels te onthullen. " +
                            "Het bronregelblad wordt uitsluitend op een aparte " +
                            "pagina afgedrukt als je het afdrukvakje vooraf aanvinkt.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        enabled = savedPracticePassages.isNotEmpty() && !guidedActive &&
                            privatePracticeRouteCandidates(
                                savedPracticePassages, passageRatings, worksheetScope
                            ).isNotEmpty(),
                        onClick = {
                            choosePracticeReport(PrivatePracticeReportFormat.WORKSHEET_HTML)
                        }
                    ) {
                        Text(if (worksheetAudience == PrivateListeningWorksheetAudience.STUDENT_HANDOUT)
                            "Bewaar leerlingenblad zonder bronregels…"
                        else "Bewaar luisterwerkblad · vragen en aparte bronregels…")
                    }
                    TextButton(
                        enabled = savedPracticePassages.isNotEmpty() && !guidedActive,
                        onClick = { choosePracticeReport(PrivatePracticeReportFormat.TXT) }
                    ) { Text("Bewaar als privé TXT-notitie…") }
                    TextButton(
                        enabled = savedPracticePassages.isNotEmpty() && !guidedActive,
                        onClick = { practiceCsvPreviewOpen = !practiceCsvPreviewOpen }
                    ) {
                        Text(if (practiceCsvPreviewOpen) "Verberg volledige CSV-inhoud"
                            else "Bekijk mijn volledige CSV vóór opslaan")
                    }
                    if (practiceCsvPreviewOpen) {
                        Text("CSV-selectie: " + worksheetScope.title + " (" +
                            privatePracticeRouteCandidates(
                                savedPracticePassages, passageRatings, worksheetScope
                            ).size + " fragmenten).",
                            style = MaterialTheme.typography.bodySmall)
                        val csvPreview = PrivatePrintablePracticeSummary.snapshotCsv(
                            cues, subtitleOffsetMs, savedPracticePassages,
                            passageRatings, passageNotes, worksheetScope
                        )?.toString(Charsets.UTF_8)
                        SelectionContainer {
                            Text(
                                csvPreview ?: "Er is geen geldige CSV om te bewaren.",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    TextButton(
                        enabled = !guidedActive &&
                            privatePracticeRouteCandidates(
                                savedPracticePassages, passageRatings, worksheetScope
                            ).isNotEmpty(),
                        onClick = { choosePracticeReport(PrivatePracticeReportFormat.CSV) }
                    ) { Text("Bewaar mijn geselecteerde oefenfragmenten als CSV…") }
                    SelectionContainer {
                        Text(
                            practiceSummaryText,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { practiceReportOpen = false }) {
                    Text("Sluit zonder opslaan")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = savedPracticePassages.isNotEmpty() && !guidedActive,
                    onClick = { choosePracticeReport(PrivatePracticeReportFormat.HTML) }
                ) { Text("Bewaar zelf HTML…") }
            }
        )
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

                    Text("Selecteer hier eerst A (eerste regel), dan B (dezelfde of een latere). " +
                        "Je bekijkt de volledige bronpassage vóór je deze bewust bewaart.",
                        style = MaterialTheme.typography.bodySmall)
                    if (rangeStartIndex >= 0) {
                        val first = cues.getOrNull(rangeStartIndex)
                        val last = cues.getOrNull(rangeEndIndex)
                        Text("Mijn A-start: regel ${rangeStartIndex + 1} · " +
                            (first?.text?.take(70) ?: "?") +
                            if (last == null) " · kies nog B-einde" else
                                " · B-einde: regel ${rangeEndIndex + 1} · " +
                                    last.text.take(70),
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDirection = TextDirection.ContentOrRtl))
                        val bounds = rangeBounds()
                        if (bounds != null) {
                            Text("Volledige passage: ${rangeEndIndex - rangeStartIndex + 1} regels; " +
                                "${clock(bounds.first)} tot ${clock(bounds.second)} " +
                                "(0,75 seconde–5 minuten). Geen automatische synchronisatie.",
                                style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                enabled = !guidedActive && savedPracticePassages.size < 4,
                                onClick = {
                                    saveCueRangeForPractice()
                                    // When invalid due to an existing nearby audio marker,
                                    // keep search open so the refusal remains visible.
                                    if (bounds in savedPracticePassages) cuePickerOpen = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("＋ Bewaar deze bekeken A–B-passage als oefenfragment")
                            }
                        } else if (last != null) {
                            Text("Deze selectie heeft ongeldige of te lange ondertiteltijden; " +
                                "kies een kleinere, volledige passage.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
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


                                OutlinedButton(
                                    enabled = !guidedActive && cue.text.isNotBlank() &&
                                        chosenSubtitleLanguage in setOf("kmr", "ckb", "ps"),
                                    onClick = {
                                        cuePickerOpen = false
                                        openPersonalSubtitlePhrase(cue)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("＋ Regel ${index + 1} als eigen zin met betekenis bewaren")
                                }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        enabled = !guidedActive && cuePlayable(cue),
                                        onClick = {
                                            rangeStartIndex = index
                                            rangeEndIndex = -1
                                            rangeToolsOpen = true
                                            rangeRepeating = false
                                            bookmarkRepeating = false
                                            loopCueIndex = -1
                                            confirmClipOpen = false
                                            notice = "Begin A gekozen op regel ${index + 1}; kies B in dezelfde zoeklijst."
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (rangeStartIndex == index) "✓ A-start" else "Kies A-start",
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(
                                        enabled = !guidedActive && rangeStartIndex >= 0 &&
                                            index >= rangeStartIndex && cuePlayable(cue),
                                        onClick = {
                                            rangeEndIndex = index
                                            rangeToolsOpen = true
                                            rangeRepeating = false
                                            bookmarkRepeating = false
                                            loopCueIndex = -1
                                            confirmClipOpen = false
                                            notice = "Einde B gekozen op regel ${index + 1}; " +
                                                "bekijk de passage bovenaan vóór bewaren."
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (rangeEndIndex == index && index >= rangeStartIndex)
                                            "✓ B-einde" else "Kies B-einde",
                                            style = MaterialTheme.typography.labelSmall)
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
