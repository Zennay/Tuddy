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
    var editing by remember { mutableStateOf<OwnWord?>(null) }
    var pendingDelete by remember { mutableStateOf<OwnWord?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }
    var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
    var pendingTsvExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingRestore by remember { mutableStateOf<List<OwnWord>?>(null) }
    var restoreOrigin by remember { mutableStateOf("json") }
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
        it.language == language && (query.isBlank() ||
            it.word.contains(query.trim(), ignoreCase = true) ||
            it.ownMeaning.contains(query.trim(), ignoreCase = true))
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
            title = { Text(if (restoreOrigin == "tsv")
                "Zelfgeschreven woordenlijst importeren?" else "Eigen woorden herstellen?") },
            text = {
                Text("Dit bestand bevat " + restore.size + " zelfgeschreven woorden. " +
                    newCount + " zijn nieuw, " + duplicateCount + " staan al lokaal. " +
                    "Samenvoegen bewaart altijd je huidige betekenis bij dubbele woorden. " +
                    (if (canMerge) "Samenvoegen past binnen de limiet."
                    else "Samenvoegen kan niet door een onleesbare lijst of capaciteitslimiet.") +
                    " Vervangen wist eerst al je huidige eigen woordbetekenissen. " +
                    "Deze teksten zijn nooit geverifieerde vertalingen.")
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
                        val saved = deck.save(language, word, meaning)
                        notice = if (saved) "Jouw eigen betekenis is lokaal opgeslagen."
                            else "Niet opgeslagen: controleer tekst, dubbele gegevens " +
                                "of limiet van 120 woorden."
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
                Text("Mijn woorden in " + language.uppercase() +
                    ": " + filtered.size, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(70) },
                    label = { Text("Zoek in eigen woorden") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                filtered.forEach { saved ->
                    Text(saved.word + " — " + saved.ownMeaning,
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Zelf ingevoerd · niet geverifieerd",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            editing = saved
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
