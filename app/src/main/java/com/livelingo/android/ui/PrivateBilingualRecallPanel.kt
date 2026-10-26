package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.livelingo.android.BilingualReplayPair
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

/**
 * An opt-in, entirely in-memory recall round over an existing bounded,
 * translated conversation snapshot. A learner sees the original phrase,
 * deliberately reveals the already-completed local translation, and marks
 * whether to try it again. Wrong answers are never sent to ML Kit or stored.
 *
 * The caller has already validated the pairs and both language codes. This
 * panel never mutates conversation rows or starts Android audio automatically.
 */
@Composable
internal fun PrivateBilingualRecallPanel(
    practice: List<BilingualReplayPair>,
    sourceCode: String,
    targetCode: String,
    speaker: LocalCaptionSpeaker,
    enabled: Boolean
) {
    // When a caption is edited/deleted or the selected language, start or
    // count changes, discard every old answer/review index and stop old audio.
    var started by remember(practice, sourceCode, targetCode) { mutableStateOf(false) }
    var revealed by remember(practice, sourceCode, targetCode) { mutableStateOf(false) }
    var position by remember(practice, sourceCode, targetCode) { mutableStateOf(0) }
    var missed by remember(practice, sourceCode, targetCode) {
        mutableStateOf(emptySet<Int>())
    }
    var retryOnly by remember(practice, sourceCode, targetCode) {
        mutableStateOf(false)
    }
    var retryIndices by remember(practice, sourceCode, targetCode) {
        mutableStateOf(emptyList<Int>())
    }
    var round by remember(practice, sourceCode, targetCode) { mutableStateOf(1) }
    var audioNotice by remember(practice, sourceCode, targetCode) {
        mutableStateOf("")
    }

    DisposableEffect(speaker, practice, sourceCode, targetCode) {
        onDispose { speaker.stop() }
    }

    val currentIndices = if (retryOnly) retryIndices else practice.indices.toList()
    val current = currentIndices.getOrNull(position)?.let(practice::get)
    val done = started && current == null && currentIndices.isNotEmpty()

    fun begin() {
        speaker.stop()
        started = true
        position = 0
        revealed = false
        missed = emptySet()
        retryOnly = false
        retryIndices = emptyList()
        round = 1
        audioNotice = ""
    }

    fun mark(tryAgain: Boolean) {
        val index = currentIndices.getOrNull(position) ?: return
        speaker.stop()
        if (tryAgain) missed = missed + index
        position++
        revealed = false
        audioNotice = ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HorizontalDivider()
        Text(
            "Zelf overhoren · eerst herinneren, daarna antwoord onthullen",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Dezelfde ${practice.size} afgeronde lokale vertaalparen, alleen in " +
                "het huidige RAM-gesprek. Jij bepaalt wanneer je kijkt of luistert. " +
                "Een fout antwoord verandert geen ondertitel, model of woordenlijst.",
            style = MaterialTheme.typography.bodySmall
        )
        if (!started) {
            OutlinedButton(
                onClick = ::begin,
                enabled = enabled && practice.isNotEmpty()
            ) { Text("Begin stille overhoorronde") }
        } else if (done) {
            Text(
                "Ronde $round voltooid · ${currentIndices.size - missed.size} goed " +
                    "· ${missed.size} nog oefenen.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (missed.isNotEmpty()) {
                OutlinedButton(
                    enabled = enabled,
                    onClick = {
                        speaker.stop()
                        retryIndices = missed.sorted()
                        retryOnly = true
                        missed = emptySet()
                        position = 0
                        revealed = false
                        audioNotice = ""
                        round++
                    }
                ) { Text("Oefen alleen gemiste zinnen opnieuw") }
            } else {
                Text(
                    "Alle gekozen zinnen zelf als goed beoordeeld. " +
                        "Dit is oefenvoortgang, geen onafhankelijke taaltoets.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = ::begin, enabled = enabled) {
                Text("Begin volledige ronde opnieuw")
            }
        } else if (current != null) {
            Text(
                "Ronde $round · zin ${position + 1}/${currentIndices.size}" +
                    " · ${sourceCode.uppercase()} → ${targetCode.uppercase()}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(current.original, style = MaterialTheme.typography.bodyLarge)
            if (!revealed) {
                Text(
                    "Denk eerst zelf aan de vertaling. De antwoordtekst blijft " +
                        "hier verborgen totdat je op Onthul tikt.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = {
                        speaker.stop()
                        revealed = true
                        audioNotice = ""
                    },
                    enabled = enabled,
                    modifier = Modifier.semantics {
                        contentDescription = "Onthul bestaande lokale vertaling voor zin ${position + 1}"
                    }
                ) { Text("Onthul vertaling") }
            } else {
                Text(
                    "Bestaande lokale vertaling · kan fouten bevatten",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(current.translated, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { mark(false) }, enabled = enabled) {
                        Text("Zelf goed")
                    }
                    TextButton(onClick = { mark(true) }, enabled = enabled) {
                        Text("Nog oefenen")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = enabled,
                    onClick = {
                        speaker.speak(
                            current.original, sourceCode, LocalSpeechPace.NORMAL
                        ) { audioNotice = it }
                    }
                ) { Text("▶ Luister origineel") }
                if (revealed) {
                    TextButton(
                        enabled = enabled,
                        onClick = {
                            speaker.speak(
                                current.translated, targetCode, LocalSpeechPace.NORMAL
                            ) { audioNotice = it }
                        }
                    ) { Text("▶ Luister antwoord") }
                }
            }
            if (audioNotice.isNotBlank()) {
                Text(audioNotice, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = ::begin, enabled = enabled) {
                Text("Stop en begin opnieuw")
            }
        }
    }
}
