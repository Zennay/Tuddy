package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PersonalPhrase

/**
 * Two-person, offline presentation of the exact temporary conversation turns.
 * Unlike a phrase-deck slideshow, each turn keeps its chosen speaking direction.
 * Nothing is saved, translated, pronounced, shared or requested over the network.
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
    val turn = turns.getOrNull(index)
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
    fun TurnSide(text: String, language: String, label: String, inverted: Boolean) {
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
                text.ifBlank { "Geen geselecteerde zin." },
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
        onDismissRequest = onBack,
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
                    if (!swapSides) TurnSide(answer, answerLanguage, "TEGENOVER · MIJN EIGEN PAAR", true)
                    else TurnSide(prompt, promptLanguage, "TEGENOVER · MIJN BRONZIN", true)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(enabled = index > 0, onClick = { index-- }) {
                            Text("← Vorige")
                        }
                        Text("Beurt ${if (turn == null) 0 else index + 1} / ${turns.size}",
                            style = MaterialTheme.typography.labelMedium)
                        TextButton(enabled = index < turns.lastIndex, onClick = { index++ }) {
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
                        TextButton(onClick = onBack) { Text("Terug") }
                    }
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
                    if (!swapSides) TurnSide(prompt, promptLanguage, "HIER · MIJN BRONZIN", false)
                    else TurnSide(answer, answerLanguage, "HIER · MIJN EIGEN PAAR", false)
                }
            }
        }
    }
}
