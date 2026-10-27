package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.OfflineSessionDiagnostics

/**
 * Keeps a physical Android acceptance session visible while the operator
 * returns to the conversation for each real microphone turn.
 * Aggregates only RAM-only counters; never copies text, audio or identities.
 * This is a guide, not device attestation or automatic acceptance.
 */
@Composable
internal fun OfflineAcceptanceLiveGuide(
    draft: OfflineAcceptanceDraft,
    diagnostics: OfflineSessionDiagnostics,
    currentSourceCode: String,
    currentTargetCode: String,
    isListening: Boolean,
    strictRadioArmed: Boolean,
    canReverse: Boolean,
    onReverse: () -> Unit,
    onOpenReport: () -> Unit
) {
    if (draft.baseline.value == null) return
    val attempts = draft.attempts.value
    val nl = OfflineObservationLedger.totals(attempts, OfflineObservationDirection.NL_EN)
    val en = OfflineObservationLedger.totals(attempts, OfflineObservationDirection.EN_NL)
    val required = OfflineObservationLedger.REQUIRED_ATTEMPTS_PER_DIRECTION
    val expected = when {
        nl.attempts < required -> OfflineObservationDirection.NL_EN
        en.attempts < required -> OfflineObservationDirection.EN_NL
        else -> null
    }
    val current = when {
        currentSourceCode == "nl" && currentTargetCode == "en" ->
            OfflineObservationDirection.NL_EN
        currentSourceCode == "en" && currentTargetCode == "nl" ->
            OfflineObservationDirection.EN_NL
        else -> null
    }
    val started = draft.startedDiagnostics.value
    val speechDelta = when (expected) {
        OfflineObservationDirection.NL_EN ->
            diagnostics.nlEnFinalUtterances - (started?.nlEnFinalUtterances ?: diagnostics.nlEnFinalUtterances)
        OfflineObservationDirection.EN_NL ->
            diagnostics.enNlFinalUtterances - (started?.enNlFinalUtterances ?: diagnostics.enNlFinalUtterances)
        null -> 0
    }
    val translationDelta = when (expected) {
        OfflineObservationDirection.NL_EN ->
            diagnostics.nlEnLocalTranslations - (started?.nlEnLocalTranslations ?: diagnostics.nlEnLocalTranslations)
        OfflineObservationDirection.EN_NL ->
            diagnostics.enNlLocalTranslations - (started?.enNlLocalTranslations ?: diagnostics.enNlLocalTranslations)
        null -> 0
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Fysieke offline proef · concept actief",
                style = MaterialTheme.typography.titleSmall)
            Text("NL → EN: " + nl.attempts + "/" + required +
                " · EN → NL: " + en.attempts + "/" + required + " vastgelegd",
                style = MaterialTheme.typography.bodyMedium)
            if (expected == null) {
                Text("20 observaties vastgelegd. Controleer de afzonderlijke " +
                    "toestelclaims, sla het rapport op en laat iemand onafhankelijk beoordelen. " +
                    "Dit is nog geen vrijgave.",
                    style = MaterialTheme.typography.bodySmall)
            } else {
                val completed = if (expected == OfflineObservationDirection.NL_EN)
                    nl.attempts else en.attempts
                Text("Volgende: " + expected.label + " · poging " +
                    (completed + 1) + " van " + required,
                    style = MaterialTheme.typography.bodyMedium)
                Text("Sinds proefstart in deze richting: " +
                    speechDelta.coerceAtLeast(0) + " echte spraakresultaten · " +
                    translationDelta.coerceAtLeast(0) + " lokale vertalingen.",
                    style = MaterialTheme.typography.bodySmall)
                val previous = draft.lastRecordedDiagnostics.value ?: started
                val freshVoiceEvent = previous != null &&
                    diagnostics.hasNewVoiceAttemptSince(previous)
                Text(
                    if (freshVoiceEvent)
                        "Nieuwe lokale opname-/herkenningsactiviteit gezien sinds de " +
                            "vorige registratie. Controleer zelf de echte uitkomst."
                    else "Nog geen nieuwe lokale opname-/herkenningsactiviteit " +
                        "sinds de vorige registratie. Doe eerst een echte spraakpoging.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (current != expected) {
                    Text(
                        "Actieve talen komen niet overeen met de volgende proefrichting. " +
                            "Wissel eerst de talen en controleer beide offline modellen opnieuw.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (current != null) {
                        TextButton(
                            onClick = onReverse,
                            enabled = !isListening && canReverse
                        ) { Text("Wissel naar " + expected.label) }
                    }
                }
                if (!strictRadioArmed) {
                    Text("Strikte vliegtuigmodus staat niet aan. Zet die aan in de startcheck.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (isListening) {
                    Text("Stop Luister voordat je de observatie handmatig vastlegt.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    enabled = !isListening,
                    onClick = onOpenReport
                ) {
                    Text(if (expected == null) "Controleer en exporteer proef"
                        else "Leg laatste poging vast")
                }
            }
        }
    }
}
