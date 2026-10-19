package com.livelingo.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.OwnWord
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.PrivatePrintableOwnWordbook
import com.livelingo.android.TargetCatalog
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Human-authored offline word notes, strictly separated from vetted glossary
 * facts and speech/translation models. Nothing is captured automatically.
 */
@Composable
internal fun MyOwnWordsPanel(
    deck: PrivateOwnWordDeck,
    initialLanguage: String,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultLanguage = initialLanguage.replace('_', '-').substringBefore('-')
        .lowercase().takeIf { it in PrivateOwnWordDeck.SUPPORTED_CODES } ?: "nl"
    var language by remember { mutableStateOf(defaultLanguage) }
    var pickerOpen by remember { mutableStateOf(false) }
    var languageSearch by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var searchAllLanguages by remember { mutableStateOf(false) }
    var practiceScope by remember { mutableStateOf<List<OwnWord>?>(null) }
    var practiceLanguage by remember { mutableStateOf(initialLanguage) }
    var practiceOpen by remember { mutableStateOf(false) }
    var plannedReviewOpen by remember { mutableStateOf(false) }
    var consentStartReview by remember { mutableStateOf(false) }
    var consentEraseReview by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<OwnWord?>(null) }
    var pendingDelete by remember { mutableStateOf<OwnWord?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }
    var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
    var pendingTsvExport by remember { mutableStateOf<ByteArray?>(null) }
    var printableConsent by remember { mutableStateOf(false) }
    var printableAllLanguages by remember { mutableStateOf(false) }
    var pendingPrintable by remember { mutableStateOf<Pair<String?, ByteArray>?>(null) }
    var pendingRestore by remember { mutableStateOf<List<OwnWord>?>(null) }
    var restoreOrigin by remember { mutableStateOf("json") }
    var pasteWordsOpen by remember { mutableStateOf(false) }
    var pasteLines by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf("") }
    val context = LocalContext.current
    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val snapshot = pendingBackup
        pendingBackup = null
        if (uri != null && snapshot != null) {
            val fresh = deck.exportSnapshot()
            if (fresh == null || !fresh.contentEquals(snapshot)) {
                notice = "Eigen woorden zijn gewijzigd. Kies opnieuw back-up bewaren."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(snapshot)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                notice = if (saved) "Eigen woorden bewaard op je gekozen locatie."
                    else "Bestand niet opgeslagen. Je privéwoorden blijven behouden."
            }
        }
    }
    val tsvExportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/tab-separated-values")
    ) { uri ->
        val snapshot = pendingTsvExport
        pendingTsvExport = null
        if (uri != null && snapshot != null) {
            if (deck.exportTsvSnapshot()?.contentEquals(snapshot) != true) {
                notice = "Eigen woorden zijn gewijzigd. Maak de TSV-lijst opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(snapshot)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                notice = if (saved) "Eigen woordenlijst opgeslagen als UTF-8 TSV."
                    else "TSV niet opgeslagen. Je privéwoorden blijven behouden."
            }
        }
    }
    val printablePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val pending = pendingPrintable
        pendingPrintable = null
        if (uri != null && pending != null) {
            val current = deck.entries()?.let {
                PrivatePrintableOwnWordbook.snapshot(it, pending.first)
            }
            if (current == null || !current.contentEquals(pending.second)) {
                notice = "Eigen woorden zijn gewijzigd. Kies opnieuw afdrukbaar woordenboek."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(pending.second)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                notice = if (saved)
                    "Afdrukbaar woordenboek opgeslagen. Controleer de bestandslocatie."
                else "Afdrukbaar woordenboek kon niet worden opgeslagen."
            }
        }
    }
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingRestore = null
        if (uri != null) {
            val imported = runCatching {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("onleesbaar")
                val output = ByteArrayOutputStream()
                input.use { stream ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val size = stream.read(buffer)
                        if (size < 0) break
                        if (size == 0 || output.size() + size > PrivateOwnWordDeck.MAX_BACKUP_BYTES)
                            throw IllegalArgumentException("bestand te groot")
                        output.write(buffer, 0, size)
                    }
                }
                deck.previewImport(output.toByteArray())
            }.getOrNull()
            if (imported == null) {
                notice = "Back-up geweigerd: onbekende versie, ongeldig of te groot. " +
                    "Je bestaande woorden zijn behouden."
            } else {
                pendingRestore = imported
                notice = "Back-up ingelezen. Kies hieronder bewust samenvoegen of vervangen."
            }
        }
    }
    val tsvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingRestore = null
        if (uri != null) {
            val imported = runCatching {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("onleesbaar bestand")
                val output = ByteArrayOutputStream()
                input.use { stream ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        if (read == 0 || output.size() + read > PrivateOwnWordDeck.MAX_BACKUP_BYTES)
                            throw IllegalArgumentException("onjuist of te groot bestand")
                        output.write(buffer, 0, read)
                    }
                }
                deck.previewTsvImport(output.toByteArray())
            }.getOrNull()
            if (imported == null) {
                notice = "TSV-lijst geweigerd: verwacht UTF-8 met kopregel " +
                    "language, word, meaning (tabs). Niets overschreven."
            } else {
                restoreOrigin = "tsv"
                pendingRestore = imported
                notice = "Woordlijst veilig ingelezen. Kies bewust samenvoegen of vervangen."
            }
        }
    }
    val items = remember(revision) { deck.entries() }
    val filtered = items.orEmpty().filter {
        (searchAllLanguages || it.language == language) && (query.isBlank() ||
            it.word.contains(query.trim(), ignoreCase = true) ||
            it.ownMeaning.contains(query.trim(), ignoreCase = true))
    }

    if (plannedReviewOpen) {
        ScheduledOwnWordReviewPanel(
            deck = deck,
            onChanged = { revision++; onChanged() },
            onDismiss = { plannedReviewOpen = false }
        )
        return
    }
    if (consentStartReview) {
        AlertDialog(
            onDismissRequest = { consentStartReview = false },
            title = { Text("Eigen woorden lokaal plannen?") },
            text = { Text("Tuddy bewaart pas na jouw keuze per bewust opgeslagen eigen " +
                "woord een lokale vingerafdruk, niveau en herhaaldatum. " +
                "Geen cloud, opname, taalcontrole, account of meldingen. " +
                "Je kunt de planning later apart verwijderen zonder woorden te wissen.") },
            confirmButton = {
                TextButton(onClick = {
                    val started = deck.enableReviews()
                    if (started) { revision++; onChanged() }
                    notice = if (started) "Herhaalplanning gestart. Je woorden staan klaar."
                        else "Herhaalplanning starten is mislukt."
                    consentStartReview = false
                }) { Text("Start alleen lokaal") }
            },
            dismissButton = {
                TextButton(onClick = { consentStartReview = false }) { Text("Annuleren") }
            }
        )
        return
    }
    if (consentEraseReview) {
        AlertDialog(
            onDismissRequest = { consentEraseReview = false },
            title = { Text("Herhaalplanning eigen woorden wissen?") },
            text = { Text("Dit wist de optionele lokale datums en niveaus definitief. " +
                "De zelfgeschreven woorden, betekenissen en back-ups blijven bestaan.") },
            confirmButton = {
                TextButton(onClick = {
                    val cleared = deck.disableReviews()
                    if (cleared) { revision++; onChanged() }
                    notice = if (cleared) "Herhaalplanning eigen woorden gewist."
                        else "Wissen mislukt; probeer opnieuw."
                    consentEraseReview = false
                }) { Text("Wis alleen planning") }
            },
            dismissButton = {
                TextButton(onClick = { consentEraseReview = false }) { Text("Annuleren") }
            }
        )
        return
    }
    if (practiceOpen) {
        OwnWordPracticePanel(
            notes = practiceScope ?: items.orEmpty(),
            initialLanguage = practiceLanguage,
            onDismiss = {
                practiceOpen = false
                practiceScope = null
            }
        )
        return
    }

    if (pasteWordsOpen) {
        AlertDialog(
            onDismissRequest = {
                pasteWordsOpen = false
                pasteLines = ""
                pasteError = ""
            },
            title = { Text("Plak mijn eigen woorden · offline") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Plak maximaal 50 regels uit je eigen notities: " +
                        "WOORD [tab] MIJN BETEKENIS. Tuddy verzint of " +
                        "vertaalt geen betekenissen en leest niet automatisch het klembord.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Vaste taalkeuze: ${language.uppercase()}. " +
                        "Sluit dit venster om een andere taal te kiezen.",
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = pasteLines,
                        onValueChange = { incoming ->
                            if (incoming.length <= 12000) {
                                pasteLines = incoming
                                pasteError = ""
                            } else {
                                pasteError = "Te lange invoer; maximaal 12.000 tekens."
                            }
                        },
                        label = { Text("Woord [tab] zelfgeschreven betekenis") },
                        supportingText = { Text("Per regel twee tabkolommen, geen kopregel.") },
                        minLines = 4,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pasteError.isNotBlank()) Text(
                        pasteError, color = MaterialTheme.colorScheme.error)
                    Text("Eerst verschijnt een volledige controlelijst; " +
                        "pas daarna kies je bewust samenvoegen of vervangen. " +
                        "Er wordt geen tekst naar een server verzonden.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val lines = pasteLines.split('\n').map { it.removeSuffix("\r") }
                        .dropLastWhile { it.isEmpty() }
                    if (lines.size !in 1..50 || lines.any { it.isBlank() }) {
                        pasteError = "Gebruik maximaal 50 niet-lege regels zonder lege tussenregels."
                    } else {
                        val cells = lines.map { it.split('\t') }
                        if (cells.any { it.size != 2 || it[0].isBlank() || it[1].isBlank() }) {
                            pasteError = "Iedere regel moet één woord en één eigen betekenis " +
                                "met precies één tab ertussen hebben."
                        } else {
                            val tsv = buildString {
                                append("language\tword\tmeaning\n")
                                cells.forEach { row ->
                                    append(language).append('\t').append(row[0].trim())
                                    append('\t').append(row[1].trim()).append('\n')
                                }
                            }
                            val parsed = deck.previewTsvImport(tsv.toByteArray(Charsets.UTF_8))
                            if (parsed == null) {
                                pasteError = "Ongeldige of dubbele woorden. Controleer lengte " +
                                    "(woord maximaal 48, betekenis 180), tabs en tekens; " +
                                    "niets is gewijzigd."
                            } else {
                                pendingRestore = parsed
                                restoreOrigin = "paste"
                                pasteWordsOpen = false
                                pasteLines = ""
                                pasteError = ""
                                notice = "Geplakte eigen woorden gecontroleerd. " +
                                    "Bekijk ALLE woorden voordat je samenvoegt of vervangt."
                            }
                        }
                    }
                }) { Text("Bekijk mijn woorden vóór opslaan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pasteWordsOpen = false
                    pasteLines = ""
                    pasteError = ""
                }) { Text("Annuleren") }
            }
        )
        return
    }

    val restore = pendingRestore
    if (restore != null) {
        val existing = items.orEmpty().map {
            it.language + "|" + it.word.lowercase(Locale.ROOT)
        }.toSet()
        val newCount = restore.count {
            it.language + "|" + it.word.lowercase(Locale.ROOT) !in existing
        }
        val duplicateCount = restore.size - newCount
        val canMerge = items != null && items.size + newCount <= PrivateOwnWordDeck.MAX_ENTRIES
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(if (restoreOrigin == "paste")
                "Controleer mijn geplakte woorden" else if (restoreOrigin == "tsv")
                "Zelfgeschreven woordenlijst importeren?" else "Eigen woorden herstellen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Deze lijst bevat ${restore.size} eigen woorden: " +
                        "$newCount nieuw, $duplicateCount al aanwezig. " +
                        "Samenvoegen bewaart altijd je huidige betekenis bij dubbelen. " +
                        (if (canMerge) "Samenvoegen past binnen de limiet. "
                            else "Samenvoegen kan niet wegens onleesbare opslag of capaciteit. ") +
                        "Vervangen wist alle huidige eigen woordbetekenissen. " +
                        "Niets is door Tuddy vertaald of gecontroleerd.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Volledige eigen invoer ter controle:",
                        style = MaterialTheme.typography.labelMedium)
                    restore.forEachIndexed { index, note ->
                        HorizontalDivider()
                        Text("${index + 1}. ${note.language.uppercase()} · ${note.word}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = if (note.language in
                                    setOf("ckb", "ps", "ar", "fa", "ur")) TextDirection.Rtl
                                    else TextDirection.Ltr
                            ))
                        Text(note.ownMeaning, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Column {
                    TextButton(enabled = canMerge, onClick = {
                        val saved = deck.mergeImported(restore)
                        notice = if (saved) "Nieuwe eigen woorden toegevoegd; lokale betekenissen behouden."
                            else "Samenvoegen mislukt; je huidige woorden zijn behouden."
                        if (saved) {
                            revision++
                            onChanged()
                            editing = null
                            word = ""
                            meaning = ""
                        }
                        pendingRestore = null
                    }) { Text("Voeg alleen nieuwe woorden toe") }
                    TextButton(onClick = {
                        val saved = deck.replaceImported(restore)
                        notice = if (saved) "Eigen woorden zijn na jouw keuze vervangen."
                            else "Vervangen is mislukt; je huidige lijst is behouden."
                        if (saved) {
                            revision++
                            onChanged()
                            editing = null
                            word = ""
                            meaning = ""
                        }
                        pendingRestore = null
                    }) { Text("Vervang volledige eigen lijst") }
                }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) {
                Text("Annuleren")
            } }
        )
        return
    }

    if (printableConsent) {
        AlertDialog(
            onDismissRequest = { printableConsent = false },
            title = { Text("Mijn eigen woorden afdrukbaar bewaren?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alleen jouw zelf ingevoerde woorden en betekenissen, " +
                        "niet door Tuddy gecontroleerd. De Android-bestandsprovider " +
                        "kan deze tekst buiten de app bewaren of synchroniseren.")
                    TextButton(onClick = { printableAllLanguages = false }) {
                        Text(if (!printableAllLanguages) "Huidige taal · " +
                            language.uppercase() + " ✓" else "Huidige taal · " +
                            language.uppercase())
                    }
                    TextButton(onClick = { printableAllLanguages = true }) {
                        Text(if (printableAllLanguages) "Alle eigen talen ✓"
                            else "Alle eigen talen")
                    }
                    Text("Het HTML-bestand bevat geen scripts, online lettertypen " +
                        "of gelinkte bronnen en is zelfstandig offline afdrukbaar.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    printableConsent = false
                    val selected = if (printableAllLanguages) null else language
                    val snapshot = deck.entries()?.let {
                        PrivatePrintableOwnWordbook.snapshot(it, selected)
                    }
                    if (snapshot == null) {
                        notice = "Geen geldige woorden voor dit afdrukbare woordenboek."
                    } else {
                        pendingPrintable = selected to snapshot
                        printablePicker.launch("tuddy-mijn-eigen-woorden.html")
                    }
                }) { Text("Ik begrijp dit, kies opslag…") }
            },
            dismissButton = {
                TextButton(onClick = { printableConsent = false }) { Text("Annuleren") }
            }
        )
        return
    }

    if (pendingDelete != null) {
        val selected = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eigen woordnotitie verwijderen?") },
            text = { Text("Dit verwijdert alleen jouw zelf ingevoerde betekenis bij " +
                selected.word + ". Gesprekken en ingebouwde woorden worden niet gewijzigd.") },
            confirmButton = {
                TextButton(onClick = {
                    val removed = deck.remove(selected)
                    notice = if (removed) "Eigen woordnotitie verwijderd."
                        else "De notitie is gewijzigd of kon niet worden verwijderd."
                    if (removed) {
                        revision++
                        onChanged()
                    }
                    pendingDelete = null
                }) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) {
                Text("Annuleren")
            } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen offline woorden") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Schrijf zelf een woord én de betekenis. Deze notities zijn " +
                    "NIET door Tuddy vertaald of gecontroleerd en worden alleen " +
                    "na jouw bewaaractie lokaal in de app opgeslagen. " +
                    "Bewaar geen gevoelige persoonsgegevens.",
                    style = MaterialTheme.typography.bodySmall)
                if (items == null) {
                    Text("Lokale gegevens konden niet veilig worden gelezen. " +
                        "Er wordt niets overschreven.", color = MaterialTheme.colorScheme.error)
                }
                Text("Woordtaal: " + language.uppercase(),
                    style = MaterialTheme.typography.titleSmall)
                if (editing == null) {
                    OutlinedButton(onClick = { pickerOpen = !pickerOpen }) {
                        Text(if (pickerOpen) "Sluit taalkeuze" else "Kies taal")
                    }
                    if (pickerOpen) {
                        OutlinedTextField(
                            value = languageSearch,
                            onValueChange = { languageSearch = it.take(35) },
                            label = { Text("Zoek taalnaam of code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        val preview = listOf("kmr" to "Kurmanji", "ckb" to "Sorani",
                            "ps" to "Pasjtoe")
                        val supported = (preview + TargetCatalog.search(languageSearch)
                            .map { it.code to it.title })
                            .distinctBy { it.first }
                            .filter {
                                languageSearch.isBlank() ||
                                    it.first.contains(languageSearch, ignoreCase = true) ||
                                    it.second.contains(languageSearch, ignoreCase = true)
                            }.take(12)
                        supported.forEach { (code, label) ->
                            TextButton(onClick = {
                                language = code
                                pickerOpen = false
                                languageSearch = ""
                                word = ""
                                meaning = ""
                            }) { Text(label + " (" + code + ")") }
                        }
                    }
                } else {
                    TextButton(onClick = {
                        editing = null
                        word = ""
                        meaning = ""
                    }) { Text("Annuleer bewerken") }
                }
                OutlinedTextField(
                    value = word,
                    onValueChange = { if (editing == null) word = it.take(48) },
                    label = { Text("Woord (zonder spaties)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = editing != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (language in setOf("ckb", "ps", "ar", "fa", "ur"))
                            TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it.take(180) },
                    label = { Text("Mijn eigen betekenis (niet gecontroleerd)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3
                )
                Button(
                    enabled = items != null && word.isNotBlank() && meaning.isNotBlank(),
                    onClick = {
                        val old = editing
                        val saved = if (old == null) {
                            deck.saveNew(language, word, meaning)
                        } else {
                            deck.edit(old, meaning)
                        }
                        notice = if (saved) {
                            if (old == null) "Nieuw eigen woord veilig lokaal opgeslagen."
                            else "Je gekozen eigen betekenis is bewust bijgewerkt."
                        } else if (old == null) {
                            "Niet opgeslagen: dit woord bestaat mogelijk al. " +
                                "Gebruik de knop Bewerk betekenis bij het bestaande woord. " +
                                "Je eerdere betekenis is behouden."
                        } else {
                            "Wijziging geweigerd: dit woord is intussen aangepast, " +
                                "niet beschikbaar of de nieuwe betekenis is ongeldig. " +
                                "De huidige woordenlijst is behouden."
                        }
                        if (saved) {
                            editing = null
                            word = ""
                            meaning = ""
                            revision++
                            onChanged()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editing == null) "Bewaar mijn betekenis" else "Bewaar wijziging") }
                OutlinedButton(
                    enabled = items?.isNotEmpty() == true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        practiceScope = null
                        practiceLanguage = language
                        practiceOpen = true
                    }
                ) { Text("🧠 Oefen alle eigen woorden · zonder opslag") }
                Text(
                    "Alleen je eigen, ongeverifieerde betekenissen. Je ziet het " +
                        "antwoord pas na een tik; resultaten verdwijnen bij sluiten.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!deck.reviewsEnabled()) {
                    OutlinedButton(
                        enabled = items?.isNotEmpty() == true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { consentStartReview = true }
                    ) { Text("Start privé herhaalplanning eigen woorden…") }
                    Text("Deze extra planning staat standaard uit.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    val scheduled = deck.scheduledReviews().orEmpty()
                    val dueCount = scheduled.count {
                        it.dueDay <= java.time.LocalDate.now().toEpochDay()
                    }
                    OutlinedButton(
                        enabled = dueCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { plannedReviewOpen = true }
                    ) { Text("Herhaal mijn eigen woorden vandaag · $dueCount") }
                    val next = scheduled.filter {
                        it.dueDay > java.time.LocalDate.now().toEpochDay()
                    }.minOfOrNull { it.dueDay }
                    Text(if (dueCount > 0) "$dueCount eigen woorden klaar voor vandaag."
                        else if (next != null) "Volgende herhaling: " +
                            java.time.LocalDate.ofEpochDay(next).toString()
                        else "Nog geen eigen woorden gepland.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { consentEraseReview = true }) {
                        Text("Wis herhaalplanning eigen woorden")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        enabled = items?.isNotEmpty() == true,
                        onClick = {
                            val snapshot = deck.exportSnapshot()
                            if (snapshot == null) {
                                notice = "Geen veilige eigen woorden beschikbaar voor export."
                            } else {
                                pendingBackup = snapshot
                                backupPicker.launch("tuddy-mijn-eigen-woorden.json")
                            }
                        }
                    ) { Text("Bewaar back-up…") }
                    OutlinedButton(onClick = {
                        pendingRestore = null
                        restoreOrigin = "json"
                        restorePicker.launch(arrayOf("application/json", "text/plain"))
                    }) { Text("Herstel back-up…") }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        pendingRestore = null
                        pasteLines = ""
                        pasteError = ""
                        pasteWordsOpen = true
                    }
                ) { Text("Plak eigen woorden uit notities…") }
                Text("Voeg tot 50 zelfgeschreven woord-betekenisregels toe zonder " +
                    "bestand. Eerst zie je de hele lijst; je beslist daarna zelf " +
                    "wat bewaard wordt.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        pendingRestore = null
                        restoreOrigin = "tsv"
                        tsvPicker.launch(arrayOf(
                            "text/tab-separated-values", "text/plain",
                            "application/octet-stream"
                        ))
                    }
                ) { Text("Importeer eigen woordenlijst (TSV)…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = items?.isNotEmpty() == true,
                    onClick = {
                        val snapshot = deck.exportTsvSnapshot()
                        if (snapshot == null) {
                            notice = "Geen geldige eigen woorden voor TSV-export."
                        } else {
                            pendingTsvExport = snapshot
                            tsvExportPicker.launch("tuddy-mijn-eigen-woorden.tsv")
                        }
                    }
                ) { Text("Exporteer mijn eigen woorden (TSV)…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = items?.isNotEmpty() == true,
                    onClick = {
                        printableAllLanguages = false
                        printableConsent = true
                    }
                ) { Text("▤ Maak afdrukbaar eigen woordenboek…") }
                Text("Maak één zelfstandig offline HTML-woordenboek voor " +
                    "bekijken of afdrukken. Alleen jouw ongecontroleerde eigen notities.",
                    style = MaterialTheme.typography.bodySmall)
                Text("TSV bevat alleen je zelfgeschreven betekenissen. " +
                    "Je kunt de lijst buiten Tuddy wijzigen en later bewust " +
                    "samenvoegen of vervangen. Geen automatische uploads.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Maak zelf een UTF-8-tekstbestand met kolommen language, word, meaning " +
                    "gescheiden door tabs. Verplicht eerste regel: " +
                    "language<TAB>word<TAB>meaning. Maximaal 120 eigen woorden. " +
                    "Bestaande betekenissen worden pas na jouw aparte keuze vervangen.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Back-ups zijn optioneel en bevatten alleen je eigen woordnotities. " +
                    "De gekozen Android-bestandsprovider kan ze extern opslaan of synchroniseren.",
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text((if (searchAllLanguages) "Eigen woorden in alle talen"
                    else "Mijn woorden in " + language.uppercase()) +
                    ": " + filtered.size, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { searchAllLanguages = !searchAllLanguages }) {
                    Text(if (searchAllLanguages) "✓ Zoek in alle eigen talen"
                        else "Zoek ook in andere eigen talen")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(70) },
                    label = { Text("Zoek in eigen woorden") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedButton(
                    enabled = items != null && filtered.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Explicitly capture the visible read-only search result
                        // set. Search never silently alters whole-deck practice.
                        practiceScope = filtered.toList()
                        practiceLanguage = if (searchAllLanguages) "all" else language
                        practiceOpen = true
                    }
                ) { Text("Oefen uitsluitend deze ${filtered.size} gevonden woorden") }
                Text("Alleen de bovenstaande taal- en zoekfilters bepalen deze " +
                    "oefenronde. Niets wordt automatisch opgeslagen of gedeeld.",
                    style = MaterialTheme.typography.bodySmall)
                filtered.forEach { saved ->
                    Text(saved.language.uppercase() + " · Mijn eigen woord",
                        style = MaterialTheme.typography.labelSmall)
                    Text(saved.word,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = if (saved.language in setOf("ckb", "ps", "ar",
                                    "fa", "ur", "sd", "ug")) TextDirection.Rtl
                                else TextDirection.Ltr
                        ))
                    Text(saved.ownMeaning, style = MaterialTheme.typography.bodySmall)
                    Text("Zelf ingevoerd · niet geverifieerd",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            editing = saved
                            language = saved.language
                            word = saved.word
                            meaning = saved.ownMeaning
                            notice = ""
                        }) { Text("Bewerk betekenis") }
                        TextButton(onClick = { pendingDelete = saved }) {
                            Text("Verwijder")
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    Text("Nog geen eigen woorden in deze selectie.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (notice.isNotBlank()) {
                    Text(notice, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
