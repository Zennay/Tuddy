package com.livelingo.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.SessionTranslationCorrectionTransfer
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
    var preview by remember { mutableStateOf<List<SessionTranslationCorrection>?>(null) }
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

    val open = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            preview = null
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
                        SessionTranslationCorrectionTransfer.decode(out.toByteArray())
                    }.getOrNull()
                }
                busy = false
                if (decoded == null) {
                    notice = "Bestand geweigerd: verwacht een geldig Tuddy-JSON met " +
                        "maximaal 40 exacte eigen correcties. Er is niets gewijzigd."
                } else {
                    preview = decoded
                    notice = "Controleer alle zinnen voordat je huidige tijdelijke " +
                        "correcties bewust vervangt."
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
                    "Alleen handmatig: exporteer een apart JSON-bestand of open " +
                        "een eerder door jezelf opgeslagen Tuddy-bestand. " +
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
                    onClick = { preview = null; notice = ""; open.launch(
                        arrayOf("application/json", "text/plain")
                    ) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open eerder opgeslagen correcties…") }
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
                            "Dit vervangt de huidige " + entries.size +
                            " tijdelijke correcties volledig. Niet samenvoegen.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    imported.forEachIndexed { index, row ->
                        Text(
                            (index + 1).toString() + ". " + row.sourceCode +
                                " → " + row.targetCode + ": " + row.original +
                                " → " + row.translation,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = {
                            if (onReplace(imported)) {
                                undoBefore = entries.toList()
                                undoAfter = imported.toList()
                                preview = null
                                notice = "Geselecteerde correcties zijn alleen in RAM geladen. " +
                                    "Bestaande gespreksregels zijn niet gewijzigd."
                            } else {
                                notice = "Import geweigerd. Er zijn geen wijzigingen toegepast."
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ja, vervang mijn tijdelijke correcties") }
                    TextButton(
                        onClick = { preview = null; notice = "Import geannuleerd." }
                    ) { Text("Annuleren: behoud huidige correcties") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Sluiten") }
        }
    )
}
