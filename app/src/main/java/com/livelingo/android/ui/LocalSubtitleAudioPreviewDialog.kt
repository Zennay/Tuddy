package com.livelingo.android.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
    var notice by remember { mutableStateOf("Kies zelf audio bij deze ondertitels.") }

    fun closeAudio() {
        val old = player
        player = null
        prepared = false
        playing = false
        positionMs = 0L
        dragPositionMs = -1L
        durationMs = 0L
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

    fun seekTo(position: Long) {
        val current = player ?: return
        if (!prepared || durationMs <= 0L) return
        val target = position.coerceIn(0L, durationMs)
        runCatching { current.seekTo(target.toInt()) }
            .onSuccess { positionMs = target }
            .onFailure { notice = "Naar deze tijd springen is niet gelukt." }
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            closeAudio()
            if (uri.scheme != "content") {
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
                            playing = false
                            positionMs = durationMs
                        }
                    }
                    next.setOnErrorListener { failed, _, _ ->
                        if (player === failed) {
                            notice = "Dit audiobestand kan hier niet lokaal worden afgespeeld."
                            closeAudio()
                        }
                        true
                    }
                    next.setDataSource(context, uri)
                    player = next
                    notice = "Audio wordt geopend; afspelen start niet automatisch."
                    next.prepareAsync()
                }.onFailure {
                    if (player === next) closeAudio() else runCatching { next.release() }
                    notice = "Audio openen mislukt. Kies een ondersteund audiobestand."
                }
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

    LaunchedEffect(player, playing) {
        val current = player
        while (playing && current != null && player === current) {
            val now = runCatching { current.currentPosition.toLong() }.getOrNull()
            if (now != null) positionMs = now.coerceIn(0L, durationMs)
            delay(160L)
        }
    }

    val displayTimeMs = if (dragPositionMs >= 0L) dragPositionMs else positionMs
    val activeIndex = if (prepared) cues.indexOfLast {
        displayTimeMs >= it.startMillis && displayTimeMs < it.endMillis
    } else -1
    val active = cues.getOrNull(activeIndex)
    val nextIndex = cues.indexOfFirst { it.startMillis > positionMs + 250L }
    val previousIndex = cues.indexOfLast { it.startMillis < positionMs - 500L }

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
                    audioPicker.launch(arrayOf("audio/*"))
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
                        OutlinedButton(onClick = { seekTo(cues[previousIndex].startMillis) },
                            enabled = previousIndex >= 0) { Text("◀ Vorige") }
                        OutlinedButton(onClick = {
                            val current = player ?: return@OutlinedButton
                            if (playing) pauseAudio() else {
                                runCatching {
                                    current.start()
                                    playing = true
                                }.onFailure { notice = "Afspelen is niet gelukt." }
                            }
                        }) { Text(if (playing) "Ⅱ Pauze" else "▶ Afspelen") }
                        OutlinedButton(onClick = { seekTo(cues[nextIndex].startMillis) },
                            enabled = nextIndex >= 0) { Text("Volgende ▶") }
                    }
                    Text("Verschuiving? Pas de tijdcodes in de editor aan; " +
                        "de originele SRT/VTT wordt niet overschreven.",
                        style = MaterialTheme.typography.labelSmall)
                    HorizontalDivider()
                    Text(if (activeIndex >= 0)
                        "Regel ${activeIndex + 1} van ${cues.size} · " +
                            "${active?.startLabel} → ${active?.endLabel}"
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
}
