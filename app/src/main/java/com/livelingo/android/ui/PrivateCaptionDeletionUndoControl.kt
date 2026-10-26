package com.livelingo.android.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * One accessible, explicit action to restore a single short-lived deleted
 * RAM-only caption. The owning Android screen alone owns the snapshot,
 * generation and deadline checks. Never retains conversation text here.
 */
@Composable
internal fun PrivateCaptionDeletionUndoControl(
    canRestore: Boolean,
    onRestore: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = canRestore,
        onClick = onRestore
    ) {
        Text("↶ Verwijderde ondertitel terugzetten (30 sec)")
    }
}
