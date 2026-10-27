package com.livelingo.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Accessible, opt-in OCR readback. The reviewed text never leaves the owning
 * screen; only the already validated, short chunks reach LocalCaptionSpeaker.
 */
@Composable
internal fun PrivateImageTextReadbackPanel(
    canRead: Boolean,
    playing: Boolean,
    languageTag: String,
    completed: Int,
    total: Int,
    message: String,
    onLanguageChange: (String) -> Unit,
    onPlay: (LocalSpeechPace) -> Unit,
    onStop: () -> Unit
) {
    var languageOpen by remember { mutableStateOf(false) }
    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Lees de gecontroleerde afbeeldingstekst voor",
            style = MaterialTheme.typography.titleSmall)
        Text(
            "Kies zelf de gesproken taal en tik op een tempo. " +
                "Tuddy leest alleen jouw gecorrigeerde tekst met een reeds " +
                "geïnstalleerde, door Android als lokaal gemelde stem. " +
                "Maximaal 2.400 tekens per keer, zonder automatische vertaling.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Box {
                TextButton(
                    enabled = !playing,
                    onClick = { languageOpen = true }
                ) { Text("Uitspraaktaal: ${languageTag.uppercase()} ▾") }
                DropdownMenu(
                    expanded = languageOpen,
                    onDismissRequest = { languageOpen = false }
                ) {
                    for ((code, label) in listOf(
                        "nl" to "Nederlands",
                        "en" to "Engels",
                        "de" to "Duits",
                        "fr" to "Frans",
                        "es" to "Spaans"
                    )) {
                        DropdownMenuItem(
                            text = { Text("${label} (${code.uppercase()})") },
                            onClick = {
                                languageOpen = false
                                onLanguageChange(code)
                            }
                        )
                    }
                }
            }
        }
        OutlinedButton(
            enabled = canRead && !playing,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onPlay(LocalSpeechPace.NORMAL) }
        ) { Text("▶ Lees na · normaal") }
        OutlinedButton(
            enabled = canRead && !playing,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onPlay(LocalSpeechPace.SLOW) }
        ) { Text("▶ Lees na · langzaam") }
        OutlinedButton(
            enabled = playing,
            modifier = Modifier.fillMaxWidth(),
            onClick = onStop
        ) { Text("■ Stop lokaal voorlezen") }
        if (playing && total > 0) {
            Text(
                "Voorleesvoortgang: ${completed.coerceAtMost(total)} van $total fragmenten",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Wijziging van tekst of blokselectie en verlaten van het scherm " +
                "stoppen audio meteen. Android-audiofocus en headsetbewaking " +
                "gelden ook hier; geen garantie op een volledig stille luidspreker.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
