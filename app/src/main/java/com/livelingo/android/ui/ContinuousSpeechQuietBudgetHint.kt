package com.livelingo.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.livelingo.android.ContinuousSpeechRestartPolicy

/**
 * Keep informational Compose UI outside the already large main caption screen.
 * The active microphone and its silence budget remain owned by MainActivity.
 */
@Composable
internal fun ContinuousSpeechQuietBudgetHint(showHint: Boolean) {
    if (!showHint) return
    Text(
        "Bij ${ContinuousSpeechRestartPolicy.MAX_QUIET_TURNS} stille " +
            "herkenningspogingen op rij stopt de microfoon vanzelf. " +
            "Echte uitgesproken zinnen zetten deze teller terug.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
