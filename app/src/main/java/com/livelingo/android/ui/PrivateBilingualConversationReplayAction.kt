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
import com.livelingo.android.BilingualReplayCaption
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.PrivateBilingualConversationReplay

/**
 * Isolated Compose/action lifecycle to keep both TuddyScreen's bytecode and
 * the Activity's existing microphone cleanup policy unchanged.
 * Holds only a private RAM-only practice selection and ephemeral TTS engine.
 */
@Composable
internal fun PrivateBilingualConversationReplayAction(
    captions: List<BilingualReplayCaption>,
    sourceCode: String,
    targetCode: String,
    enabled: Boolean
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    var notice by remember { mutableStateOf("") }
    DisposableEffect(owner, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) speaker.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    val eligible = PrivateBilingualConversationReplay.available(
        captions, sourceCode, targetCode
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Tweetalig terugluisteren · ${sourceCode.uppercase()} → " +
                "${targetCode.uppercase()} · ${eligible.size} afgeronde zinnen (maximaal 8)",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Alleen bestaande lokale vertalingen, op verzoek. " +
                "Eerst het origineel, dan de vertaling. " +
                "Vereist twee geïnstalleerde offline Android-stemmen.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (pace in listOf(LocalSpeechPace.NORMAL, LocalSpeechPace.SLOW)) {
                TextButton(
                    enabled = enabled && eligible.isNotEmpty(),
                    onClick = {
                        if (enabled) {
                            speaker.speakBilingualConversation(
                                eligible, sourceCode, targetCode, pace,
                                onProgress = { done, total ->
                                    notice = "Tweetalige oefening: $done/$total zinnen voltooid."
                                },
                                feedback = { notice = it },
                                onFinished = { complete ->
                                    if (!complete && notice.isBlank()) {
                                        notice = "Voorlezen gestopt."
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Text(if (pace == LocalSpeechPace.NORMAL) "▶ Tweetalig" else "▶ Rustig")
                }
            }
        }
        TextButton(onClick = {
            speaker.stop()
            notice = "Tweetalig voorlezen gestopt."
        }) { Text("■ Stop tweetalig voorlezen") }
        if (notice.isNotBlank()) {
            Text(notice, style = MaterialTheme.typography.bodySmall)
        }
    }
}
