package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.PrivateOfflineSearch

/**
 * Voluntary, non-persistent recall practice using only corrections the user
 * consciously authored. Never infers accuracy, trains a model or exports text.
 * The response stays concealed until the user explicitly reveals it.
 */
@Composable
internal fun SessionCorrectionPracticeDialog(
    entries: List<SessionTranslationCorrection>,
    speaker: LocalCaptionSpeaker,
    onDismiss: () -> Unit
) {
    val snapshot = remember { entries.asReversed().toList() }
    var offset by remember { mutableIntStateOf(0) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var recalled by remember { mutableIntStateOf(0) }
    var toRepeat by remember { mutableIntStateOf(0) }
    var hardCards by remember { mutableStateOf(emptyList<SessionTranslationCorrection>()) }
    var retryCards by remember { mutableStateOf(emptyList<SessionTranslationCorrection>()) }
    var retryOnly by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var speechMessage by remember { mutableStateOf("") }
    var pairMenuOpen by remember { mutableStateOf(false) }
    var selectedPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var chosenCards by remember { mutableStateOf(snapshot.toSet()) }
    var selectionOpen by remember { mutableStateOf(false) }
    var selectionQuery by remember { mutableStateOf("") }
    var shuffled by remember { mutableStateOf(false) }
    var shuffledCards by remember { mutableStateOf(snapshot.shuffled()) }
    var batchSize by remember { mutableIntStateOf(12) }
    val pairs = snapshot.map { it.sourceCode to it.targetCode }.distinct()
    val ordered = if (shuffled) shuffledCards else snapshot
    val filtered = ordered.filter {
        it in chosenCards &&
            (selectedPair == null || (it.sourceCode to it.targetCode) == selectedPair)
    }
    val batch = if (retryOnly) retryCards else filtered.drop(offset).take(batchSize)
    val current = batch.getOrNull(currentIndex)
    val prompt = current?.let { if (reverse) it.translation else it.original }
    val answer = current?.let { if (reverse) it.original else it.translation }
    val promptLang = current?.let { if (reverse) it.targetCode else it.sourceCode }
    val answerLang = current?.let { if (reverse) it.sourceCode else it.targetCode }
    DisposableEffect(speaker) { onDispose { speaker.stop() } }

    fun resetRound() {
        speaker.stop()
        offset = 0
        currentIndex = 0
        revealed = false
        recalled = 0
        toRepeat = 0
        hardCards = emptyList()
        retryCards = emptyList()
        retryOnly = false
        speechMessage = ""
    }

    fun selectPair(pair: Pair<String, String>?) {
        selectedPair = pair
        pairMenuOpen = false
        resetRound()
    }

    fun advance(known: Boolean) {
        speaker.stop()
        if (known) recalled++ else {
            toRepeat++
            val card = current
            if (card != null && card !in hardCards) hardCards = hardCards + card
        }
        currentIndex++
        revealed = false
        speechMessage = ""
    }

    AlertDialog(
        onDismissRequest = { speaker.stop(); onDismiss() },
        title = { Text("Oefen mijn eigen correcties") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Dit zijn uitsluitend eigen, tijdelijke vertaalvoorstellen; " +
                        "geen bewezen woordenboek of taalmodel. " +
                        "Je antwoorden worden nergens bewaard.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (snapshot.isNotEmpty()) {
                    Box {
                        TextButton(onClick = { pairMenuOpen = true }) {
                            val selected = selectedPair
                            Text("Oefen taalpaar: " +
                                (if (selected == null) "Alle" else
                                    selected.first + " → " + selected.second) + " ▾")
                        }
                        DropdownMenu(
                            expanded = pairMenuOpen,
                            onDismissRequest = { pairMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alle taalparen") },
                                onClick = { selectPair(null) }
                            )
                            pairs.forEach { pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.first + " → " + pair.second) },
                                    onClick = { selectPair(pair) }
                                )
                            }
                        }
                    }
                }
                if (snapshot.isNotEmpty()) {
                    Text(
                        "Gekozen voor deze oefensessie: " + chosenCards.size +
                            " van " + snapshot.size + " eigen correcties. " +
                            "Een selectie wijzigt of bewaart geen zinnen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { speaker.stop(); selectionOpen = !selectionOpen }
                    ) { Text(if (selectionOpen) "Sluit mijn kaartselectie"
                        else "Kies zelf welke correcties ik oefen…") }
                    if (selectionOpen) {
                        OutlinedTextField(
                            value = selectionQuery,
                            onValueChange = { selectionQuery = it.take(70) },
                            singleLine = true,
                            label = { Text("Zoek correcties voor selectie") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val matching = snapshot.filter { row ->
                            val query = selectionQuery.trim()
                            query.isEmpty() || PrivateOfflineSearch.contains(row.original, query) ||
                                PrivateOfflineSearch.contains(row.translation, query) ||
                                PrivateOfflineSearch.contains(
                                    row.sourceCode + " → " + row.targetCode, query
                                )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                chosenCards = chosenCards + matching
                                resetRound()
                            }) { Text("Voeg zichtbare toe") }
                            TextButton(onClick = {
                                chosenCards = chosenCards - matching.toSet()
                                resetRound()
                            }) { Text("Haal zichtbare weg") }
                        }
                        matching.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = entry in chosenCards,
                                    onCheckedChange = { checked ->
                                        chosenCards = if (checked) chosenCards + entry
                                            else chosenCards - entry
                                        resetRound()
                                    }
                                )
                                Text(
                                    entry.sourceCode + " → " + entry.targetCode +
                                        "\\n" + entry.original + " → " + entry.translation,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            shuffled = !shuffled
                            if (shuffled) shuffledCards = snapshot.shuffled()
                            resetRound()
                        }) {
                            Text(if (shuffled) "✓ Willekeurige volgorde" else "Mix kaarten")
                        }
                        TextButton(onClick = {
                            if (shuffled) shuffledCards = snapshot.shuffled()
                            resetRound()
                        }) { Text("Ronde opnieuw") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 12).forEach { size ->
                            TextButton(onClick = {
                                batchSize = size
                                resetRound()
                            }) {
                                Text(if (batchSize == size) "✓ " + size + " kaarten"
                                    else size.toString() + " kaarten")
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    Text("Er zijn nog geen eigen correcties om te oefenen.")
                } else if (current == null) {
                    Text(
                        "Deze ronde voltooid: " + recalled + " zelf herkend, " +
                            toRepeat + " nog oefenen.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (hardCards.isNotEmpty()) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                speaker.stop()
                                retryCards = hardCards.toList()
                                retryOnly = true
                                hardCards = emptyList()
                                currentIndex = 0
                                revealed = false
                                recalled = 0
                                toRepeat = 0
                                speechMessage = ""
                            }
                        ) { Text("Herhaal mijn moeilijke zinnen (" +
                            hardCards.size + ")") }
                    }
                    val normalSize = filtered.drop(offset).take(batchSize).size
                    val hasNextBatch = offset + normalSize < filtered.size
                    Button(
                        onClick = {
                            speaker.stop()
                            offset = if (hasNextBatch) offset + normalSize else 0
                            retryOnly = false
                            retryCards = emptyList()
                            hardCards = emptyList()
                            currentIndex = 0
                            recalled = 0
                            toRepeat = 0
                            revealed = false
                            speechMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasNextBatch) "Volgende eigen correcties"
                            else "Opnieuw beginnen")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !revealed,
                            onClick = {
                                speaker.stop()
                                reverse = false
                                speechMessage = ""
                            }
                        ) { Text(if (reverse) "Bron → vertaling" else "✓ Bron → vertaling") }
                        OutlinedButton(
                            enabled = !revealed,
                            onClick = {
                                speaker.stop()
                                reverse = true
                                speechMessage = ""
                            }
                        ) { Text(if (reverse) "✓ Omgekeerd" else "Omgekeerd") }
                    }
                    Text(
                        "Kaart " + (if (retryOnly) currentIndex + 1 else
                            offset + currentIndex + 1) + " van " +
                            (if (retryOnly) batch.size else filtered.size) + " · " +
                            promptLang + " → " + answerLang +
                            (if (retryOnly) " · moeilijke zinnen" else "") +
                            (if (shuffled && !retryOnly) " · gemixt" else ""),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(prompt.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        speaker.speak(
                            prompt.orEmpty(), promptLang.orEmpty(), LocalSpeechPace.NORMAL
                        ) { speechMessage = it }
                    }) { Text("🔊 Beluister vraag lokaal") }
                    if (!revealed) {
                        Text(
                            "Probeer de andere taalrichting zelf te herinneren. " +
                                "Tik pas daarna om jouw eigen antwoord te zien.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { speaker.stop(); revealed = true; speechMessage = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Onthul mijn antwoord") }
                    } else {
                        Text(
                            answer.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = {
                            speaker.speak(
                                answer.orEmpty(), answerLang.orEmpty(), LocalSpeechPace.SLOW
                            ) { speechMessage = it }
                        }) { Text("🔊 Beluister antwoord rustig lokaal") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { advance(false) }) {
                                Text("Nog oefenen")
                            }
                            Button(onClick = { advance(true) }) { Text("Ik wist het") }
                        }
                    }
                    if (speechMessage.isNotBlank()) {
                        Text(speechMessage, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { speaker.stop(); onDismiss() }) { Text("Sluiten") }
        }
    )
}
