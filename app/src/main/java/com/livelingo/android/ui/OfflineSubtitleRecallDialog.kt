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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue

/**
 * Temporary practice of text from an explicitly opened subtitle document.
 * No semantic translation, AI grading, speech recording, network or new
 * persistent memory. The user is responsible for the original text.
 */
@Composable
internal fun OfflineSubtitleRecallDialog(
    selected: List<PrivateSubtitleCue>,
    onDismiss: () -> Unit
) {
    val initial = remember(selected) { selected.take(24).toList() }
    var round by remember(selected) { mutableStateOf(initial) }
    var position by remember(selected) { mutableIntStateOf(0) }
    var revealed by remember(selected) { mutableStateOf(false) }
    var missed by remember(selected) { mutableStateOf<List<PrivateSubtitleCue>>(emptyList()) }
    var remembered by remember(selected) { mutableIntStateOf(0) }
    var skipped by remember(selected) { mutableIntStateOf(0) }
    var retry by remember(selected) { mutableStateOf(false) }
    val cue = round.getOrNull(position)

    fun next(knew: Boolean?) {
        val current = cue ?: return
        when (knew) {
            true -> remembered++
            false -> missed = missed + current
            null -> skipped++
        }
        position++
        revealed = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline ondertitel-oefenkaarten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Text(
                    "Tijdelijke oefening met maximaal 24 regels uit het door jou " +
                        "geopende SRT/VTT-bestand. Eerst een beginstuk zien, " +
                        "de rest bedenken en alleen na tik de volledige tekst tonen.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (cue == null) {
                    Text(
                        if (round.isEmpty()) "Geen leesbare regels gekozen."
                        else "Ronde voltooid.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${remembered} keer zelf herkend · ${missed.size} lastig · " +
                            "${skipped} overgeslagen. Dit is geen taaltoets of " +
                            "onafhankelijk gecontroleerd resultaat.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (missed.isNotEmpty()) {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            round = missed.take(24).toList()
                            missed = emptyList()
                            position = 0
                            revealed = false
                            retry = true
                        }) { Text("Oefen alleen de lastige regels opnieuw") }
                    }
                    if (retry) Text(
                        "Deze herhaling en alle tellingen verdwijnen zodra je sluit.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        round = initial
                        position = 0
                        revealed = false
                        missed = emptyList()
                        remembered = 0
                        skipped = 0
                        retry = false
                    }) { Text("Begin opnieuw met dezelfde selectie") }
                } else {
                    Text(
                        "Regel ${position + 1} van ${round.size} · " +
                            "${cue.startLabel} → ${cue.endLabel}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    HorizontalDivider()
                    val words = cue.text.trim().split(Regex("\\s+"))
                    val head = if (words.size >= 3) {
                        words.take((words.size / 3).coerceAtLeast(1)).joinToString(" ")
                    } else ""
                    Text(
                        if (head.isNotEmpty()) "${head} …"
                        else "Denk aan de volledige ondertitel bij deze tijdcode.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = TextDirection.ContentOrRtl
                        )
                    )
                    if (!revealed) {
                        Button(modifier = Modifier.fillMaxWidth(),
                            onClick = { revealed = true }) {
                            Text("Onthul volledige originele tekst")
                        }
                    } else {
                        Text(
                            cue.text,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = TextDirection.ContentOrRtl
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Alleen jouw eigen inschatting; deze ondertitel is " +
                                "niet onafhankelijk vertaald of gecontroleerd.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OutlinedButton(modifier = Modifier.weight(1f),
                                onClick = { next(false) }) { Text("Nog oefenen") }
                            Button(modifier = Modifier.weight(1f),
                                onClick = { next(true) }) { Text("Herinnerd") }
                        }
                    }
                    TextButton(onClick = { next(null) }) {
                        Text("Sla deze regel over (geen beoordeling)")
                    }
                }
                Text(
                    "Er wordt geen tekst, score, microfoondata of oefenhistorie " +
                        "bewaard. Geen internet of modelaanroep; tijdcodes komen " +
                        "uit je eigen bestand en zijn niet met audio gecontroleerd.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten · wis oefenronde") }
        }
    )
}
