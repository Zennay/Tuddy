package com.livelingo.android.ui

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
import com.livelingo.android.PrivatePhraseTextPolicy

/**
 * Lets the user consciously turn one bundled starter example into an editable
 * private note. The preview is not native-speaker verified and is not promoted
 * to a model result; both fields require explicit review before local storage.
 */
@Composable
internal fun OfflineStarterPhraseSaveDialog(
    language: String,
    meaningLanguage: String,
    initialPhrase: String,
    initialMeaning: String,
    onSaveNew: (String, String, String, String) -> Boolean,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val languageLabel = when (language) {
        "kmr" -> "Kurmanji"
        "ckb" -> "Sorani"
        "ps" -> "Pasjtoe"
        else -> "onbekende taal"
    }
    val meaningLabel = if (meaningLanguage == "en") "English" else "Nederlands"
    var phrase by remember(language, meaningLanguage, initialPhrase, initialMeaning) {
        mutableStateOf(initialPhrase)
    }
    var meaning by remember(language, meaningLanguage, initialPhrase, initialMeaning) {
        mutableStateOf(initialMeaning)
    }
    var approved by remember(language, meaningLanguage, initialPhrase, initialMeaning) {
        mutableStateOf(false)
    }
    var message by remember(language, meaningLanguage, initialPhrase, initialMeaning) {
        mutableStateOf("")
    }
    val valid = PrivatePhraseTextPolicy.valid(phrase, 120) &&
        PrivatePhraseTextPolicy.valid(meaning, 180)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voorbeeld als eigen offline zin bewaren?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Dit is een vast voorbeeldwoord, geen volledige vertaling en niet " +
                        "onafhankelijk door moedertaalsprekers geverifieerd. Controleer " +
                        "beide velden en pas ze aan voordat je iets bewaart.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Eigen zin · $languageLabel", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { incoming ->
                        if (incoming.length <= 2400 && PrivatePhraseTextPolicy.wellFormed(incoming)) {
                            phrase = incoming
                            approved = false
                            message = ""
                        }
                    },
                    label = { Text("Mijn zin · maximaal 120 tekens") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                if (!PrivatePhraseTextPolicy.valid(phrase, 120)) {
                    Text(
                        "Gebruik één geldige regel van maximaal 120 tekens.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("Eigen betekenis · $meaningLabel", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { incoming ->
                        if (incoming.length <= 180 && PrivatePhraseTextPolicy.wellFormed(incoming)) {
                            meaning = incoming
                            approved = false
                            message = ""
                        }
                    },
                    label = { Text("Mijn betekenis · maximaal 180 tekens") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Text(
                    "Bewaren maakt dit alleen een privé notitie op dit toestel. Het controleert " +
                        "de vertaling niet, voegt geen model toe en verstuurt niets.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Checkbox(
                        checked = approved,
                        enabled = valid,
                        onCheckedChange = { approved = it; message = "" }
                    )
                    Text(
                        "Ik heb beide velden nagelezen en wil precies deze eigen notitie bewaren.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && approved, onClick = {
                if (valid && approved && onSaveNew(language, phrase, meaningLanguage, meaning)) {
                    onSaved()
                } else {
                    approved = false
                    message = "Niet opgeslagen: deze zin bestaat mogelijk al, de lijst is vol " +
                        "of privéopslag is niet beschikbaar. Er is niets overschreven."
                }
            }) { Text("Bewaar alleen deze eigen zin") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleer · niets opslaan") }
        }
    )
}
