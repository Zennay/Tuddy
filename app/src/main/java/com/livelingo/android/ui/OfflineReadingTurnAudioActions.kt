package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalSpeechPace

/**
 * User-triggered audio for one completed/unfinished temporary two-way turn.
 * The caller owns ephemeral follow-along state and uses LocalCaptionSpeaker's
 * installed non-network voice requirement. Never starts a download, remote
 * TTS, sharing or automatic transcript save.
 */
@Composable
internal fun OfflineReadingTurnAudioActions(
    turn: TypedConversationLine,
    activeSpeechText: String?,
    onSpeak: (text: String, language: String, side: String, pace: LocalSpeechPace) -> Unit
) {
    val playableOriginal = turn.original.isNotBlank() && turn.original.length <= 400
    val playableTranslation = turn.status == "Lokaal vertaald" &&
        turn.translated.isNotBlank() && turn.translated.length <= 400
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            enabled = activeSpeechText == null && playableOriginal,
            onClick = {
                onSpeak(turn.original, turn.source, "original", LocalSpeechPace.NORMAL)
            },
            modifier = Modifier.weight(1f)
        ) { Text("🔊 Origineel") }
        TextButton(
            enabled = activeSpeechText == null && playableTranslation,
            onClick = {
                onSpeak(turn.translated, turn.target, "translated", LocalSpeechPace.NORMAL)
            },
            modifier = Modifier.weight(1f)
        ) { Text("🔊 Vertaling") }
        TextButton(
            enabled = activeSpeechText == null && playableTranslation,
            onClick = {
                onSpeak(turn.translated, turn.target, "translated", LocalSpeechPace.SLOW)
            },
            modifier = Modifier.weight(1f)
        ) { Text("Rustiger") }
    }
}
