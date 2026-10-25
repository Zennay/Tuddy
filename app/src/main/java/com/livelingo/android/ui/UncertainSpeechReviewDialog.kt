package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * In-memory index of imperfect Android speech confidence, not a second speech
 * recognition system. The owning activity rechecks each caption's exact
 * snapshot and conversation generation before acting.
 */
internal data class UncertainSpeechReviewItem(
    val id: Long,
    val original: String,
    val sourceCode: String,
    val targetCode: String,
    val confidencePercent: Int,
    val translationRevision: Int
)

@Composable
internal fun UncertainSpeechReviewDialog(
    candidates: List<UncertainSpeechReviewItem>,
    totalRemaining: Int,
    onSeen: (UncertainSpeechReviewItem) -> Unit,
    onCorrect: (UncertainSpeechReviewItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Onzekere spraakzinnen nalopen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Android heeft deze spraakzinnen met lage, niet-gekalibreerde " +
                        "zekerheid gemeld. Luister of controleer zelf wat er " +
                        "werkelijk is gezegd. Er wordt niets automatisch goedgekeurd, " +
                        "opgeslagen, gedeeld of opnieuw ingesproken.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${totalRemaining} nog niet bekeken · maximaal 30 tegelijk zichtbaar",
                    style = MaterialTheme.typography.labelMedium
                )
                if (candidates.isEmpty()) {
                    Text("Geen onzekere spraakzinnen meer om hier te bekijken.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(candidates, key = { it.id }) { row ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    "Android-score ${row.confidencePercent}% · " +
                                        "${row.sourceCode.uppercase()} → " +
                                        row.targetCode.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    row.original,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    TextButton(
                                        onClick = { onSeen(row) }
                                    ) { Text("Bekeken · niet bevestigd") }
                                    TextButton(
                                        onClick = { onCorrect(row) }
                                    ) { Text("Corrigeer…") }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Gereed") }
        }
    )
}
