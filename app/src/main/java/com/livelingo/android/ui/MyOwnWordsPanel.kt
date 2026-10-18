package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.livelingo.android.OwnWord
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.TargetCatalog

/**
 * Human-authored offline word notes, strictly separated from vetted glossary
 * facts and speech/translation models. Nothing is captured automatically.
 */
@Composable
internal fun MyOwnWordsPanel(
    deck: PrivateOwnWordDeck,
    initialLanguage: String,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultLanguage = initialLanguage.replace('_', '-').substringBefore('-')
        .lowercase().takeIf { it in PrivateOwnWordDeck.SUPPORTED_CODES } ?: "nl"
    var language by remember { mutableStateOf(defaultLanguage) }
    var pickerOpen by remember { mutableStateOf(false) }
    var languageSearch by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<OwnWord?>(null) }
    var pendingDelete by remember { mutableStateOf<OwnWord?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }
    val items = remember(revision) { deck.entries() }
    val filtered = items.orEmpty().filter {
        it.language == language && (query.isBlank() ||
            it.word.contains(query.trim(), ignoreCase = true) ||
            it.ownMeaning.contains(query.trim(), ignoreCase = true))
    }

    if (pendingDelete != null) {
        val selected = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eigen woordnotitie verwijderen?") },
            text = { Text("Dit verwijdert alleen jouw zelf ingevoerde betekenis bij " +
                selected.word + ". Gesprekken en ingebouwde woorden worden niet gewijzigd.") },
            confirmButton = {
                TextButton(onClick = {
                    val removed = deck.remove(selected)
                    notice = if (removed) "Eigen woordnotitie verwijderd."
                        else "De notitie is gewijzigd of kon niet worden verwijderd."
                    if (removed) {
                        revision++
                        onChanged()
                    }
                    pendingDelete = null
                }) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) {
                Text("Annuleren")
            } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen offline woorden") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Schrijf zelf een woord én de betekenis. Deze notities zijn " +
                    "NIET door Tuddy vertaald of gecontroleerd en worden alleen " +
                    "na jouw bewaaractie lokaal in de app opgeslagen. " +
                    "Bewaar geen gevoelige persoonsgegevens.",
                    style = MaterialTheme.typography.bodySmall)
                if (items == null) {
                    Text("Lokale gegevens konden niet veilig worden gelezen. " +
                        "Er wordt niets overschreven.", color = MaterialTheme.colorScheme.error)
                }
                Text("Woordtaal: " + language.uppercase(),
                    style = MaterialTheme.typography.titleSmall)
                if (editing == null) {
                    OutlinedButton(onClick = { pickerOpen = !pickerOpen }) {
                        Text(if (pickerOpen) "Sluit taalkeuze" else "Kies taal")
                    }
                    if (pickerOpen) {
                        OutlinedTextField(
                            value = languageSearch,
                            onValueChange = { languageSearch = it.take(35) },
                            label = { Text("Zoek taalnaam of code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        val preview = listOf("kmr" to "Kurmanji", "ckb" to "Sorani",
                            "ps" to "Pasjtoe")
                        val supported = (preview + TargetCatalog.search(languageSearch)
                            .map { it.code to it.title })
                            .distinctBy { it.first }
                            .filter {
                                languageSearch.isBlank() ||
                                    it.first.contains(languageSearch, ignoreCase = true) ||
                                    it.second.contains(languageSearch, ignoreCase = true)
                            }.take(12)
                        supported.forEach { (code, label) ->
                            TextButton(onClick = {
                                language = code
                                pickerOpen = false
                                languageSearch = ""
                                word = ""
                                meaning = ""
                            }) { Text(label + " (" + code + ")") }
                        }
                    }
                } else {
                    TextButton(onClick = {
                        editing = null
                        word = ""
                        meaning = ""
                    }) { Text("Annuleer bewerken") }
                }
                OutlinedTextField(
                    value = word,
                    onValueChange = { if (editing == null) word = it.take(48) },
                    label = { Text("Woord (zonder spaties)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = editing != null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (language in setOf("ckb", "ps", "ar", "fa", "ur"))
                            TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it.take(180) },
                    label = { Text("Mijn eigen betekenis (niet gecontroleerd)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3
                )
                Button(
                    enabled = items != null && word.isNotBlank() && meaning.isNotBlank(),
                    onClick = {
                        val saved = deck.save(language, word, meaning)
                        notice = if (saved) "Jouw eigen betekenis is lokaal opgeslagen."
                            else "Niet opgeslagen: controleer tekst, dubbele gegevens " +
                                "of limiet van 120 woorden."
                        if (saved) {
                            editing = null
                            word = ""
                            meaning = ""
                            revision++
                            onChanged()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editing == null) "Bewaar mijn betekenis" else "Bewaar wijziging") }
                HorizontalDivider()
                Text("Mijn woorden in " + language.uppercase() +
                    ": " + filtered.size, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(70) },
                    label = { Text("Zoek in eigen woorden") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                filtered.forEach { saved ->
                    Text(saved.word + " — " + saved.ownMeaning,
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Zelf ingevoerd · niet geverifieerd",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            editing = saved
                            word = saved.word
                            meaning = saved.ownMeaning
                            notice = ""
                        }) { Text("Bewerk betekenis") }
                        TextButton(onClick = { pendingDelete = saved }) {
                            Text("Verwijder")
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    Text("Nog geen eigen woorden in deze selectie.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (notice.isNotBlank()) {
                    Text(notice, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
