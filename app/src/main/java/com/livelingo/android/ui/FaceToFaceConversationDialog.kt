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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.livelingo.android.OfflineTypedCaptionPolicy
import com.livelingo.android.TargetCatalog

/**
 * One device on the table, with the far-side line rotated for the person
 * opposite the owner. The user opts in: never start the microphone, speech,
 * model installation, recording, storage, file provider or clipboard.
 *
 * Crucially, only lines with a completed local translation are admitted:
 * partial text, unavailable models and failed translations are NOT advice to
 * the other reader. History is bounded by the existing RAM conversation.
 */
@Composable
internal fun FaceToFaceConversationDialog(
    lines: List<TypedConversationLine>,
    onBack: () -> Unit,
    onSubmitTurn: ((String, String, String) -> Boolean)? = null,
    initialSource: String = "nl",
    initialTarget: String = "en"
) {
    val completed = lines.takeLast(80).filter {
        it.status == "Lokaal vertaald" &&
            it.original.isNotBlank() && it.translated.isNotBlank()
    }
    var followNewest by remember { mutableStateOf(true) }
    var chosenIndex by remember { mutableStateOf(0) }
    var translatedOnFarSide by remember { mutableStateOf(true) }
    var largeText by remember { mutableStateOf(true) }
    // Only the deliberate two-way typed caller offers input; live is read-only.
    var input by remember { mutableStateOf("") }
    var turnFrom by remember(initialSource) { mutableStateOf(initialSource) }
    var turnTo by remember(initialTarget) { mutableStateOf(initialTarget) }
    var turnNotice by remember { mutableStateOf("") }
    val index = if (followNewest) completed.lastIndex
        else chosenIndex.coerceIn(0, completed.lastIndex.coerceAtLeast(0))
    val selected = completed.getOrNull(index)
    val rtlLanguages = setOf("ar", "fa", "ur", "he", "ps", "ckb")
    fun direction(code: String): TextDirection =
        if (code.substringBefore('-') in rtlLanguages) TextDirection.Rtl
        else TextDirection.Ltr

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Rotate the ENTIRE far-side text block, not individual letters.
                // The phone is placed flat between two people, without rotating
                // the device or changing Android's orientation settings.
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer { rotationZ = 180f }
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val lang = if (translatedOnFarSide) selected?.target
                            else selected?.source
                        val value = if (translatedOnFarSide) selected?.translated
                            else selected?.original
                        Text("TEGENOVER · ${lang?.uppercase() ?: "—"}",
                            style = MaterialTheme.typography.labelLarge)
                        Text(value ?: "Nog geen afgeronde lokale vertaling.",
                            fontSize = if (largeText) 32.sp else 24.sp,
                            lineHeight = if (largeText) 41.sp else 32.sp,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = direction(lang ?: "nl")
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = 12.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            followNewest = false
                            chosenIndex = (index - 1).coerceAtLeast(0)
                        }, enabled = index > 0) { Text("← Vorige") }
                        Text("${if (selected == null) 0 else index + 1}/${completed.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 14.dp))
                        TextButton(onClick = {
                            if (index >= completed.lastIndex - 1) {
                                followNewest = true
                            } else {
                                followNewest = false
                                chosenIndex = index + 1
                            }
                        }, enabled = selected != null && index < completed.lastIndex
                        ) { Text("Volgende →") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            translatedOnFarSide = !translatedOnFarSide
                        }) { Text("↕ Wissel kanten") }
                        TextButton(onClick = { largeText = !largeText }) {
                            Text(if (largeText) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(onClick = {
                            followNewest = true
                        }) { Text("↓ Nieuwste") }
                        TextButton(onClick = onBack) { Text("Terug") }
                    }
                    if (onSubmitTurn != null) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Typ: ${turnFrom.uppercase()} → ${turnTo.uppercase()}",
                                style = MaterialTheme.typography.labelMedium)
                            TextButton(onClick = {
                                val prior = turnFrom
                                turnFrom = turnTo
                                turnTo = prior
                                turnNotice = "Handmatig van richting gewisseld."
                            }) { Text("↔ Richting") }
                        }
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                if (it.length <= OfflineTypedCaptionPolicy.MAX_INPUT_UNITS)
                                    input = it
                            },
                            label = { Text("Typ één zin · pas na afgeronde vertaling zichtbaar") },
                            minLines = 1, maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = direction(turnFrom))
                        )
                        Button(
                            enabled = turnFrom != turnTo &&
                                turnFrom in TargetCatalog.codes &&
                                turnTo in TargetCatalog.codes &&
                                OfflineTypedCaptionPolicy.normalize(input) != null,
                            onClick = {
                                val normalized = OfflineTypedCaptionPolicy.normalize(input)
                                if (normalized != null &&
                                    onSubmitTurn(normalized, turnFrom, turnTo)) {
                                    input = ""
                                    val previous = turnFrom
                                    turnFrom = turnTo
                                    turnTo = previous
                                    followNewest = true
                                    turnNotice = "Beurt toegevoegd. Nog geen resultaat " +
                                        "tot de lokale vertaling voltooid is."
                                } else {
                                    turnNotice = "Niet toegevoegd; controleer dit via de typmodus."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Vertaal beurt lokaal · wissel taal") }
                        if (turnNotice.isNotBlank()) {
                            Text(turnNotice, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("Alleen afgeronde lokale vertalingen · controleer de betekenis. " +
                        "Geen live audio, opslag of automatische synchronisatie.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState()).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val lang = if (translatedOnFarSide) selected?.source
                            else selected?.target
                        val value = if (translatedOnFarSide) selected?.original
                            else selected?.translated
                        Text("HIER · ${lang?.uppercase() ?: "—"}",
                            style = MaterialTheme.typography.labelLarge)
                        Text(value ?: "Kies in de typmodus twee talen en vertaal een zin " +
                            "met reeds aanwezige lokale modellen.",
                            fontSize = if (largeText) 32.sp else 24.sp,
                            lineHeight = if (largeText) 41.sp else 32.sp,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = direction(lang ?: "nl")
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    }
                }
            }
        }
    }
}
