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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.livelingo.android.LanguageSearch
import com.livelingo.android.LocalSpeechVoiceEntry
import com.livelingo.android.LocalSpeechVoiceInventory
import com.livelingo.android.LocalSpeechVoicePreview
import com.livelingo.android.LocalSpeechVoicePreferences
import com.livelingo.android.TargetCatalog
import com.livelingo.android.UnavailableTextLanguageCatalog
import com.livelingo.android.SessionCorrectionLanguageCatalog

/**
 * User-facing capability inventory. It intentionally separates target text
 * translation, device-provided speech candidates, and unavailable language
 * requests; no section implies installed packs or validated accuracy.
 */
@Composable
internal fun LanguageSupportDialog(
    spokenCandidates: List<String>,
    onPreviewLanguage: (String) -> Unit,
    onCreateOwnPhrases: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val localVoiceInventory = remember(context) { LocalSpeechVoiceInventory(context) }
    val localVoicePreview = remember(context) { LocalSpeechVoicePreview(context) }
    var installedLocalVoices by remember { mutableStateOf<List<LocalSpeechVoiceEntry>?>(null) }
    var checkingLocalVoices by remember { mutableStateOf(false) }
    var voicePreferenceRevision by remember { mutableStateOf(0) }
    var localVoiceNotice by remember { mutableStateOf("Nog niet gecontroleerd op dit toestel.") }
    var localVoiceQuery by remember { mutableStateOf("") }
    var onlyPreferredLocalVoices by remember { mutableStateOf(false) }
    var visibleLocalVoiceCount by remember { mutableStateOf(20) }
    var previewingVoice by remember { mutableStateOf<String?>(null) }
    var localVoicePreviewNotice by remember { mutableStateOf("") }
    DisposableEffect(localVoiceInventory, localVoicePreview) {
        onDispose {
            localVoiceInventory.close()
            localVoicePreview.close()
        }
    }
    var linkNotice by remember { mutableStateOf("") }
    var researchCopyNotice by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var modelTaskFilter by remember { mutableStateOf("Alle") }
    var modelAccessFilter by remember { mutableStateOf("Alle") }
    fun candidateAccessClass(license: String, limitation: String): String {
        val evidence = "$license $limitation".lowercase()
        val declared = license.lowercase()
        return when {
            listOf("cc-by-nc", "niet-commercieel", "non-commercial", "noncommercial")
                .any { phrase -> evidence.contains(phrase) } -> "Niet-commercieel"
            listOf("gated", "contactinformatie", "contact information", "share contact")
                .any { phrase -> evidence.contains(phrase) } -> "Toegang beperkt"
            listOf(
                "conflict onopgelost", "licentieconflict", "licentiecompatibiliteit",
                "provenance onopgelost", "licentieherkomst onopgelost", "onopgelost"
            ).any { phrase -> declared.contains(phrase) } -> "Licentieconflict/onopgelost"
            listOf("apache-2.0", "apache 2.0", "mit", "bsd-2-clause", "bsd-3-clause", "cc0-1.0", "isc")
                .any { phrase -> declared.contains(phrase) } -> "Permissief volgens modelkaart"
            else -> "Overig/onbekend"
        }
    }
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
    val taskFilteredCandidates = remember(
        normalizedQuery, unavailable, modelTaskFilter, modelAccessFilter
    ) {
        unavailable.associateWith { item ->
            UnavailableTextLanguageCatalog.candidatesFor(item, query).filter { candidate ->
                val taskMatches = when (modelTaskFilter) {
                    "ASR" -> candidate.category.contains("ASR", ignoreCase = true)
                    "MT" -> candidate.category.contains("Tekstvertaling", ignoreCase = true)
                    else -> true
                }
                val accessMatches = modelAccessFilter == "Alle" ||
                    candidateAccessClass(candidate.declaredLicense, candidate.limitation) == modelAccessFilter
                taskMatches && accessMatches
            }
        }.filterValues { it.isNotEmpty() }
    }
    val hasAnyResearchCandidates = unavailable.any {
        UnavailableTextLanguageCatalog.candidatesFor(it, query).isNotEmpty()
    }
    val hasResearchCandidates = taskFilteredCandidates.isNotEmpty()
    val researchSummary = remember(
        normalizedQuery, taskFilteredCandidates, modelTaskFilter, modelAccessFilter
    ) {
        val fullSummary = buildString {
            appendLine("Tuddy — modelonderzoek")
            appendLine("Taakfilter: ${modelTaskFilter}; licentie-/toegangsfilter: ${modelAccessFilter}.")
            appendLine("Onderzoekskandidaten zijn niet geïntegreerd, gedownload of op toestel gevalideerd; labels zijn geen distributiegoedkeuring.")
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
                    HorizontalDivider()
                    Text(
                        "Lokale voorleesstemmen · tekst naar spraak",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Controleer welke stemmen op dit toestel al geïnstalleerd zijn en niet als netwerkafhankelijk zijn gemarkeerd. Met Luister hoor je een korte, vaste zin in de stemtaal wanneer beschikbaar; voor overige talen worden alleen cijfers uitgesproken. Kies desgewenst een voorkeursstem; die keuze wordt alleen lokaal per taal bewaard. Inventariseren spreekt niets uit en downloadt geen stemmen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            enabled = !checkingLocalVoices,
                            onClick = {
                                localVoicePreview.close()
                                previewingVoice = null
                                localVoicePreviewNotice = ""
                                visibleLocalVoiceCount = 20
                                checkingLocalVoices = true
                                installedLocalVoices = null
                                localVoiceNotice = "Geïnstalleerde lokale stemmen controleren…"
                                localVoiceInventory.inspect { result ->
                                    checkingLocalVoices = false
                                    result.fold(
                                        onSuccess = { voices ->
                                            installedLocalVoices = voices
                                            localVoiceNotice = if (voices.isEmpty()) {
                                                "Geen geïnstalleerde lokale voorleesstemmen gevonden. Er is niets gedownload."
                                            } else {
                                                "${voices.size} geïnstalleerde lokale stemvarianten gevonden."
                                            }
                                        },
                                        onFailure = {
                                            installedLocalVoices = null
                                            localVoiceNotice = "Android kon de lokale stemlijst niet openen. Er is niets gedownload."
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(if (checkingLocalVoices) "Controleren…" else "Controleer lokale stemmen")
                        }
                        TextButton(
                            onClick = {
                                val opened = runCatching {
                                    context.startActivity(
                                        android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    )
                                }.isSuccess
                                localVoiceNotice = if (opened) {
                                    "Android-toegankelijkheidsinstellingen geopend. Open Tekst-naar-spraakuitvoer en controleer daarna opnieuw."
                                } else {
                                    "Android biedt geen toegankelijkheidsinstellingen op dit toestel."
                                }
                            }
                        ) { Text("Android-toegankelijkheidsinstellingen") }
                    }
                    Text(
                        localVoiceNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (localVoicePreviewNotice.isNotBlank()) {
                        Text(
                            localVoicePreviewNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    installedLocalVoices?.let { voices ->
                        if (voices.isEmpty()) {
                            Text(
                                "Geen lokale voorleesstemmen beschikbaar. Een stem kan eventueel via Android-instellingen worden geïnstalleerd; Tuddy start zelf geen download.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            val filteredVoices = remember(
                                voices, localVoiceQuery, onlyPreferredLocalVoices, voicePreferenceRevision
                            ) {
                                voices.filter { voice ->
                                    LanguageSearch.matches(
                                        localVoiceQuery,
                                        listOf(voice.displayName, voice.languageTag, voice.voiceName)
                                    ) && (!onlyPreferredLocalVoices ||
                                        LocalSpeechVoicePreferences.selectedVoiceName(
                                            context, voice.languageTag
                                        ) == voice.voiceName)
                                }
                            }
                            val displayedVoices = filteredVoices.take(visibleLocalVoiceCount)
                            OutlinedTextField(
                                value = localVoiceQuery,
                                onValueChange = {
                                    localVoiceQuery = it.take(80)
                                    visibleLocalVoiceCount = 20
                                    if (previewingVoice != null) {
                                        localVoicePreview.close()
                                        previewingVoice = null
                                        localVoicePreviewNotice = "Stemproef gestopt. Er is niets opgeslagen."
                                    }
                                },
                                label = { Text("Zoek stem, taal of engine-ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !onlyPreferredLocalVoices,
                                    onClick = {
                                        onlyPreferredLocalVoices = false
                                        visibleLocalVoiceCount = 20
                                    },
                                    label = { Text("Alle lokale stemmen") }
                                )
                                FilterChip(
                                    selected = onlyPreferredLocalVoices,
                                    onClick = {
                                        onlyPreferredLocalVoices = true
                                        visibleLocalVoiceCount = 20
                                    },
                                    label = { Text("Mijn voorkeursstemmen") }
                                )
                            }
                            Text(
                                "${displayedVoices.size} van ${filteredVoices.size} passende stemmen getoond " +
                                    "(${voices.size} lokale stemmen geïnstalleerd).",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (filteredVoices.isEmpty()) {
                                Text(
                                    if (onlyPreferredLocalVoices) {
                                        "Geen gekozen voorkeursstem gevonden. Kies Alle lokale stemmen om er een in te stellen."
                                    } else {
                                        "Geen lokale stemvarianten gevonden voor deze zoekopdracht."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                displayedVoices.forEach { voice ->
                                    val isPreferred = remember(
                                        voicePreferenceRevision, voice.languageTag, voice.voiceName
                                    ) {
                                        LocalSpeechVoicePreferences.selectedVoiceName(
                                            context, voice.languageTag
                                        ) == voice.voiceName
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            "${voice.displayName} (${voice.languageTag})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            voice.voiceName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (previewingVoice == voice.voiceName) {
                                            Text(
                                                "Voorbeeld: " + LocalSpeechVoicePreview.sampleFor(voice.languageTag),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = {
                                                    val saved = LocalSpeechVoicePreferences.selectVoice(
                                                        context, voice.languageTag, voice.voiceName
                                                    )
                                                    if (saved) {
                                                        voicePreferenceRevision++
                                                        localVoiceNotice =
                                                            "Voorkeursstem voor ${voice.languageTag} lokaal opgeslagen. Geen online fallback."
                                                    } else {
                                                        localVoiceNotice =
                                                            "Deze stemvoorkeur kon niet worden opgeslagen."
                                                    }
                                                }
                                            ) {
                                                Text(if (isPreferred) "Voorkeur voor deze taal" else "Gebruik deze stem")
                                            }
                                            TextButton(
                                                onClick = {
                                                    if (previewingVoice == voice.voiceName) {
                                                        localVoicePreview.close()
                                                        previewingVoice = null
                                                        localVoicePreviewNotice =
                                                            "Stemproef gestopt. Er is niets opgeslagen."
                                                    } else {
                                                        previewingVoice = voice.voiceName
                                                        localVoicePreviewNotice =
                                                            "Korte lokale stemproef wordt afgespeeld; er wordt geen gebruikersinhoud opgeslagen."
                                                        localVoicePreview.preview(voice) { result ->
                                                            if (previewingVoice == voice.voiceName) {
                                                                previewingVoice = null
                                                                localVoicePreviewNotice = result.fold(
                                                                    onSuccess = {
                                                                        "Stemproef afgerond. Er is geen gebruikersinhoud opgeslagen."
                                                                    },
                                                                    onFailure = { failure ->
                                                                        failure.message
                                                                            ?: "Lokale stemproef mislukt. Er is niets gedownload."
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text(if (previewingVoice == voice.voiceName) "Stop stemproef" else "Luister")
                                            }
                                        }
                                    }
                                }
                                if (displayedVoices.size < filteredVoices.size) {
                                    TextButton(
                                        onClick = { visibleLocalVoiceCount += 20 }
                                    ) {
                                        Text("Toon volgende stemmen (${filteredVoices.size - displayedVoices.size} over)")
                                    }
                                }
                            }
                            TextButton(
                                onClick = {
                                    LocalSpeechVoicePreferences.clearAll(context)
                                    voicePreferenceRevision++
                                    localVoiceNotice =
                                        "Lokale stemvoorkeuren gewist. Tuddy kiest weer automatisch een passende lokale stem."
                                }
                            ) {
                                Text("Automatische stemkeuze herstellen")
                            }
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
                            "Geen modelkandidaten voor deze taak-/toegangsfilters en zoekopdracht. Kies Alle filters of pas de zoektekst aan.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Modelkaarten hieronder zijn alleen onderzoekskandidaten. " +
                        "‘Permissief vermeld’ leest alleen de opgegeven modellicentie; datasetrechten, toegangsgates en distributiegoedkeuring blijven aparte vragen. " +
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
                        Text("Filter licentie/toegang", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Alle", "Niet-commercieel", "Toegang beperkt",
                                "Licentieconflict/onopgelost", "Permissief volgens modelkaart", "Overig/onbekend"
                            ).forEach { filter ->
                                FilterChip(
                                    selected = modelAccessFilter == filter,
                                    onClick = {
                                        modelAccessFilter = filter
                                        researchCopyNotice = ""
                                    },
                                    label = {
                                        Text(
                                            when (filter) {
                                                "Niet-commercieel" -> "Niet-commercieel"
                                                "Toegang beperkt" -> "Toegang gated"
                                                "Permissief volgens modelkaart" -> "Permissief vermeld"
                                                "Licentieconflict/onopgelost" -> "Licentieconflict / open"
                                                "Overig/onbekend" -> "Overig / onbekend"
                                                else -> "Alle statussen"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            "Dit filter classificeert alleen tekst uit de modelkaart/catalogus. Expliciet vermelde licentieconflicten vallen niet onder ‘Permissief vermeld’, ook als één bron Apache of MIT noemt. Geen enkel label is een automatische vrijgave.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (hasResearchCandidates &&
                        (normalizedQuery.isNotEmpty() || modelTaskFilter != "Alle" || modelAccessFilter != "Alle")
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
                                    Text(
                                        "Classificatie: " + candidateAccessClass(
                                            candidate.declaredLicense, candidate.limitation
                                        ) + " · alleen op catalogustekst gebaseerd; geen vrijgave.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
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
                                if (item.code in SessionCorrectionLanguageCatalog.manualOnlyCodes) {
                                    TextButton(
                                        onClick = { onPreviewLanguage(item.code) }
                                    ) {
                                        Text("Open vijfwoordenpreview")
                                    }
                                    TextButton(
                                        onClick = { onCreateOwnPhrases(item.code) }
                                    ) {
                                        Text("Schrijf eigen zinnen zonder model…")
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
