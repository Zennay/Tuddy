package com.livelingo.android.ui

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

/**
 * Permission-free, consented Android accessibility announcement of one
 * completed private caption. This is deliberately NEVER an automatic live
 * region: enabled accessibility services may be third-party and can process
 * the spoken text. The user confirms each exact unchanged line before the
 * Android View receives it. No voice synthesis, microphone, cloud job or
 * transcript storage is started by Tuddy.
 */
@Composable
internal fun ExplicitAccessibilityCaptionAction(
    title: String,
    original: String,
    translation: String?,
    sourceCode: String,
    targetCode: String,
    enabled: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current
    // Only a short, exact text snapshot exists while a confirmation is open.
    var pending by remember { mutableStateOf<AnnouncementSnapshot?>(null) }
    var notice by remember { mutableStateOf("") }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                pending = null
                notice = ""
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            pending = null
        }
    }

    fun request(text: String, tag: String, label: String) {
        notice = ""
        if (!enabled || !lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            notice = "Open Tuddy op de voorgrond en stop eerst met luisteren."
            return
        }
        if (text.isBlank() || text.length > MAX_ANNOUNCEMENT_CHARS ||
            text.any { it == '\u0000' || (it.isISOControl() && it != '\n') }) {
            notice = "Kies een voltooide tekst van maximaal $MAX_ANNOUNCEMENT_CHARS tekens."
            return
        }
        pending = AnnouncementSnapshot(text, tag, label)
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                enabled = enabled && original.isNotBlank(),
                onClick = { request(original, sourceCode, "Originele tekst") }
            ) { Text("Lees origineel via TalkBack…") }
            TextButton(
                enabled = enabled && translation != null && translation.isNotBlank(),
                onClick = {
                    translation?.let { request(it, targetCode, "Vertaling") }
                }
            ) { Text("Lees vertaling via TalkBack…") }
        }
        Text(
            "Alleen na bevestiging: Android kan de zin doorgeven aan een " +
                "ingeschakelde toegankelijkheidsdienst, ook van derden. " +
                "Geen automatische voorlezing en geen opslag door Tuddy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (notice.isNotBlank()) {
            Text(notice, style = MaterialTheme.typography.bodySmall)
        }
    }

    pending?.let { candidate ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Privétekst aan Android-toegankelijkheid geven?") },
            text = {
                Text(
                    "${candidate.label} (${candidate.languageTag.uppercase(Locale.ROOT)}) " +
                        "wordt één keer aangekondigd aan de actieve Android-" +
                        "toegankelijkheidsdienst. Een externe dienst kan de tekst " +
                        "zelf verwerken of bewaren. Tuddy doet dat niet. " +
                        "Zonder actieve schermlezer gebeurt er niets."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    val currentText = if (candidate.label == "Originele tekst")
                        original else translation
                    val currentCode = if (candidate.label == "Originele tekst")
                        sourceCode else targetCode
                    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                        as? AccessibilityManager
                    notice = when {
                        !enabled ||
                            !lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                            !view.isAttachedToWindow ->
                            "Scherm niet langer actief; privétekst niet doorgegeven."
                        currentText != candidate.text || currentCode != candidate.languageTag ->
                            "De regel is inmiddels gewijzigd; bevestig de nieuwe tekst opnieuw."
                        manager?.isEnabled != true || !manager.isTouchExplorationEnabled ->
                            "Android heeft geen actieve schermlezer met aanraakverkenning. " +
                                "Schakel TalkBack via Android-instellingen in als je dit wilt."
                        else -> {
                            val ok = runCatching {
                                view.announceForAccessibility(
                                    "${candidate.label}, taal ${candidate.languageTag}: ${candidate.text}"
                                )
                            }.isSuccess
                            if (ok) "Eenmalige aankondiging bij Android aangevraagd; " +
                                "het daadwerkelijk voorlezen is niet bevestigd."
                            else "Android kon de aankondiging niet ontvangen."
                        }
                    }
                }) { Text("Eenmalig aankondigen") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Niet delen") }
            }
        )
    }
}

private data class AnnouncementSnapshot(
    val text: String,
    val languageTag: String,
    val label: String
)

private const val MAX_ANNOUNCEMENT_CHARS = 600
