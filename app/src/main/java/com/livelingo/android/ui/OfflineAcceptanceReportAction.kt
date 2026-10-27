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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.os.Build
import com.livelingo.android.OfflineRadioSnapshot
import com.livelingo.android.OfflineSessionDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    draft: OfflineAcceptanceDraft,
    sessionDiagnostics: OfflineSessionDiagnostics,
    onArmStrictOffline: () -> Unit,
    onReturnToConversation: () -> Unit,
    onPickerStarted: () -> Unit,
    onPickerFinished: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var baseline by draft.baseline
    var recordedAt by draft.recordedAt
    var opened by draft.opened
    var confirmDiscard by draft.confirmDiscard
    var message by draft.message
    var expectedApkSha256 by draft.expectedApkSha256
    var sidecarNotice by draft.sidecarNotice
    var disconnectedConfirmed by draft.disconnectedConfirmed
    var permissionAndStopConfirmed by draft.permissionAndStopConfirmed
    var noCloudFallbackConfirmed by draft.noCloudFallbackConfirmed
    var privacyClearingConfirmed by draft.privacyClearingConfirmed
    // A bounded, transient observation ledger. Never store user utterances.
    var attempts by draft.attempts
    var chosenDirection by draft.chosenDirection
    var observedSpeech by draft.observedSpeech
    var observedTranslation by draft.observedTranslation
    var attemptLatency by draft.attemptLatency
    var attemptMessage by draft.attemptMessage
    var pendingReport by draft.pendingReport
    var pendingBaseline by draft.pendingBaseline
    var pendingCheckpoint by draft.pendingCheckpoint
    var checkpointImported by draft.checkpointImported
    var checkpointNotice by draft.checkpointNotice
    var startedDiagnostics by draft.startedDiagnostics
    var newAttemptFlags by draft.newAttemptFlags

    fun installedApkDigest(bytes: ByteArray?): String? {
        val line = bytes?.toString(Charsets.UTF_8)?.lineSequence()
            ?.firstOrNull { it.startsWith("Installed base APK SHA-256:") } ?: return null
        return line.substringAfter(':').trim().lowercase()
            .takeIf { it.matches(Regex("[0-9a-f]{64}")) }
    }
    fun expectedDigestMatches(bytes: ByteArray?): Boolean {
        val expected = expectedApkSha256.trim().lowercase()
        val installed = installedApkDigest(bytes) ?: return false
        return expected.matches(Regex("[0-9a-f]{64}")) && expected == installed
    }
    fun reportLines(bytes: ByteArray?): Set<String> =
        bytes?.toString(Charsets.UTF_8)?.lineSequence()?.map { it.trim() }?.toSet()
            ?: emptySet()

    fun reportsAirplaneModeOn(bytes: ByteArray?): Boolean =
        "Airplane mode: true" in reportLines(bytes)

    fun reportsWifiRadioDisabled(bytes: ByteArray?): Boolean =
        "Wifi radio enabled (Android reported): false" in reportLines(bytes)

    fun reportsNoNetworkTransports(bytes: ByteArray?): Boolean {
        val lines = reportLines(bytes)
        return "Network transport read failed: false" in lines &&
            "Network transports Android reports: none reported" in lines
    }

    fun reportsOfflineRadioState(bytes: ByteArray?): Boolean =
        reportsAirplaneModeOn(bytes) &&
            reportsWifiRadioDisabled(bytes) &&
            reportsNoNetworkTransports(bytes)

    fun offlineRadioBlockerMessage(bytes: ByteArray?): String = when {
        !reportsAirplaneModeOn(bytes) ->
            "Android rapporteert geen ingeschakelde vliegtuigmodus. De sessie blijft onvolledig."
        !reportsWifiRadioDisabled(bytes) ->
            "Android rapporteert dat de wifi-radio aan staat of de status niet kan lezen. Schakel wifi uit en open het rapport opnieuw."
        !reportsNoNetworkTransports(bytes) ->
            "Android rapporteert een actieve netwerkverbinding of kan de netwerkstatus niet lezen. Verbreek verbindingen en open het rapport opnieuw."
        else -> "De radio-indicatoren zijn niet voldoende voor een complete offline-waarneming."
    }
    fun freshClaims(direction: OfflineObservationDirection, speech: Boolean): Int =
        attempts.zip(newAttemptFlags).count { (entry, isFresh) ->
            isFresh && entry.direction == direction &&
                (if (speech) entry.recognizedSpeech else entry.translatedLocally)
        }

    fun recordAttempt() {
        // A user must return to actual offline mode for EACH entered attempt.
        // This check samples radios at entry, not during microphone capture.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            attemptMessage = "Deze toestelproef vereist Android 12 of hoger."
            return
        }
        val radio = OfflineRadioSnapshot.capture(context)
        if (radio.airplaneModeOn != true || radio.wifiEnabled != false ||
            radio.networkReadFailed || radio.visibleTransports.isNotEmpty()) {
            attemptMessage = "Poging niet vastgelegd: controleer vliegtuigmodus, " +
                "wifi uit en geen Android-netwerktransporten. " +
                "Dit bewijst niet dat eerdere spraak offline verliep."
            return
        }
        val currentIdentity = deviceSnapshot()
        if (installedApkDigest(currentIdentity) == null ||
            installedApkDigest(currentIdentity) != installedApkDigest(baseline)) {
            attemptMessage = "De geïnstalleerde APK-identiteit is niet meer dezelfde " +
                "als bij aanvang. De poging is niet geregistreerd."
            return
        }
        // The counter baseline was captured before the microphone was armed.
        // These counters do NOT prove correctness, only that this app observed
        // local events in the requested direction since the draft started.
        val started = startedDiagnostics
        if (started == null || newAttemptFlags.size != attempts.size) {
            attemptMessage = "Geen geldige live-proefbasis. Bewaar of wis dit concept " +
                "en start opnieuw vóór de volgende microfoonsessie."
            return
        }
        if (sessionDiagnostics.listeningStarts <= started.listeningStarts) {
            attemptMessage = "Start eerst daadwerkelijk een lokale microfoonsessie " +
                "na het openen van dit proefconcept."
            return
        }
        val localSpeechNow = if (chosenDirection == OfflineObservationDirection.NL_EN)
            sessionDiagnostics.nlEnFinalUtterances else sessionDiagnostics.enNlFinalUtterances
        val localSpeechStart = if (chosenDirection == OfflineObservationDirection.NL_EN)
            started.nlEnFinalUtterances else started.enNlFinalUtterances
        val localTranslationNow = if (chosenDirection == OfflineObservationDirection.NL_EN)
            sessionDiagnostics.nlEnLocalTranslations else sessionDiagnostics.enNlLocalTranslations
        val localTranslationStart = if (chosenDirection == OfflineObservationDirection.NL_EN)
            started.nlEnLocalTranslations else started.enNlLocalTranslations
        if (observedSpeech && localSpeechNow - localSpeechStart <=
            freshClaims(chosenDirection, speech = true)) {
            attemptMessage = "Nog geen extra definitieve lokale spraakherkenning " +
                "in deze richting geteld sinds de start van de proef."
            return
        }
        if (observedTranslation && localTranslationNow - localTranslationStart <=
            freshClaims(chosenDirection, speech = false)) {
            attemptMessage = "Nog geen extra afgeronde lokale vertaling " +
                "in deze richting geteld sinds de start van de proef."
            return
        }
        val time = attemptLatency.toLongOrNull()
        if (attemptLatency.isNotBlank() && (time == null || time !in 1L..600000L)) {
            attemptMessage = "Voer een geldige duur in (1–600000 ms) of laat dit veld leeg."
            return
        }
        if (!observedSpeech && observedTranslation) {
            attemptMessage = "Zonder geslaagde spraakherkenning kan geen lokale spraakvertaling slagen."
            return
        }
        if (!observedTranslation && time != null) {
            attemptMessage = "Een vertaaltijd hoort alleen bij een geslaagde lokale vertaling."
            return
        }
        val proposed = OfflineObservedAttempt(
            chosenDirection, observedSpeech, observedTranslation, time
        )
        if (!OfflineObservationLedger.canRecord(attempts, proposed)) {
            attemptMessage = "Maximaal tien pogingen per richting, zonder extra tekstgegevens."
            return
        }
        attempts = attempts + proposed
        newAttemptFlags = newAttemptFlags + true
        attemptMessage = "Poging opgeslagen in tijdelijk appgeheugen."
        observedSpeech = false
        observedTranslation = false
        attemptLatency = ""
        if (chosenDirection == OfflineObservationDirection.NL_EN &&
            OfflineObservationLedger.totals(
                attempts, OfflineObservationDirection.NL_EN
            ).attempts == OfflineObservationLedger.REQUIRED_ATTEMPTS_PER_DIRECTION
        ) chosenDirection = OfflineObservationDirection.EN_NL
    }


    fun reportBytes(): ByteArray? {
        val identity = baseline ?: return null
        val installedDigest = installedApkDigest(identity)
        val expectedDigest = expectedApkSha256.trim().lowercase()
        val digestMatches = expectedDigestMatches(identity)
        val airplaneModeReportedOn = reportsAirplaneModeOn(identity)
        val wifiRadioReportedOff = reportsWifiRadioDisabled(identity)
        val networkTransportsReportedClear = reportsNoNetworkTransports(identity)
        val radioSnapshotReady = airplaneModeReportedOn && wifiRadioReportedOff &&
            networkTransportsReportedClear
        val nlEn = OfflineObservationLedger.totals(attempts, OfflineObservationDirection.NL_EN)
        val enNl = OfflineObservationLedger.totals(attempts, OfflineObservationDirection.EN_NL)
        val worstLatency = OfflineObservationLedger.worstEndToEndLatencyMs(attempts)
        // A failed utterance is still a valid documented attempt. Outcomes
        // must NEVER be converted into an acceptance or success assertion.
        val recordedAllAttempts = OfflineObservationLedger.complete(attempts)
        val allSuccessfulTranslationsTimed =
            OfflineObservationLedger.timedSuccessesComplete(attempts)
        val android12Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val complete = android12Plus && digestMatches && radioSnapshotReady &&
            disconnectedConfirmed &&
            permissionAndStopConfirmed && noCloudFallbackConfirmed &&
            privacyClearingConfirmed && recordedAllAttempts &&
            allSuccessfulTranslationsTimed
        val baseText = identity.toString(Charsets.UTF_8)
        val report = buildString {
            appendLine("Tuddy manual offline acceptance record")
            appendLine("Record format: aggregate observations only; no raw speech, audio or transcript")
            appendLine("Self-reported by device operator: yes")
            appendLine("Manually imported earlier attempt classifications: $checkpointImported")
            appendLine("Imported observations are NOT proof of one continuous offline radio session")
            appendLine("Independently verified by Tuddy: no")
            appendLine("Android 12+ minimum for this record: $android12Plus")
            appendLine("Radio status rechecked before each NEW manually entered attempt: yes (not proof during utterance; imports excluded)")
            appendLine("New on-device-counter-gated attempts in current app session: ${newAttemptFlags.count { it }}")
            appendLine("Previously imported manual attempts not matched to live counters: ${newAttemptFlags.count { !it }}")
            appendLine("RAM source-language ASR and local MT counters used only as consistency checks, not human quality or radio proof")
            appendLine("Session recorded at epoch milliseconds UTC: $recordedAt")
            appendLine("Observation completeness: ${if (complete) "required observations recorded (NOT acceptance)" else "incomplete; inspect unrecorded or unchecked observations"}")
            appendLine("Outcome acceptance by an independent reviewer: NOT GRANTED")
            appendLine("Expected APK SHA-256 supplied by operator (APK file, not artifact ZIP): ${expectedDigest.ifBlank { "not supplied" }}")
            appendLine("Installed base APK SHA-256: ${installedDigest ?: "unavailable"}")
            appendLine("Installed APK digest matches supplied expected APK digest: $digestMatches")
            appendLine("Captured airplane mode reported enabled: $airplaneModeReportedOn")
            appendLine("Captured Wi-Fi radio reported disabled: $wifiRadioReportedOff")
            appendLine("Captured network status readable with no transports: $networkTransportsReportedClear")
            appendLine("Captured radio indicators consistent with offline session: $radioSnapshotReady")
            appendLine("Airplane mode enabled and Wi-Fi/mobile connectivity independently disabled: $disconnectedConfirmed")
            appendLine("Microphone permission and explicit Stop/lifecycle behavior observed: $permissionAndStopConfirmed")
            appendLine("No cloud/network speech or translation fallback observed: $noCloudFallbackConfirmed")
            appendLine("Conversation cleared and no old transcript returned after relaunch: $privacyClearingConfirmed")
            appendLine("NL to EN observed attempts: ${nlEn.attempts}/10")
            appendLine("NL to EN successful on-device recognition: ${nlEn.recognized}")
            appendLine("NL to EN failed recognition: ${nlEn.failedSpeech}")
            appendLine("NL to EN successful local translation: ${nlEn.translated}")
            appendLine("NL to EN failed local translation after recognized speech: ${nlEn.failedTranslationAfterSpeech}")
            appendLine("NL to EN timed successful translations: ${nlEn.timedTranslations}")
            appendLine("EN to NL observed attempts: ${enNl.attempts}/10")
            appendLine("EN to NL successful on-device recognition: ${enNl.recognized}")
            appendLine("EN to NL failed recognition: ${enNl.failedSpeech}")
            appendLine("EN to NL successful local translation: ${enNl.translated}")
            appendLine("EN to NL failed local translation after recognized speech: ${enNl.failedTranslationAfterSpeech}")
            appendLine("EN to NL timed successful translations: ${enNl.timedTranslations}")
            appendLine("Maximum operator-observed end-to-end latency in milliseconds: ${worstLatency ?: "not observed"}")
            appendLine("All twenty human-observed attempts recorded: $recordedAllAttempts")
            appendLine("Every successful translation has a manually measured latency: $allSuccessfulTranslationsTimed")
            appendLine()
            appendLine("Device and installed-build snapshot (captured before this session report):")
            append(baseText.trimEnd())
            appendLine()
            appendLine()
            appendLine("LIMITATION: this file is a human-entered observation record, not independent proof, a signed release, or production authorization.")
        }
        return report.toByteArray(Charsets.UTF_8).takeIf { it.size in 1..16384 }
    }

    fun resetDraft() {
        confirmDiscard = false
        baseline = null
        recordedAt = 0L
        opened = false
        expectedApkSha256 = ""
        sidecarNotice = ""
        disconnectedConfirmed = false
        permissionAndStopConfirmed = false
        noCloudFallbackConfirmed = false
        privacyClearingConfirmed = false
        attempts = emptyList()
        newAttemptFlags = emptyList()
        startedDiagnostics = null
        chosenDirection = OfflineObservationDirection.NL_EN
        observedSpeech = false
        observedTranslation = false
        attemptLatency = ""
        attemptMessage = ""
        pendingReport = null
        pendingBaseline = null
        pendingCheckpoint = null
        checkpointImported = false
        checkpointNotice = ""
    }

    // Twenty operator-observed attempts take time to enter. A stray Back press
    // or outside tap must never silently erase this session-only evidence.
    fun requestClose() {
        val hasUnsavedInput = attempts.isNotEmpty() ||
            expectedApkSha256.isNotBlank() ||
            disconnectedConfirmed || permissionAndStopConfirmed ||
            noCloudFallbackConfirmed || privacyClearingConfirmed ||
            observedSpeech || observedTranslation || attemptLatency.isNotBlank()
        if (hasUnsavedInput) {
            confirmDiscard = true
        } else {
            resetDraft()
            message = ""
        }
    }

    // Explicit user action: read only 257 bytes from the selected document.
    // No cached URI, file name, speech text or remote request is retained.
    val chooseChecksumSidecar = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        onPickerFinished()
        if (uri == null) {
            sidecarNotice = "Geen controlebestand geselecteerd."
        } else {
            coroutineScope.launch {
                val parsed = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val limit = OfflineApkChecksumSidecar.MAX_BYTES
                            val bytes = ByteArray(limit + 1)
                            var used = 0
                            while (used < bytes.size) {
                                val count = stream.read(bytes, used, bytes.size - used)
                                if (count <= 0) break
                                used += count
                            }
                            if (used in 1..limit) OfflineApkChecksumSidecar.parse(
                                String(bytes, 0, used, Charsets.UTF_8)
                            ) else null
                        }
                    }.getOrNull()
                }
                if (opened && baseline != null && parsed != null) {
                    expectedApkSha256 = parsed
                    sidecarNotice = "Checksum uit het gekozen controlebestand ingevuld. " +
                        "Controleer onafhankelijk dat dit bestand bij de bedoelde GitHub-build hoort."
                } else {
                    sidecarNotice = "Geen bruikbare sha256sum-regel gevonden. " +
                        "Kies bewust app-debug.apk.sha256 uit het juiste build-artifact. " +
                        "Het veld is niet gewijzigd."
                }
            }
        }
    }

    // Explicit SAF export only. No app-private persistence or hidden sync.
    // External document providers may themselves synchronize selected files.
    val saveCheckpoint = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        onPickerFinished()
        val copy = pendingCheckpoint
        pendingCheckpoint = null
        if (uri == null || copy == null) {
            checkpointNotice = "Concept opslaan geannuleerd; huidige pogingen blijven staan."
        } else {
            val wrote = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(copy)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            checkpointNotice = if (wrote)
                "Tijdelijk concept opgeslagen in de door jou gekozen Android-provider. " +
                    "Controleer of deze provider bestanden synchroniseert."
            else "Opslaan van concept mislukt. Je pogingen blijven tijdelijk in de app."
        }
    }

    // Import restores ONLY attempt classifications after comparing the
    // current installed APK's SHA to the checkpoint. Radio/mic/consent
    // observations are reset and must be independently re-checked today.
    val openCheckpoint = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        onPickerFinished()
        if (uri == null) {
            checkpointNotice = "Geen concept gekozen."
        } else if (opened && (attempts.isNotEmpty() ||
                observedSpeech || observedTranslation || attemptLatency.isNotBlank())) {
            checkpointNotice = "Bestaande waarnemingen blijven staan. Wis of bewaar " +
                "eerst het huidige concept voordat je een ander opent."
        } else {
            coroutineScope.launch {
                val parsed = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = ByteArray(OfflineObservationCheckpointCodec.MAX_BYTES + 1)
                            var used = 0
                            while (used < bytes.size) {
                                val read = input.read(bytes, used, bytes.size - used)
                                if (read <= 0) break
                                used += read
                            }
                            if (used in 1..OfflineObservationCheckpointCodec.MAX_BYTES)
                                OfflineObservationCheckpointCodec.decode(bytes.copyOf(used))
                            else null
                        }
                    }.getOrNull()
                }
                val latest = deviceSnapshot()
                val installed = installedApkDigest(latest)
                if (parsed == null || latest == null || installed == null ||
                    parsed.installedApkSha256 != installed) {
                    checkpointNotice = "Concept ongeldig of gemaakt met een andere APK. " +
                        "Er zijn geen uitkomsten geladen en geen vinkjes overgenomen."
                } else if (opened && attempts.isNotEmpty()) {
                    checkpointNotice = "Dit concept bevat al waarnemingen; import is geweigerd."
                } else {
                    baseline = latest.copyOf()
                    recordedAt = System.currentTimeMillis()
                    expectedApkSha256 = parsed.expectedApkSha256.orEmpty()
                    attempts = parsed.attempts
                    newAttemptFlags = List(parsed.attempts.size) { false }
                    startedDiagnostics = sessionDiagnostics
                    onArmStrictOffline()
                    chosenDirection = OfflineObservationDirection.entries.firstOrNull {
                        OfflineObservationLedger.totals(attempts, it).attempts <
                            OfflineObservationLedger.REQUIRED_ATTEMPTS_PER_DIRECTION
                    } ?: OfflineObservationDirection.EN_NL
                    disconnectedConfirmed = false
                    permissionAndStopConfirmed = false
                    noCloudFallbackConfirmed = false
                    privacyClearingConfirmed = false
                    observedSpeech = false
                    observedTranslation = false
                    attemptLatency = ""
                    checkpointImported = true
                    opened = true
                    checkpointNotice = "Alleen de uitkomsten van ${attempts.size} pogingen " +
                        "zijn heropend. Controleer nu opnieuw toestel, vliegtuigmodus, " +
                        "wifi en alle vier handmatige waarnemingen. " +
                        "Een import bewijst nooit één ononderbroken offline proef."
                }
            }
        }
    }

    val saveReport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        onPickerFinished()
        val selected = pendingReport
        val expectedBaseline = pendingBaseline
        pendingReport = null
        pendingBaseline = null
        // Never erase twenty real observations because a document chooser was
        // cancelled, a provider failed, or device identity changed mid-save.
        var saved = false
        if (uri == null || selected == null || expectedBaseline == null) {
            message = "Opslaan geannuleerd. Je ingevoerde waarnemingen blijven in " +
                "deze app-sessie bewaard; kies opnieuw Bewaar rapport."
        } else {
            val current = deviceSnapshot()
            if (current == null || !current.contentEquals(expectedBaseline)) {
                message = "Apparaat- of modelstatus is veranderd tijdens het kiezen. " +
                    "Niets opgeslagen of gewist. Het oorspronkelijke momentopnameverslag " +
                    "kan niet veilig worden geëxporteerd; begin zo nodig een nieuwe sessie."
            } else {
                saved = runCatching {
                    val output = context.contentResolver.openOutputStream(uri, "w")
                        ?: return@runCatching false
                    output.use {
                        it.write(selected)
                        it.flush()
                    }
                    true
                }.getOrDefault(false)
                message = if (saved)
                    "Handmatig waarnemingsrapport opgeslagen. Controleer de gekozen provider op externe synchronisatie."
                else "Bestandsprovider kon niet opslaan. Je concept blijft in het " +
                    "appgeheugen zodat je een andere opslaglocatie kunt kiezen."
            }
        }
        if (saved) resetDraft() else opened = true
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
                val installed = installedApkDigest(captured)
                val prior = installedApkDigest(baseline)
                when {
                    captured == null || installed == null ->
                        message = "De exacte apparaat-/APK-identiteit is niet beschikbaar."
                    prior != null && installed != prior ->
                        message = "De APK is sinds dit proefconcept veranderd. " +
                            "Wis of bewaar eerst het oude concept; pogingen zijn niet overgenomen."
                    baseline != null && !reportsOfflineRadioState(captured) ->
                        message = "Hervatten geweigerd: zet vliegtuigmodus aan, wifi uit " +
                            "en verbreek alle Android-netwerktransporten."
                    else -> {
                        onArmStrictOffline()
                        if (startedDiagnostics == null) startedDiagnostics = sessionDiagnostics
                        baseline = captured.copyOf()
                        if (recordedAt == 0L) recordedAt = System.currentTimeMillis()
                        opened = true
                        message = ""
                    }
                }
            }
        ) {
            Text(if (baseline == null) "Start fysieke offline-waarnemingen…"
                else "Hervat ${attempts.size}/20 offline-waarnemingen…")
        }
        OutlinedButton(
            enabled = enabled && !opened,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onPickerStarted()
                openCheckpoint.launch(arrayOf("text/plain", "application/octet-stream"))
            }
        ) { Text("Heropen eerder handmatig proefconcept…") }
        if (checkpointNotice.isNotBlank()) {
            Text(checkpointNotice, style = MaterialTheme.typography.bodySmall)
        }
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
            onDismissRequest = { requestClose() },
            title = { Text("Offline sessie vastleggen") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (checkpointImported) {
                        Text(
                            "Heropend concept: dit bevat alleen eerder handmatig " +
                                "geregistreerde uitkomsten. Radiostilte en de overige " +
                                "acceptatiepunten moet je opnieuw waarnemen. " +
                                "Geen acceptatie of onafgebroken sessie bewezen.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedButton(
                        enabled = enabled && attempts.isNotEmpty() &&
                            installedApkDigest(baseline) != null,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val digest = installedApkDigest(baseline)
                            val checkpoint = if (digest == null) null
                                else OfflineObservationCheckpointCodec.encode(
                                    OfflineObservationCheckpoint(
                                        digest,
                                        expectedApkSha256.trim().lowercase()
                                            .takeUnless { it.isEmpty() },
                                        attempts
                                    )
                                )
                            if (checkpoint == null) {
                                checkpointNotice = "Concept kon niet worden gevalideerd. " +
                                    "Controleer de APK-checksum en de pogingen."
                            } else {
                                pendingCheckpoint = checkpoint
                                onPickerStarted()
                                saveCheckpoint.launch("Tuddy-offline-proefconcept.txt")
                            }
                        }
                    ) { Text("Bewaar voorlopig proefconcept (zonder transcript)…") }
                    if (checkpointNotice.isNotBlank()) {
                        Text(checkpointNotice, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Vul alleen waarnemingen in die je zojuist op dit fysieke toestel hebt gedaan. " +
                            "Een vinkje of dit rapport verandert geen releasegate en wordt niet automatisch geverifieerd.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (message.isNotBlank()) {
                        Text(message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Geïnstalleerde APK SHA-256: ${installedApkDigest(baseline) ?: "onbeschikbaar"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        enabled = enabled,
                        onClick = {
                            onPickerStarted()
                            chooseChecksumSidecar.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Lees app-debug.apk.sha256 uit zelf gekozen bestand…") }
                    Text(
                        "Tuddy leest maximaal 256 bytes uit één bewust gekozen " +
                            "checksum-bestand, zonder het APK-bestand of gesprek te uploaden. " +
                            "Een externe bestandsprovider kan wel synchroniseren. " +
                            "Een match bewijst alleen bestandsidentiteit, geen afzender of apparaatacceptatie.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (sidecarNotice.isNotBlank()) {
                        Text(sidecarNotice, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = expectedApkSha256,
                        onValueChange = {
                            expectedApkSha256 = it.filter { ch ->
                                ch in '0'..'9' || ch.lowercaseChar() in 'a'..'f'
                            }.take(64).lowercase()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Verwachte APK-SHA-256 (64 hex; niet ZIP)") },
                        singleLine = true
                    )
                    Text(
                        when {
                            installedApkDigest(baseline) == null ->
                                "APK-identiteit niet controleerbaar: geïnstalleerde digest ontbreekt."
                            expectedApkSha256.isBlank() ->
                                "Vul de APK-bestandschecksum in uit de builddocumentatie; gebruik niet de ZIP-checksum."
                            expectedDigestMatches(baseline) ->
                                "APK-checksum komt exact overeen met de opgegeven build."
                            else ->
                                "APK-checksum komt niet overeen. Controleer de juiste APK-build en checksum."
                        },
                        color = if (expectedDigestMatches(baseline))
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!reportsOfflineRadioState(baseline)) {
                        Text(
                            offlineRadioBlockerMessage(baseline),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    AcceptanceObservationCheck(
                        "Vliegtuigmodus aan; wifi en mobiele data zijn onafhankelijk uitgeschakeld.",
                        disconnectedConfirmed,
                        enabled = reportsOfflineRadioState(baseline)
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
                    Text(
                        "Leg precies tien ECHT waargenomen pogingen per richting vast. " +
                            "Mislukte herkenningen en vertalingen tellen ook als poging; " +
                            "noteer geen namen, inhoud of audio.",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Tijdens dit concept blijft de strikte radio-/microfoonbewaking " +
                            "ingeschakeld. Nieuwe claims van geslaagde herkenning en " +
                            "vertaling vereisen ook overeenkomstige lokale RAM-tellerstappen. " +
                            "Dit bewijst niet dat een zin correct of fysiek offline was.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OfflineObservationDirection.entries.forEach { direction ->
                        val totals = OfflineObservationLedger.totals(attempts, direction)
                        OutlinedButton(
                            onClick = {
                                chosenDirection = direction
                                observedSpeech = false
                                observedTranslation = false
                                attemptLatency = ""
                                attemptMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${direction.label}: ${totals.attempts}/10 pogingen · " +
                                "${totals.recognized} spraak · ${totals.translated} vertaald" +
                                if (chosenDirection == direction) " ✓ geselecteerd" else "")
                        }
                    }
                    val selectedTotals = OfflineObservationLedger.totals(attempts, chosenDirection)
                    if (selectedTotals.attempts < OfflineObservationLedger.REQUIRED_ATTEMPTS_PER_DIRECTION) {
                        Text("${chosenDirection.label} · poging ${selectedTotals.attempts + 1} van 10")
                        AcceptanceObservationCheck(
                            "Deze poging gaf een correct herkende lokale spraakzin.",
                            observedSpeech
                        ) {
                            observedSpeech = it
                            if (!it) {
                                observedTranslation = false
                                attemptLatency = ""
                            }
                        }
                        AcceptanceObservationCheck(
                            "Deze poging gaf ook een correcte lokale vertaling.",
                            observedTranslation,
                            enabled = observedSpeech
                        ) {
                            observedTranslation = it
                            if (!it) attemptLatency = ""
                        }
                        if (observedSpeech && observedTranslation) {
                            OutlinedTextField(
                                value = attemptLatency,
                                onValueChange = { attemptLatency = it.filter(Char::isDigit).take(6) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Zelf gemeten totale vertraging in ms") },
                                singleLine = true
                            )
                            Text(
                                "Meet zelf vanaf spreken tot vertaling op het scherm. " +
                                    "Leeg laten is toegestaan maar blijft zichtbaar als ontbrekend.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = { recordAttempt() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Leg deze ${chosenDirection.label}-poging vast")
                        }
                    }
                    if (attempts.isNotEmpty()) {
                        TextButton(onClick = {
                            val index = attempts.indexOfLast { it.direction == chosenDirection }
                            if (index >= 0) {
                                attempts = attempts.filterIndexed { i, _ -> i != index }
                                newAttemptFlags = newAttemptFlags.filterIndexed { i, _ -> i != index }
                                attemptMessage = "Laatste ${chosenDirection.label}-poging ongedaan gemaakt."
                            } else {
                                attemptMessage = "Voor ${chosenDirection.label} is nog niets vastgelegd."
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Herstel laatste poging in gekozen richting")
                        }
                    }
                    if (attemptMessage.isNotBlank()) {
                        Text(attemptMessage, style = MaterialTheme.typography.bodySmall)
                    }
                    val attemptsReady = OfflineObservationLedger.complete(attempts)
                    Text(
                        if (attemptsReady) "Alle twintig pogingen geregistreerd, inclusief fouten. " +
                            "Dit is geen keuringsgoedkeuring."
                        else "Nog ${20 - attempts.size} afzonderlijke pogingen te registreren. " +
                            "Een onvolledig rapport blijft bewust exporteerbaar.",
                        style = MaterialTheme.typography.bodySmall
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
                        val oldInstalled = installedApkDigest(baseline)
                        val fresh = deviceSnapshot()
                        val freshInstalled = installedApkDigest(fresh)
                        if (oldInstalled == null || freshInstalled != oldInstalled) {
                            attemptMessage = "APK-identiteit gewijzigd of onleesbaar. " +
                                "Concept behouden; export van deze toestelproef geweigerd."
                        } else if (!reportsOfflineRadioState(fresh)) {
                            attemptMessage = "Export geweigerd: Android meldt momenteel geen " +
                                "vliegtuigmodus met uitgeschakelde wifi en netwerktransporten. " +
                                "Herstel de offline toestand en probeer opnieuw."
                        } else {
                            // Live RAM counters and model receipts change during
                            // real attempts; refresh before export, not after it.
                            baseline = fresh!!.copyOf()
                            val bytes = reportBytes()
                            if (bytes == null) {
                                attemptMessage = "Rapport niet beschikbaar; concept behouden."
                            } else {
                                pendingReport = bytes
                                pendingBaseline = fresh.copyOf()
                                opened = false
                                onPickerStarted()
                                saveReport.launch("Tuddy-offline-waarnemingsrapport.txt")
                            }
                        }
                    }
                ) { Text("Bewaar rapport…") }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = {
                        // Keep RAM draft while the operator returns to speech.
                        // The offline confirmation cannot survive that gap.
                        opened = false
                        disconnectedConfirmed = false
                        noCloudFallbackConfirmed = false
                        observedSpeech = false
                        observedTranslation = false
                        attemptLatency = ""
                        attemptMessage = "Hervat na de echte spraakpoging en registreer " +
                            "uitsluitend wat je zelf hebt waargenomen."
                        onReturnToConversation()
                    }) { Text("Terug naar gesprek · concept bewaren") }
                    TextButton(onClick = { requestClose() }) {
                        Text("Concept wissen of terug")
                    }
                }
            }
        )
    }
    if (opened && confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Ingevoerde waarnemingen wissen?") },
            text = {
                Text(
                    "Je hebt ${attempts.size} van de 20 fysieke pogingen vastgelegd " +
                        "en mogelijk ook andere apparaatwaarnemingen ingevuld. " +
                        "Dit concept staat alleen in het tijdelijke appgeheugen. " +
                        "Als je nu wist, kun je deze gegevens niet herstellen. " +
                        "Kies 'Verder invullen' om ze te behouden of bewaar eerst " +
                        "een onvolledig rapport."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text("Verder invullen")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetDraft(); message = "" }) {
                    Text("Wis concept")
                }
            }
        )
    }
}

@Composable
private fun AcceptanceObservationCheck(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium)
    }
}
