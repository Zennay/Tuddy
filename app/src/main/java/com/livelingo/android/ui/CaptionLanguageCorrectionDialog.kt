package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.TargetCatalog

/**
 * Human correction of a caption's TEXT language metadata. Deliberately
 * separate from changing the device's speech recognition language.
 */
@Composable
internal fun CaptionLanguageCorrectionDialog(
    original: String,
    initialSource: String,
    initialTarget: String,
    onApply: (String, String) -> Boolean,
    onDismiss: () -> Unit
) {
    var source by remember(initialSource) { mutableStateOf(initialSource) }
    var target by remember(initialTarget) { mutableStateOf(initialTarget) }
    var choosing by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val rtl = setOf("ar", "fa", "ur", "he")
    fun languageLabel(code: String): String =
        TargetCatalog.targets.firstOrNull { it.code == code }?.title ?: code

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Controleer taalrichting van deze zin") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Een verkeerde lokale taaldetectie kan een zin fout vertalen. " +
                    "Kies de daadwerkelijk gesproken of getypte brontaal en gewenste " +
                    "tekstdoeltaal. Alleen deze ene tijdelijke zin wordt opnieuw " +
                    "lokaal vertaald; de microfoontaal verandert niet.",
                    style = MaterialTheme.typography.bodySmall)
                Text(original.take(360), style = MaterialTheme.typography.titleMedium.copy(
                    textDirection = if (source in rtl) TextDirection.Rtl else TextDirection.Ltr
                ))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { choosing = "source"; search = "" }
                ) { Text("Oorspronkelijke tekst: " + languageLabel(source)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { choosing = "target"; search = "" }
                ) { Text("Lokaal vertalen naar: " + languageLabel(target)) }
                if (choosing != null) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it.take(35) },
                        label = { Text("Zoek teksttaal of taalcode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(
                        modifier = Modifier.heightIn(max = 165.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TargetCatalog.search(search).take(16).forEach { choice ->
                            TextButton(onClick = {
                                if (choosing == "source") source = choice.code
                                else target = choice.code
                                choosing = null
                                search = ""
                            }) { Text(choice.title + " (" + choice.code + ")") }
                        }
                    }
                    TextButton(onClick = { choosing = null }) { Text("Sluit taalkeuze") }
                }
                Text("Deze lijst bevat ondersteunde ML Kit-teksttalen, niet " +
                    "noodzakelijk geïnstalleerde modellen of offline spraakpakketten. " +
                    "Er wordt niets automatisch gedownload. Een ontbrekend model " +
                    "blijft duidelijk als onvoltooide vertaling zichtbaar.",
                    style = MaterialTheme.typography.bodySmall)
                if (message.isNotBlank()) Text(message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = source in TargetCatalog.codes &&
                    target in TargetCatalog.codes &&
                    (source != initialSource || target != initialTarget),
                onClick = {
                    if (onApply(source, target)) onDismiss()
                    else message = "De zin is intussen gewijzigd of niet meer beschikbaar."
                }
            ) { Text("Pas taalrichting toe en vertaal opnieuw") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
