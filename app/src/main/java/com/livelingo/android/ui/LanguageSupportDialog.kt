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
import com.livelingo.android.LanguageSearch
import com.livelingo.android.TargetCatalog
import com.livelingo.android.UnavailableTextLanguageCatalog

/**
 * User-facing capability inventory. It intentionally separates target text
 * translation, device-provided speech candidates, and unavailable language
 * requests; no section implies installed packs or validated accuracy.
 */
@Composable
internal fun LanguageSupportDialog(
    spokenCandidates: List<String>,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = LanguageSearch.normalize(query)
    val textTargets = remember(normalizedQuery) {
        TargetCatalog.search(query)
    }
    val speechCandidates = remember(normalizedQuery, spokenCandidates) {
        spokenCandidates.filter { LanguageSearch.matches(query, listOf(it)) }
    }
    val unavailable = remember(normalizedQuery) {
        if (normalizedQuery.isEmpty()) UnavailableTextLanguageCatalog.entries
        else UnavailableTextLanguageCatalog.search(query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Taalondersteuning") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Spraakherkenning, tekstvertaling en een woordenpreview zijn drie aparte functies. " +
                        "Een taalnaam of geïnstalleerd tekstmodel bewijst niet dat spraak offline werkt.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    label = { Text("Zoek taal, code of alias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Text(
                    "Tekstvertaling · " + TargetCatalog.targets.size + " talen",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "De talen hieronder zijn ML Kit-tekstvertalingsdoelen. Voor een gekozen paar moeten de vereiste modellen lokaal klaarstaan; controleer dat via Offline startcheck.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (textTargets.isEmpty()) {
                    Text("Geen tekstdoel gevonden.", style = MaterialTheme.typography.bodySmall)
                } else {
                    textTargets.forEach { target ->
                        Text(
                            target.title + " (" + target.code + ")",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                HorizontalDivider()
                Text(
                    "Spraakherkenning · " + spokenCandidates.size + " kandidaatkeuzes",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Dit zijn alleen locale-kandidaten uit de Android-spraakdienst. Werkelijke ondersteuning en installatie worden per toestel gecontroleerd; Tuddy schakelt niet over op cloudspraak.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (speechCandidates.isEmpty()) {
                    Text("Geen spraakkandidaat gevonden.", style = MaterialTheme.typography.bodySmall)
                } else {
                    speechCandidates.forEach { candidate ->
                        Text(candidate, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()
                Text(
                    "Niet beschikbaar als vrije offlinevertaling · " +
                        UnavailableTextLanguageCatalog.entries.size + " taalverzoeken",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Deze talen worden niet als selecteerbare ML Kit-doelen aangeboden. Een vijfwoordenpreview is geen vertaalmodel of spraakherkenning.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (unavailable.isEmpty()) {
                    Text("Geen niet-beschikbare taal gevonden.", style = MaterialTheme.typography.bodySmall)
                } else {
                    unavailable.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                item.title + " (" + item.code + ")",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(item.reason, style = MaterialTheme.typography.bodySmall)
                            if (item.hasFivePhrasePreview) {
                                Text(
                                    "Alleen vijf vaste basiswoorden; geen vrije zinsvertaling of bewezen ASR.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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
