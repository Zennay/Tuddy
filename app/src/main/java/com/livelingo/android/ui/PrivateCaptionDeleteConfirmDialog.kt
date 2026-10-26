package com.livelingo.android.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirmation UI only. Current-row equality and all RAM-only deletion state
 * belong to the caller, not to this view or a persistent repository.
 */
@Composable
internal fun PrivateCaptionDeleteConfirmDialog(
    stillSelected: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eén ondertitel wissen?") },
        text = {
            Text("Alleen deze regel verdwijnt uit het tijdelijke gesprek. " +
                "Je kunt haar 30 seconden lokaal herstellen zolang Tuddy openblijft. " +
                "Lopende vertalingen worden niet opnieuw gestart. " +
                "Herstel verdwijnt bij verlaten of volledig wissen. " +
                "Externe kopieën blijven buiten Tuddy bestaan.")
        },
        confirmButton = {
            TextButton(enabled = stillSelected, onClick = onConfirm) {
                Text("Wis deze regel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Behouden") }
        }
    )
}
