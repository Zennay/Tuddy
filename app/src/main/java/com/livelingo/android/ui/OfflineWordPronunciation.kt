package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

/**
 * Word-card-only, user-triggered pronunciation. Composed only while the
 * offline word dialog is visible; no TTS objects retained for transcript rows.
 * Uses the shared installed/non-network voice policy and shuts down on pause.
 */
@Composable
internal fun OfflineWordPronunciation(word: String, language: String?) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    var notice by remember { mutableStateOf("") }
    DisposableEffect(owner, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    LaunchedEffect(word, language) {
        // Editing the word or its language never keeps pronouncing old text.
        speaker.stop()
        notice = ""
    }
    val playable = language != null && word.isNotBlank() && word.length <= 40
    Column(verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                modifier = Modifier.weight(1f),
                enabled = playable,
                onClick = {
                    speaker.speak(word, language.orEmpty(), LocalSpeechPace.NORMAL) {
                        notice = it
                    }
                }
            ) { Text("🔊 Lees woord") }
            TextButton(
                modifier = Modifier.weight(1f),
                enabled = playable,
                onClick = {
                    speaker.speak(word, language.orEmpty(), LocalSpeechPace.SLOW) {
                        notice = it
                    }
                }
            ) { Text("Rustig") }
            TextButton(onClick = {
                speaker.stop()
                notice = "Lokale voorlezing gestopt."
            }) { Text("■ Stop") }
        }
        if (notice.isNotBlank()) {
            Text(notice, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Alleen na tik met een bestaande, door Android als lokaal " +
                "gemelde stem. Geen stemdownload of cloudspraak; " +
                "uitspraak is niet onafhankelijk gecontroleerd.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
