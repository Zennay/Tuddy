package com.livelingo.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

/** An explicitly authored personal note from ONE user-selected subtitle cue.
 * Never infer source language, invent a meaning or save source files/audio. */
@Composable
internal fun PrivateSubtitlePersonalPhraseDialog(
    sourceText: String,
    onSaveNew: (String, String, String, String) -> Boolean,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    var language by remember(sourceText) { mutableStateOf("kmr") }
    var meaningLanguage by remember(sourceText) { mutableStateOf("nl") }
    // Show the entire original cue, even when it exceeds the note limit.
    var phrase by remember(sourceText) { mutableStateOf(sourceText) }
    var meaning by remember(sourceText) { mutableStateOf("") }
    var approved by remember(sourceText) { mutableStateOf(false) }
    var message by remember(sourceText) { mutableStateOf("") }
    fun safe(value: String, max: Int): Boolean =
        value.isNotBlank() && value.length <= max && value == value.trim() &&
            value.none { ch ->
                Character.isISOControl(ch) || Character.isSurrogate(ch) ||
                    (Character.getType(ch) == Character.FORMAT.toInt() && ch != '\u200C') ||
                    ch == '\u2028' || ch == '\u2029'
            }
    val valid = safe(phrase, 120) && safe(meaning, 180)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eigen ondertitelzin bewust bewaren?") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dit is jouw zelf gekozen ondertitelregel. Tuddy heeft de " +
                    "spraak, bron-taal of betekenis niet herkend of geverifieerd. " +
                    "Kies de taal, verbeter de tekst en schrijf je betekenis zelf.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Mijn zelf gekozen zinta(a)l:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
                        .forEach { (code, label) ->
                            TextButton(onClick = { language = code; approved = false }) {
                                Text((if (language == code) "✓ " else "") + label,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                }
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { if (it.length <= 2400) phrase = it
                        approved = false; message = "" },
                    label = { Text("Mijn eigen zin · maximaal 120 tekens, één regel") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl),
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                if (!safe(phrase, 120)) Text(
                    "Werk zelf terug naar één geldige regel van maximaal 120 tekens. " +
                        "De oorspronkelijke cue wordt nooit stilzwijgend afgekapt.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text("Mijn zelfgeschreven betekenis in:",
                    style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("nl" to "Nederlands", "en" to "English")
                        .forEach { (code, label) ->
                            TextButton(onClick = { meaningLanguage = code; approved = false }) {
                                Text((if (meaningLanguage == code) "✓ " else "") + label)
                            }
                        }
                }
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { if (it.length <= 180) meaning = it
                        approved = false; message = "" },
                    label = { Text("Mijn betekenis · maximaal 180 tekens") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl),
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                Text("Een bestaande eigen zin wordt nooit overschreven. " +
                    "De nieuwe kaart is alleen lokaal in Mijn eigen zinnen beschikbaar.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Checkbox(checked = approved, enabled = valid,
                        onCheckedChange = { approved = it; message = "" })
                    Text("Ik heb taal, zin en mijn eigen betekenis nagelezen en " +
                        "wil precies deze kaart privé bewaren.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (message.isNotBlank()) Text(message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = valid && approved, onClick = {
                if (safe(phrase, 120) && safe(meaning, 180) && approved &&
                    onSaveNew(language, phrase, meaningLanguage, meaning)) onSaved()
                else {
                    approved = false
                    message = "Niet opgeslagen: de zin bestaat mogelijk al, " +
                        "de lijst is vol of privéopslag is niet beschikbaar."
                }
            }) { Text("Bewaar alleen deze eigen zin") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleer · niets opslaan") }
        }
    )
}
