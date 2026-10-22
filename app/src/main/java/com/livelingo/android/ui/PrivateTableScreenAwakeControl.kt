package com.livelingo.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Optional display-only keep-awake for the current face-to-face dialog.
 * Uses the Dialog View, not the Activity's shared window flag used by
 * live microphone captioning. No wake lock, permission or saved preference.
 */
@Composable
internal fun PrivateTableScreenAwakeControl() {
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(false) }

    DisposableEffect(view, owner, enabled) {
        val previous = view.keepScreenOn
        val observer = LifecycleEventObserver { _, event ->
            if (enabled && event == Lifecycle.Event.ON_RESUME) {
                view.keepScreenOn = true
            } else if (enabled && (event == Lifecycle.Event.ON_PAUSE ||
                        event == Lifecycle.Event.ON_STOP)) {
                view.keepScreenOn = previous
            }
        }
        if (enabled) {
            owner.lifecycle.addObserver(observer)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                view.keepScreenOn = true
            }
        }
        onDispose {
            if (enabled) {
                owner.lifecycle.removeObserver(observer)
                view.keepScreenOn = previous
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { enabled = !enabled }
        ) {
            Text(if (enabled) "☼ Scherm aanhouden tijdens dit tafelgesprek ✓"
                else "☼ Scherm aanhouden tijdens dit tafelgesprek")
        }
        Text("Alleen zolang deze weergave zichtbaar is, na jouw keuze. " +
            "Kost extra batterij en maakt eigen zinnen langer leesbaar voor omstanders.",
            style = MaterialTheme.typography.labelSmall)
    }
}
