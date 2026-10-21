package com.livelingo.android.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.SpeechPackReceipt
import com.livelingo.android.SpeechPackStage

/** Read-only device inventory; nothing here downloads a model or opens the mic. */
internal data class SpeechPackBrowserOption(val label: String, val tag: String)

@Composable
internal fun SpeechPackBrowserDialog(
    options: List<SpeechPackBrowserOption>,
    currentTag: String,
    receipts: Map<String, SpeechPackReceipt>,
    scanning: Boolean,
    downloadTag: String?,
    scannedCount: Int,
    speechServiceAvailable: Boolean,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onPrepare: (String) -> Unit,
    onCancelFollow: () -> Unit,
    onCheckOne: (String) -> Unit,
    onCopyCatalog: () -> Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var onlyReady by remember { mutableStateOf(false) }
    var copyNotice by remember { mutableStateOf("") }
    var confirmTag by remember { mutableStateOf<String?>(null) }
    val visibleOptions = options.filter { option ->
        (!onlyReady || receipts[option.tag]?.stage == SpeechPackStage.READY) &&
            (query.isBlank() || option.label.contains(query, ignoreCase = true) ||
                option.tag.contains(query, ignoreCase = true))
    }
    val ready = options.count { receipts[it.tag]?.stage == SpeechPackStage.READY }
    val offered = options.count { receipts[it.tag]?.stage == SpeechPackStage.DOWNLOADABLE }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline spreektaalpakketten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Controleer welke taalpakketten de Android-spraakdienst op dit toestel meldt. " +
                        "Alleen exact gerapporteerde geïnstalleerde pakketten krijgen de status 'Aanwezig'. " +
                        "Geen cloudspraak, microfoonopname of automatische download.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (!speechServiceAvailable)
                        "Dit toestel biedt geen on-device spraakdienst. Je kunt wel de taalkeuzes bekijken."
                    else if (android.os.Build.VERSION.SDK_INT < 33)
                        "Android 12 kan de lokale status niet per taal opvragen. Selecteer je taal en controleer het spraakpakket in Android-instellingen."
                    else if (scanning)
                        "Controleren: $scannedCount van ${options.size} · aanwezig $ready · download aangeboden $offered."
                    else
                        "Gecontroleerd: ${receipts.size} van ${options.size} · aanwezig $ready · download aangeboden $offered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onScan,
                        enabled = speechServiceAvailable && !scanning && downloadTag == null
                    ) { Text("Controleer alle talen") }
                    if (scanning) {
                        OutlinedButton(onClick = onStopScan) { Text("Stop controle") }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    label = { Text("Zoek spreektaal of regiocode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { onlyReady = !onlyReady }) {
                    Text(if (onlyReady) "Toon ook onbekende talen" else "Toon alleen aanwezig")
                }
                OutlinedButton(
                    onClick = {
                        copyNotice = if (onCopyCatalog())
                            "Pakketoverzicht gekopieerd; bevat geen transcript of audio."
                        else "Kopiëren is niet gelukt."
                    },
                    enabled = !scanning && receipts.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kopieer pakketoverzicht") }
                if (copyNotice.isNotBlank()) {
                    Text(copyNotice, style = MaterialTheme.typography.bodySmall)
                }
                if (visibleOptions.isEmpty()) {
                    Text("Geen talen in deze selectie.", style = MaterialTheme.typography.bodySmall)
                }
                visibleOptions.forEach { option ->
                    HorizontalDivider()
                    Text(
                        "${option.label} (${option.tag})" +
                            if (option.tag == currentTag) " · nu gekozen" else "",
                        style = MaterialTheme.typography.titleSmall
                    )
                    val receipt = receipts[option.tag]
                    val state = when (receipt?.stage) {
                        null, SpeechPackStage.UNCHECKED -> "Niet gecontroleerd"
                        SpeechPackStage.CHECKING -> "Wordt gecontroleerd"
                        SpeechPackStage.READY -> "Aanwezig volgens Android"
                        SpeechPackStage.DOWNLOADABLE -> "Download mogelijk via aparte startcheck"
                        SpeechPackStage.PENDING, SpeechPackStage.REQUESTED -> "Wacht op installatie"
                        SpeechPackStage.ONLINE_ONLY -> "Alleen online beschikbaar; niet bruikbaar voor Tuddy"
                        SpeechPackStage.UNSUPPORTED -> "Niet lokaal beschikbaar"
                        SpeechPackStage.DOWNLOADING -> "Download loopt in Android"
                        SpeechPackStage.ERROR, SpeechPackStage.UNKNOWN -> "Status onbekend"
                    }
                    Text(state, style = MaterialTheme.typography.bodySmall)
                    if (receipt != null && receipt.detail.isNotBlank()) {
                        Text(receipt.detail, style = MaterialTheme.typography.labelSmall)
                    }
                    if (option.tag != currentTag) {
                        TextButton(onClick = { onSelect(option.tag) }) {
                            Text("Kies als spreektaal")
                        }
                    }
                }
                Text(
                    "Kiezen activeert geen microfoon. Gebruik daarna 'Offline startcheck' " +
                        "voor jouw gekozen taal en afzonderlijk het tekstmodel. " +
                        "Kurmanji, Sorani en Pasjtoe hebben nog geen complete offline ASR.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
