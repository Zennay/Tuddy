package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
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
 * User-triggered audio for one completed/unfinished temporary two-way turn.
 * Reuses LocalCaptionSpeaker's installed non-network voice requirement.
 * Never starts a download, remote TTS, sharing or automatic transcript save.
 */
@Composable
internal fun OfflineReadingTurnAudioActions(
    turn: TypedConversationLine,
    speaker: LocalCaptionSpeaker,
    onNotice: (String) -> Unit
) {
    val playableOriginal = turn.original.isNotBlank() && turn.original.length <= 400
    val playableTranslation = turn.status == "Lokaal vertaald" &&
        turn.translated.isNotBlank() && turn.translated.length <= 400
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            enabled = playableOriginal,
            onClick = {
                speaker.speak(turn.original, turn.source, LocalSpeechPace.NORMAL, onNotice)
            },
            modifier = Modifier.weight(1f)
        ) { Text("🔊 Origineel") }
        TextButton(
            enabled = playableTranslation,
            onClick = {
                speaker.speak(turn.translated, turn.target, LocalSpeechPace.NORMAL, onNotice)
            },
            modifier = Modifier.weight(1f)
        ) { Text("🔊 Vertaling") }
        TextButton(
            enabled = playableTranslation,
            onClick = {
                speaker.speak(turn.translated, turn.target, LocalSpeechPace.SLOW, onNotice)
            },
            modifier = Modifier.weight(1f)
        ) { Text("Rustiger") }
    }
}
