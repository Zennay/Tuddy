package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Only a temporary display snapshot; private speech is never persisted. */
internal data class PausedCaptionChoice(
    val id: Long,
    val original: String,
    val source: String,
    val target: String
)

/**
 * Explicit per-line review before restarting a *bounded* set of previously
 * withheld speech translations. Selecting one language direction disables
 * other pairs; never implicitly translates the entire transcript.
 */
@Composable
internal fun PausedCaptionReviewDialog(
    choices: List<PausedCaptionChoice>,
    checking: Boolean,
    notice: String,
    onTranslateSelected: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    val availableIds = choices.map { it.id }.toSet()
    val validSelection = selected.intersect(availableIds)
    val chosenPair = choices.firstOrNull { it.id in validSelection }
        ?.let { it.source to it.target }
    val count = validSelection.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vertaal overgeslagen zinnen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Selecteer zelf maximaal 6 oorspronkelijke zinnen uit één " +
                        "taalrichting. Tuddy controleert eerst de al geïnstalleerde " +
                        "lokale tekstmodellen. Er volgt geen modeldownload, " +
                        "automatische hervatting of cloudvertaling.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (notice.isNotBlank()) {
                    Text(notice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (choices.isEmpty()) {
                    Text("Geen overgeslagen zinnen in dit tijdelijke gesprek.")
                } else {
                    Text(
                        "$count van 6 gekozen" +
                            if (chosenPair != null)
                                " · ${chosenPair.first.uppercase()} → ${chosenPair.second.uppercase()}"
                            else "",
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 330.dp)) {
                        items(choices, key = { it.id }) { item ->
                            val checked = item.id in validSelection
                            val samePair = chosenPair == null ||
                                chosenPair == (item.source to item.target)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    enabled = !checking && (checked || (samePair && count < 6)),
                                    onCheckedChange = { value ->
                                        selected = if (value && samePair && count < 6)
                                            selected + item.id
                                        else if (!value) selected - item.id
                                        else selected
                                    }
                                )
                                Column {
                                    Text(
                                        "${item.source.uppercase()} → ${item.target.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(item.original, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !checking && count in 1..6,
                onClick = {
                    // Stable order follows current visible caption positions.
                    onTranslateSelected(choices.filter { it.id in validSelection }.map { it.id })
                    // Keep selection if Android reports a missing model.
                    // Successfully submitted rows disappear from eligible choices.
                }
            ) { Text("Controleer lokaal en vertaal $count") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
