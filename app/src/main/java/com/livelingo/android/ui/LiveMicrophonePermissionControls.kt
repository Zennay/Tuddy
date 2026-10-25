package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Extracted from the very large TuddyScreen Compose tree so Android's Kotlin
 * bytecode emitter stays below JVM's method-size limit. The component owns
 * only UI; all microphone checks, Android Settings navigation and lifecycle
 * remain guarded by the Activity's explicit user-action callbacks.
 */
@Composable
internal fun LiveMicrophonePermissionControls(
    listening: Boolean,
    offlineAvailable: Boolean,
    modelStorageBusy: Boolean,
    follow: Boolean,
    microphonePermissionRequested: Boolean,
    microphonePermissionGranted: Boolean,
    microphoneCanAskAgain: Boolean,
    microphoneRecoveryNotice: String,
    liveSpeechPhaseLabel: String,
    speechPhaseInterrupted: Boolean,
    onMicClick: () -> Unit,
    onClearConversation: () -> Unit,
    onToggleFollow: () -> Unit,
    onCoverConversation: () -> Unit,
    onRetryPermission: () -> Unit,
    onOpenAndroidSettings: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onMicClick,
            enabled = offlineAvailable && !modelStorageBusy
        ) { Text(if (listening) "■ Stop" else "🎙 Luister") }
        OutlinedButton(onClick = onClearConversation) { Text("Wis gesprek") }
        TextButton(onClick = onToggleFollow) {
            Text(if (follow) "Pauzeer volgen" else "Volg live")
        }
    }
    if (microphonePermissionRequested && !listening &&
        (!microphonePermissionGranted || microphoneRecoveryNotice.isNotBlank())) {
        MicrophonePermissionRecoveryPanel(
            permissionGranted = microphonePermissionGranted,
            canRequestAgain = microphoneCanAskAgain,
            message = microphoneRecoveryNotice,
            onRequestAgain = onRetryPermission,
            onOpenSystemSettings = onOpenAndroidSettings
        )
    }
    OutlinedButton(
        onClick = onCoverConversation,
        modifier = Modifier.fillMaxWidth()
    ) { Text("🔒 Gesprek afdekken · stop microfoon en audio") }
    Text(
        liveSpeechPhaseLabel,
        style = MaterialTheme.typography.bodySmall,
        color = if (speechPhaseInterrupted)
            MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
