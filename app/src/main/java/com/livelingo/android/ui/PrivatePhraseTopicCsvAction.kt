package com.livelingo.android.ui

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

private data class TopicCsvExport(val bytes: ByteArray, val archive: PrivatePhraseArchive)
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
    var pendingWrite by remember { mutableStateOf<ByteArray?>(null) }
    var importReview by remember { mutableStateOf<TopicCsvImport?>(null) }
    var mergeOnly by remember { mutableStateOf(true) }

    val saveFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val bytes = pendingWrite
        pendingWrite = null
        if (uri != null && bytes != null) {
            val fresh = PrivatePhraseTopicCsv.snapshot(context, deck)?.first
            if (fresh == null || !fresh.contentEquals(bytes)) {
                notice = "Eigen zinnen of thema's zijn veranderd: controleer CSV opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(bytes)
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
                notice = ""
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedButton(
            enabled = deck.entries()?.isNotEmpty() == true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val candidate = PrivatePhraseTopicCsv.snapshot(context, deck)
                if (candidate == null) {
                    notice = "Geen geldige zinnen- en thema-CSV mogelijk."
                } else {
                    exportReview = TopicCsvExport(candidate.first, candidate.second)
                    notice = ""
                }
            }
        ) { Text("Exporteer zinnen mét thema's als CSV…") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { openFile.launch(arrayOf("text/csv", "text/plain", "application/octet-stream")) }
        ) { Text("Importeer zinnen mét thema's uit CSV…") }
        Text(
            "Bewerkbare vijfkoloms-CSV met een eigen thema per zin. Dit is anders " +
                "dan de gewone vierkoloms-CSV. Alles wordt eerst volledig getoond; " +
                "geen automatische vertaling, upload of achtergrondopslag.",
            style = MaterialTheme.typography.bodySmall
        )
        if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodySmall)
    }

    val export = exportReview
    if (export != null) {
        AlertDialog(
            onDismissRequest = { exportReview = null },
            title = { Text("CSV met alle eigen zinnen en thema's bewaren?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Bekijk alle persoonlijke zinnen en zelfgekozen thema's hieronder. " +
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
                TextButton(onClick = {
                    pendingWrite = export.bytes
                    exportReview = null
                    saveFile.launch("Tuddy-eigen-zinnen-met-themas.csv")
                }) { Text("Ik begrijp dit, kies opslag…") }
            },
            dismissButton = {
                TextButton(onClick = { exportReview = null }) { Text("Annuleren") }
            }
        )
    }

    val review = importReview
    if (review != null) {
        AlertDialog(
            onDismissRequest = { importReview = null },
            title = { Text("Eigen CSV-zinnen én thema's controleren") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    TextButton(onClick = { mergeOnly = true }) {
                        Text(if (mergeOnly) "☑ Voeg veilig alleen nieuwe toe"
                            else "☐ Voeg veilig alleen nieuwe toe")
                    }
                    TextButton(onClick = { mergeOnly = false }) {
                        Text(if (!mergeOnly) "☑ Vervang hele privé-lijst (destructief)"
                            else "☐ Vervang hele privé-lijst (destructief)")
                    }
                    Text(
                        if (mergeOnly)
                            "Behoud je eigen bestaande betekenissen, thema's, favorieten " +
                                "en herhaaldatums. Alleen nieuwe zinnen en hun thema's " +
                                "worden toegevoegd binnen de limiet van 80 zinnen en 12 thema's."
                        else
                            "Let op: dit verwijdert al je bestaande eigen zinnen, " +
                                "thema's, favorieten en herhaalplanning. Alleen de " +
                                "hieronder getoonde zinnen en thema's komen terug.",
                        color = if (mergeOnly) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    review.archive.notes.forEachIndexed { i, note ->
                        HorizontalDivider()
                        Text((i + 1).toString() + ". " + note.language.uppercase() +
                            " → " + note.meaningLanguage.uppercase())
                        Text(note.phrase, style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (note.language == "ckb" || note.language == "ps")
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                        Text("Eigen betekenis: " + note.ownMeaning,
                            style = MaterialTheme.typography.bodySmall)
                        Text("Thema: " + (review.archive.assignments[
                            PrivatePhraseShortcuts.fingerprint(note)
                        ] ?: "zonder thema"), style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Dit zijn zelfgeschreven, niet beoordeelde betekenissen. " +
                        "De bestanden zijn nooit automatisch uit een gesprek gekopieerd.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val saved = if (mergeOnly) {
                        deck.mergeArchiveIfUnchanged(
                            context, review.archive, review.originalNotes, review.originalTopics
                        )
                    } else {
                        deck.replaceArchiveIfUnchanged(
                            context, review.archive, review.originalNotes, review.originalTopics
                        )
                    }
                    if (saved) onChanged()
                    notice = if (saved && mergeOnly)
                        "Nieuwe eigen zinnen en thema's toegevoegd; bestaande informatie behouden."
                    else if (saved)
                        "Eigen zinnen en thema's bewust vervangen; oude planning gewist."
                    else "Import geweigerd: gegevens zijn gewijzigd of de " +
                        "taal/thema-limiet is bereikt. Niets gewijzigd."
                    importReview = null
                }) {
                    Text(if (mergeOnly) "Voeg uitsluitend nieuwe zinnen toe"
                        else "Vervang alle eigen zinnen en thema's")
                }
            },
            dismissButton = {
                TextButton(onClick = { importReview = null }) { Text("Annuleren") }
            }
        )
    }
}
