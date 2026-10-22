package com.livelingo.android.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseArchive
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivatePhraseShortcuts
import com.livelingo.android.PrivatePhraseTopicCsv
import com.livelingo.android.PrivatePhraseTopics
import java.io.ByteArrayOutputStream

private data class TopicCsvExport(
    val bytes: ByteArray,
    val archive: PrivatePhraseArchive,
    val language: String?,
    val topic: String?,
    // Exactly the previewed rows: no newly added row can slip into a share.
    val selectedIds: Set<String>
)
private data class TopicCsvImport(
    val archive: PrivatePhraseArchive,
    val originalNotes: List<PersonalPhrase>,
    val originalTopics: ByteArray?
)

/**
 * User-selected complete, editable CSV phrasebook with local topics.
 * Export, import and confirmation are entirely manual; no clipboard, network
 * or microphone operations and no implicit writes to the personal phrase deck.
 */
@Composable
internal fun PrivatePhraseTopicCsvAction(
    deck: PrivatePhraseDeck,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val topics = remember(context) { PrivatePhraseTopics(context) }
    var notice by remember { mutableStateOf("") }
    var exportReview by remember { mutableStateOf<TopicCsvExport?>(null) }
    var pendingWrite by remember { mutableStateOf<TopicCsvExport?>(null) }
    var sharePreview by remember { mutableStateOf<TopicCsvExport?>(null) }
    var choosingScope by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var excludedExportRows by remember { mutableStateOf<Set<String>>(emptySet()) }
    fun selection(language: String?, topic: String?): List<PersonalPhrase>? =
        deck.entries()?.filter { note ->
            (language == null || note.language == language) &&
                (topic == null ||
                    if (topic == "__without__") topics.topicFor(note) == null
                    else topics.topicFor(note) == topic)
        }
    var importReview by remember { mutableStateOf<TopicCsvImport?>(null) }
    var pasteOpen by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf("") }
    var mergeOnly by remember { mutableStateOf(true) }
    var importLanguage by remember { mutableStateOf<String?>(null) }
    var importTopic by remember { mutableStateOf<String?>(null) }
    // Row exclusions belong to the transient preview, never to storage.
    var excludedImportRows by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Existing meanings are never modified unless individually opted in here.
    // Kept in RAM only for this exact CSV preview.
    var correctedImportRows by remember { mutableStateOf<Set<String>>(emptySet()) }

    val saveFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val pending = pendingWrite
        pendingWrite = null
        if (uri != null && pending != null) {
            val selected = selection(pending.language, pending.topic)?.filter {
                PrivatePhraseShortcuts.fingerprint(it) in pending.selectedIds
            }
            val fresh = selected?.let { PrivatePhraseTopicCsv.snapshot(context, deck, it)?.first }
            if (fresh == null || !fresh.contentEquals(pending.bytes)) {
                notice = "Eigen zinnen of thema's zijn veranderd: controleer CSV opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(pending.bytes)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                notice = if (saved) "Eigen zinnen en thema's als CSV bewaard."
                    else "CSV niet opgeslagen; de lokale zinnen blijven behouden."
            }
        }
    }

    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        importReview = null
        if (uri != null) {
            val incoming = runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("CSV onleesbaar")
                val bytes = ByteArrayOutputStream()
                stream.use { input ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        if (size == 0 || size + bytes.size() > 50_000) {
                            throw IllegalArgumentException("CSV te groot")
                        }
                        bytes.write(buffer, 0, size)
                    }
                }
                PrivatePhraseTopicCsv.preview(bytes.toByteArray(), deck)
            }.getOrNull()
            val baseline = deck.entries()
            if (incoming == null || baseline == null) {
                notice = "CSV geweigerd: ongeldige zinnen, thema's of bestandsgrootte. Niets gewijzigd."
            } else {
                importReview = TopicCsvImport(
                    incoming, baseline.toList(), topics.exportSnapshot()
                )
                mergeOnly = true
                importLanguage = null
                importTopic = null
                excludedImportRows = emptySet()
                correctedImportRows = emptySet()
                notice = ""
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedButton(
            enabled = deck.entries()?.isNotEmpty() == true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                selectedLanguage = null
                selectedTopic = null
                excludedExportRows = emptySet()
                choosingScope = true
                notice = ""
            }
        ) { Text("Exporteer gekozen zinnen mét thema's als CSV…") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { openFile.launch(arrayOf("text/csv", "text/plain", "application/octet-stream")) }
        ) { Text("Importeer zinnen mét thema's uit CSV…") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                pasteOpen = true
                pasteText = ""
                pasteError = ""
                notice = ""
            }
        ) { Text("Plak mijn eigen zinnen mét thema's als CSV…") }
        Text(
            "Bewerkbare vijfkoloms-CSV met een eigen thema per zin. Dit is anders " +
                "dan de gewone vierkoloms-CSV. Alles wordt eerst volledig getoond; " +
                "geen automatische vertaling, upload of achtergrondopslag.",
            style = MaterialTheme.typography.bodySmall
        )
        if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodySmall)
    }

    if (pasteOpen) {
        AlertDialog(
            onDismissRequest = {
                pasteOpen = false
                pasteText = ""
                pasteError = ""
            },
            title = { Text("Plak een eigen CSV met thema's · zonder bestand") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("Plak bewust maximaal 12.000 tekens inclusief de exacte " +
                        "vijfkoloms-kopregel: language,phrase,meaningLanguage,ownMeaning,topic. " +
                        "Geen automatische klembordlezing of netwerkvertaaldienst. " +
                        "Bij geldige invoer volgt eerst een afzonderlijke volledige preview.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { incoming ->
                            if (incoming.length <= 12_000) {
                                pasteText = incoming
                                pasteError = ""
                            } else {
                                pasteError = "Maximaal 12.000 tekens; invoer ongewijzigd."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Volledige eigen CSV inclusief de vijf kolomnamen") },
                        minLines = 4,
                        maxLines = 10
                    )
                    if (pasteError.isNotBlank()) {
                        Text(pasteError, color = MaterialTheme.colorScheme.error)
                    }
                    Text("Een lege themakolom is toegestaan. Alle betekenissen zijn " +
                        "door jou zelf geschreven en worden niet door Tuddy vertaald.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val candidate = PrivatePhraseTopicCsv.preview(
                        pasteText.toByteArray(Charsets.UTF_8), deck
                    )
                    val baseline = deck.entries()
                    if (candidate == null || baseline == null) {
                        pasteError = "CSV onjuist of te groot: controleer vijf kolommen, " +
                            "taalcodes, dubbele quotes, thema's en dubbele bronzinnen. " +
                            "Niets opgeslagen."
                    } else {
                        importReview = TopicCsvImport(
                            candidate, baseline.toList(), topics.exportSnapshot()
                        )
                        mergeOnly = true
                        importLanguage = null
                        importTopic = null
                        excludedImportRows = emptySet()
                        correctedImportRows = emptySet()
                        pasteOpen = false
                        pasteText = ""
                        pasteError = ""
                        notice = ""
                    }
                }) { Text("Controleer alle geplakte zinnen…") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pasteOpen = false
                    pasteText = ""
                    pasteError = ""
                }) { Text("Annuleren") }
            }
        )
    }

    if (choosingScope) {
        val visible = selection(selectedLanguage, selectedTopic).orEmpty()
        val selected = visible.filter {
            PrivatePhraseShortcuts.fingerprint(it) !in excludedExportRows
        }
        AlertDialog(
            onDismissRequest = { choosingScope = false },
            title = { Text("Kies bewust welke eigen zinnen in CSV komen") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Welke taal mag in de CSV? (geen andere zinnen worden gedeeld)")
                    TextButton(onClick = { selectedLanguage = null }) {
                        Text(if (selectedLanguage == null) "✓ Alle drie talen" else "Alle drie talen")
                    }
                    listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe").forEach {
                        (code, title) ->
                        TextButton(onClick = { selectedLanguage = code }) {
                            Text(if (selectedLanguage == code) "✓ " + title else title)
                        }
                    }
                    Text("Welke persoonlijke themagroep?")
                    TextButton(onClick = { selectedTopic = null }) {
                        Text(if (selectedTopic == null) "✓ Alle thema's" else "Alle thema's")
                    }
                    TextButton(onClick = { selectedTopic = "__without__" }) {
                        Text(if (selectedTopic == "__without__") "✓ Zonder thema" else "Zonder thema")
                    }
                    topics.topics().forEach { title ->
                        TextButton(onClick = { selectedTopic = title }) {
                            Text(if (selectedTopic == title) "✓ " + title else title)
                        }
                    }
                    Text("Alleen regels met ☑ Meenemen komen in het externe bestand. " +
                        "Je kunt iedere rij afzonderlijk uitsluiten of herstellen; " +
                        "de resterende selectie volgt eerst een volledige privacy-preview.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        val visibleIds = visible.map {
                            PrivatePhraseShortcuts.fingerprint(it)
                        }.toSet()
                        excludedExportRows = excludedExportRows - visibleIds
                    }) { Text("↺ Herstel alle zichtbare zinnen") }
                    TextButton(
                        enabled = visible.any {
                            PrivatePhraseShortcuts.fingerprint(it) !in excludedExportRows
                        },
                        onClick = {
                            excludedExportRows = excludedExportRows + visible.map {
                                PrivatePhraseShortcuts.fingerprint(it)
                            }
                        }
                    ) { Text("☐ Sla alle zichtbare zinnen over") }
                    visible.forEachIndexed { index, note ->
                        val identity = PrivatePhraseShortcuts.fingerprint(note)
                        val included = identity !in excludedExportRows
                        HorizontalDivider()
                        TextButton(onClick = {
                            excludedExportRows = if (included)
                                excludedExportRows + identity else excludedExportRows - identity
                        }) {
                            Text((if (included) "☑ Meenemen" else "☐ Overgeslagen") +
                                " · regel " + (index + 1))
                        }
                        Text(note.language.uppercase() + " · " +
                            note.phrase, style = MaterialTheme.typography.bodySmall.copy(
                                textDirection = if (note.language == "ckb" || note.language == "ps")
                                    TextDirection.Rtl else TextDirection.Ltr
                            ))
                        Text("Mijn eigen betekenis: " + note.ownMeaning,
                            style = MaterialTheme.typography.labelSmall)
                        Text("Thema: " + (topics.topicFor(note) ?: "zonder thema"),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Geselecteerd voor export: " + selected.size +
                        " van " + visible.size + " getoonde zinnen. " +
                        "Hierna volgt eerst de volledige inhoudscontrole.")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selected.isNotEmpty(),
                    onClick = {
                        val candidate = PrivatePhraseTopicCsv.snapshot(context, deck, selected)
                        if (candidate == null) {
                            notice = "Selectie ongeldig of intussen gewijzigd; kies opnieuw."
                        } else {
                            exportReview = TopicCsvExport(
                                candidate.first, candidate.second, selectedLanguage,
                                selectedTopic, selected.map {
                                    PrivatePhraseShortcuts.fingerprint(it)
                                }.toSet()
                            )
                            choosingScope = false
                            notice = ""
                        }
                    }
                ) { Text("Bekijk geselecteerde zinnen volledig…") }
            },
            dismissButton = {
                TextButton(onClick = { choosingScope = false }) { Text("Annuleren") }
            }
        )
    }

    val export = exportReview
    if (export != null) {
        AlertDialog(
            onDismissRequest = { exportReview = null },
            title = { Text("Alleen mijn geselecteerde zinnen als CSV bewaren?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Bekijk uitsluitend deze gekozen zinnen en hun thema's hieronder. " +
                            "Pas na bevestiging kies jij een Android-bestandslocatie. " +
                            "Deze export bevat geen audio, favorieten, transcript of herhaaldatums."
                    )
                    export.archive.notes.forEachIndexed { i, note ->
                        HorizontalDivider()
                        Text((i + 1).toString() + ". " + note.language.uppercase() +
                            " · " + note.meaningLanguage.uppercase())
                        Text(note.phrase, style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (note.language == "ckb" || note.language == "ps")
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                        Text(note.ownMeaning, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Thema: " + (export.archive.assignments[
                                PrivatePhraseShortcuts.fingerprint(note)
                            ] ?: "zonder thema"),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Text(
                        "Een door jou gekozen externe opslagprovider kan dit " +
                            "privébestand zelf synchroniseren. De export is geen gecontroleerde vertaling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        pendingWrite = export
                        exportReview = null
                        saveFile.launch("Tuddy-geselecteerde-zinnen-met-themas.csv")
                    }) { Text("Ik begrijp dit, kies opslag…") }
                    TextButton(onClick = {
                        exportReview = null
                        sharePreview = export
                    }) { Text("Bekijk volledige CSV en deel bewust…") }
                }
            },
            dismissButton = {
                TextButton(onClick = { exportReview = null }) { Text("Annuleren") }
            }
        )
    }

    val sharing = sharePreview
    if (sharing != null) {
        AlertDialog(
            onDismissRequest = { sharePreview = null },
            title = { Text("Mijn privé-CSV echt met een andere app delen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("Hieronder staat de volledige UTF-8 CSV die je gaat delen, " +
                        "inclusief jouw eigen zinnen, betekenissen en thema's. " +
                        "Lees alles nogmaals voordat je Android Delen opent.")
                    Text(
                        sharing.bytes.toString(Charsets.UTF_8),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Een door jou geselecteerde externe app kan de CSV " +
                        "opslaan, uploaden of doorsturen. Tuddy kan die externe " +
                        "kopie niet verwijderen. Geen automatische vertaling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val selected = selection(sharing.language, sharing.topic)?.filter {
                        PrivatePhraseShortcuts.fingerprint(it) in sharing.selectedIds
                    }
                    val fresh = selected?.let {
                        PrivatePhraseTopicCsv.snapshot(context, deck, it)?.first
                    }
                    if (fresh == null || !fresh.contentEquals(sharing.bytes)) {
                        notice = "De selectie of eigen thema's zijn gewijzigd; " +
                            "controleer vóór delen opnieuw."
                    } else {
                        val sent = runCatching {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    sharing.bytes.toString(Charsets.UTF_8)
                                )
                            }
                            val chooser = Intent.createChooser(
                                send, "Deel uitsluitend deze eigen Tuddy-CSV"
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                            true
                        }.getOrDefault(false)
                        notice = if (sent)
                            "Android Delen geopend voor exact jouw geselecteerde CSV."
                        else "Geen externe deelapp beschikbaar; niets gedeeld."
                    }
                    sharePreview = null
                }) { Text("Ik begrijp dit, open Android Delen…") }
            },
            dismissButton = {
                TextButton(onClick = { sharePreview = null }) { Text("Annuleren") }
            }
        )
    }

    val review = importReview
    if (review != null) {
        val scoped = importLanguage != null || importTopic != null ||
            excludedImportRows.isNotEmpty()
        val filteredNotes = review.archive.notes.filter { note ->
            (importLanguage == null || note.language == importLanguage) &&
                (importTopic == null ||
                    if (importTopic == "__without__")
                        PrivatePhraseShortcuts.fingerprint(note) !in review.archive.assignments
                    else review.archive.assignments[
                        PrivatePhraseShortcuts.fingerprint(note)
                    ] == importTopic)
        }
        // Excluded rows stay visible, so each opt-out can be undone. Only
        // explicitly included rows reach either atomic persistence operation.
        val chosenNotes = filteredNotes.filter { note ->
            PrivatePhraseShortcuts.fingerprint(note) !in excludedImportRows
        }
        val identities = chosenNotes.map { PrivatePhraseShortcuts.fingerprint(it) }.toSet()
        val chosenLinks = review.archive.assignments.filterKeys { it in identities }
        val names = review.archive.topics.filter { it in chosenLinks.values }
        val chosenArchive = PrivatePhraseArchive(chosenNotes, names, chosenLinks)
        fun sourceKey(note: PersonalPhrase) =
            note.language + "|" + note.phrase.lowercase(java.util.Locale.ROOT)
        val existingByKey = review.originalNotes.associateBy(::sourceKey)
        fun storedVersion(note: PersonalPhrase): PersonalPhrase? =
            existingByKey[sourceKey(note)]
        fun alreadyStored(note: PersonalPhrase): Boolean = storedVersion(note) != null
        val newCount = chosenNotes.count { !alreadyStored(it) }
        val existingCount = chosenNotes.size - newCount
        val correctedIds = chosenNotes.mapNotNull { incoming ->
            val old = storedVersion(incoming)
            val id = PrivatePhraseShortcuts.fingerprint(incoming)
            if (id in correctedImportRows && old != null &&
                old.language == incoming.language && old.phrase == incoming.phrase &&
                old != incoming) id else null
        }.toSet()
        val unchangedCount = existingCount - correctedIds.size
        AlertDialog(
            onDismissRequest = { importReview = null },
            title = { Text("Eigen CSV-zinnen én thema's controleren") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("Kies de zinnen die je écht wilt overnemen. " +
                        "Taal- en themafilters beperken de lijst. Tik bij iedere regel " +
                        "op meenemen of overslaan; overgeslagen regels blijven zichtbaar. " +
                        "Alleen een volledige CSV-selectie kan de privé-lijst vervangen.")
                    TextButton(onClick = {
                        importLanguage = null
                        mergeOnly = true
                    }) { Text(if (importLanguage == null) "✓ Alle drie talen" else "Alle drie talen") }
                    listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe").forEach {
                        (code, label) ->
                        TextButton(onClick = {
                            importLanguage = code
                            mergeOnly = true
                        }) { Text(if (importLanguage == code) "✓ " + label else label) }
                    }
                    TextButton(onClick = {
                        importTopic = null
                        mergeOnly = true
                    }) { Text(if (importTopic == null) "✓ Alle thema's" else "Alle thema's") }
                    TextButton(onClick = {
                        importTopic = "__without__"
                        mergeOnly = true
                    }) {
                        Text(if (importTopic == "__without__") "✓ Zonder thema"
                            else "Zonder thema")
                    }
                    review.archive.topics.forEach { name ->
                        TextButton(onClick = {
                            importTopic = name
                            mergeOnly = true
                        }) { Text(if (importTopic == name) "✓ " + name else name) }
                    }
                    TextButton(onClick = {
                        importLanguage = null
                        importTopic = null
                        excludedImportRows = emptySet()
                        correctedImportRows = emptySet()
                        mergeOnly = true
                    }) { Text("↺ Herstel volledige CSV-selectie") }
                    TextButton(
                        enabled = filteredNotes.any {
                            PrivatePhraseShortcuts.fingerprint(it) in excludedImportRows
                        },
                        onClick = {
                            val visible = filteredNotes.map {
                                PrivatePhraseShortcuts.fingerprint(it)
                            }.toSet()
                            excludedImportRows = excludedImportRows - visible
                            mergeOnly = true
                        }
                    ) { Text("✓ Neem alle zichtbare zinnen mee") }
                    TextButton(
                        enabled = filteredNotes.any {
                            PrivatePhraseShortcuts.fingerprint(it) !in excludedImportRows
                        },
                        onClick = {
                            excludedImportRows = excludedImportRows + filteredNotes.map {
                                PrivatePhraseShortcuts.fingerprint(it)
                            }
                            mergeOnly = true
                        }
                    ) { Text("☐ Sla alle zichtbare zinnen over") }
                    Text("Geselecteerd: " + chosenNotes.size +
                        " van " + review.archive.notes.size + " eigen zinnen. " +
                        newCount + " nieuw, " + correctedIds.size +
                        " bestaande betekenissen bewust geselecteerd voor correctie, " +
                        unchangedCount + " bestaande zinnen blijven gelijk. " +
                        "Thema's en favorieten blijven altijd lokaal leidend.",
                        style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { mergeOnly = true }) {
                        Text(if (mergeOnly) "☑ Veilig samenvoegen / gekozen correcties"
                            else "☐ Veilig samenvoegen / gekozen correcties")
                    }
                    TextButton(
                        enabled = !scoped && correctedImportRows.isEmpty(),
                        onClick = { mergeOnly = false }
                    ) {
                        Text(if (!mergeOnly) "☑ Vervang hele privé-lijst (destructief)"
                            else "☐ Vervang hele privé-lijst (destructief)")
                    }
                    Text(
                        if (mergeOnly)
                            "Nieuwe zinnen worden toegevoegd. Bestaande betekenissen " +
                                "veranderen uitsluitend na de aparte keuze bij die rij. " +
                                "Een gecorrigeerd antwoord begint opnieuw met leren; " +
                                "favoriet en huidig thema blijven behouden. Alle andere " +
                                "herhaaldatums blijven onaangeroerd."
                        else
                            "Let op: dit verwijdert al je bestaande eigen zinnen, " +
                                "thema's, favorieten en herhaalplanning. Alleen de " +
                                "hieronder getoonde zinnen en thema's komen terug.",
                        color = if (mergeOnly) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    filteredNotes.forEachIndexed { i, note ->
                        val identity = PrivatePhraseShortcuts.fingerprint(note)
                        val included = identity !in excludedImportRows
                        HorizontalDivider()
                        TextButton(onClick = {
                            excludedImportRows = if (included)
                                excludedImportRows + identity else excludedImportRows - identity
                            mergeOnly = true
                        }) {
                            Text((if (included) "☑ Meenemen" else "☐ Overgeslagen") +
                                " · regel " + (i + 1))
                        }
                        Text(note.language.uppercase() +
                            " → " + note.meaningLanguage.uppercase())
                        Text(note.phrase, style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (note.language == "ckb" || note.language == "ps")
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                        Text("Eigen betekenis: " + note.ownMeaning,
                            style = MaterialTheme.typography.bodySmall)
                        val old = storedVersion(note)
                        if (old != null) {
                            val eligible = old.language == note.language &&
                                old.phrase == note.phrase && old != note
                            Text("Lokaal opgeslagen: [" + old.meaningLanguage + "] " +
                                old.ownMeaning, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (eligible) "CSV stelt voor: [" + note.meaningLanguage +
                                    "] " + note.ownMeaning
                                else if (old == note)
                                    "Deze betekenis is gelijk aan de lokale versie."
                                else "Bronzin verschilt in hoofdletters; deze " +
                                    "identiteit is niet automatisch te corrigeren.",
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (eligible) {
                                TextButton(
                                    enabled = included,
                                    onClick = {
                                        correctedImportRows = if (identity in correctedImportRows)
                                            correctedImportRows - identity
                                        else correctedImportRows + identity
                                        mergeOnly = true
                                    }
                                ) {
                                    Text(if (identity in correctedImportRows)
                                        "☑ Vervang uitsluitend deze bestaande betekenis"
                                    else "☐ Corrigeer deze bestaande betekenis (apart akkoord)")
                                }
                            }
                            Text("Het lokale thema en de favoriet van deze zin blijven " +
                                "onaangeroerd. Bij betekeniswijziging vervalt uitsluitend " +
                                "de herhaalvoortgang van deze zin.",
                                style = MaterialTheme.typography.labelSmall)
                        } else Text(
                            "Nieuwe zin: deze eigen zin wordt inclusief het gekozen " +
                                "thema toegevoegd bij veilig samenvoegen.",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text("Thema: " + (review.archive.assignments[identity]
                            ?: "zonder thema"), style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Dit zijn zelfgeschreven, niet beoordeelde betekenissen. " +
                        "De bestanden zijn nooit automatisch uit een gesprek gekopieerd.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = chosenNotes.isNotEmpty(), onClick = {
                    val saved = if (mergeOnly || scoped) {
                        deck.mergeArchiveIfUnchanged(
                            context, chosenArchive, review.originalNotes, review.originalTopics,
                            correctedIds
                        )
                    } else {
                        deck.replaceArchiveIfUnchanged(
                            context, chosenArchive, review.originalNotes, review.originalTopics
                        )
                    }
                    if (saved) onChanged()
                    notice = if (saved && (mergeOnly || scoped))
                        "Geselecteerde zinnen verwerkt: " + newCount + " nieuw, " +
                            correctedIds.size + " bestaande betekenissen bewust gecorrigeerd. " +
                            "Overige gegevens blijven behouden."
                    else if (saved)
                        "Eigen zinnen en thema's bewust vervangen; oude planning gewist."
                    else "Import geweigerd: gegevens zijn gewijzigd of de " +
                        "taal/thema-limiet is bereikt. Niets gewijzigd."
                    importReview = null
                }) {
                    Text(if (mergeOnly) "Voeg nieuw toe en corrigeer gekozen betekenissen"
                        else "Vervang alle eigen zinnen en thema's")
                }
            },
            dismissButton = {
                TextButton(onClick = { importReview = null }) { Text("Annuleren") }
            }
        )
    }
}
