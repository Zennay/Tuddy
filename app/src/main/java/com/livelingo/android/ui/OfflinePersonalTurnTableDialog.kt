package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

/**
 * Two-person, offline presentation of the exact temporary conversation turns.
 * Unlike a phrase-deck slideshow, each turn keeps its chosen speaking direction.
 * User-triggered local voice is optional, and practice can conceal the reply.
 * Difficult-turn markers and focus navigation also stay only in UI memory.
 * No automatic recording, translation, sharing or persistence.
 */
@Composable
internal fun OfflinePersonalTurnTableDialog(
    turns: List<Pair<PersonalPhrase, Boolean>>,
    initialIndex: Int,
    onBack: () -> Unit
) {
    var index by remember(turns, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, (turns.size - 1).coerceAtLeast(0)))
    }
    var swapSides by remember { mutableStateOf(false) }
    var large by remember { mutableStateOf(true) }
    var practice by remember { mutableStateOf(false) }
    var revealStep by remember { mutableIntStateOf(0) }
    var revealed by remember(index, practice, revealStep) { mutableStateOf(false) }
    var slowSpeech by remember { mutableStateOf(false) }
    var repeatIndices by remember(turns) { mutableStateOf<Set<Int>>(emptySet()) }
    var repeatOnly by remember(turns) { mutableStateOf(false) }
    var mixed by remember(turns) { mutableStateOf(false) }
    var reviewOrder by remember(turns) { mutableStateOf(turns.indices.toList()) }
    val navigation = reviewOrder.filter { !repeatOnly || it in repeatIndices }
    val position = navigation.indexOf(index)
    fun visit(next: Int) {
        if (next in turns.indices) {
            index = next
            revealStep++ // Never carry an answer reveal to a new recall question.
        }
    }
    var audioNotice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    DisposableEffect(lifecycle, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    val turn = turns.getOrNull(index)
    LaunchedEffect(index, swapSides, practice, turn) {
        speaker.stop()
        audioNotice = ""
    }
    val responseHidden = practice && !revealed
    fun readAuthored(text: String, language: String) {
        speaker.speak(
            text, language,
            if (slowSpeech) LocalSpeechPace.SLOW else LocalSpeechPace.NORMAL
        ) { audioNotice = it }
    }

    val prompt = turn?.let { (note, fromMeaning) ->
        if (fromMeaning) note.ownMeaning else note.phrase
    }.orEmpty()
    val answer = turn?.let { (note, fromMeaning) ->
        if (fromMeaning) note.phrase else note.ownMeaning
    }.orEmpty()
    val promptLanguage = turn?.let { (note, fromMeaning) ->
        if (fromMeaning) note.meaningLanguage else note.language
    }.orEmpty()
    val answerLanguage = turn?.let { (note, fromMeaning) ->
        if (fromMeaning) note.language else note.meaningLanguage
    }.orEmpty()
    fun direction(code: String): TextDirection =
        if (code in setOf("ckb", "ps", "ar", "fa", "ur", "he"))
            TextDirection.Rtl else TextDirection.Ltr

    @Composable
    fun TurnSide(text: String, language: String, label: String, inverted: Boolean, hidden: Boolean) {
        Column(
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { rotationZ = if (inverted) 180f else 0f }
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                if (hidden) "Antwoord verborgen · onthul in het midden"
                else text.ifBlank { "Geen geselecteerde zin." },
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = direction(language)
                ),
                fontSize = if (large) 32.sp else 23.sp,
                lineHeight = if (large) 42.sp else 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
            Text(language.uppercase(), style = MaterialTheme.typography.labelSmall)
        }
    }

    Dialog(
        onDismissRequest = { speaker.stop(); onBack() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    if (!swapSides) TurnSide(answer, answerLanguage, "TEGENOVER · MIJN EIGEN PAAR", true, responseHidden)
                    else TurnSide(prompt, promptLanguage, "TEGENOVER · MIJN BRONZIN", true, false)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(enabled = position > 0,
                            onClick = { visit(navigation[position - 1]) }) {
                            Text("← Vorige")
                        }
                        Text(
                            if (repeatOnly)
                                "Herhalen ${if (position >= 0) position + 1 else 0}/${navigation.size}"
                            else "Beurt ${if (turn == null) 0 else position + 1}/${navigation.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        TextButton(enabled = position >= 0 &&
                            position < navigation.lastIndex,
                            onClick = { visit(navigation[position + 1]) }) {
                            Text("Volgende →")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { swapSides = !swapSides }) { Text("↕ Kanten") }
                        TextButton(onClick = { large = !large }) {
                            Text(if (large) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(onClick = {
                            speaker.stop()
                            onBack()
                        }) { Text("Terug") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(enabled = turn != null, onClick = {
                            if (index !in repeatIndices) {
                                repeatIndices = repeatIndices + index
                            } else {
                                val remaining = repeatIndices - index
                                repeatIndices = remaining
                                if (repeatOnly) {
                                    if (remaining.isEmpty()) {
                                        repeatOnly = false
                                    } else {
                                        visit(reviewOrder.first { it in remaining })
                                    }
                                }
                            }
                        }) {
                            Text(if (index in repeatIndices) "✓ Nog lastig"
                                else "↻ Markeer lastig")
                        }
                        TextButton(
                            enabled = repeatIndices.isNotEmpty(),
                            onClick = {
                                repeatOnly = !repeatOnly
                                if (repeatOnly && index !in repeatIndices) {
                                    visit(reviewOrder.first { it in repeatIndices })
                                }
                            }
                        ) {
                            Text(if (repeatOnly) "Alle beurten"
                                else "Herhaal (${repeatIndices.size})")
                        }
                        Text("${repeatIndices.size} lastig",
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(enabled = turns.size > 1, onClick = {
                            mixed = !mixed
                            reviewOrder = if (mixed) turns.indices.shuffled()
                                else turns.indices.toList()
                            revealStep++
                            speaker.stop()
                        }) {
                            Text(if (mixed) "✓ Willekeurig" else "⇄ Mix beurten")
                        }
                        Text(
                            if (mixed) "Willekeurige oefenvolgorde"
                            else "Oorspronkelijke volgorde",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { practice = !practice }) {
                            Text(if (practice) "✓ Oefenmodus" else "○ Oefenmodus")
                        }
                        TextButton(
                            enabled = practice && responseHidden && turn != null,
                            onClick = { revealed = true }
                        ) { Text(if (responseHidden) "Toon antwoord" else "Antwoord zichtbaar") }
                        TextButton(onClick = { slowSpeech = !slowSpeech }) {
                            Text(if (slowSpeech) "Langzaam" else "Normaal")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(enabled = turn != null &&
                            (!responseHidden || !turn.second), onClick = {
                            turn?.first?.let { readAuthored(it.phrase, it.language) }
                        }) { Text("🔊 Eigen zin") }
                        TextButton(enabled = turn != null &&
                            (!responseHidden || turn.second), onClick = {
                            turn?.first?.let { readAuthored(it.ownMeaning, it.meaningLanguage) }
                        }) { Text("🔊 Betekenis") }
                        TextButton(onClick = {
                            speaker.stop()
                            audioNotice = "Lokale voorlezing gestopt."
                        }) { Text("■ Stop") }
                    }
                    if (audioNotice.isNotBlank()) {
                        Text(audioNotice, style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center)
                    }
                    Text(
                        "Voorlezen alleen na jouw tik met een door Android als " +
                            "geïnstalleerd én lokaal gemelde stem. " +
                            "Ontbreekt die stem, dan geen netwerkfallback.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                    PrivateTableScreenAwakeControl()
                    Text(
                        "Uitsluitend jouw zelfgeschreven, niet gecontroleerde zinnen. " +
                            "Tijdelijk in geheugen · geen spraakherkenning of automatische vertaling.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (!swapSides) TurnSide(prompt, promptLanguage, "HIER · MIJN BRONZIN", false, false)
                    else TurnSide(answer, answerLanguage, "HIER · MIJN EIGEN PAAR", false, responseHidden)
                }
            }
        }
    }
}
