package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
 * Display-only receipt for retryable, transient conversation rows.
 * The owning screen performs actual model checks, exact-row revalidation
 * and translation dispatch; this dialog never stores or exports text.
 */
internal data class FailedCaptionPair(
    val source: String,
    val target: String,
    val modelMissing: Int,
    val failed: Int
) {
    val count: Int get() = modelMissing + failed
}

@Composable
internal fun OfflineFailedCaptionsDialog(
    pairs: List<FailedCaptionPair>,
    checking: Boolean,
    notice: String,
    onRetryPair: (source: String, target: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mislukte vertalingen herstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Kies zelf een taalrichting. Tuddy controleert eerst de " +
                        "reeds geïnstalleerde offline tekstmodellen. Daarna " +
                        "worden maximaal 6 mislukte regels per keer opnieuw " +
                        "vertaald; de oorspronkelijke tekst en taal blijven staan. " +
                        "Geen download of online fallback.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (notice.isNotBlank()) {
                    Text(notice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (pairs.isEmpty()) {
                    Text("Er zijn geen herstelbare mislukte regels meer in dit gesprek.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(pairs, key = { "${it.source}:${it.target}" }) { pair ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "${pair.source.uppercase()} → ${pair.target.uppercase()} · " +
                                        "${pair.count} regels",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "${pair.modelMissing} model ontbreekt · " +
                                        "${pair.failed} lokaal mislukt",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    enabled = !checking,
                                    onClick = {
                                        onRetryPair(pair.source, pair.target)
                                    }
                                ) {
                                    Text("Controleer lokaal en herstel maximaal 6")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
