package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalBackReviewReceipt
import com.livelingo.android.LocalBackReviewStage
import com.livelingo.android.PrivateOfflineBackTranslationReview

/**
 * Explicit comparison without overwriting the conversation or exporting
 * private captions. All state is in this mounted Compose dialog only.
 */
@Composable
internal fun PrivateOfflineBackTranslationDialog(
    original: String,
    translation: String,
    sourceCode: String,
    targetCode: String,
    onDismiss: () -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current
    val controller = remember { PrivateOfflineBackTranslationReview() }
    var result by remember {
        mutableStateOf<LocalBackReviewReceipt?>(null)
    }
    DisposableEffect(controller, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                controller.close()
                onDismiss()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            controller.close()
        }
    }
    val running = result?.stage in setOf(
        LocalBackReviewStage.CHECKING, LocalBackReviewStage.TRANSLATING
    )
    AlertDialog(
        onDismissRequest = {
            controller.close()
            onDismiss()
        },
        title = { Text("Privé terugvertaling vergelijken") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Alleen na jouw tik controleert Tuddy de al geïnstalleerde " +
                        "modellen voor $targetCode → $sourceCode. Er wordt niets " +
                        "gedownload, naar buiten verzonden of teruggeschreven " +
                        "naar het gesprek.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Oorspronkelijk · ${sourceCode.uppercase()}",
                    style = MaterialTheme.typography.labelLarge)
                Text(original, style = MaterialTheme.typography.bodyMedium)
                Text("Huidige vertaling · ${targetCode.uppercase()}",
                    style = MaterialTheme.typography.labelLarge)
                Text(translation, style = MaterialTheme.typography.bodyMedium)
                result?.let { state ->
                    Text(state.message, style = MaterialTheme.typography.bodySmall,
                        color = if (state.stage == LocalBackReviewStage.UNAVAILABLE)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.stage == LocalBackReviewStage.COMPLETED) {
                        Text("Terug naar ${sourceCode.uppercase()} · modeluitkomst",
                            style = MaterialTheme.typography.labelLarge)
                        Text(state.reverseText,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Let op: twee lokale vertalingen kunnen dezelfde fout " +
                        "bevatten. Dit is alleen een tweede formulering om zelf " +
                        "te beoordelen, geen onafhankelijke kwaliteitscontrole.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !running,
                onClick = {
                    controller.request(
                        original, translation, sourceCode, targetCode
                    ) { receipt -> result = receipt }
                }
            ) {
                Text(if (running) "Lokaal bezig…" else "Vergelijk lokaal")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                controller.close()
                onDismiss()
            }) { Text("Sluiten") }
        }
    )
}
