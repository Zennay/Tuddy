package com.livelingo.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.SessionTranslationCorrectionTransfer
import com.livelingo.android.SessionTranslationCorrectionCsv
import com.livelingo.android.SessionCorrectionPrintableWorksheet
import com.livelingo.android.PrivateOfflineSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * A consciously selected Android document provider carries a portable JSON.
 * No app-internal disk cache, implicit backup, analytics or remote service.
 * The user's selected document provider MAY sync to cloud; always disclose it.
 */
@Composable
internal fun SessionCorrectionTransferDialog(
    entries: List<SessionTranslationCorrection>,
    onReplace: (List<SessionTranslationCorrection>) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingCsvExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingHtmlExport by remember { mutableStateOf<ByteArray?>(null) }
    var exportSelectionOpen by remember { mutableStateOf(false) }
    var pasteOpen by remember { mutableStateOf(false) }
    var pastedCsv by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<SessionTranslationCorrection>?>(null) }
    var selectedRows by remember { mutableStateOf(emptySet<Int>()) }
    var importSearch by remember { mutableStateOf("") }
    var pairPickerOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    // One explicit rollback inside this dialog only; never saved to app storage.
    var undoBefore by remember { mutableStateOf<List<SessionTranslationCorrection>?>(null) }
    var undoAfter by remember { mutableStateOf<List<SessionTranslationCorrection>?>(null) }

    val save = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val bytes = pendingExport
        pendingExport = null
        if (uri != null && bytes != null) {
            busy = true
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            stream.write(bytes)
                            stream.flush()
                        } ?: error("Kan geen bestand schrijven")
                    }.isSuccess
                }
                busy = false
                notice = if (success) {
                    "Eigen correcties opgeslagen in de door jou gekozen locatie. " +
                        "Tuddy houdt zelf geen bestand bij."
                } else "Bestand opslaan mislukt; huidige correcties blijven ongewijzigd."
            }
        }
    }

    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val bytes = pendingCsvExport
        pendingCsvExport = null
        if (uri != null && bytes != null) {
            busy = true
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            stream.write(bytes)
                            stream.flush()
                        } ?: error("Kan geen CSV-bestand schrijven")
                    }.isSuccess
                }
                busy = false
                notice = if (success) {
                    "Bewerkbare CSV bewust opgeslagen bij jouw bestandsprovider. " +
                        "Tuddy bewaart zelf geen kopie."
                } else "CSV opslaan mislukt; huidige correcties blijven ongewijzigd."
            }
        }
    }

    val saveHtml = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        val bytes = pendingHtmlExport
        pendingHtmlExport = null
        if (uri != null && bytes != null) {
            busy = true
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            stream.write(bytes)
                            stream.flush()
                        } ?: error("Kan geen HTML-bestand schrijven")
                    }.isSuccess
                }
                busy = false
                notice = if (success) {
                    "Privé oefenblad bewaard bij de door jou gekozen bestandsprovider. " +
                        "Tuddy bewaart zelf geen kopie."
                } else "Oefenblad opslaan mislukt; geen correcties gewijzigd."
            }
        }
    }

    val open = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            preview = null
            selectedRows = emptySet()
            importSearch = ""
            busy = true
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    runCatching {
                        val out = ByteArrayOutputStream()
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("Kan bestand niet openen")
                        input.use { stream ->
                            val buffer = ByteArray(4096)
                            var emptyReads = 0
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                if (count == 0) {
                                    if (++emptyReads > 16) error("Bestandsprovider reageert niet")
                                    continue
                                }
                                emptyReads = 0
                                if (count + out.size() > SessionTranslationCorrectionTransfer.MAX_BYTES)
                                    error("Bestand te groot")
                                out.write(buffer, 0, count)
                            }
                        }
                        val bytes = out.toByteArray()
                        SessionTranslationCorrectionTransfer.decode(bytes)
                            ?: SessionTranslationCorrectionCsv.decode(bytes)
                    }.getOrNull()
                }
                busy = false
                if (decoded == null) {
                    notice = "Bestand geweigerd: verwacht geldig Tuddy-JSON of CSV met " +
                        "maximaal 40 exacte eigen correcties. Er is niets gewijzigd."
                } else {
                    preview = decoded
                    selectedRows = decoded.indices.toSet()
                    importSearch = ""
                    notice = "Controleer alle zinnen. Kies bewust hoe je deze " +
                        "samenvoegt of als vervanging in RAM laadt."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Mijn eigen correcties meenemen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Alleen handmatig: exporteer een JSON of een bewerkbare CSV, of open " +
                        "een eerder door jezelf opgeslagen Tuddy-correctiebestand. " +
                        "De eigen correcties blijven in Tuddy verder alleen in RAM. " +
                        "Kies bij voorkeur lokale opslag: een externe bestandsprovider " +
                        "kan bestanden naar een clouddienst synchroniseren.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Huidig gesprek: " + entries.size + " onthouden correcties.",
                    style = MaterialTheme.typography.labelMedium
                )
                OutlinedButton(
                    onClick = {
                        val bytes = SessionTranslationCorrectionTransfer.encode(entries)
                        if (bytes == null) {
                            notice = "Huidige correcties zijn niet als veilig JSON te exporteren."
                        } else {
                            pendingExport = bytes
                            notice = ""
                            save.launch("tuddy-eigen-vertaalcorrecties.json")
                        }
                    },
                    enabled = !busy && entries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Bewaar mijn eigen JSON-bestand…") }
                OutlinedButton(
                    onClick = {
                        val bytes = SessionTranslationCorrectionCsv.encode(entries)
                        if (bytes == null) {
                            notice = "CSV geweigerd: ongeldige gegevens of een zin die " +
                                "in een rekenblad als formule kan worden gelezen. " +
                                "Gebruik voor zulke zinnen de veilige JSON-export."
                        } else {
                            pendingCsvExport = bytes
                            notice = ""
                            saveCsv.launch("tuddy-eigen-vertaalcorrecties.csv")
                        }
                    },
                    enabled = !busy && entries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Bewaar bewerkbare correcties als CSV…") }
                OutlinedButton(
                    onClick = { exportSelectionOpen = true },
                    enabled = !busy && entries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kies zinnen voor export…") }
                OutlinedButton(
                    onClick = { preview = null; notice = ""; open.launch(
                        arrayOf("application/json", "text/plain", "text/csv", "application/vnd.ms-excel")
                    ) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open eerder opgeslagen correcties…") }
                OutlinedButton(
                    onClick = {
                        pasteOpen = !pasteOpen
                        if (!pasteOpen) pastedCsv = ""
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (pasteOpen) "Sluit plakveld" else "Plak eigen correcties-CSV…") }
                if (pasteOpen) {
                    Text(
                        "Plak bewust een eigen CSV inclusief de kolomkoppen. " +
                            "Tuddy leest je klembord niet automatisch, slaat deze " +
                            "tekst niet op en toont eerst een importvoorbeeld.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pastedCsv,
                        onValueChange = { candidate ->
                            if (candidate.toByteArray(Charsets.UTF_8).size <=
                                SessionTranslationCorrectionCsv.MAX_BYTES) {
                                pastedCsv = candidate
                            } else {
                                notice = "CSV-plaktekst is groter dan 48 KB en geweigerd."
                            }
                        },
                        label = { Text("Eigen CSV-tekst (maximaal 48 KB)") },
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        minLines = 3,
                        maxLines = 6,
                        enabled = !busy
                    )
                    Button(
                        onClick = {
                            val decoded = SessionTranslationCorrectionCsv.decode(
                                pastedCsv.toByteArray(Charsets.UTF_8)
                            )
                            if (decoded == null) {
                                notice = "Ongeldige CSV: controleer de vier exacte " +
                                    "kolommen en maximaal 40 unieke correcties. " +
                                    "Er is niets gewijzigd."
                            } else {
                                preview = decoded
                                selectedRows = decoded.indices.toSet()
                                pastedCsv = ""
                                pasteOpen = false
                                notice = "Controleer eerst alle geplakte correcties. " +
                                    "Kies daarna bewust samenvoegen of vervangen."
                            }
                        },
                        enabled = !busy && pastedCsv.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Toon importvoorbeeld van geplakte CSV") }
                }
                if (notice.isNotBlank()) Text(
                    notice, style = MaterialTheme.typography.bodySmall
                )
                val rollback = undoBefore
                if (rollback != null && entries == undoAfter) {
                    OutlinedButton(
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (onReplace(rollback)) {
                                undoBefore = null
                                undoAfter = null
                                preview = null
                                notice = "De vorige tijdelijke correcties zijn hersteld. " +
                                    "Bestaande gespreksregels zijn niet gewijzigd."
                            } else notice = "Herstellen geweigerd; er is niets gewijzigd."
                        }
                    ) { Text("↶ Maak laatste import ongedaan") }
                }
                val imported = preview
                if (imported != null) {
                    Text(
                        "Importvoorbeeld: " + imported.size + " correcties. " +
                            "Huidige lijst: " + entries.size + ". " +
                            "Kies bewust tussen samenvoegen en volledig vervangen.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        selectedRows.size.toString() + " van " + imported.size +
                            " eigen correcties gekozen. Alleen deze worden overgenomen.",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = importSearch,
                        onValueChange = { importSearch = it.take(80) },
                        label = { Text("Zoek eigen correctie voor selectie") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    val foldedImportSearch = PrivateOfflineSearch.fold(importSearch.trim())
                    val visibleIndexes = imported.indices.filter { index ->
                        val item = imported[index]
                        foldedImportSearch.isEmpty() ||
                            PrivateOfflineSearch.contains(item.original, foldedImportSearch) ||
                            PrivateOfflineSearch.contains(item.translation, foldedImportSearch) ||
                            PrivateOfflineSearch.contains(
                                item.sourceCode + " → " + item.targetCode, foldedImportSearch
                            )
                    }
                    Text(
                        "Zichtbaar bij zoekopdracht: " + visibleIndexes.size +
                            " van " + imported.size + ". Totaal aangevinkt: " +
                            selectedRows.size + ". Verborgen aangevinkte rijen blijven gekozen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { selectedRows = imported.indices.toSet() },
                            enabled = !busy
                        ) { Text("Kies alles") }
                        TextButton(
                            onClick = { selectedRows = emptySet() },
                            enabled = !busy
                        ) { Text("Kies niets") }
                        TextButton(
                            onClick = { selectedRows = selectedRows + visibleIndexes },
                            enabled = !busy && visibleIndexes.isNotEmpty()
                        ) { Text("Kies zichtbare") }
                        TextButton(
                            onClick = { selectedRows = selectedRows - visibleIndexes.toSet() },
                            enabled = !busy && visibleIndexes.isNotEmpty()
                        ) { Text("Haal zichtbare weg") }
                    }
                    val languagePairs = imported.map {
                        it.sourceCode to it.targetCode
                    }.distinct()
                    if (languagePairs.size > 1) {
                        Box {
                            TextButton(
                                enabled = !busy,
                                onClick = { pairPickerOpen = true }
                            ) { Text("Kies alleen één taalpaar… ▾") }
                            DropdownMenu(
                                expanded = pairPickerOpen,
                                onDismissRequest = { pairPickerOpen = false }
                            ) {
                                languagePairs.forEach { pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.first + " → " + pair.second) },
                                        onClick = {
                                            selectedRows = imported.indices.filter { index ->
                                                val row = imported[index]
                                                row.sourceCode == pair.first &&
                                                    row.targetCode == pair.second
                                            }.toSet()
                                            pairPickerOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    visibleIndexes.forEach { index ->
                        val row = imported[index]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = index in selectedRows,
                                onCheckedChange = { selected ->
                                    selectedRows = if (selected) selectedRows + index
                                        else selectedRows - index
                                },
                                enabled = !busy
                            )
                            Text(
                                (index + 1).toString() + ". " + row.sourceCode +
                                    " → " + row.targetCode + ": " + row.original +
                                    " → " + row.translation,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    val chosen = imported.filterIndexed { index, _ ->
                        index in selectedRows
                    }
                    if (chosen.isEmpty()) {
                        Text(
                            "Kies minstens één zin. Zonder selectie wordt niets overschreven.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        SessionCorrectionMergeOptions(
                            current = entries,
                            imported = chosen,
                            enabled = !busy,
                        onCommit = { merged, summary ->
                            val before = entries.toList()
                            if (onReplace(merged)) {
                                undoBefore = before
                                undoAfter = merged.toList()
                                preview = null
                                notice = "Bewust samengevoegd: " + summary +
                                    " Alleen in tijdelijk geheugen. Ongedaan maken is beschikbaar."
                            } else {
                                notice = "Samenvoegen geweigerd. Er is niets gewijzigd."
                            }
                        }
                        )
                    }
                    Button(
                        onClick = {
                            if (onReplace(chosen)) {
                                undoBefore = entries.toList()
                                undoAfter = chosen.toList()
                                preview = null
                                notice = "Geselecteerde correcties zijn alleen in RAM geladen. " +
                                    "Bestaande gespreksregels zijn niet gewijzigd."
                            } else {
                                notice = "Import geweigerd. Er zijn geen wijzigingen toegepast."
                            }
                        },
                        enabled = !busy && chosen.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ja, vervang door mijn selectie") }
                    TextButton(
                        onClick = {
                            preview = null
                            selectedRows = emptySet()
                            notice = "Import geannuleerd."
                        }
                    ) { Text("Annuleren: behoud huidige correcties") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Sluiten") }
        }
    )
    if (exportSelectionOpen) {
        SessionCorrectionExportSelectionDialog(
            entries = entries,
            onExport = { chosen ->
                val bytes = SessionTranslationCorrectionTransfer.encode(chosen)
                if (bytes == null) {
                    notice = "Deze selectie kan niet veilig als JSON worden opgeslagen."
                } else {
                    pendingExport = bytes
                    exportSelectionOpen = false
                    notice = ""
                    save.launch("tuddy-geselecteerde-vertaalcorrecties.json")
                }
            },
            onExportCsv = { chosen ->
                val bytes = SessionTranslationCorrectionCsv.encode(chosen)
                if (bytes == null) {
                    notice = "CSV geweigerd: ongeldige inhoud of mogelijke " +
                        "rekenbladformule. Bewaar deze selectie liever als JSON."
                } else {
                    pendingCsvExport = bytes
                    exportSelectionOpen = false
                    notice = ""
                    saveCsv.launch("tuddy-geselecteerde-vertaalcorrecties.csv")
                }
            },
            onExportHtml = { chosen, showAnswers ->
                val bytes = SessionCorrectionPrintableWorksheet.create(chosen, showAnswers)
                if (bytes == null) {
                    notice = "Afdrukbaar blad geweigerd: ongeldige of te grote selectie."
                } else {
                    pendingHtmlExport = bytes
                    exportSelectionOpen = false
                    notice = ""
                    val name = if (showAnswers) "tuddy-mijn-antwoordblad.html"
                        else "tuddy-mijn-invulblad.html"
                    saveHtml.launch(name)
                }
            },
            onDismiss = { exportSelectionOpen = false }
        )
    }
}
