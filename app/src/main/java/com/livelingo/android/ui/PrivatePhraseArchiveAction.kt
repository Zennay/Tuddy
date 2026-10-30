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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseArchive
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivatePhraseShortcuts
import com.livelingo.android.PrivatePhraseTopics
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ArchiveExport(val bytes: ByteArray, val archive: PrivatePhraseArchive)
private data class ArchiveRestore(
    val archive: PrivatePhraseArchive,
    val originalNotes: List<PersonalPhrase>,
    val originalThemes: ByteArray?
)

/** Only user-selected files; no automatic network, backup or restore. */
@Composable
internal fun PrivatePhraseArchiveAction(
    deck: PrivatePhraseDeck,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topics = remember(context) { PrivatePhraseTopics(context) }
    var feedback by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var previewExport by remember { mutableStateOf<ArchiveExport?>(null) }
    var pendingWrite by remember { mutableStateOf<ByteArray?>(null) }
    var previewRestore by remember { mutableStateOf<ArchiveRestore?>(null) }
    var mergeMode by remember { mutableStateOf(true) }

    val saveFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val bytes = pendingWrite
        pendingWrite = null
        if (uri != null && bytes != null) {
            if (deck.exportArchiveSnapshot(context)?.contentEquals(bytes) != true) {
                feedback = "Je zinnen of thema’s zijn veranderd. Kies opnieuw back-up."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(bytes)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                feedback = if (saved) "Zinnen én thema’s bewaard op jouw gekozen locatie."
                    else "Bewaren mislukt; al je lokale gegevens blijven staan."
            }
        }
    }
    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        previewRestore = null
        if (uri != null && !importing) {
            importing = true
            feedback = "Privébestand wordt veilig gelezen…"
            scope.launch {
                // A chosen Android provider may be remote or slow: never block
                // the Compose UI thread during its bounded stream reads.
                val bytes = withContext(Dispatchers.IO) {
                    runCatching {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalArgumentException("Unreadable file")
                        val data = ByteArrayOutputStream()
                        input.use { stream ->
                            val buffer = ByteArray(4096)
                            while (true) {
                                val n = stream.read(buffer)
                                if (n < 0) break
                                if (n == 0 || data.size() + n > 70_000)
                                    throw IllegalArgumentException("Oversized archive")
                                data.write(buffer, 0, n)
                            }
                        }
                        data.toByteArray()
                    }.getOrNull()
                }
                // Keep deck/preview state and the human confirmation on Main.
                val candidate = bytes?.let { deck.previewArchiveImport(it) }
                val baseline = deck.entries()
                importing = false
                if (candidate == null || baseline == null) {
                    feedback = "Bestand geweigerd: ongeldig, onbekende versie of te groot. " +
                        "De eigen zinnen blijven intact."
                } else {
                    mergeMode = true
                    previewRestore = ArchiveRestore(
                        candidate, baseline.toList(), topics.exportSnapshot()
                    )
                    feedback = ""
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = deck.entries()?.isNotEmpty() == true,
            onClick = {
                val bytes = deck.exportArchiveSnapshot(context)
                val model = bytes?.let { deck.previewArchiveImport(it) }
                if (bytes == null || model == null) {
                    feedback = "Geen geldige complete back-up mogelijk."
                } else {
                    previewExport = ArchiveExport(bytes, model)
                    feedback = ""
                }
            }
        ) { Text("Bewaar zinnen én thema’s in één bestand…") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !importing,
            onClick = {
                openFile.launch(arrayOf("application/json", "text/plain"))
            }
        ) { Text("Herstel zinnen én thema’s uit één bestand…") }
        Text("Voor toestelwissels. Alles blijft lokaal tot je bewust een " +
            "Android-bestandsprovider kiest. Favorieten en herhaalplanning " +
            "horen niet bij deze back-up.", style = MaterialTheme.typography.bodySmall)
        if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
    }

    val exporting = previewExport
    if (exporting != null) {
        AlertDialog(
            onDismissRequest = { previewExport = null },
            title = { Text("Gecombineerde privé-back-up maken?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("Controleer alle zelfgeschreven zinnen, betekenissen en " +
                        "thema’s voordat je zelf een opslaglocatie kiest. " +
                        "Geen gesprek, audio, favorieten of herhaalplanning.")
                    Text("Thema’s: " + (if (exporting.archive.topics.isEmpty())
                        "geen" else exporting.archive.topics.joinToString(", ")))
                    exporting.archive.notes.forEachIndexed { index, note ->
                        HorizontalDivider()
                        val theme = exporting.archive.assignments[
                            PrivatePhraseShortcuts.fingerprint(note)
                        ] ?: "zonder thema"
                        Text("${index + 1}. [${note.language}] ${note.phrase}")
                        Text("${note.meaningLanguage.uppercase()}: ${note.ownMeaning} · $theme",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Een externe Android-provider kan dit bestand " +
                        "synchroniseren of delen. Het wordt niet verwijderd wanneer " +
                        "je Tuddy-gegevens wist. Betekenissen zijn niet geverifieerd.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingWrite = exporting.bytes
                    previewExport = null
                    saveFile.launch("Tuddy-zinnen-en-themas.json")
                }) { Text("Ik begrijp dit, kies opslag…") }
            },
            dismissButton = {
                TextButton(onClick = { previewExport = null }) { Text("Annuleren") }
            }
        )
    }

    val restoring = previewRestore
    if (restoring != null) {
        AlertDialog(
            onDismissRequest = { previewRestore = null },
            title = { Text("Privéback-up samenvoegen of vervangen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    TextButton(onClick = { mergeMode = true }) {
                        Text(if (mergeMode) "☑ Veilig samenvoegen (aanbevolen)"
                            else "☐ Veilig samenvoegen")
                    }
                    TextButton(onClick = { mergeMode = false }) {
                        Text(if (!mergeMode) "☑ Hele lijst vervangen (destructief)"
                            else "☐ Hele lijst vervangen")
                    }
                    val existing = restoring.originalNotes.map {
                        it.language + "|" + it.phrase.lowercase(java.util.Locale.ROOT)
                    }.toSet()
                    val newRows = restoring.archive.notes.count {
                        it.language + "|" + it.phrase.lowercase(java.util.Locale.ROOT) !in existing
                    }
                    Text(if (mergeMode)
                        "Samenvoegen voegt maximaal $newRows nieuwe zinnen en " +
                            "nieuwe thema’s toe zonder bestaande betekenissen, " +
                            "favorieten, eigen thema-indeling of herhaalplanning " +
                            "te overschrijven. Duplicaten worden overgeslagen; " +
                            "als de limiet van 80 zinnen of 12 thema’s bereikt " +
                            "wordt, verandert niets."
                    else "Vervangen verwijdert de bestaande eigen zinnen en thema’s " +
                        "en wist hun favorieten en herhaalplanning. " +
                        "Andere studieboeken en gesprekken blijven intact.",
                        color = if (mergeMode) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    Text("Nieuwe thema’s: " + (if (restoring.archive.topics.isEmpty())
                        "geen" else restoring.archive.topics.joinToString(", ")))
                    restoring.archive.notes.forEachIndexed { index, note ->
                        HorizontalDivider()
                        val theme = restoring.archive.assignments[
                            PrivatePhraseShortcuts.fingerprint(note)
                        ] ?: "zonder thema"
                        Text("${index + 1}. [${note.language}] ${note.phrase}")
                        Text("${note.meaningLanguage.uppercase()}: ${note.ownMeaning} · $theme",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Alle betekenissen zijn door gebruikers geschreven, " +
                        "niet automatisch vertaald of onafhankelijk gecontroleerd.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val okay = if (mergeMode) deck.mergeArchiveIfUnchanged(
                        context, restoring.archive, restoring.originalNotes,
                        restoring.originalThemes
                    ) else deck.replaceArchiveIfUnchanged(
                        context, restoring.archive, restoring.originalNotes,
                        restoring.originalThemes
                    )
                    if (okay) onChanged()
                    feedback = if (okay && mergeMode)
                        "Nieuwe eigen zinnen en thema’s toegevoegd. Bestaande betekenissen, favorieten en planning behouden."
                    else if (okay)
                        "Alle eigen zinnen en thema’s vervangen; oude oefenplanning gewist."
                    else "Samenvoegen/herstel geweigerd: lokale gegevens zijn gewijzigd, " +
                        "bestand onveilig of limiet bereikt. Niets gewijzigd."
                    previewRestore = null
                }) {
                    Text(if (mergeMode) "Voeg alleen nieuwe zinnen/thema’s toe"
                        else "Vervang ALLES bewust")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewRestore = null }) { Text("Annuleren") }
            }
        )
    }
}
