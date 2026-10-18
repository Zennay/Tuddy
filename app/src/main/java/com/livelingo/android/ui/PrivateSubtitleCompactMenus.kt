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

/**
 * Compact access to opt-in document operations. Never performs edits itself,
 * leaving authorization, format gates and undo integration with the parent.
 * The menu is scrollable by Material's standard dropdown on small screens.
 */
@Composable
internal fun PrivateSubtitleDocumentMenu(
    canInsert: Boolean,
    canRestore: Boolean,
    onFindReplace: () -> Unit,
    onWrapLines: () -> Unit,
    onShiftEarlier: () -> Unit,
    onShiftLater: () -> Unit,
    onRestore: () -> Unit,
    onInsert: () -> Unit,
    onClip: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text("Bewerk document ▾") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Zoek en vervang…") },
                onClick = { open = false; onFindReplace() })
            DropdownMenuItem(text = { Text("Lange regels afbreken…") },
                onClick = { open = false; onWrapLines() })
            DropdownMenuItem(text = { Text("Alle tijden 0,5 s eerder") },
                onClick = { open = false; onShiftEarlier() })
            DropdownMenuItem(text = { Text("Alle tijden 0,5 s later") },
                onClick = { open = false; onShiftLater() })
            DropdownMenuItem(text = { Text("+ Nieuwe ondertitel…") },
                enabled = canInsert,
                onClick = { open = false; onInsert() })
            DropdownMenuItem(text = { Text("✂ Maak een ondertitelclip…") },
                onClick = { open = false; onClip() })
            DropdownMenuItem(text = { Text("Herstel origineel") },
                enabled = canRestore,
                onClick = { open = false; onRestore() })
        }
    }
}

/** All single-cue editing remains available without crowding the reading pane. */
@Composable
internal fun PrivateSubtitleCueMenu(
    canSplit: Boolean,
    canMerge: Boolean,
    canDelete: Boolean,
    onCorrectText: () -> Unit,
    onExactTime: () -> Unit,
    onStartEarlier: () -> Unit,
    onStartLater: () -> Unit,
    onEndEarlier: () -> Unit,
    onEndLater: () -> Unit,
    onSplit: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text("✎ Wijzig deze regel ▾") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Tekst corrigeren…") },
                onClick = { open = false; onCorrectText() })
            DropdownMenuItem(text = { Text("⏱ Tijdcodes exact invoeren…") },
                onClick = { open = false; onExactTime() })
            DropdownMenuItem(text = { Text("Begin 0,5 s eerder") },
                onClick = { open = false; onStartEarlier() })
            DropdownMenuItem(text = { Text("Begin 0,5 s later") },
                onClick = { open = false; onStartLater() })
            DropdownMenuItem(text = { Text("Einde 0,5 s eerder") },
                onClick = { open = false; onEndEarlier() })
            DropdownMenuItem(text = { Text("Einde 0,5 s later") },
                onClick = { open = false; onEndLater() })
            DropdownMenuItem(text = { Text("Splits deze regel…") },
                enabled = canSplit,
                onClick = { open = false; onSplit() })
            DropdownMenuItem(text = { Text("Combineer volgende…") },
                enabled = canMerge,
                onClick = { open = false; onMerge() })
            DropdownMenuItem(text = { Text("Verwijder deze regel…") },
                enabled = canDelete,
                onClick = { open = false; onDelete() })
        }
    }
}
