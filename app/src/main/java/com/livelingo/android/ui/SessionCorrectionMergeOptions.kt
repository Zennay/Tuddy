package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.CorrectionConflictChoice
import com.livelingo.android.CorrectionMergeKey
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.SessionTranslationCorrectionMerge

/**
 * All conflicts are visible before either of two explicit merge decisions.
 * No action auto-applies a translation; only replaces the in-memory proposals.
 */
@Composable
internal fun SessionCorrectionMergeOptions(
    current: List<SessionTranslationCorrection>,
    imported: List<SessionTranslationCorrection>,
    enabled: Boolean,
    onCommit: (List<SessionTranslationCorrection>, String) -> Unit
) {
    var chosenConflicts by remember(current, imported) {
        mutableStateOf(emptySet<CorrectionMergeKey>())
    }
    val keep = SessionTranslationCorrectionMerge.prepare(
        current, imported, CorrectionConflictChoice.KEEP_CURRENT
    )
    val replace = SessionTranslationCorrectionMerge.prepare(
        current, imported, CorrectionConflictChoice.USE_IMPORTED
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (keep == null || replace == null) {
            Text(
                "Samenvoegen is niet mogelijk: ongeldige inhoud of meer dan 40 " +
                    "unieke zinnen. Er wordt niets verwijderd. Je kunt het bestand " +
                    "ook bewust volledig in plaats van de huidige lijst laden.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "Samenvoegen: " + keep.added + " nieuwe, " + keep.unchanged +
                    " identieke en " + keep.conflicts.size +
                    " conflicterende correcties. Alle huidige niet-conflicterende " +
                    "zinnen blijven behouden.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (keep.conflicts.isNotEmpty()) {
                Text("Verschillen vooraf bekijken:",
                    style = MaterialTheme.typography.labelMedium)
                keep.conflicts.forEachIndexed { index, conflict ->
                    Text(
                        (index + 1).toString() + ". " +
                            conflict.current.sourceCode + " → " +
                            conflict.current.targetCode + " · " +
                            conflict.current.original + "\n" +
                            "Huidig: " + conflict.current.translation + "\n" +
                            "Geïmporteerd: " + conflict.incoming.translation,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = conflict.key in chosenConflicts,
                            enabled = enabled,
                            onCheckedChange = { useImported ->
                                chosenConflicts = if (useImported) {
                                    chosenConflicts + conflict.key
                                } else chosenConflicts - conflict.key
                            }
                        )
                        Text(
                            "Gebruik voor deze zin de geïmporteerde vertaling",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (keep.conflicts.isNotEmpty()) {
                val individually = SessionTranslationCorrectionMerge.prepareSelected(
                    current, imported, chosenConflicts
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled && individually != null,
                    onClick = {
                        if (individually != null) {
                            onCommit(
                                individually.rows,
                                individually.added.toString() + " toegevoegd; " +
                                    individually.replaced + " individueel gekozen " +
                                    "vertalingen overgenomen; " +
                                    individually.kept + " bestaande behouden."
                            )
                        }
                    }
                ) {
                    Text("Voeg samen met mijn keuzes (" +
                        chosenConflicts.size + " van " + keep.conflicts.size + ")")
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                onClick = {
                    onCommit(
                        keep.rows,
                        keep.added.toString() + " toegevoegd; " +
                            keep.kept + " conflicten met huidige vertaling behouden."
                    )
                }
            ) { Text("Voeg samen · houd huidige vertalingen") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                onClick = {
                    onCommit(
                        replace.rows,
                        replace.added.toString() + " toegevoegd; " +
                            replace.replaced + " conflicterende vertalingen vervangen."
                    )
                }
            ) { Text("Voeg samen · kies geïmporteerde vertalingen") }
        }
    }
}
