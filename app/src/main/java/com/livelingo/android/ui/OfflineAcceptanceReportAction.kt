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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Explicitly records a human-observed offline acceptance session.
 *
 * Nothing is uploaded or retained between compositions. Only aggregate counts
 * and checkboxes are recorded; raw speech, audio and transcript text are never
 * requested. This is self-reported evidence, not automatic device certification.
 */
@Composable
internal fun OfflineAcceptanceReportAction(
    enabled: Boolean,
    deviceSnapshot: () -> ByteArray?,
    onPickerStarted: () -> Unit,
    onPickerFinished: () -> Unit
) {
    val context = LocalContext.current
    var baseline by remember { mutableStateOf<ByteArray?>(null) }
    var recordedAt by remember { mutableStateOf(0L) }
    var opened by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var apkIdentityConfirmed by remember { mutableStateOf(false) }
    var disconnectedConfirmed by remember { mutableStateOf(false) }
    var permissionAndStopConfirmed by remember { mutableStateOf(false) }
    var noCloudFallbackConfirmed by remember { mutableStateOf(false) }
    var privacyClearingConfirmed by remember { mutableStateOf(false) }
    var nlEnSpeech by remember { mutableStateOf("0") }
    var enNlSpeech by remember { mutableStateOf("0") }
    var nlEnTranslation by remember { mutableStateOf("0") }
    var enNlTranslation by remember { mutableStateOf("0") }
    var maxLatencyMs by remember { mutableStateOf("") }
    var pendingReport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingBaseline by remember { mutableStateOf<ByteArray?>(null) }

    fun count(value: String): Int? = value.toIntOrNull()?.takeIf { it in 0..10 }
    fun latency(value: String): Long? = value.toLongOrNull()?.takeIf { it in 1..600000 }
    fun reportBytes(): ByteArray? {
        val identity = baseline ?: return null
        val speechCounts = listOf(nlEnSpeech, enNlSpeech).map(::count)
        val translationCounts = listOf(nlEnTranslation, enNlTranslation).map(::count)
        val worstLatency = latency(maxLatencyMs)
        val complete = apkIdentityConfirmed && disconnectedConfirmed &&
            permissionAndStopConfirmed && noCloudFallbackConfirmed &&
            privacyClearingConfirmed && speechCounts.all { it == 10 } &&
            translationCounts.all { it == 10 } && worstLatency != null
        val baseText = identity.toString(Charsets.UTF_8)
        val report = buildString {
            appendLine("Tuddy manual offline acceptance record")
            appendLine("Record format: aggregate observations only; no raw speech, audio or transcript")
            appendLine("Self-reported by device operator: yes")
            appendLine("Independently verified by Tuddy: no")
            appendLine("Session recorded at epoch milliseconds UTC: $recordedAt")
            appendLine("Observation completeness: ${if (complete) "all required observations marked complete" else "incomplete or one or more observations failed"}")
            appendLine("APK identity and SHA-256 matched the selected release candidate: $apkIdentityConfirmed")
            appendLine("Airplane mode enabled and Wi-Fi/mobile connectivity independently disabled: $disconnectedConfirmed")
            appendLine("Microphone permission and explicit Stop/lifecycle behavior observed: $permissionAndStopConfirmed")
            appendLine("No cloud/network speech or translation fallback observed: $noCloudFallbackConfirmed")
            appendLine("Conversation cleared and no old transcript returned after relaunch: $privacyClearingConfirmed")
            appendLine("NL to EN successful offline speech utterances (0-10): ${nlEnSpeech.ifBlank { "not recorded" }}")
            appendLine("EN to NL successful offline speech utterances (0-10): ${enNlSpeech.ifBlank { "not recorded" }}")
            appendLine("NL to EN successful local translations (0-10): ${nlEnTranslation.ifBlank { "not recorded" }}")
            appendLine("EN to NL successful local translations (0-10): ${enNlTranslation.ifBlank { "not recorded" }}")
            appendLine("Maximum observed end-to-end latency in milliseconds: ${maxLatencyMs.ifBlank { "not recorded" }}")
            appendLine()
            appendLine("Device and installed-build snapshot (captured before this session report):")
            append(baseText.trimEnd())
            appendLine()
            appendLine()
            appendLine("LIMITATION: this file is a human-entered observation record, not independent proof, a signed release, or production authorization.")
        }
        return report.toByteArray(Charsets.UTF_8).takeIf { it.size in 1..16384 }
    }

    val saveReport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        onPickerFinished()
        val selected = pendingReport
        val expectedBaseline = pendingBaseline
        pendingReport = null
        pendingBaseline = null
        if (uri == null || selected == null || expectedBaseline == null) {
            message = "Rapportopslag geannuleerd; er is geen bestand geschreven."
        } else {
            val current = deviceSnapshot()
            if (current == null || !current.contentEquals(expectedBaseline)) {
                message = "Apparaat- of modelstatus veranderde tijdens de bestandskeuze. " +
                    "Open het rapport opnieuw om een verouderd verslag te voorkomen."
            } else {
                val written = runCatching {
                    val output = context.contentResolver.openOutputStream(uri, "w")
                        ?: return@runCatching false
                    output.use {
                        it.write(selected)
                        it.flush()
                    }
                    true
                }.getOrDefault(false)
                message = if (written)
                    "Handmatig waarnemingsrapport opgeslagen. Controleer de gekozen provider op externe synchronisatie."
                else "Opslaan mislukt; het rapport is niet bewaard."
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
                val captured = deviceSnapshot()
                if (captured == null || captured.isEmpty()) {
                    message = "De exacte apparaat-/APK-identiteit is nog niet beschikbaar. Open de offline startcheck opnieuw."
                } else {
                    baseline = captured.copyOf()
                    recordedAt = System.currentTimeMillis()
                    opened = true
                    message = ""
                }
            }
        ) { Text("Leg fysieke offline-waarnemingen vast…") }
        Text(
            "Leg na een echte Android 12+-toestelsessie alleen uitkomsten en aantallen vast. " +
                "Geen gesproken woorden, transcript, audio of vrije notities. Het rapport is zelfgerapporteerd.",
            style = MaterialTheme.typography.bodySmall
        )
        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }

    if (opened) {
        val readyReport = reportBytes()
        AlertDialog(
            onDismissRequest = { opened = false; baseline = null },
            title = { Text("Offline sessie vastleggen") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Vul alleen waarnemingen in die je zojuist op dit fysieke toestel hebt gedaan. " +
                            "Een vinkje of dit rapport verandert geen releasegate en wordt niet automatisch geverifieerd.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    AcceptanceObservationCheck(
                        "Geïnstalleerde APK en SHA-256 komen exact overeen met de gekozen build.",
                        apkIdentityConfirmed
                    ) { apkIdentityConfirmed = it }
                    AcceptanceObservationCheck(
                        "Vliegtuigmodus aan; wifi en mobiele data zijn onafhankelijk uitgeschakeld.",
                        disconnectedConfirmed
                    ) { disconnectedConfirmed = it }
                    AcceptanceObservationCheck(
                        "Microfoonmachtiging, expliciet Stoppen en stoppen bij verlaten van de app waargenomen.",
                        permissionAndStopConfirmed
                    ) { permissionAndStopConfirmed = it }
                    AcceptanceObservationCheck(
                        "Geen netwerk- of cloudfallback waargenomen tijdens spraak en vertaling.",
                        noCloudFallbackConfirmed
                    ) { noCloudFallbackConfirmed = it }
                    AcceptanceObservationCheck(
                        "Gesprek gewist; na herstart kwam geen oude transcripttekst terug.",
                        privacyClearingConfirmed
                    ) { privacyClearingConfirmed = it }
                    Text("Geslaagde waarnemingen", fontWeight = FontWeight.SemiBold)
                    CountField("NL → EN · offline spraak (0–10)", nlEnSpeech) { nlEnSpeech = it }
                    CountField("EN → NL · offline spraak (0–10)", enNlSpeech) { enNlSpeech = it }
                    CountField("NL → EN · lokale vertaling (0–10)", nlEnTranslation) { nlEnTranslation = it }
                    CountField("EN → NL · lokale vertaling (0–10)", enNlTranslation) { enNlTranslation = it }
                    OutlinedTextField(
                        value = maxLatencyMs,
                        onValueChange = { maxLatencyMs = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hoogste waargenomen latentie (ms)") },
                        singleLine = true
                    )
                    Text(
                        "Het bestand bevat de exacte APK-/apparaatstatus plus bovenstaande aggregaten. " +
                            "Een externe Android-bestandsprovider kan het bestand synchroniseren.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (readyReport != null) {
                        Text(
                            readyReport.toString(Charsets.UTF_8),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = readyReport != null,
                    onClick = {
                        val capturedBaseline = baseline?.copyOf()
                        val fresh = deviceSnapshot()
                        val bytes = reportBytes()
                        if (capturedBaseline == null || fresh == null ||
                            !fresh.contentEquals(capturedBaseline) || bytes == null) {
                            message = "Apparaatstatus is veranderd of onvolledig. Open het rapport opnieuw."
                            opened = false
                            baseline = null
                        } else {
                            pendingReport = bytes
                            pendingBaseline = capturedBaseline
                            opened = false
                            onPickerStarted()
                            saveReport.launch("Tuddy-offline-waarnemingsrapport.txt")
                        }
                    }
                ) { Text("Bewaar rapport…") }
            },
            dismissButton = {
                TextButton(onClick = { opened = false; baseline = null }) {
                    Text("Annuleren")
                }
            }
        )
    }
}

@Composable
private fun AcceptanceObservationCheck(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(2)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true
    )
}
