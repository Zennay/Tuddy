package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.OfflineTypedCaptionPolicy

/**
 * One-shot Android on-device result is a PRIVATE DRAFT until explicitly
 * confirmed. Never persist, translate, share or enqueue speech automatically.
 * Alternate hypotheses are unverified Android suggestions, not predictions
 * made or corrected by Tuddy.
 */
@Composable
internal fun SingleSentenceSpeechReviewDialog(
    original: String,
    sourceCode: String,
    targetCode: String,
    alternatives: List<String>,
    onConfirm: (String) -> Boolean,
    onDiscard: () -> Unit
) {
    var draft by remember(original) { mutableStateOf(original) }
    var notice by remember(original) { mutableStateOf("") }
    val normalized = OfflineTypedCaptionPolicy.normalize(draft)

    AlertDialog(
        onDismissRequest = onDiscard,
        title = { Text("Controleer je herkende zin") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "De microfoon is gestopt. Dit is alleen een tijdelijke " +
                        "Android-herkenningssuggestie, nog geen gespreksregel. " +
                        "Pas de tekst aan, kies eventueel een ander voorstel, " +
                        "of gooi de zin weg.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Tekstrichting: ${sourceCode.uppercase()} → ${targetCode.uppercase()}. " +
                        "Geen online herkenning of automatische vertaling.",
                    style = MaterialTheme.typography.labelMedium
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(OfflineTypedCaptionPolicy.MAX_INPUT_UNITS) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    label = { Text("Jouw definitieve oorspronkelijke tekst") }
                )
                if (alternatives.isNotEmpty()) {
                    Text("Andere niet-geverifieerde Android-voorstellen:",
                        style = MaterialTheme.typography.labelSmall)
                    alternatives.take(3).forEach { candidate ->
                        TextButton(
                            onClick = { draft = candidate },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(candidate, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (draft != original) {
                    TextButton(onClick = { draft = original }) {
                        Text("Herstel eerste herkenningsvoorstel")
                    }
                }
                if (notice.isNotBlank()) Text(notice,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text(
                    "Alleen na Bevestig verschijnt de zin in het tijdelijke gesprek " +
                        "en controleert Tuddy het al aanwezige lokale tekstmodel. " +
                        "Er wordt niets gedownload.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalized != null,
                onClick = {
                    if (!onConfirm(draft)) {
                        notice = "Deze zin kon niet veilig worden toegevoegd. " +
                            "Je tekst blijft hier staan."
                    }
                }
            ) { Text("Bevestig en vertaal lokaal") }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) { Text("Gooi zin weg") }
        }
    )
}
