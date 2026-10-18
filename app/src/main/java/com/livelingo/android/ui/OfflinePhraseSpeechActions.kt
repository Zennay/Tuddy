package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

/**
 * Explicit, disposable on-device pronunciation for one visible phrase card.
 *
 * The speaker only admits a voice Android reports as installed/non-network.
 * Neither this UI nor the speaker attempts voice installation or uses a
 * cloud speech service. Meanings and translations are not verified.
 */
@Composable
internal fun OfflinePhraseSpeechActions(
    original: String,
    originalLanguage: String,
    meaning: String,
    meaningLanguage: String,
    originalLabel: String,
    meaningLabel: String,
    speaker: LocalCaptionSpeaker,
    onNotice: (String) -> Unit
) {
    val originalPlayable = original.isNotBlank() && original.length <= 400
    val meaningPlayable = meaning.isNotBlank() && meaning.length <= 400
    Column(modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                enabled = originalPlayable,
                modifier = Modifier.weight(1f),
                onClick = {
                    speaker.speak(original, originalLanguage, LocalSpeechPace.NORMAL, onNotice)
                }
            ) { Text("🔊 $originalLabel") }
            TextButton(
                enabled = meaningPlayable,
                modifier = Modifier.weight(1f),
                onClick = {
                    speaker.speak(meaning, meaningLanguage, LocalSpeechPace.NORMAL, onNotice)
                }
            ) { Text("🔊 $meaningLabel") }
        }
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                enabled = meaningPlayable,
                modifier = Modifier.weight(1f),
                onClick = {
                    speaker.speak(meaning, meaningLanguage, LocalSpeechPace.SLOW, onNotice)
                }
            ) { Text("Rustig $meaningLabel") }
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    speaker.stop()
                    onNotice("Lokale voorlezing gestopt.")
                }
            ) { Text("■ Stop") }
        }
    }
}
