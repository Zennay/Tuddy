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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onPreparePair: (source: String, target: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Consent is specific to exactly one visible failed source -> target pair.
    // Dismissing this warning never triggers a network/model request.
    var preparationApproval by remember { mutableStateOf<Pair<String, String>?>(null) }
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
                        "Voorbereiding van ontbrekende modellen vraagt apart " +
                        "toestemming via wifi. Herstel krijgt voorrang op nog " +
                        "wachtende automatische regels, niet op lopende taken. " +
                        "Nooit een cloudvertaling.",
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
                                TextButton(
                                    enabled = !checking,
                                    onClick = {
                                        preparationApproval = pair.source to pair.target
                                    }
                                ) {
                                    Text("Ontbrekende tekstmodellen via wifi voorbereiden…")
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
    // Emit confirmation last so Android stacks it above the recovery panel.
    preparationApproval?.let { pair ->
        AlertDialog(
            onDismissRequest = { preparationApproval = null },
            title = { Text("Alleen ${pair.first.uppercase()} → ${pair.second.uppercase()} voorbereiden?") },
            text = {
                Text("Tuddy controleert eerst de geïnstalleerde lokale tekstmodellen. " +
                    "Alleen als dit taalpaar ontbreekt, mag ML Kit na je bevestiging " +
                    "de benodigde modellen via wifi downloaden. Android kan een eenmaal " +
                    "gestarte download ook na sluiten van het scherm afronden. " +
                    "Er wordt niet automatisch vertaald of iets geüpload.")
            },
            confirmButton = {
                TextButton(onClick = {
                    preparationApproval = null
                    onPreparePair(pair.first, pair.second)
                }) { Text("Bevestig wifi-modelvoorbereiding") }
            },
            dismissButton = {
                TextButton(onClick = { preparationApproval = null }) { Text("Annuleren") }
            }
        )
    }

}
