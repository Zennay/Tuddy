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
import androidx.compose.ui.unit.dp

/**
 * A user-owned diagnostic document, never a background upload or approval.
 * The app gives no provider permanent read access and keeps no report history.
 */
@Composable
internal fun OfflineReadinessFileExportAction(
    enabled: Boolean,
    snapshot: () -> ByteArray?
) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<ByteArray?>(null) }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var message by remember { mutableStateOf("") }

    val saveFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val selected = pending
        pending = null
        if (uri != null && selected != null) {
            val fresh = snapshot()
            if (fresh == null || !fresh.contentEquals(selected)) {
                message = "De apparaat- of pakketstatus veranderde. Bekijk het rapport opnieuw."
            } else {
                val written = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(selected)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                message = if (written)
                    "Diagnoserapport bewust opgeslagen. Het bestand kan extern worden gedeeld."
                else "Opslaan mislukt. Je lokale gegevens blijven ongewijzigd."
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val bytes = snapshot()
                if (bytes == null || bytes.size !in 1..8192 ||
                    !bytes.toString(Charsets.UTF_8).toByteArray(Charsets.UTF_8)
                        .contentEquals(bytes)) {
                    message = "Er is nu geen geldig apparaatrapport beschikbaar."
                } else {
                    preview = bytes
                    message = ""
                }
            }
        ) { Text("Bewaar apparaatrapport als tekstbestand…") }
        Text(
            "Alleen op jouw verzoek. Bevat apparaatmodel, systeemversie, " +
                "buildidentiteit, taalkeuzes en lokale pakketstatus; " +
                "geen gesprekken, uitgesproken woorden of audio. " +
                "Een Android-bestandsprovider kan dit extern synchroniseren.",
            style = MaterialTheme.typography.bodySmall
        )
        if (message.isNotBlank())
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
    }
    val shown = preview
    if (shown != null) {
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Zelf apparaatrapport bewaren?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Volledig lokaal rapport, precies zoals het naar jouw " +
                        "gekozen bestandsprovider gaat. De status is door Android " +
                        "gerapporteerd en geen bewijs van echte vliegtuigmodusspraak.",
                        style = MaterialTheme.typography.bodySmall)
                    Text(shown.toString(Charsets.UTF_8),
                        style = MaterialTheme.typography.bodySmall)
                    Text("Het rapport bevat apparaatidentificatie en kan " +
                        "door jouw bestandsprovider worden gesynchroniseerd. " +
                        "Een externe kopie wordt niet gewist door Tuddy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = shown.copyOf()
                    preview = null
                    saveFile.launch("Tuddy-offline-apparaatrapport.txt")
                }) { Text("Ik begrijp dit, kies opslag…") }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }) { Text("Annuleren") }
            }
        )
    }
}
