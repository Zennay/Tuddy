package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivateOfflineSearch
import com.livelingo.android.PrivatePhraseShortcuts
import com.livelingo.android.PrivatePhraseTopics
import java.util.Locale

/** Pick an explicitly authored note for a RAM-only conversational turn. */
@Composable
internal fun OfflinePhraseTurnTopicPicker(
    notes: List<PersonalPhrase>,
    language: String,
    meaningLanguage: String,
    onPick: (PersonalPhrase) -> Unit
) {
    val context = LocalContext.current
    val topics = remember(context) { PrivatePhraseTopics(context) }
    val pins = remember(context) { PrivatePhraseShortcuts(context) }
    val themeNames = remember(notes) { topics.topics() }
    val favorites = remember(notes) { pins.pinnedNotes(notes) }
    var theme by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // Search-only normalization tolerates script/keyboard variants without
    // altering the saved phrase or the user's own meaning.
    val foldedQuery = PrivateOfflineSearch.fold(searchQuery.trim())
    val terms = foldedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
    val eligible = notes.filter { note ->
        note.language == language && note.meaningLanguage == meaningLanguage &&
            (!favoritesOnly || note in favorites) &&
            (theme == null ||
                (theme == "__without__" && topics.topicFor(note) == null) ||
                (theme != "__without__" && topics.topicFor(note) == theme)) &&
            terms.all { term ->
                PrivateOfflineSearch.contains(note.phrase, term) ||
                    PrivateOfflineSearch.contains(note.ownMeaning, term)
            }
    }.sortedWith(compareBy<PersonalPhrase>(
        { if (foldedQuery.isNotBlank() &&
            (PrivateOfflineSearch.equalsTo(it.phrase, foldedQuery) ||
                PrivateOfflineSearch.equalsTo(it.ownMeaning, foldedQuery))) 0 else 1 },
        { if (foldedQuery.isNotBlank() &&
            (PrivateOfflineSearch.beginsWith(it.phrase, foldedQuery) ||
                PrivateOfflineSearch.beginsWith(it.ownMeaning, foldedQuery))) 0 else 1 },
        { if (it in favorites) 0 else 1 },
        { topics.topicFor(it).orEmpty().lowercase(Locale.ROOT) },
        { it.phrase.lowercase(Locale.ROOT) }
    ))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Kies zelf een eerder opgeslagen zin om als losse beurt te tonen. " +
            "Niets wordt vertaald of naar het gesprek gekopieerd vóór je tikt.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.take(80).filterNot { char -> char.isISOControl() } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Zoek in eigen zin of betekenis") }
        )
        Text(
            "Meerdere zoekwoorden mogelijk; elk woord mag in de zin of je eigen betekenis staan. " +
                "Exacte en beginmatches staan bovenaan, daarna favorieten.",
            style = MaterialTheme.typography.bodySmall
        )
        if (searchQuery.isNotBlank()) {
            TextButton(onClick = { searchQuery = "" }) { Text("Wis zoeken") }
        }
        TextButton(onClick = { favoritesOnly = !favoritesOnly }) {
            Text(if (favoritesOnly) "✓ Alleen favoriete zinnen"
                else "☆ Alleen favoriete zinnen")
        }
        Text("Filter op jouw eigen thema", style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = { theme = null }) {
            Text(if (theme == null) "✓ Alle thema’s" else "Alle thema’s")
        }
        TextButton(onClick = { theme = "__without__" }) {
            Text(if (theme == "__without__") "✓ Zonder thema" else "Zonder thema")
        }
        themeNames.forEach { name ->
            TextButton(onClick = { theme = name }) {
                Text(if (theme == name) "✓ " + name else name)
            }
        }
        HorizontalDivider()
        Text(
            if (foldedQuery.isBlank()) eligible.size.toString() + " eigen zinnen in deze selectie"
            else eligible.size.toString() + " eigen zinnen gevonden",
            style = MaterialTheme.typography.labelMedium
        )
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            eligible.forEach { note ->
                val topic = topics.topicFor(note)
                if (note in favorites || !topic.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (note in favorites) {
                            Text("★ Favoriet", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        if (!topic.isNullOrBlank()) {
                            Text("Thema · $topic", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(note.phrase, style = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = if (note.language in setOf("ckb", "ps"))
                        TextDirection.Rtl else TextDirection.Ltr))
                Text(note.meaningLanguage.uppercase() + ": " + note.ownMeaning,
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onPick(note) }) {
                    Text("Gebruik dit eigen paar als beurt")
                }
                HorizontalDivider()
            }
        }
        if (eligible.isEmpty()) Text(
            if (foldedQuery.isBlank())
                "Geen eigen zinnen in deze selectie. Er wordt geen overeenkomst verzonnen."
            else "Geen eigen zinnen gevonden. Wis of wijzig de zoekwoorden; Tuddy verzint geen overeenkomst.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
