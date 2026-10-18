package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * This is an Activity-memory read model, not a dictionary or verified translation.
 * It has no persistence, network, export, clipboard, mic or model capability.
 */
internal data class SessionMeaningRow(
    val id: Long,
    val original: String,
    val translation: String,
    val meaning: String,
    val sourceCode: String,
    val targetCode: String,
    val canShowCard: Boolean
)

@Composable
internal fun SessionMeaningIndexDialog(
    rows: List<SessionMeaningRow>,
    onOpenCard: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onClose: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val query = search.trim()
    val visible = if (query.isBlank()) rows else rows.filter {
        it.original.contains(query, ignoreCase = true) ||
            it.translation.contains(query, ignoreCase = true) ||
            it.meaning.contains(query, ignoreCase = true)
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mijn eigen uitlegregels",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClose) { Text("Sluiten") }
                }
                Text(
                    "Alleen eigen betekenisnotities uit dit tijdelijke gesprek. " +
                        "Niet geverifieerd, niet opgeslagen of gedeeld; de modelvertaling blijft afzonderlijk.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(80) },
                    label = { Text("Zoek in origineel, lokale vertaling of eigen uitleg") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    "${visible.size} gevonden van ${rows.size} eigen uitlegregels",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (rows.isEmpty()) {
                        item(key = "none") {
                            Text("Nog geen eigen uitleg. Stop de microfoon en tik " +
                                "bij een gespreksregel op ✎ Eigen uitleg.")
                        }
                    } else if (visible.isEmpty()) {
                        item(key = "no-match") {
                            Text("Geen eigen uitleg gevonden. Verander je zoektekst.")
                        }
                    }
                    items(visible, key = { it.id }) { entry ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text("${entry.sourceCode.uppercase()} → " +
                                    entry.targetCode.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Origineel · " + entry.original,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Eigen uitleg · niet geverifieerd",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(entry.meaning,
                                    style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = entry.canShowCard,
                                        onClick = { onOpenCard(entry.id) }
                                    ) { Text("Toon gesprekskaart") }
                                    TextButton(onClick = { onEdit(entry.id) }) {
                                        Text("Wijzig of wis…")
                                    }
                                }
                                if (!entry.canShowCard) {
                                    Text("De vertaling van deze regel is nog niet afgerond; " +
                                        "je eigen uitleg kun je wel wijzigen of verwijderen.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
