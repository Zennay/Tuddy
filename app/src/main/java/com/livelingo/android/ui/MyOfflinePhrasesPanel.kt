package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivatePhraseDeck

/**
 * User-authored, editable offline study notes. No model calls, chat-log import
 * or claims that any phrase has been translated/checked by native speakers.
 */
@Composable
internal fun MyOfflinePhrasesPanel(
    deck: PrivatePhraseDeck,
    initialLanguage: String,
    onDismiss: () -> Unit
) {
    var language by remember { mutableStateOf(initialLanguage.takeIf {
        it in listOf("kmr", "ckb", "ps")
    } ?: "kmr") }
    var meaningLanguage by remember { mutableStateOf("nl") }
    var phrase by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var info by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val phrases = remember(revision) { deck.entries() }
    val labels = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen offline zinnen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "Vul beide kanten ZELF in. Dit is geen vertaling door Tuddy, geen " +
                        "spraakherkenning en geen gecontroleerde woordenlijst. " +
                        "Bewaar geen gevoelige gesprekken of persoonsgegevens.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Taal van je eigen zin")
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    labels.forEach { (code, title) ->
                        TextButton(onClick = { language = code }) {
                            Text(if (language == code) "$title ✓" else title)
                        }
                    }
                }
                OutlinedTextField(
                    value = phrase, onValueChange = { phrase = it.take(120) },
                    label = { Text("Eigen zin in ${labels[language]}") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (language == "ckb" || language == "ps")
                            TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                Text("Betekenis die jij zelf invoert")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { meaningLanguage = "nl" }) {
                        Text(if (meaningLanguage == "nl") "Nederlands ✓" else "Nederlands")
                    }
                    TextButton(onClick = { meaningLanguage = "en" }) {
                        Text(if (meaningLanguage == "en") "English ✓" else "English")
                    }
                }
                OutlinedTextField(
                    value = meaning, onValueChange = { meaning = it.take(180) },
                    label = { Text("Je eigen betekenis (geen automatische vertaling)") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
                OutlinedButton(
                    enabled = phrase.isNotBlank() && meaning.isNotBlank(),
                    onClick = {
                        if (deck.save(language, phrase, meaningLanguage, meaning)) {
                            phrase = ""
                            meaning = ""
                            revision++
                            info = "Je eigen zin is lokaal opgeslagen. Niet automatisch vertaald."
                        } else {
                            info = "Kon zin niet bewaren. Controleer beide teksten " +
                                "(één regel, maximaal 120/180 tekens, maximaal 80 zinnen)."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Bewaar mijn eigen zin") }
                Text("Alleen op jouw verzoek blijven deze zinnen in privé-appopslag staan. " +
                    "Ze worden niet gedeeld of automatisch overgenomen uit gesprekken.",
                    style = MaterialTheme.typography.bodySmall)
                if (info.isNotBlank()) Text(info,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                OutlinedTextField(
                    value = search, onValueChange = { search = it.take(60) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Zoek in mijn eigen zinnen") }
                )
                if (phrases == null) {
                    Text("Lokale lijst is beschadigd. Opslaan is geblokkeerd. " +
                        "Je kunt de lokale lijst hieronder zelf wissen.",
                        color = MaterialTheme.colorScheme.error)
                } else {
                    val query = search.trim()
                    val shown = phrases.filter {
                        query.isEmpty() ||
                            it.phrase.contains(query, ignoreCase = true) ||
                            it.ownMeaning.contains(query, ignoreCase = true) ||
                            labels[it.language].orEmpty().contains(query, ignoreCase = true)
                    }
                    Text("${shown.size} van ${phrases.size} zelf ingevoerde zinnen",
                        style = MaterialTheme.typography.bodySmall)
                    shown.forEach { item ->
                        HorizontalDivider()
                        Text("${labels[item.language]} · ${if (item.meaningLanguage == "nl") "NL" else "EN"}",
                            style = MaterialTheme.typography.labelSmall)
                        Text(item.phrase,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (item.language == "ckb" || item.language == "ps")
                                    TextDirection.Rtl else TextDirection.Ltr
                            ))
                        Text(item.ownMeaning, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            if (deck.remove(item.language, item.phrase)) {
                                revision++
                                info = "Eigen zin verwijderd."
                            } else info = "Verwijderen mislukt."
                        }) { Text("Verwijder eigen zin") }
                    }
                }
                TextButton(onClick = { confirmClear = true }) {
                    Text("Wis mijn volledige eigen zinnenlijst")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Alle eigen zinnen wissen?") },
            text = { Text("Deze actie verwijdert je lokaal bewaarde zinnen definitief. " +
                "De ingebouwde vijf basiszinnen blijven beschikbaar.") },
            confirmButton = {
                TextButton(onClick = {
                    val cleared = deck.clear()
                    if (cleared) revision++
                    info = if (cleared) "Je eigen zinnen zijn gewist."
                        else "Wissen mislukt; probeer opnieuw."
                    confirmClear = false
                }) { Text("Wis mijn zinnen") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Annuleren") }
            }
        )
    }
}
