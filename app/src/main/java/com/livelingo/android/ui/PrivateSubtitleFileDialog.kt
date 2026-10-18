package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PrivateSubtitleCue

/**
 * Separate, read-only document viewer. Never shares, translates, speaks,
 * changes the ongoing conversation, or attempts timestamp synchronization.
 */
@Composable
internal fun PrivateSubtitleFileDialog(
    cues: List<PrivateSubtitleCue>,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var shownIndex by remember { mutableIntStateOf(-1) }
    var largeText by remember { mutableStateOf(true) }
    val found = cues.filter { it.text.contains(search.trim(), ignoreCase = true) }
    val focused = found.getOrNull(shownIndex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Eigen SRT/WebVTT-bestand · alleen lezen",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text("Tijden en tekst komen uitsluitend uit het gekozen bestand; " +
                    "geen onafhankelijke transcriptie, taaldetectie, " +
                    "bevestigde vertaling of audio-synchronisatie.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it.take(100)
                        shownIndex = -1
                    },
                    label = { Text("Zoek lokaal in dit bestand") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("${found.size} van ${cues.size} ondertitelregels",
                    style = MaterialTheme.typography.labelMedium)
                HorizontalDivider()
                if (focused == null) {
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        itemsIndexed(found) { index, cue ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { shownIndex = index }) {
                                    Text("${index + 1}. ${cue.startLabel} → ${cue.endLabel} · Groot lezen")
                                }
                                Text(cue.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrRtl),
                                    maxLines = 3)
                                HorizontalDivider()
                            }
                        }
                    }
                } else {
                    Text("${focused.startLabel} → ${focused.endLabel}",
                        style = MaterialTheme.typography.labelLarge)
                    Text(
                        focused.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl),
                        fontSize = if (largeText) 32.sp else 23.sp,
                        lineHeight = if (largeText) 42.sp else 31.sp,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = shownIndex > 0,
                            onClick = { shownIndex-- }) { Text("← Vorige") }
                        TextButton(onClick = { largeText = !largeText }) {
                            Text(if (largeText) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(enabled = shownIndex < found.lastIndex,
                            onClick = { shownIndex++ }) { Text("Volgende →") }
                        TextButton(onClick = { shownIndex = -1 }) { Text("Lijst") }
                    }
                }
                Text("Geen internet-, model-, microfoon-, bewaar- of deelactie. " +
                    "De gekozen bestandsprovider kan wél buiten Tuddy synchroniseren. " +
                    "Deze preview verdwijnt bij sluiten of app-achtergrond.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
