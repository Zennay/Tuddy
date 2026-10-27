package com.livelingo.android.ui

import android.content.Intent
import android.provider.Settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.OfflineRadioSnapshot
import com.livelingo.android.LocalModelStorageAdmission
import com.livelingo.android.LocalModelNetworkAdmission
import com.livelingo.android.LocalSpeechPackDownloadAdmission
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.OfflineTextReceipt
import com.livelingo.android.OfflineSessionDiagnostics
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
    onOpenAirplaneSettings: () -> Unit,
    requireOfflineRadioBeforeListening: Boolean,
    onSetRequireOfflineRadio: (Boolean) -> Unit,
    onBrowseSpeechPacks: () -> Unit,
    onCopyDiagnostics: () -> Boolean,
    sessionDiagnostics: OfflineSessionDiagnostics,
    acceptanceDraft: OfflineAcceptanceDraft,
    onArmStrictOffline: () -> Unit,
    onReturnToConversation: () -> Unit,
    onResetSessionCounters: () -> Unit,
    onReportSnapshot: () -> ByteArray?,
    onReportPickerStarted: () -> Unit,
    onReportPickerFinished: () -> Unit,
    onCheckText: () -> Unit,
    onPrepareText: () -> Unit,
    onEditLanguages: () -> Unit,
    onOpenTyped: () -> Unit,
    onDismiss: () -> Unit
) {
    val diagnosticsMessage = remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var radioStatus by remember { mutableStateOf(OfflineRadioSnapshot.capture(context)) }
    var storageReceipt by remember { mutableStateOf(LocalModelStorageAdmission.inspect()) }
    var networkReceipt by remember(context) {
        mutableStateOf(LocalModelNetworkAdmission.inspect(context))
    }
    var networkSettingsNotice by remember { mutableStateOf("") }
    // Observe on every recomposition after the existing speech-pack receipt.
    // The slot itself is process-wide and survives dialog/activity dismissal.
    val pendingSpeechDownloadTag = LocalSpeechPackDownloadAdmission.pendingTag()
    // Android settings is outside Tuddy. Refresh when the user returns.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                radioStatus = OfflineRadioSnapshot.capture(context)
                storageReceipt = LocalModelStorageAdmission.inspect()
                networkReceipt = LocalModelNetworkAdmission.inspect(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
                Text("Vrije ruimte vóór een nieuwe offline modeldownload",
                    style = MaterialTheme.typography.titleSmall)
                Text(
                    storageReceipt.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (storageReceipt.canBegin)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
                OutlinedButton(
                    onClick = { storageReceipt = LocalModelStorageAdmission.inspect() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Controleer interne opslag opnieuw") }
                Text(
                    "Bestaande lokale vertalingen en modelcontroles werken ook " +
                        "bij weinig ruimte. Een geslaagde opslagcheck garandeert " +
                        "niet dat een nieuw Android-/ML Kit-model past.",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                Text("Wifi voor een bewuste offline modeldownload",
                    style = MaterialTheme.typography.titleSmall)
                Text(networkReceipt.message, style = MaterialTheme.typography.bodySmall,
                    color = if (networkReceipt.canBegin)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error)
                OutlinedButton(
                    onClick = { networkReceipt = LocalModelNetworkAdmission.inspect(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Controleer wifi-toelating opnieuw") }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                        }.onFailure {
                            networkSettingsNotice =
                                "Android-wifiinstellingen konden niet worden geopend. " +
                                    "Open wifi handmatig via je telefooninstellingen."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Android-wifiinstellingen") }
                if (networkSettingsNotice.isNotBlank()) {
                    Text(networkSettingsNotice, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Ook een wifi-hotspot kan mobiele databundels kosten. " +
                        "Tuddy vraagt nieuwe modellen alleen aan op door Android " +
                        "als gevalideerd en ongemeten aangemerkte wifi. " +
                        "Bestaande lokale spraak en vertalingen blijven werken.",
                    style = MaterialTheme.typography.bodySmall
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
                            enabled = canInspect && speechServicePresent &&
                                storageReceipt.canBegin && networkReceipt.canBegin &&
                                pendingSpeechDownloadTag == null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Spraak via wifi") }
                    }
                    if (pendingSpeechDownloadTag != null) {
                        Text(
                            "Android heeft nog een onbevestigd spraakpakketverzoek " +
                                "voor $pendingSpeechDownloadTag. Tuddy start geen " +
                                "tweede aanvraag, ook niet na het sluiten van dit " +
                                "venster. Controleer de daadwerkelijk geïnstalleerde " +
                                "pakketten opnieuw; een eerdere aanvraag is geen " +
                                "bewijs van een bruikbaar lokaal spraakmodel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (checkingSpeech) {
                        OutlinedButton(
                            onClick = onCancelSpeech,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop volgen") }
                    }
                    if (speechCurrent && speechServicePresent &&
                        speech.stage in setOf(
                            SpeechPackStage.UNKNOWN, SpeechPackStage.PENDING,
                            SpeechPackStage.REQUESTED, SpeechPackStage.ONLINE_ONLY,
                            SpeechPackStage.UNSUPPORTED, SpeechPackStage.ERROR,
                            SpeechPackStage.DOWNLOADABLE
                        )) {
                        OutlinedButton(
                            onClick = onOpenSpeechSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open Android spraakinstellingen") }
                    }
                }
                Text(
                    "Tijdens het luisteren controleert Tuddy ook ongeveer elke twee " +
                        "seconden of Android de microfoontoestemming, mute-stand " +
                        "of een oproep-/communicatiemodus meldt. Bij een onveilige " +
                        "of onleesbare status stopt Tuddy de microfoon en de " +
                        "automatische herstart. Dit gebruikt geen belgeschiedenis, " +
                        "audio-opname of extra telefoonmachtiging. Android-status " +
                        "is geen onafhankelijke fysieke privacygarantie.",
                    style = MaterialTheme.typography.bodySmall
                )
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
                        Button(onClick = onPrepareText,
                            enabled = canInspect && storageReceipt.canBegin &&
                                networkReceipt.canBegin,
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
                OutlinedButton(onClick = onBrowseSpeechPacks,
                    enabled = !isListening, modifier = Modifier.fillMaxWidth()) {
                    Text("Bekijk offline spreektaalpakketten")
                }
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
                HorizontalDivider()
                Text("3 · Vliegtuigmodus voorbereiden",
                    style = MaterialTheme.typography.titleSmall)
                Text(
                    "Open zelf Android-instellingen, zet vliegtuigmodus aan en " +
                        "controleer ook wifi en andere verbindingen. Keer daarna " +
                        "terug naar Tuddy om werkelijk zonder verbinding te luisteren. " +
                        "Dit opent alleen instellingen en schakelt niets zelf uit.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Optioneel, alleen voor deze app-sessie: bij strikte " +
                        "vliegtuigmodus weigert Tuddy het starten of automatisch " +
                        "herstarten van de microfoon als Android vliegtuigmodus niet " +
                        "bevestigt, wifi nog aan staat of een netwerktransport " +
                        "wordt gemeld. Ook dit garandeert geen volledige isolatie.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = { onSetRequireOfflineRadio(!requireOfflineRadioBeforeListening) },
                    enabled = !isListening && acceptanceDraft.baseline.value == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (acceptanceDraft.baseline.value != null)
                        "Fysieke proef actief · strikte bewaking vergrendeld"
                    else if (requireOfflineRadioBeforeListening)
                        "Strikte vliegtuigmodus: aan · uitschakelen"
                    else "Strikte vliegtuigmodus voor luisteren inschakelen")
                }
                Text(
                    "Strikte vliegtuigmodus: Tuddy controleert de huidige " +
                        "vliegtuigmodus, Wi-Fi en Android-netwerktransporten bij " +
                        "de start, bij systeemmeldingen én iedere 3 seconden " +
                        "tijdens het luisteren. Bij een verbinding, onbekende " +
                        "status of uitgevallen bewaking stopt de microfoon; " +
                        "automatische herstarts worden geannuleerd. " +
                        "Starten wordt geweigerd als de bewaking niet veilig " +
                        "kan worden ingeschakeld. Dit bewijst geen radiostilte.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(radioStatus.display(), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { radioStatus = OfflineRadioSnapshot.capture(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ververs verbindingsstatus") }
                OutlinedButton(
                    onClick = onOpenAirplaneSettings,
                    enabled = !isListening,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open vliegtuigmodusinstellingen") }
                HorizontalDivider()
                Text("4 · Mijn lokale luistersessie",
                    style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tuddy telt alleen gewijzigde tussenteksten, definitieve herkenningen, " +
                        "spraakfouten en lokaal afgeronde vertalingen. Alle tellers blijven " +
                        "tijdelijk in het geheugen. Een getal is geen bewijs dat vliegtuigmodus " +
                        "actief was of dat de vertaling correct is.",
                    style = MaterialTheme.typography.bodySmall
                )
                sessionDiagnostics.asReportLines().forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = onResetSessionCounters,
                    enabled = !isListening && acceptanceDraft.baseline.value == null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Wis sessietellers") }
                OfflineReadinessFileExportAction(
                    enabled = !isListening,
                    snapshot = onReportSnapshot,
                    onPickerStarted = onReportPickerStarted,
                    onPickerFinished = onReportPickerFinished
                )
                OfflineAcceptanceReportAction(
                    enabled = !isListening,
                    deviceSnapshot = onReportSnapshot,
                    draft = acceptanceDraft,
                    sessionDiagnostics = sessionDiagnostics,
                    currentSourceCode = sourceTextCode,
                    currentTargetCode = targetTextCode,
                    onArmStrictOffline = onArmStrictOffline,
                    onReturnToConversation = onReturnToConversation,
                    onPickerStarted = onReportPickerStarted,
                    onPickerFinished = onReportPickerFinished
                )
                Text("Het rapport bevat apparaat-, taal- en modelstatus plus " +
                    "RAM-sessietellers, maar geen transcript of audio.",
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
