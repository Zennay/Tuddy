package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.PrivateOfflineSearch

/** In-memory management only; deleting does not rewrite existing captions. */
@Composable
internal fun SessionTranslationCorrectionsDialog(
    entries: List<SessionTranslationCorrection>,
    onRemove: (SessionTranslationCorrection) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val foldedQuery = PrivateOfflineSearch.fold(query.trim())
    val visible = entries.asReversed().filter { entry ->
        foldedQuery.isBlank() ||
            PrivateOfflineSearch.contains(entry.original, foldedQuery) ||
            PrivateOfflineSearch.contains(entry.translation, foldedQuery) ||
            PrivateOfflineSearch.contains(
                entry.sourceCode + " → " + entry.targetCode, foldedQuery
            )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn tijdelijke vertaalcorrecties") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Alleen correcties die je bewust voor dit gesprek onthield. " +
                        "Maximaal 40 exacte zinnen, enkel in het geheugen. " +
                        "Tuddy past deze voorstellen nooit automatisch toe.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    label = { Text("Zoek eigen zinnen, vertaling of taalrichting") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Zichtbaar: " + visible.size + " van " + entries.size,
                    style = MaterialTheme.typography.bodySmall)
                if (entries.isEmpty()) Text("Nog geen correcties onthouden.")
                else if (visible.isEmpty()) Text("Geen exact passend voorstel gevonden.")
                visible.forEach { entry ->
                    HorizontalDivider()
                    Text(entry.sourceCode + " → " + entry.targetCode,
                        style = MaterialTheme.typography.labelMedium)
                    Text(entry.original, style = MaterialTheme.typography.bodyMedium)
                    Text(entry.translation, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { onRemove(entry) }) { Text("Vergeet deze zin") }
                }
                if (entries.isNotEmpty()) {
                    if (confirmClear) {
                        Text("Wis alle tijdelijke correcties definitief?",
                            color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { onClear(); confirmClear = false }) {
                            Text("Ja, wis alles")
                        }
                        TextButton(onClick = { confirmClear = false }) { Text("Annuleren") }
                    } else {
                        OutlinedButton(onClick = { confirmClear = true },
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Wis alle onthouden correcties…")
                        }
                    }
                }
                Text("Verwijderen verandert eerdere gespreksregels niet. " +
                    "Bij 'Wis gesprek' en wanneer de app-sessie eindigt, " +
                    "verdwijnen alle onthouden voorstellen.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
