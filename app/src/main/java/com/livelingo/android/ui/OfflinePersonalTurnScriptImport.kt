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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateAuthoredConversationScript
import com.livelingo.android.PrivateAuthoredConversationTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Read one explicitly selected text script into RAM after a full consent
 * preview. No cloud, storage of imported turns, translation, or transcript scan.
 */
@Composable
internal fun OfflinePersonalTurnScriptImport(
    current: List<PrivateAuthoredConversationTurn>,
    onImport: (
        expected: List<PrivateAuthoredConversationTurn>,
        imported: List<PrivateAuthoredConversationTurn>,
        replace: Boolean
    ) -> Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val latestCurrent by rememberUpdatedState(current)
    var inProgress by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var snapshotAtPicker by remember {
        mutableStateOf<List<PrivateAuthoredConversationTurn>?>(null)
    }
    var consentPreview by remember {
        mutableStateOf<List<PrivateAuthoredConversationTurn>?>(null)
    }
    var snapshotAtPreview by remember {
        mutableStateOf<List<PrivateAuthoredConversationTurn>?>(null)
    }
    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val before = snapshotAtPicker
        snapshotAtPicker = null
        if (uri == null) {
            inProgress = false
        } else if (before == null || latestCurrent != before) {
            status = "Je gesprek veranderde tijdens het kiezen. Open het bestand opnieuw."
            inProgress = false
        } else {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.readNBytes(PrivateAuthoredConversationScript.MAX_BYTES + 1)
                        }
                    }.getOrNull()
                }
                inProgress = false
                if (latestCurrent != before) {
                    status = "Je gesprek veranderde tijdens het lezen. Kies opnieuw."
                } else {
                    val restored = bytes?.let(PrivateAuthoredConversationScript::read)
                    if (restored == null) {
                        status = "Geen geldig Tuddy-gesprek: kies een ongewijzigd " +
                            "Tuddy-tekstscript van maximaal 8 KiB en 12 beurten. " +
                            "Je huidige gesprek blijft intact."
                    } else {
                        snapshotAtPreview = before.toList()
                        consentPreview = restored.toList()
                        status = ""
                    }
                }
            }
        }
    }

    fun apply(replace: Boolean) {
        val script = consentPreview ?: return
        val before = snapshotAtPreview ?: return
        if (latestCurrent != before || (!replace && before.size + script.size > 12)) {
            status = "Je huidige gesprek veranderde of bevat te veel beurten. " +
                "Bekijk de import opnieuw; er is niets vervangen."
        } else {
            val success = onImport(before, script, replace)
            status = if (success) {
                if (replace) "Dit gekozen script is tijdelijk geopend; oude beurten vervangen."
                else "Gekozen script tijdelijk toegevoegd aan je gesprek."
            } else "Het gesprek veranderde; er is geen bestand toegepast."
        }
        consentPreview = null
        snapshotAtPreview = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedButton(
            onClick = {
                status = ""
                snapshotAtPicker = current.toList()
                inProgress = true
                runCatching {
                    openFile.launch(arrayOf("text/plain", "application/octet-stream"))
                }.onFailure {
                    inProgress = false
                    snapshotAtPicker = null
                    status = "Android kon de documentkiezer niet openen."
                }
            },
            enabled = !inProgress,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (inProgress) "Gesprek wordt gelezen…" else
            "Open mijn eerder bewaarde Tuddy-gesprek…") }
        Text(
            "Kies handmatig je eigen Tuddy-tekstscript. De inhoud wordt " +
                "lokaal gecontroleerd en pas na een volledige preview tijdelijk geopend. " +
                "Er wordt niets automatisch aan je woordenlijst toegevoegd.",
            style = MaterialTheme.typography.bodySmall
        )
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }

    val preview = consentPreview
    if (preview != null) {
        val appendAvailable = (snapshotAtPreview?.size ?: 12) + preview.size <= 12
        AlertDialog(
            onDismissRequest = {
                consentPreview = null
                snapshotAtPreview = null
            },
            title = { Text("Dit eigen offline gesprekscript openen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 485.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Controleer alle " + preview.size + " originele, " +
                        "zelfgeschreven paren en hun gekozen leesrichting. " +
                        "Deze betekenissen zijn niet onafhankelijk gecontroleerd.",
                        style = MaterialTheme.typography.bodySmall)
                    preview.forEachIndexed { index, turn ->
                        val n = turn.note
                        val first = if (turn.fromMeaning) n.ownMeaning else n.phrase
                        val second = if (turn.fromMeaning) n.phrase else n.ownMeaning
                        val firstCode = if (turn.fromMeaning) n.meaningLanguage else n.language
                        val secondCode = if (turn.fromMeaning) n.language else n.meaningLanguage
                        Text((index + 1).toString() + ". " + firstCode.uppercase() +
                            " → " + secondCode.uppercase(),
                            style = MaterialTheme.typography.labelMedium)
                        Text(first, style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (firstCode in setOf("ckb", "ps"))
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                        Text(second, style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = if (secondCode in setOf("ckb", "ps"))
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                    }
                    Text("Kies Vervang om de huidige beurten te wissen en alleen " +
                        "deze te tonen; kies Voeg toe als er ruimte is. " +
                        "Niets wordt opgeslagen, geüpload of automatisch vertaald. " +
                        "De externe bestandsprovider kan zelf wel gegevens synchroniseren.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Column {
                    TextButton(onClick = { apply(true) }) {
                        Text("Vervang mijn tijdelijke gesprek")
                    }
                    TextButton(
                        onClick = { apply(false) },
                        enabled = appendAvailable
                    ) {
                        Text(if (appendAvailable) "Voeg toe aan mijn tijdelijke gesprek"
                            else "Te veel beurten om toe te voegen (max. 12)")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    consentPreview = null
                    snapshotAtPreview = null
                }) { Text("Annuleren") }
            }
        )
    }
}
