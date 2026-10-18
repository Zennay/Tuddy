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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.random.Random
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PersonalPhrasePracticeRound
import com.livelingo.android.PrivatePhraseShortcuts

/**
 * Optional study of notes typed by the user. No translation engine, online
 * dictionary, or claim that a native speaker verified the source material.
 */
@Composable
internal fun PersonalPhrasePracticePanel(
    notes: List<PersonalPhrase>,
    initialLanguage: String,
    onDismiss: () -> Unit
) {
    // The user can switch between their personally authored language groups
    // without importing any data, restarting the app, or disclosing study state.
    var selectedLanguage by remember { mutableStateOf(
        initialLanguage.takeIf { it in setOf("kmr", "ckb", "ps") } ?: "all"
    ) }
    var shuffleVersion by remember { mutableIntStateOf(0) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var pinsRevision by remember { mutableIntStateOf(0) }
    var favoriteNotice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val shortcuts = remember(context) { PrivatePhraseShortcuts(context) }
    val pinnedNotes = remember(notes, pinsRevision) {
        shortcuts.pinnedNotes(notes)
    }
    // A bookmark change should restart the current round ONLY if the
    // favorites-only filter is enabled and the actual deck changes.
    val selected = remember(notes, selectedLanguage, shuffleVersion,
        favoritesOnly, if (favoritesOnly) pinsRevision else -1) {
        val filtered = notes.filter { note ->
            (selectedLanguage == "all" || note.language == selectedLanguage) &&
                (!favoritesOnly || note in pinnedNotes)
        }
        if (shuffleVersion == 0) filtered else filtered.shuffled(Random(shuffleVersion))
    }
    var offset by remember(selected) { mutableIntStateOf(0) }
    var reverse by remember { mutableStateOf(false) }
    var round by remember(selected) {
        mutableStateOf(PersonalPhrasePracticeRound.start(selected))
    }
    val card = round.current
    val names = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val isRtl = card?.language == "ckb" || card?.language == "ps"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oefen mijn eigen zinnen · offline") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 490.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Alle zinnen en betekenissen zijn door jou zelf geschreven; " +
                        "Tuddy controleert ze niet of met een taalmodel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Kies welke van je eigen zinnen je oefent",
                    style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { selectedLanguage = "all" }) {
                        Text(if (selectedLanguage == "all") "Alle ✓" else "Alle")
                    }
                    TextButton(onClick = { selectedLanguage = "kmr" }) {
                        Text(if (selectedLanguage == "kmr") "Kurmanji ✓" else "Kurmanji")
                    }
                    TextButton(onClick = { selectedLanguage = "ckb" }) {
                        Text(if (selectedLanguage == "ckb") "Sorani ✓" else "Sorani")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { selectedLanguage = "ps" }) {
                        Text(if (selectedLanguage == "ps") "Pasjtoe ✓" else "Pasjtoe")
                    }
                    TextButton(
                        enabled = selected.size > 1,
                        onClick = { shuffleVersion++ }
                    ) { Text("↻ Schud kaarten") }
                    Text("${selected.size} zinnen",
                        style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = {
                    favoritesOnly = !favoritesOnly
                    favoriteNotice = ""
                }) {
                    Text(if (favoritesOnly) "★ Alleen favorieten ✓" else
                        "☆ Oefen favorieten (${pinnedNotes.size})")
                }
                if (favoriteNotice.isNotBlank()) {
                    Text(favoriteNotice, style = MaterialTheme.typography.bodySmall)
                }
                Text("Je oefent ${selected.size} eigen zinnen. Favorieten zijn " +
                    "een privé-selectie, geen controle op juistheid.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { reverse = !reverse }) {
                    Text(if (reverse) "Oefen: eigen betekenis → zin  ↔"
                        else "Oefen: zin → eigen betekenis  ↔")
                }
                if (round.total == 0) {
                    Text(if (favoritesOnly)
                        "Geen favoriete zinnen voor deze taal. Kies Alle zinnen, " +
                            "bewaar eerst een favoriet in Mijn gesprekskaarten " +
                            "of schakel Alleen favorieten uit."
                        else "Geen eigen zinnen voor deze taal. Kies Alle of sla eerst " +
                            "zelf een zin op in je privé-oefenlijst.")
                } else if (card != null) {
                    Text(
                        "Afgerond ${round.recalled + round.needsPractice}/${round.total} " +
                            "· nog ${round.remaining.size} kaarten",
                        style = MaterialTheme.typography.labelMedium
                    )
                    HorizontalDivider()
                    Text(
                        if (reverse) card.ownMeaning else card.phrase,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            textDirection = if (!reverse && isRtl) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    Text(
                        if (reverse) "Eigen betekenis (${card.meaningLanguage})"
                            else "Eigen zin · ${names[card.language] ?: card.language}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextButton(onClick = {
                        val result = shortcuts.toggle(card, notes)
                        if (result == null) {
                            favoriteNotice = "Favoriet niet opgeslagen. Maximaal " +
                                PrivatePhraseShortcuts.MAX_PINS +
                                " kaarten of lokale opslag niet beschikbaar."
                        } else {
                            pinsRevision++
                            favoriteNotice = if (result)
                                "Mijn eigen zin als favoriet bewaard."
                            else "Favoriet verwijderd."
                        }
                    }) {
                        Text(if (card in pinnedNotes) "★ Favoriet verwijderen"
                            else "☆ Bewaar als favoriet")
                    }
                    if (round.revealed) {
                        Text(
                            if (reverse) card.phrase else card.ownMeaning,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDirection = if (reverse && isRtl) TextDirection.Rtl
                                    else TextDirection.Ltr
                            )
                        )
                        Text(
                            "Beoordeel zelf. Dit bewijst geen correcte vertaling.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { round = round.grade(false) }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { round = round.grade(true) }
                            ) { Text("Ik wist het") }
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { round = round.reveal() }
                        ) { Text("Toon mijn betekenis") }
                    }
                    Text(
                        "Alleen de geselecteerde eigen taalset. Schud kaarten begint " +
                            "een nieuwe ronde; lastige zinnen komen hoogstens eenmaal terug.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Oefenronde klaar", style = MaterialTheme.typography.headlineSmall)
                    Text("${round.recalled} zelf herkend, ${round.needsPractice} nog lastig.")
                    Text(
                        "Dit is alleen jouw oordeel; resultaten verdwijnen bij sluiten.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            round = PersonalPhrasePracticeRound.start(selected, offset)
                        }
                    ) { Text("Oefen dezelfde set opnieuw") }
                    if (selected.size > PersonalPhrasePracticeRound.BATCH_SIZE) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                offset = (offset + PersonalPhrasePracticeRound.BATCH_SIZE) % selected.size
                                round = PersonalPhrasePracticeRound.start(selected, offset)
                            }
                        ) { Text("Volgende zinnen oefenen") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar eigen zinnen") }
        }
    )
}
