package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.OfflineTextReceipt
import com.livelingo.android.OfflineTextStage
import com.livelingo.android.SpeechPackReceipt
import com.livelingo.android.SpeechPackStage

/**
 * Two independent device evidence checks, no implied downloads or cloud fallback.
 * Android 12 cannot report speech-pack readiness per language: never green-light it.
 */
@Composable
internal fun OfflineReadinessDialog(
    spokenLabel: String,
    targetLabel: String,
    speechTag: String,
    sourceTextCode: String,
    targetTextCode: String,
    speechServicePresent: Boolean,
    speech: SpeechPackReceipt,
    textModel: OfflineTextReceipt,
    isListening: Boolean,
    isPreparingText: Boolean,
    isModelStorageBusy: Boolean,
    downloadNotice: String,
    onCheckBoth: () -> Unit,
    onCheckSpeech: () -> Unit,
    onDownloadSpeech: () -> Unit,
    onCancelSpeech: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onCopyDiagnostics: () -> Boolean,
    onCheckText: () -> Unit,
    onPrepareText: () -> Unit,
    onEditLanguages: () -> Unit,
    onOpenTyped: () -> Unit,
    onDismiss: () -> Unit
) {
    val diagnosticsMessage = remember { mutableStateOf("") }
    val speechCurrent = speech.tag == speechTag
    val textCurrent = textModel.source == sourceTextCode &&
        textModel.target == targetTextCode
    val speechReady = speechServicePresent && speechCurrent &&
        speech.stage == SpeechPackStage.READY
    val textReady = textCurrent && textModel.stage == OfflineTextStage.READY
    val checkingSpeech = speechCurrent && speech.stage in setOf(
        SpeechPackStage.CHECKING, SpeechPackStage.DOWNLOADING)
    val checkingText = textCurrent && textModel.stage == OfflineTextStage.CHECKING
    val canInspect = !isListening && !isPreparingText && !isModelStorageBusy
    val complete = speechReady && textReady
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline startcheck") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("$spokenLabel → $targetLabel",
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (complete) "Beide lokale pakketten zijn door de afzonderlijke " +
                        "apparaatcontroles bevestigd. Dit bewijst niet hoe goed spraak " +
                        "of de vertaling in de praktijk werkt."
                    else "Controleer twee aparte lokale voorzieningen. Een taal in de lijst " +
                        "of een gedownloade vertaler bewijst niet dat offline spraak werkt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                Text("1 · Spraakherkenning op dit toestel",
                    style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        !speechServicePresent -> "Geen on-device Android-spraakdienst. " +
                            "Alleen tekstvertaling is mogelijk; nooit cloudspraak."
                        !speechCurrent -> "Voor deze spreektaal is nog geen actuele controle."
                        else -> speech.detail
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCheckSpeech,
                        enabled = canInspect && speechServicePresent && !checkingSpeech,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Controleer spraak") }
                    if (speechCurrent && speech.canDownload) {
                        Button(
                            onClick = onDownloadSpeech,
                            enabled = canInspect && speechServicePresent,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Spraak via wifi") }
                    }
                    if (checkingSpeech) {
                        OutlinedButton(
                            onClick = onCancelSpeech,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop volgen") }
                    }
                    if (speechCurrent && speechServicePresent &&
                        speech.stage == SpeechPackStage.UNKNOWN) {
                        OutlinedButton(
                            onClick = onOpenSpeechSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open Android spraakinstellingen") }
                    }
                }
                HorizontalDivider()
                Text("2 · Tekst vertalen zonder internet",
                    style = MaterialTheme.typography.titleSmall)
                Text(
                    if (textCurrent) textModel.detail
                    else "Voor deze talen zijn de lokale tekstmodellen nog niet gecontroleerd.",
                    style = MaterialTheme.typography.bodySmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onCheckText,
                        enabled = canInspect && !checkingText,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Controleer tekst")
                    }
                    if (!textReady && sourceTextCode != targetTextCode) {
                        Button(onClick = onPrepareText, enabled = canInspect,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Tekst via wifi")
                        }
                    }
                }
                if (downloadNotice.isNotBlank())
                    Text(downloadNotice, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                OutlinedButton(
                    onClick = onCheckBoth,
                    enabled = canInspect && speechServicePresent &&
                        !checkingSpeech && !checkingText,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Controleer beide opnieuw") }
                OutlinedButton(onClick = onEditLanguages, modifier = Modifier.fillMaxWidth()) {
                    Text("Wijzig talen of bekijk details")
                }
                OutlinedButton(
                    onClick = {
                        diagnosticsMessage.value = if (onCopyDiagnostics())
                            "Diagnoserapport gekopieerd. Het bevat geen transcript of audio."
                        else "Kopiëren is niet gelukt."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kopieer diagnoserapport") }
                if (diagnosticsMessage.value.isNotBlank())
                    Text(diagnosticsMessage.value, style = MaterialTheme.typography.bodySmall)
                Text("Het rapport bevat alleen apparaat-, taal- en modelstatus; geen transcript of audio.",
                    style = MaterialTheme.typography.bodySmall)
                if (!isListening) {
                    TextButton(onClick = onOpenTyped, modifier = Modifier.fillMaxWidth()) {
                        Text("Of typ een zin zonder microfoon")
                    }
                }
                Text("Downloads gebeuren alleen na een aparte tik via wifi. " +
                    "Een groen modelrapport is geen fysieke vliegtuigmodusproef. " +
                    "Kurmanji, Sorani en Pasjtoe hebben nog geen geverifieerde " +
                    "vrije offline spraak- en tekstvertaling.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar gesprek") }
        }
    )
}
