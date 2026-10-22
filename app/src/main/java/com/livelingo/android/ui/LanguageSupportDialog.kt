package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
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
    onPreviewLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    var linkNotice by remember { mutableStateOf("") }
    var researchCopyNotice by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var modelTaskFilter by remember { mutableStateOf("Alle") }
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
    val modelOnlyQuery = normalizedQuery.isNotEmpty() &&
        textTargets.isEmpty() &&
        speechCandidates.isEmpty() &&
        unavailable.isNotEmpty() &&
        unavailable.none { item ->
            LanguageSearch.matches(query, listOf(item.code, item.title, item.aliases, item.reason))
        }
    val taskFilteredCandidates = remember(normalizedQuery, unavailable, modelTaskFilter) {
        unavailable.associateWith { item ->
            UnavailableTextLanguageCatalog.candidatesFor(item, query).filter { candidate ->
                when (modelTaskFilter) {
                    "ASR" -> candidate.category.contains("ASR", ignoreCase = true)
                    "MT" -> candidate.category.contains("Tekstvertaling", ignoreCase = true)
                    else -> true
                }
            }
        }.filterValues { it.isNotEmpty() }
    }
    val hasAnyResearchCandidates = unavailable.any {
        UnavailableTextLanguageCatalog.candidatesFor(it, query).isNotEmpty()
    }
    val hasResearchCandidates = taskFilteredCandidates.isNotEmpty()
    val researchSummary = remember(normalizedQuery, taskFilteredCandidates, modelTaskFilter) {
        val fullSummary = buildString {
            appendLine("Tuddy — modelonderzoek")
            appendLine("Filter: ${modelTaskFilter}; onderzoekskandidaten zijn niet geïntegreerd, gedownload of op toestel gevalideerd.")
            taskFilteredCandidates.forEach { (item, candidates) ->
                appendLine()
                appendLine("${item.title} (${item.code})")
                appendLine(item.reason)
                candidates.forEach { candidate ->
                    appendLine("- ${candidate.category}: ${candidate.title}")
                    appendLine("  Modelkaart: ${candidate.url}")
                    appendLine("  Licentie volgens modelkaart: ${candidate.declaredLicense}")
                    appendLine("  Omvang: ${candidate.footprint}")
                    if (candidate.directionEvidence.isNotBlank()) {
                        appendLine("  Taalrichtingen/codes (onbevestigd): ${candidate.directionEvidence}")
                    }
                    appendLine("  Beperking: ${candidate.limitation}")
                }
            }
        }
        if (fullSummary.length <= 16_000) fullSummary
        else fullSummary.take(15_940) + "\n[Samenvatting afgekapt op 16.000 tekens.]"
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
                    onValueChange = { query = it.take(60); researchCopyNotice = "" },
                    label = { Text("Zoek taal, code, alias, model of licentie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!modelOnlyQuery) {
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
                }
                HorizontalDivider()
                Text(
                    if (normalizedQuery.isEmpty()) {
                        "Niet beschikbaar als vrije offlinevertaling · " +
                            UnavailableTextLanguageCatalog.entries.size + " taalverzoeken"
                    } else {
                        "Niet beschikbaar als vrije offlinevertaling · " +
                            unavailable.size + " passende resultaten"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Deze talen worden niet als selecteerbare ML Kit-doelen aangeboden. Een vijfwoordenpreview is geen vertaalmodel of spraakherkenning.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (unavailable.isEmpty()) {
                    Text("Geen niet-beschikbare taal of modelkandidaat gevonden.", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (taskFilteredCandidates.isEmpty()) {
                        Text(
                            "Geen modelkandidaten voor dit filter en deze zoekopdracht. Kies Alle modellen of pas de zoektekst aan.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Modelkaarten hieronder zijn alleen onderzoekskandidaten. " +
                        "Openen gaat naar een externe website; Tuddy downloadt of activeert niets.",
                        style = MaterialTheme.typography.bodySmall)
                    if (hasAnyResearchCandidates) {
                        Text("Filter modelonderzoek", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Alle", "ASR", "MT").forEach { filter ->
                                FilterChip(
                                    selected = modelTaskFilter == filter,
                                    onClick = {
                                        modelTaskFilter = filter
                                        researchCopyNotice = ""
                                    },
                                    label = {
                                        Text(
                                            when (filter) {
                                                "ASR" -> "Spraak → tekst"
                                                "MT" -> "Tekstvertaling"
                                                else -> "Alle modellen"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                    if (hasResearchCandidates &&
                        (normalizedQuery.isNotEmpty() || modelTaskFilter != "Alle")
                    ) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(researchSummary))
                            researchCopyNotice = "Gekopieerd: alleen openbare modelmetadata, geen gesprekken of audio."
                        }) { Text("Kopieer gefilterde onderzoeksresultaten") }
                        if (researchCopyNotice.isNotBlank()) {
                            Text(
                                researchCopyNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    unavailable.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                item.title + " (" + item.code + ")",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(item.reason, style = MaterialTheme.typography.bodySmall)
                            val languageMatchesQuery = LanguageSearch.matches(
                                query, listOf(item.code, item.title, item.aliases, item.reason)
                            )
                            val candidates = taskFilteredCandidates[item].orEmpty()
                            if (candidates.isNotEmpty()) {
                                Text("Modelonderzoek · niet in Tuddy geïntegreerd",
                                    style = MaterialTheme.typography.labelMedium)
                                candidates.forEach { candidate ->
                                    Text(candidate.category + " · " + candidate.title,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text("Licentie volgens modelkaart: " + candidate.declaredLicense +
                                        ". Omvang: " + candidate.footprint + ".",
                                        style = MaterialTheme.typography.bodySmall)
                                    if (candidate.directionEvidence.isNotBlank()) {
                                        Text("Taalrichtingen/codes (onbevestigd): " + candidate.directionEvidence,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(candidate.limitation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = {
                                        runCatching { uriHandler.openUri(candidate.url) }
                                            .onFailure { linkNotice =
                                                "Modelkaart kon niet worden geopend. " +
                                                    "Er is niets gedownload." }
                                    }) { Text("Open externe modelkaart ↗") }
                                }
                            }
                            if (item.hasFivePhrasePreview && (normalizedQuery.isEmpty() || languageMatchesQuery)) {
                                Text(
                                    "Alleen vijf vaste basiswoorden; geen vrije zinsvertaling of bewezen ASR.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (item.code in setOf("kmr", "ckb", "ps")) {
                                    TextButton(
                                        onClick = { onPreviewLanguage(item.code) }
                                    ) {
                                        Text("Open vijfwoordenpreview")
                                    }
                                }
                            }
                        }
                    }
                }
                if (linkNotice.isNotBlank()) {
                    Text(linkNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
