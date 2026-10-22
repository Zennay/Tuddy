package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.SessionTranslationCorrection

/**
 * Voluntary, non-persistent recall practice using only corrections the user
 * consciously authored. Never infers accuracy, trains a model or exports text.
 * The response stays concealed until the user explicitly reveals it.
 */
@Composable
internal fun SessionCorrectionPracticeDialog(
    entries: List<SessionTranslationCorrection>,
    speaker: LocalCaptionSpeaker,
    onDismiss: () -> Unit
) {
    val snapshot = remember { entries.asReversed().toList() }
    var offset by remember { mutableIntStateOf(0) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var recalled by remember { mutableIntStateOf(0) }
    var toRepeat by remember { mutableIntStateOf(0) }
    var speechMessage by remember { mutableStateOf("") }
    val batch = snapshot.drop(offset).take(12)
    val current = batch.getOrNull(currentIndex)
    DisposableEffect(speaker) { onDispose { speaker.stop() } }

    fun advance(known: Boolean) {
        speaker.stop()
        if (known) recalled++ else toRepeat++
        currentIndex++
        revealed = false
        speechMessage = ""
    }

    AlertDialog(
        onDismissRequest = { speaker.stop(); onDismiss() },
        title = { Text("Oefen mijn eigen correcties") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Dit zijn uitsluitend eigen, tijdelijke vertaalvoorstellen; " +
                        "geen bewezen woordenboek of taalmodel. " +
                        "Je antwoorden worden nergens bewaard.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (snapshot.isEmpty()) {
                    Text("Er zijn nog geen eigen correcties om te oefenen.")
                } else if (current == null) {
                    Text(
                        "Deze ronde voltooid: " + recalled + " zelf herkend, " +
                            toRepeat + " nog oefenen.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val hasNextBatch = offset + batch.size < snapshot.size
                    Button(
                        onClick = {
                            speaker.stop()
                            offset = if (hasNextBatch) offset + batch.size else 0
                            currentIndex = 0
                            recalled = 0
                            toRepeat = 0
                            revealed = false
                            speechMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasNextBatch) "Volgende eigen correcties"
                            else "Opnieuw beginnen")
                    }
                } else {
                    Text(
                        "Kaart " + (offset + currentIndex + 1) + " van " +
                            snapshot.size + " · " + current.sourceCode +
                            " → " + current.targetCode,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(current.original, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        speaker.speak(
                            current.original, current.sourceCode, LocalSpeechPace.NORMAL
                        ) { speechMessage = it }
                    }) { Text("🔊 Beluister bronzin lokaal") }
                    if (!revealed) {
                        Text(
                            "Probeer de vertaling zelf te herinneren. Tik pas daarna " +
                                "om jouw eerder opgeslagen antwoord te zien.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { speaker.stop(); revealed = true; speechMessage = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Toon mijn vertaling") }
                    } else {
                        Text(
                            current.translation,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = {
                            speaker.speak(
                                current.translation, current.targetCode, LocalSpeechPace.SLOW
                            ) { speechMessage = it }
                        }) { Text("🔊 Beluister vertaling rustig lokaal") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { advance(false) }) {
                                Text("Nog oefenen")
                            }
                            Button(onClick = { advance(true) }) { Text("Ik wist het") }
                        }
                    }
                    if (speechMessage.isNotBlank()) {
                        Text(speechMessage, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { speaker.stop(); onDismiss() }) { Text("Sluiten") }
        }
    )
}
