package com.livelingo.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.livelingo.android.LocalSpeechPace

/**
 * Explicit private playback choices, without guessing which Android TTS
 * voices are installed. Voice admission remains in LocalCaptionSpeaker.
 */
@Composable
internal fun LocalCaptionPlaybackMenu(
    hasCompletedTranslation: Boolean,
    bilingualAvailable: Boolean = true,
    enabled: Boolean,
    headphonesOnly: Boolean,
    onHeadphonesOnlyChange: (Boolean) -> Unit,
    respectQuietMode: Boolean,
    onRespectQuietModeChange: (Boolean) -> Unit,
    onPlayOriginal: (LocalSpeechPace) -> Unit,
    onPlayTranslation: (LocalSpeechPace) -> Unit,
    onPlayBilingual: (LocalSpeechPace) -> Unit,
    onStop: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Text("🔊 Lokaal voorlezen · kies tempo")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Headset pauze/stop beëindigt lokale spraak · geen automatisch hervatten") },
                onClick = {},
                enabled = false
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (headphonesOnly) "✓ Alleen met hoofdtelefoon · uitschakelen"
                        else "○ Alleen met hoofdtelefoon · inschakelen"
                    )
                },
                onClick = {
                    expanded = false
                    onHeadphonesOnlyChange(!headphonesOnly)
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (respectQuietMode)
                            "✓ Stil/trillen respecteren · uitschakelen"
                        else "○ Stil/trillen respecteren · inschakelen"
                    )
                },
                onClick = {
                    expanded = false
                    onRespectQuietModeChange(!respectQuietMode)
                }
            )
            DropdownMenuItem(
                text = { Text("Origineel · normaal") },
                onClick = {
                    expanded = false
                    onPlayOriginal(LocalSpeechPace.NORMAL)
                }
            )
            DropdownMenuItem(
                text = { Text("Origineel · langzaam") },
                onClick = {
                    expanded = false
                    onPlayOriginal(LocalSpeechPace.SLOW)
                }
            )
            if (hasCompletedTranslation) {
                DropdownMenuItem(
                    text = { Text("Vertaling · normaal") },
                    onClick = {
                        expanded = false
                        onPlayTranslation(LocalSpeechPace.NORMAL)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Vertaling · langzaam") },
                    onClick = {
                        expanded = false
                        onPlayTranslation(LocalSpeechPace.SLOW)
                    }
                )
                if (bilingualAvailable) {
                    DropdownMenuItem(
                        text = { Text("Origineel → vertaling · normaal") },
                        onClick = {
                            expanded = false
                            onPlayBilingual(LocalSpeechPace.NORMAL)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Origineel → vertaling · langzaam") },
                        onClick = {
                            expanded = false
                            onPlayBilingual(LocalSpeechPace.SLOW)
                        }
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("■ Stop voorlezen") },
                onClick = {
                    expanded = false
                    onStop()
                }
            )
        }
    }
}
