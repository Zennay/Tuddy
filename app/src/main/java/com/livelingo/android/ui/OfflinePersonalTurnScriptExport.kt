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
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivateAuthoredConversationScript
import com.livelingo.android.PrivateAuthoredConversationTurn

/**
 * Save only user-selected authored turns to an explicit Android document.
 * No automatic session persistence or upload. The temporary turn list remains
 * owned by the caller and is rechecked after the external file chooser returns.
 */
@Composable
internal fun OfflinePersonalTurnScriptExport(
    turns: List<Pair<PersonalPhrase, Boolean>>
) {
    val context = LocalContext.current
    var consentPreview by remember { mutableStateOf<ByteArray?>(null) }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var info by remember { mutableStateOf("") }
    fun snapshot(): ByteArray? = PrivateAuthoredConversationScript.render(
        turns.map { (note, fromMeaning) ->
            PrivateAuthoredConversationTurn(note, fromMeaning)
        }
    )
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val chosen = pending
        pending = null
        if (uri != null && chosen != null) {
            val fresh = snapshot()
            if (fresh == null || !fresh.contentEquals(chosen)) {
                info = "De gekozen beurten zijn veranderd. Bekijk het tekstscript opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(chosen)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                info = if (saved)
                    "Het geselecteerde tekstscript is bewust opgeslagen."
                else "Opslaan mislukt; er is geen automatisch script bewaard."
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedButton(
            enabled = turns.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val bytes = snapshot()
                if (bytes == null) info = "Geen geldig gespreksfragment geselecteerd."
                else {
                    consentPreview = bytes
                    info = ""
                }
            }
        ) { Text("Bewaar uitsluitend deze eigen beurten als tekst…") }
        Text(
            "Standaard blijft het gesprek alleen tijdelijk in geheugen. " +
                "Een export gebeurt uitsluitend als jij vooraf de volledige " +
                "tekst bekijkt en bewust een Android-bestandslocatie kiest.",
            style = MaterialTheme.typography.bodySmall
        )
        if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodySmall)
    }

    val shown = consentPreview
    if (shown != null) {
        AlertDialog(
            onDismissRequest = { consentPreview = null },
            title = { Text("Eigen zinnen buiten Tuddy bewaren?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Alleen deze geselecteerde beurten worden geschreven, " +
                        "exact zoals hieronder. Niet de microfoon, gesprekshistorie, " +
                        "andere persoonlijke zinnen of herhaalplanning.",
                        style = MaterialTheme.typography.bodySmall)
                    Text(shown.toString(Charsets.UTF_8),
                        style = MaterialTheme.typography.bodySmall)
                    Text("Een Android-bestandsprovider kan de tekst extern opslaan " +
                        "of synchroniseren. Die kopie wordt niet verwijderd door " +
                        "Tuddy te sluiten of appgegevens te wissen. " +
                        "Eigen betekenissen zijn niet onafhankelijk gecontroleerd.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = shown.copyOf()
                    consentPreview = null
                    runCatching {
                        createDocument.launch("Tuddy-mijn-offline-gespreksbeurten.txt")
                    }.onFailure {
                        pending = null
                        info = "Android-bestandskiezer kon niet worden geopend."
                    }
                }) { Text("Ik begrijp dit, kies opslag…") }
            },
            dismissButton = {
                TextButton(onClick = { consentPreview = null }) {
                    Text("Annuleren")
                }
            }
        )
    }
}
