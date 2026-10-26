package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.PrivateRecallAuditoryComparison

/**
 * Only mount this AFTER the learner explicitly reveals the reference.
 * The learner selects an existing typed or locally recognized attempt and
 * deliberately requests two local TTS utterances in the answer language:
 * their own attempt, then the revealed reference.
 *
 * This panel has no persistent state, model download, recording or grading.
 * The parent screen stops [speaker] on background, new card or editing.
 */
@Composable
internal fun PrivateRecallAuditoryComparisonPanel(
    typedAttempt: String,
    spokenAttempt: String,
    revealedReference: String,
    answerLanguageTag: String,
    speaker: LocalCaptionSpeaker,
    enabled: Boolean,
    onNotice: (String) -> Unit
) {
    var chosen by remember(typedAttempt, spokenAttempt, revealedReference) {
        mutableStateOf(if (typedAttempt.isNotBlank()) "typed" else "spoken")
    }
    var pace by remember { mutableStateOf(LocalSpeechPace.NORMAL) }
    val available = listOfNotNull(
        "typed".takeIf { typedAttempt.isNotBlank() },
        "spoken".takeIf { spokenAttempt.isNotBlank() }
    )
    if (available.isEmpty()) return
    val selected = if (chosen in available) chosen else available.first()
    val answer = if (selected == "typed") typedAttempt else spokenAttempt
    val plan = PrivateRecallAuditoryComparison.twoUtterances(
        answer, revealedReference
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HorizontalDivider()
        Text(
            "Luister naar mijn poging en de referentie",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Eerst klinkt jouw bestaande poging, daarna de al onthulde referentie, " +
                "beide in de antwoordtaal. Vergelijk zelf; uitspraak en betekenis " +
                "worden niet automatisch beoordeeld of opgeslagen.",
            style = MaterialTheme.typography.bodySmall
        )
        if (available.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selected == "typed",
                    enabled = enabled,
                    onClick = {
                        speaker.stop()
                        chosen = "typed"
                        onNotice("")
                    },
                    label = { Text("Getypt") }
                )
                FilterChip(
                    selected = selected == "spoken",
                    enabled = enabled,
                    onClick = {
                        speaker.stop()
                        chosen = "spoken"
                        onNotice("")
                    },
                    label = { Text("Lokaal herkende spraak") }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pace == LocalSpeechPace.NORMAL,
                enabled = enabled,
                onClick = {
                    speaker.stop()
                    pace = LocalSpeechPace.NORMAL
                    onNotice("")
                },
                label = { Text("Normaal") }
            )
            FilterChip(
                selected = pace == LocalSpeechPace.SLOW,
                enabled = enabled,
                onClick = {
                    speaker.stop()
                    pace = LocalSpeechPace.SLOW
                    onNotice("")
                },
                label = { Text("Langzaam") }
            )
        }
        OutlinedButton(
            enabled = enabled && plan != null,
            onClick = {
                val current = PrivateRecallAuditoryComparison.twoUtterances(
                    if (selected == "typed") typedAttempt else spokenAttempt,
                    revealedReference
                ) ?: return@OutlinedButton
                onNotice("Lokale Android-voorleesstem controleren…")
                speaker.speakSequence(
                    lines = current,
                    languageTag = answerLanguageTag,
                    pace = pace,
                    onProgress = { completed, total ->
                        onNotice(
                            if (completed < total)
                                "Mijn poging afgespeeld · referentie volgt."
                            else "Mijn poging en referentie afgespeeld."
                        )
                    },
                    feedback = onNotice,
                    onFinished = { completed ->
                        if (!completed) {
                            onNotice("Voorlezen gestopt. Geen online fallback.")
                        }
                    },
                    onLineStarted = { index ->
                        onNotice(
                            if (index == 0) "Mijn poging · lokale Android-stem."
                            else "Onthulde referentie · dezelfde lokale Android-stem."
                        )
                    }
                )
            }
        ) { Text("▶ Speel poging → referentie lokaal af") }
        TextButton(onClick = {
            speaker.stop()
            onNotice("Lokaal voorlezen gestopt.")
        }, enabled = enabled) { Text("■ Stop vergelijking") }
        if (plan == null) {
            Text(
                "Een van de twee zinnen is te lang of bevat onveilige tekens " +
                    "voor de offline Android-stem. Geen audio gestart.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
