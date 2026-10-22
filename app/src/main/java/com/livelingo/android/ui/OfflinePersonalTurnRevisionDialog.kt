package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseTextPolicy

/**
 * Correct exactly one self-authored conversation turn in RAM, without changing
 * a saved deck entry, importing content or invoking any translation service.
 */
@Composable
internal fun OfflinePersonalTurnRevisionDialog(
    original: PersonalPhrase,
    onConfirm: (PersonalPhrase) -> Boolean,
    onDismiss: () -> Unit
) {
    var source by remember(original) { mutableStateOf(original.phrase) }
    var meaning by remember(original) { mutableStateOf(original.ownMeaning) }
    var notice by remember(original) { mutableStateOf("") }
    val rtl = original.language in setOf("ckb", "ps")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corrigeer alleen deze tijdelijke gespreksbeurt") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Controleer de door jou geschreven teksten. Een wijziging maakt " +
                        "alleen deze beurt tijdelijk, ook als hij ooit uit je " +
                        "blijvende woordenlijst kwam. Tuddy overschrijft geen opgeslagen zin.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it.take(120); notice = "" },
                    label = { Text(original.language.uppercase() + " · mijn bronzin") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it.take(180); notice = "" },
                    label = { Text(original.meaningLanguage.uppercase() +
                        " · mijn eigen betekenis") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Text(
                    "Dit is geen automatische vertaling of native-speakercontrole. " +
                        "Nieuwe bronzin en betekenis moeten allebei door jou kloppen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (notice.isNotBlank()) {
                    Text(notice, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!PrivatePhraseTextPolicy.valid(source, 120) ||
                    !PrivatePhraseTextPolicy.valid(meaning, 180)) {
                    notice = "Corrigeer lege, ongeldige of te lange tekst eerst."
                } else if (!onConfirm(original.copy(
                        phrase = source,
                        ownMeaning = meaning
                    ))) {
                    notice = "Deze beurt is veranderd. Sluit en open opnieuw; " +
                        "de andere beurten zijn niet gewijzigd."
                }
            }) { Text("Pas uitsluitend deze beurt tijdelijk aan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
