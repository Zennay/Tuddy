package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateOfflineSearch
import com.livelingo.android.PrivatePhraseShortcuts
import java.util.Locale

/** Exact user-authored offline notes only: not a generated or reviewed translation. */
@Composable
internal fun OfflinePersonalPhraseLookupDialog(
    initialLanguage: String,
    onManagePersonalPhrases: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deck = remember(context) { PrivatePhraseDeck(context) }
    val notes = remember(deck) { deck.entries() }
    var language by remember(initialLanguage) {
        mutableStateOf<String?>(initialLanguage.takeIf { it in setOf("kmr", "ckb", "ps") })
    }
    var meaningLanguage by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PersonalPhrase?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var visibleLimit by remember { mutableIntStateOf(20) }
    val shortcuts = remember(context) { PrivatePhraseShortcuts(context) }
    val pinned = remember(notes, shortcuts) { shortcuts.pinnedNotes(notes.orEmpty()) }
    val labels = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    // Each word may occur in either field, including mixed-script searches.
    // Normalization applies to search only: saved user-authored text is unchanged.
    val search = PrivateOfflineSearch.fold(query.trim())
    val terms = search.split(Regex("\\s+")).filter { it.isNotBlank() }
    val matches = notes.orEmpty().filter { note ->
        (language == null || note.language == language) &&
            (meaningLanguage == null || note.meaningLanguage == meaningLanguage) &&
            (!favoritesOnly || note in pinned) &&
            terms.all { term ->
                PrivateOfflineSearch.contains(note.phrase, term) ||
                    PrivateOfflineSearch.contains(note.ownMeaning, term)
            }
    }.sortedWith(compareBy<PersonalPhrase>(
        { if (search.isNotEmpty() &&
            (PrivateOfflineSearch.equalsTo(it.phrase, search) ||
                PrivateOfflineSearch.equalsTo(it.ownMeaning, search))) 0 else 1 },
        { if (search.isNotEmpty() &&
            (PrivateOfflineSearch.beginsWith(it.phrase, search) ||
                PrivateOfflineSearch.beginsWith(it.ownMeaning, search))) 0 else 1 },
        { if (it in pinned) 0 else 1 },
        { it.language }, { it.phrase.lowercase(Locale.ROOT) }
    ))
    val shown = matches.take(visibleLimit)
    selected?.let { note ->
        if (note in shown) {
            OfflinePhrasePresenter(notes = shown, initial = note,
                mixedLanguageDeck = true, onDismiss = { selected = null })
            return
        }
        selected = null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zoek in mijn eigen zinnen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Doorzoek uitsluitend jouw eigen opgeslagen zinnen en betekenissen. " +
                    "Geen automatisch vertaalmodel, microfoon of internet. " +
                    "Niets is door moedertaalsprekers gecontroleerd.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = query,
                    onValueChange = { query = it.take(80); visibleLimit = 20 },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Zoek op zin of zelfgeschreven betekenis") })
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (listOf<String?>(null, "kmr", "ckb", "ps")).forEach { option ->
                        val label = labels[option] ?: "Alle"
                        TextButton(onClick = { language = option; visibleLimit = 20 }) {
                            Text((if (language == option) "✓ " else "") + label,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (listOf<String?>(null, "nl", "en")).forEach { option ->
                        val label = when (option) {
                            "nl" -> "Nederlands"
                            "en" -> "English"
                            else -> "NL + EN"
                        }
                        TextButton(onClick = { meaningLanguage = option; visibleLimit = 20 }) {
                            Text((if (meaningLanguage == option) "✓ " else "") + label,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                TextButton(onClick = {
                    favoritesOnly = !favoritesOnly
                    visibleLimit = 20
                }) {
                    Text(if (favoritesOnly) "★ Alleen mijn favorieten"
                        else "☆ Alleen favorieten (" + pinned.size + ")")
                }
                Text("Meerdere zoekwoorden mogen in beide taalvelden staan. " +
                    "Alle tekst blijft exact zoals jij die hebt opgeslagen; " +
                    "favorieten krijgen bij zoeken voorrang.",
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                when {
                    notes == null -> Text("Eigen zinnen konden niet worden gelezen. " +
                        "Er worden geen gegevens gewijzigd.",
                        color = MaterialTheme.colorScheme.error)
                    notes.isEmpty() -> Text("Nog geen eigen zinnen. Voeg ze handmatig " +
                        "toe of importeer je eigen bestanden via Mijn eigen zinnen.")
                    matches.isEmpty() -> Text("Geen eigen zin gevonden. Tuddy " +
                        "verzint geen ontbrekende vertalingen.")
                    else -> {
                        Text(shown.size.toString() + " zichtbaar van " +
                            matches.size + " gevonden · " + notes.size +
                            " persoonlijke zinnen", style = MaterialTheme.typography.labelMedium)
                        shown.forEach { note ->
                            HorizontalDivider()
                            Text((labels[note.language] ?: note.language) + " · " + note.phrase,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (note.language in setOf("ckb", "ps"))
                                        TextDirection.Rtl else TextDirection.Ltr))
                            Text(note.meaningLanguage.uppercase(Locale.ROOT) + ": " +
                                note.ownMeaning, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { selected = note }) {
                                Text("Toon zelfgeschreven kaart groot")
                            }
                        }
                        if (shown.size < matches.size) {
                            TextButton(onClick = { visibleLimit =
                                (visibleLimit + 20).coerceAtMost(80) }) {
                                Text("Toon volgende 20 eigen zinnen")
                            }
                        }
                    }
                }
                Text("Gebruik ongecontroleerde vertalingen niet voor urgente, " +
                    "medische of juridische beslissingen zonder professionele tolk.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar basiswoorden") }
        },
        dismissButton = {
            TextButton(onClick = onManagePersonalPhrases) { Text("Mijn zinnen beheren…") }
        }
    )
}
