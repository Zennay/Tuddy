package com.livelingo.android.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * An optional, explicitly initiated reviewer handoff. Android receives only
 * the same bounded, human-entered text the operator can save through SAF.
 * No audio, transcript, attachments, automatic uploads or approval claims.
 */
@Composable
internal fun OfflineAcceptanceReviewShareAction(
    enabled: Boolean,
    prepare: () -> Pair<ByteArray, ByteArray>?,
    stillValid: (ByteArray) -> Boolean,
    onExternalStarted: () -> Unit,
    onExternalFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Pair<ByteArray, ByteArray>?>(null) }
    var notice by remember { mutableStateOf("") }
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onExternalFinished()
        notice = "Android-deelvenster gesloten. Tuddy weet niet of een ontvanger " +
            "het rapport daadwerkelijk heeft gekregen of beoordeeld."
    }

    // The device/report preview is sensitive even without spoken text.
    // It must not be visible when the operator switches away from Tuddy.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                preview = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OutlinedButton(
        enabled = enabled,
        onClick = {
            val prepared = prepare()
            if (prepared == null) {
                notice = "Deelvoorbeeld niet beschikbaar. Het proefconcept blijft intact."
            } else {
                val report = prepared.second
                if (report.size !in 1..16384 ||
                    !report.toString(Charsets.UTF_8).toByteArray(Charsets.UTF_8)
                        .contentEquals(report)) {
                    notice = "Deelvoorbeeld ongeldig. Het proefconcept blijft intact."
                } else {
                    preview = prepared.first.copyOf() to report.copyOf()
                    notice = ""
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Bekijk en deel rapport met beoordelaar…") }
    if (notice.isNotBlank()) {
        Text(notice, style = MaterialTheme.typography.bodySmall)
    }

    val selected = preview
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Rapport bewust extern delen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Controleer het volledige handmatige rapport hieronder. " +
                            "Het bevat toestel-/APK-kenmerken, model- en radiostatus, " +
                            "tellers en eigen beoordelingen; geen transcript of audio. " +
                            "De ontvanger kan dit bewaren of via internet versturen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Het openen van een andere app onderbreekt deze Tuddy-sessie. " +
                            "Delen is geen bewijs van radiostilte, geen goedkeuring door " +
                            "een reviewer en geen ondertekende release. " +
                            "Ook een onvolledig rapport blijft onvolledig.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        selected.second.toString(Charsets.UTF_8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!stillValid(selected.first)) {
                        preview = null
                        notice = "Delen geweigerd: het toestel-/APK-/radiorapport " +
                            "veranderde tijdens het lezen. Bekijk eerst een nieuwe momentopname."
                        return@TextButton
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Tuddy handmatige offline-toestelproef")
                        putExtra(Intent.EXTRA_TEXT, selected.second.toString(Charsets.UTF_8))
                    }
                    val chooser = Intent.createChooser(intent, "Kies zelf de ontvanger")
                    preview = null
                    onExternalStarted()
                    runCatching { shareLauncher.launch(chooser) }.onFailure {
                        onExternalFinished()
                        notice = "Android kon het deelvenster niet openen. " +
                            "Het lokale proefconcept is behouden."
                    }
                }) { Text("Ik begrijp het · kies ontvanger") }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }) { Text("Niet delen") }
            }
        )
    }
}
