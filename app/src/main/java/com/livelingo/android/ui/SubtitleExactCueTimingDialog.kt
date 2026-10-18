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
import com.livelingo.android.ExactCueTimingMode
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleExactTiming
import com.livelingo.android.PrivateSubtitleRetiming

/** Deliberate editor operation: parent owns single-step undo and separate SAF export. */
@Composable
internal fun SubtitleExactCueTimingDialog(
    draft: List<PrivateSubtitleCue>,
    selectedIndex: Int,
    onApply: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = draft.getOrNull(selectedIndex) ?: return
    var typedStart by remember(selectedIndex, selected.startMillis) {
        mutableStateOf(PrivateSubtitleRetiming.label(selected.startMillis))
    }
    var typedEnd by remember(selectedIndex, selected.endMillis) {
        mutableStateOf(PrivateSubtitleRetiming.label(selected.endMillis))
    }
    var mode by remember(selectedIndex) {
        mutableStateOf(ExactCueTimingMode.ONLY_SELECTED)
    }
    val preview = PrivateSubtitleExactTiming.preview(
        draft, selectedIndex, typedStart, typedEnd, mode
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exacte tijdcodes · regel " + (selectedIndex + 1)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Voer zelf begin en einde tot op een milliseconde in. " +
                    "Er wordt geen audio geanalyseerd of vertaling gecontroleerd.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = typedStart, onValueChange = { typedStart = it.take(13) },
                    label = { Text("Begin · uu:mm:ss,mmm") }, singleLine = true
                )
                OutlinedTextField(
                    value = typedEnd, onValueChange = { typedEnd = it.take(13) },
                    label = { Text("Einde · uu:mm:ss,mmm") }, singleLine = true
                )
                Text("Wat moet met volgende regels gebeuren?",
                    style = MaterialTheme.typography.labelLarge)
                listOf(
                    ExactCueTimingMode.ONLY_SELECTED to "Alleen deze regel",
                    ExactCueTimingMode.FOLLOW_START_CHANGE to
                        "Volgende regels mee met veranderd begin",
                    ExactCueTimingMode.FOLLOW_END_CHANGE to
                        "Volgende regels mee met veranderd einde"
                ).forEach { (choice, title) ->
                    TextButton(onClick = { mode = choice }) {
                        Text((if (mode == choice) "✓ " else "") + title)
                    }
                }
                if (preview == null) {
                    Text("Geen geldige wijziging. Controleer tijden, volgorde, " +
                        "documentgrootte en grens 99:59:59,999.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Voorbeeld vóór bevestigen",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Begin: " +
                        PrivateSubtitleRetiming.label(preview.previousStart) + " → " +
                        PrivateSubtitleRetiming.label(preview.updatedStart))
                    Text("Einde: " +
                        PrivateSubtitleRetiming.label(preview.previousEnd) + " → " +
                        PrivateSubtitleRetiming.label(preview.updatedEnd))
                    Text("Meeverplaatste volgende regels: " + preview.changedFollowing)
                    Text("Overlappende buurregels daarna: " + preview.overlapPairs)
                }
                Text("Alleen het tijdelijke concept wijzigt. Gebruik Terug/Opnieuw " +
                    "of bewaar apart een nieuwe SRT/VTT-kopie.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null, onClick = {
                PrivateSubtitleExactTiming.preview(
                    draft, selectedIndex, typedStart, typedEnd, mode
                )?.let { onApply(it.cues) }
            }) { Text("Pas exacte tijden toe") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
