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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PersonalPhrasePracticeRound
import com.livelingo.android.PrivatePhraseGap
import com.livelingo.android.PrivatePhraseShortcuts
import com.livelingo.android.LocalCaptionSpeaker

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
    var audioNotice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    DisposableEffect(speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
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
    var gapMode by remember { mutableStateOf(false) }
    var typedGap by remember { mutableStateOf("") }
    var round by remember(selected) {
        mutableStateOf(PersonalPhrasePracticeRound.start(selected))
    }
    val card = round.current
    val gap = if (gapMode && card != null) PrivatePhraseGap.from(card, round.attempts) else null
    val names = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val isRtl = card?.language == "ckb" || card?.language == "ps"
    LaunchedEffect(card?.phrase, card?.language, round.revealed, round.attempts,
        selectedLanguage, shuffleVersion, favoritesOnly, reverse, gapMode) {
        speaker.stop()
        audioNotice = ""
        typedGap = ""
    }

    AlertDialog(
        onDismissRequest = { speaker.stop(); onDismiss() },
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
                TextButton(enabled = !gapMode, onClick = { reverse = !reverse }) {
                    Text(if (reverse) "Oefen: eigen betekenis → zin  ↔"
                        else "Oefen: zin → eigen betekenis  ↔")
                }
                TextButton(onClick = {
                    gapMode = !gapMode
                    typedGap = ""
                    speaker.stop()
                    round = PersonalPhrasePracticeRound.start(selected, offset)
                }) {
                    Text(if (gapMode) "✓ Ontbrekend woord oefenen" else
                        "◻ Ontbrekend woord oefenen")
                }
                if (gapMode) {
                    Text("Vul zelf één verborgen woord uit jouw eigen zin in. " +
                        "Na onthullen beoordeel je zelf; Tuddy controleert geen spelling " +
                        "of taalcorrectheid. Korte zinnen zonder twee woorden gebruiken " +
                        "de normale oefenkaart.", style = MaterialTheme.typography.bodySmall)
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
                        if (gap != null) gap.hiddenPhrase else if (reverse) card.ownMeaning else card.phrase,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            textDirection = if ((gap != null || !reverse) && isRtl) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    Text(
                        if (gap != null) "Mijn eigen zin · ontbrekend woord"
                            else if (reverse) "Eigen betekenis (${card.meaningLanguage})"
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
                    if (gap != null && !round.revealed) {
                        OutlinedTextField(
                            value = typedGap,
                            onValueChange = { typedGap = it.take(100) },
                            label = { Text("Mijn ontbrekende woord (niet opgeslagen)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                            )
                        )
                        Text("Voor het onthullen geen gesproken volledige zin: die zou " +
                            "het ontbrekende woord verraden.",
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        OfflinePhraseSpeechActions(
                            original = if (gapMode) card.phrase else if (reverse)
                                card.ownMeaning else card.phrase,
                            originalLanguage = if (gapMode) card.language else if (reverse)
                                card.meaningLanguage else card.language,
                            meaning = if (gapMode) card.ownMeaning else if (reverse)
                                card.phrase else card.ownMeaning,
                            meaningLanguage = if (gapMode) card.meaningLanguage else if (reverse)
                                card.language else card.meaningLanguage,
                            originalLabel = "Vraag",
                            meaningLabel = "Antwoord",
                            speaker = speaker,
                            onNotice = { audioNotice = it },
                            meaningReady = round.revealed
                        )
                    }
                    if (audioNotice.isNotBlank()) {
                        Text(audioNotice, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        (if (gapMode) "Volledige zin beluisteren kan pas na onthullen. "
                        else "Hoor het antwoord pas na Toon mijn betekenis. ") +
                            "Alleen reeds geïnstalleerde lokale Android-stemmen, " +
                            "geen online stem of automatisch doorspelen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (round.revealed) {
                        if (gap != null) {
                            Text("Ontbrekend woord uit je eigen zin:",
                                style = MaterialTheme.typography.labelMedium)
                            Text(gap.missingToken,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDirection = if (isRtl) TextDirection.Rtl
                                        else TextDirection.Ltr
                                ))
                            Text("Jouw poging: " + (typedGap.takeIf { it.isNotBlank() }
                                ?: "(niet ingevuld)"),
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Volledige eigen zin: " + card.phrase,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDirection = if (isRtl) TextDirection.Rtl
                                        else TextDirection.Ltr
                                ))
                            Text("Zelfgeschreven betekenis: " + card.ownMeaning,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        if (gap == null) Text(
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
                        ) { Text(if (gap != null) "Toon ontbrekend woord" else
                            "Toon mijn betekenis") }
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
            TextButton(onClick = { speaker.stop(); onDismiss() }) { Text("Terug naar eigen zinnen") }
        }
    )
}
