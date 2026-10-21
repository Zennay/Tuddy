package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.livelingo.android.PrivatePhraseShortcuts
import com.livelingo.android.PrivatePhraseTopics

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
    val eligible = notes.filter {
        it.language == language && it.meaningLanguage == meaningLanguage &&
            (!favoritesOnly || it in favorites) &&
            (theme == null ||
                (theme == "__without__" && topics.topicFor(it) == null) ||
                (theme != "__without__" && topics.topicFor(it) == theme))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Kies zelf een eerder opgeslagen zin om als losse beurt te tonen. " +
            "Niets wordt vertaald of naar het gesprek gekopieerd vóór je tikt.",
            style = MaterialTheme.typography.bodySmall)
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
        Text(eligible.size.toString() + " eigen zinnen in deze selectie",
            style = MaterialTheme.typography.labelMedium)
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            eligible.forEach { note ->
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
        if (eligible.isEmpty()) Text("Geen eigen zinnen in deze selectie. " +
            "Er wordt geen overeenkomst verzonnen.",
            style = MaterialTheme.typography.bodySmall)
    }
}
