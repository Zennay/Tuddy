package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Explicit Android-permission recovery. No permission request or system
 * settings navigation runs during composition, backgrounding, or a callback.
 * Every control requires a fresh user tap and is shared by the live and
 * fullscreen reader surfaces.
 */
@Composable
internal fun MicrophonePermissionRecoveryPanel(
    permissionGranted: Boolean,
    canRequestAgain: Boolean,
    message: String,
    onRequestAgain: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            if (permissionGranted) "Microfoontoegang beschikbaar"
            else "Microfoontoegang geblokkeerd",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            message.ifBlank {
                if (permissionGranted)
                    "Android geeft toegang. Tik opnieuw op Luister wanneer je zelf wilt spreken."
                else
                    "Tuddy luistert niet zonder toestemming. Tekstvertaling blijft beschikbaar."
            },
            style = MaterialTheme.typography.bodySmall
        )
        if (!permissionGranted) {
            if (canRequestAgain) {
                TextButton(onClick = onRequestAgain) {
                    Text("Vraag microfoontoegang opnieuw")
                }
            }
            OutlinedButton(onClick = onOpenSystemSettings) {
                Text("Open Android-appinstellingen")
            }
            Text(
                "Je verandert de toestemming zelf in Android. Tuddy opent " +
                    "bij terugkeer nooit automatisch de microfoon.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
