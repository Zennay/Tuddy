package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleRetiming
import com.livelingo.android.PrivateSubtitleTwoAnchorTimeline

/** Retime with two human-specified start anchors; parent manages undo and SAF. */
@Composable
internal fun SubtitleTwoAnchorTimelineDialog(
    draft: List<PrivateSubtitleCue>,
    onApply: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    val first = draft.firstOrNull() ?: return
    val last = draft.lastOrNull() ?: return
    var desiredFirst by remember(draft) {
        mutableStateOf(PrivateSubtitleRetiming.label(first.startMillis))
    }
    var desiredLast by remember(draft) {
        mutableStateOf(PrivateSubtitleRetiming.label(last.startMillis))
    }
    val proposal = PrivateSubtitleTwoAnchorTimeline.preview(
        draft, desiredFirst, desiredLast
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tijdlijn corrigeren met twee ankers") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Vul handmatig de gewenste starttijd van de eerste en " +
                    "laatste ondertitel in. Tuddy schaalt alle tussentijden " +
                    "en regelduren proportioneel. Geen automatische audio/video-analyse.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Eerste huidige start: " +
                    PrivateSubtitleRetiming.label(first.startMillis))
                OutlinedTextField(
                    value = desiredFirst,
                    onValueChange = { desiredFirst = it.take(13) },
                    label = { Text("Gewenste eerste start · uu:mm:ss,mmm") },
                    singleLine = true
                )
                Text("Laatste huidige start: " +
                    PrivateSubtitleRetiming.label(last.startMillis))
                OutlinedTextField(
                    value = desiredLast,
                    onValueChange = { desiredLast = it.take(13) },
                    label = { Text("Gewenste laatste start · uu:mm:ss,mmm") },
                    singleLine = true
                )
                if (proposal == null) {
                    Text("Geen geldige aanpassing. Geef twee verschillende " +
                        "oplopende tijdstippen op; ieder resulterend tijdvak " +
                        "moet geldig blijven binnen de bestaande SRT/VTT-grenzen.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Voorbeeld · " + proposal.cues.size + " regels",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Eerste regel: " +
                        PrivateSubtitleRetiming.label(proposal.originalFirst) + " → " +
                        PrivateSubtitleRetiming.label(proposal.proposedFirst))
                    Text("Laatste regel: " +
                        PrivateSubtitleRetiming.label(proposal.originalLast) + " → " +
                        PrivateSubtitleRetiming.label(proposal.proposedLast))
                    Text("Overlappende buurregels: " + proposal.overlapPairs)
                    Text("Laatste eindtijd: " +
                        PrivateSubtitleRetiming.label(proposal.cues.last().endMillis))
                }
                Text("Het brondocument verandert niet. Toepassen is één " +
                    "herstelbare stap in je tijdelijke editorconcept; " +
                    "een nieuw bestand vereist apart Bewaar kopie…",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = proposal != null, onClick = {
                PrivateSubtitleTwoAnchorTimeline.preview(
                    draft, desiredFirst, desiredLast
                )?.let { onApply(it.cues) }
            }) { Text("Pas beide ankers toe") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}
