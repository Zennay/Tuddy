package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseScriptSearch
import com.livelingo.android.PrivatePhraseShortcuts
import com.livelingo.android.PrivatePhraseTopics
import java.util.Locale

/**
 * One-tap offline access to the user's deliberate study notes as big,
 * bilingual communication cards. There is no speech/model availability claim.
 */
@Composable
internal fun OfflinePhraseQuickPicker(
    phrases: List<PersonalPhrase>?,
    initialLanguage: String,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    val names = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    var code by remember {
        mutableStateOf(initialLanguage.takeIf { it in names } ?: "kmr")
    }
    var visibleLimit by remember { mutableIntStateOf(80) }
    var query by remember { mutableStateOf("") }
    var presenting by remember { mutableStateOf<PersonalPhrase?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var pinsRevision by remember { mutableIntStateOf(0) }
    var pinNotice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val shortcuts = remember(context) { PrivatePhraseShortcuts(context) }
    val topicStore = remember(context) { PrivatePhraseTopics(context) }
    val topics = topicStore.topics()
    val filteredTopic = selectedTopic?.takeIf { it == "__without__" || it in topics }
    val availableNotes = phrases.orEmpty()
    val pinnedNotes = remember(phrases, pinsRevision) {
        shortcuts.pinnedNotes(availableNotes)
    }
    // Terms can match either field, independently, without a server search.
    // Keep the presentation deck EXACTLY equal to the user's visible filter.
    val terms = query.trim().split(Regex("\\s+")).map(PrivatePhraseScriptSearch::fold)
        .filter { it.isNotBlank() }
    val filtered = availableNotes.filter { note ->
        val assigned = topicStore.topicFor(note)
        val matchesTopic = when (filteredTopic) {
            null -> true
            "__without__" -> assigned == null
            else -> assigned == filteredTopic
        }
        matchesTopic &&
            (!favoritesOnly || note in pinnedNotes) &&
            (code == "all" || note.language == code) &&
            terms.all { term ->
                PrivatePhraseScriptSearch.contains(note.phrase, term) ||
                    PrivatePhraseScriptSearch.contains(note.ownMeaning, term)
            }
    }.sortedWith(compareByDescending<PersonalPhrase> { it in pinnedNotes }
        .thenBy { it.language }.thenBy { it.phrase.lowercase(Locale.ROOT) })
    val shown = filtered.take(visibleLimit)

    val chosen = presenting
    if (chosen != null) {
        OfflinePhrasePresenter(
            notes = shown,
            initial = chosen,
            mixedLanguageDeck = true,
            onDismiss = { presenting = null }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn offline gesprekskaarten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "Zoek je eigen vooraf opgeslagen zinnen en toon ze direct " +
                        "groot op het scherm. Dit zijn jouw eigen betekenissen: " +
                        "Tuddy heeft ze niet automatisch vertaald of gecontroleerd.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextButton(onClick = { code = "all"; visibleLimit = 80 }) {
                        Text(if (code == "all") "Alle ✓" else "Alle")
                    }
                    names.forEach { (tag, name) ->
                        TextButton(onClick = { code = tag; visibleLimit = 80 }) {
                            Text(if (code == tag) name + " ✓" else name)
                        }
                    }
                }
                Text("Mijn zelfgekozen thema’s", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextButton(onClick = { selectedTopic = null; visibleLimit = 80 }) {
                        Text(if (filteredTopic == null) "Alle thema’s ✓" else "Alle thema’s")
                    }
                    TextButton(onClick = {
                        selectedTopic = "__without__"
                        visibleLimit = 80
                    }) {
                        Text(if (filteredTopic == "__without__") "Zonder thema ✓"
                            else "Zonder thema")
                    }
                    topics.forEach { topic ->
                        TextButton(onClick = {
                            selectedTopic = topic
                            visibleLimit = 80
                        }) {
                            Text(if (filteredTopic == topic) "$topic ✓" else topic)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { favoritesOnly = !favoritesOnly; visibleLimit = 80 }) {
                        Text(if (favoritesOnly) "★ Alleen favorieten"
                            else "☆ Favorieten (" + pinnedNotes.size + ")")
                    }
                }
                if (pinNotice.isNotBlank()) {
                    Text(pinNotice, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(90); visibleLimit = 80 },
                    label = { Text("Zoek woorden in zin of eigen betekenis") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (phrases == null) {
                    Text("De persoonlijke zinnenlijst is onleesbaar. " +
                        "Er wordt niets geraden of overschreven.",
                        color = MaterialTheme.colorScheme.error)
                } else if (shown.isEmpty()) {
                    Text(if (availableNotes.isEmpty())
                        "Nog geen eigen zinnen. Voeg eerst zelf een zin en betekenis toe."
                        else "Geen kaarten voor deze taal, favorieten en zoekwoorden. " +
                            "Pas je filters aan.")
                } else {
                    Text("${shown.size} van ${filtered.size} kaart(en) · alleen offline",
                        style = MaterialTheme.typography.labelMedium)
                    Text("Toon groot opent alleen de gevonden kaarten in deze volgorde; " +
                        "andere persoonlijke zinnen blijven buiten deze presentatie.",
                        style = MaterialTheme.typography.bodySmall)
                    shown.forEach { item ->
                        HorizontalDivider()
                        Text((names[item.language] ?: item.language) + " · eigen zin",
                            style = MaterialTheme.typography.labelSmall)
                        Text(item.phrase,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDirection = if (item.language == "ckb" ||
                                    item.language == "ps") TextDirection.Rtl
                                    else TextDirection.Ltr
                            ), modifier = Modifier.fillMaxWidth())
                        Text(item.ownMeaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Privéthema: ${topicStore.topicFor(item) ?: "Geen"}",
                            style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { presenting = item }) {
                                Text("Toon groot")
                            }
                            TextButton(onClick = {
                                val changed = shortcuts.toggle(item, availableNotes)
                                if (changed == null) {
                                    pinNotice = "Niet opgeslagen. Maximaal " +
                                        PrivatePhraseShortcuts.MAX_PINS +
                                        " favorieten, of lokale opslag niet beschikbaar."
                                } else {
                                    pinsRevision++
                                    pinNotice = if (changed) "Gesprekszin als favoriet bewaard."
                                        else "Favoriet verwijderd."
                                }
                            }) {
                                Text(if (item in pinnedNotes) "★ Bewaard" else "☆ Favoriet")
                            }
                        }
                    }
                    if (shown.size < filtered.size) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { visibleLimit += 80 }
                        ) {
                            Text("Toon volgende kaarten (${filtered.size - shown.size} over)")
                        }
                    }
                }
                HorizontalDivider()
                TextButton(
                    onClick = onManage,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mijn zinnen toevoegen en beheren") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
