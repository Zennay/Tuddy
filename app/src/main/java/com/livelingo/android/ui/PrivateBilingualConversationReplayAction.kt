package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livelingo.android.BilingualReplayCaption
import com.livelingo.android.BilingualReplayOrder
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.PrivateBilingualConversationReplay

/**
 * Kept outside TuddyScreen's bytecode-heavy Compose body.
 * Uses the same private TTS engine as ordinary captions, so the screen's
 * microphone Start/Stop immediately ends any replay before live capture.
 * The only independent state is a transient user-selected replay length.
 */
@Composable
internal fun PrivateBilingualConversationReplayAction(
    captions: List<BilingualReplayCaption>,
    sourceCode: String,
    targetCode: String,
    speaker: LocalCaptionSpeaker,
    enabled: Boolean
) {
    var notice by remember { mutableStateOf("") }
    var selectedCount by remember(sourceCode, targetCode) { mutableStateOf(3) }
    var selectedOrder by remember(sourceCode, targetCode) {
        mutableStateOf(BilingualReplayOrder.SOURCE_FIRST)
    }
    var selectedStart by remember(sourceCode, targetCode) { mutableStateOf<Int?>(null) }
    var choosingStart by remember(sourceCode, targetCode) { mutableStateOf(false) }
    // The Activity owns and closes this engine on background/microphone Stop.
    // When this tool is hidden, cancel only playback, never close its owner.
    DisposableEffect(speaker) {
        onDispose { speaker.stop() }
    }
    val eligible = PrivateBilingualConversationReplay.available(
        captions, sourceCode, targetCode
    )
    // A deleted or revised caption can shrink the available snapshot while
    // this control remains composed. Never claim a stale start still exists.
    val currentStart = selectedStart?.takeIf { it in eligible.indices }
    val practice = PrivateBilingualConversationReplay.window(
        eligible, selectedCount, currentStart
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
                "Kies zelf origineel of vertaling eerst. " +
                "Vereist twee geïnstalleerde offline Android-stemmen. " +
                "Microfoonstart en verlaten van deze tools stoppen de replay.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Kies hoeveel afgeronde zinnen je wilt oefenen " +
                "(beschikbaar: ${eligible.size}).",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (count in listOf(1, 3, 5, 8)) {
                FilterChip(
                    selected = selectedCount == count,
                    modifier = Modifier.semantics {
                        contentDescription = "Laatste $count zinnen oefenen"
                    },
                    onClick = {
                        speaker.stop()
                        selectedCount = count
                        notice = "Aantal oefenzinnen gekozen: " +
                            "${minOf(count, eligible.size)}. Tik op afspelen."
                    },
                    enabled = enabled && eligible.isNotEmpty(),
                    label = { Text("${count}") }
                )
            }
        }
        Text(
            "Oefenbereik: ${practice.size} tweetalige zinnen. " +
                (if (currentStart == null)
                    "Standaard vanaf de laatste ${minOf(selectedCount, eligible.size)} zinnen."
                else "Vanaf gesprekszin ${currentStart + 1} van ${eligible.size}."),
            style = MaterialTheme.typography.bodySmall
        )
        Box {
            TextButton(
                enabled = enabled && eligible.isNotEmpty(),
                onClick = { choosingStart = true }
            ) {
                Text(if (currentStart == null) "Kies startzin · laatste eerst"
                     else "Kies startzin · zin ${currentStart + 1}")
            }
            DropdownMenu(
                expanded = choosingStart,
                onDismissRequest = { choosingStart = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Automatisch · laatste zinnen") },
                    onClick = {
                        speaker.stop()
                        selectedStart = null
                        choosingStart = false
                        notice = "Standaard de laatste zinnen gekozen."
                    }
                )
                eligible.forEachIndexed { index, pair ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Zin ${index + 1} · ${pair.original.take(48)}" +
                                    if (pair.original.length > 48) "…" else "",
                                maxLines = 1
                            )
                        },
                        onClick = {
                            speaker.stop()
                            selectedStart = index
                            choosingStart = false
                            notice = "Oefenreeks start bij zin ${index + 1}."
                        }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedOrder == BilingualReplayOrder.SOURCE_FIRST,
                enabled = enabled && practice.isNotEmpty(),
                onClick = {
                    speaker.stop()
                    selectedOrder = BilingualReplayOrder.SOURCE_FIRST
                    notice = "Oefenvolgorde: eerst origineel, dan de vertaling."
                },
                label = { Text("Origineel eerst") }
            )
            FilterChip(
                selected = selectedOrder == BilingualReplayOrder.TRANSLATION_FIRST,
                enabled = enabled && practice.isNotEmpty(),
                onClick = {
                    speaker.stop()
                    selectedOrder = BilingualReplayOrder.TRANSLATION_FIRST
                    notice = "Oefenvolgorde: eerst vertaling, dan het origineel."
                },
                label = { Text("Vertaling eerst") }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (pace in listOf(LocalSpeechPace.NORMAL, LocalSpeechPace.SLOW)) {
                TextButton(
                    enabled = enabled && practice.isNotEmpty(),
                    onClick = {
                        if (enabled && practice.isNotEmpty()) {
                            speaker.speakBilingualConversation(
                                practice, sourceCode, targetCode, pace,
                                onProgress = { done, total ->
                                    notice = "Tweetalige oefening: $done/$total zinnen voltooid."
                                },
                                feedback = { notice = it },
                                onFinished = { complete ->
                                    if (!complete && notice.isBlank()) {
                                        notice = "Voorlezen gestopt."
                                    }
                                },
                                order = selectedOrder
                            )
                        }
                    }
                ) {
                    Text(if (pace == LocalSpeechPace.NORMAL) "▶ Oefen zinnen" else "▶ Rustig")
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
