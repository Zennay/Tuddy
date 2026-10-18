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
    onBack: () -> Unit
) {
    val completed = lines.takeLast(80).filter {
        it.status == "Lokaal vertaald" &&
            it.original.isNotBlank() && it.translated.isNotBlank()
    }
    var followNewest by remember { mutableStateOf(true) }
    var chosenIndex by remember { mutableStateOf(0) }
    var translatedOnFarSide by remember { mutableStateOf(true) }
    var largeText by remember { mutableStateOf(true) }
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
