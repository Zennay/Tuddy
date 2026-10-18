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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateVocabularyDeck

/**
 * Offline review/delete screen; only curated language+word keys ever persist.
 * A full conversation is never loaded from or saved to device storage.
 */
@Composable
internal fun SavedWordsPanel(
    deck: PrivateVocabularyDeck,
    revision: Int,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val allWords = remember(revision) { deck.entries() }
    var search by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(emptySet<String>()) }
    val searchTerm = search.trim()
    val words = if (searchTerm.isBlank()) allWords else allWords.filter {
        it.word.contains(searchTerm, ignoreCase = true) ||
            it.language.contains(searchTerm, ignoreCase = true)
    }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn opgeslagen woorden") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Alleen zelf bewaarde woorden uit de kleine gecontroleerde woordenlijst. " +
                        "Je gesprekken worden niet opgeslagen en niets gaat naar een server.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(40) },
                    label = { Text("Zoek opgeslagen woord of taal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("${words.size} van ${allWords.size} woorden · betekenissen verborgen om te oefenen",
                    style = MaterialTheme.typography.bodySmall)
                if (words.isEmpty()) {
                    Text(if (allWords.isEmpty())
                        "Nog geen woorden. Tik op een woord in een ondertitel en kies Bewaar woord."
                        else "Geen opgeslagen woorden met deze zoekterm.")
                }
                words.forEach { word ->
                    HorizontalDivider()
                    val key = "${word.language}|${word.word}"
                    Column {
                        Text("${word.entry.illustration} ${word.word} · ${word.language}",
                            style = MaterialTheme.typography.titleSmall)
                        if (key in revealed) {
                            Text(word.entry.definition, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = {
                                revealed = if (key in revealed) revealed - key else revealed + key
                            }) { Text(if (key in revealed) "Verberg betekenis" else "Toon betekenis") }
                            TextButton(onClick = {
                                if (deck.remove(word.language, word.word)) {
                                    revealed = revealed - key
                                    onChanged()
                                }
                            }) { Text("Verwijder") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        },
        dismissButton = {
            if (allWords.isNotEmpty()) {
                TextButton(onClick = { confirmDelete = true }) { Text("Wis alle woorden") }
            }
        }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Alle opgeslagen woorden wissen?") },
            text = { Text("Dit wist de lokale woordenlijst definitief, niet je tijdelijke gesprek.") },
            confirmButton = {
                TextButton(onClick = {
                    if (deck.clear()) onChanged()
                    confirmDelete = false
                }) { Text("Wis woorden") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") }
            }
        )
    }
}
