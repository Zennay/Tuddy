package com.livelingo.android.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.livelingo.android.InstalledLocalVoice
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.LocalSpeechVoicePreferences
import java.util.Locale

/**
 * Explicit device-only voice selection. Only the names of already-installed
 * non-network Android voices are stored; no transcript, speech audio or
 * automatic voice download is created. Cancelling/backgrounding closes TTS.
 */
@Composable
internal fun InstalledLocalVoicePickerDialog(
    languageTag: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scanner = remember(context, languageTag) { LocalCaptionSpeaker(context) }
    var voices by remember(languageTag) { mutableStateOf(emptyList<InstalledLocalVoice>()) }
    var checking by remember(languageTag) { mutableStateOf(true) }
    var notice by remember(languageTag) { mutableStateOf("") }
    var savedName by remember(languageTag) {
        mutableStateOf(LocalSpeechVoicePreferences.selectedVoiceName(context, languageTag))
    }

    fun scan() {
        scanner.stop()
        checking = true
        voices = emptyList()
        notice = "Geïnstalleerde Android-stemmen controleren…"
        scanner.discoverInstalledVoices(languageTag) { found, message ->
            checking = false
            voices = found
            notice = message
            savedName = LocalSpeechVoicePreferences.selectedVoiceName(context, languageTag)
        }
    }

    LaunchedEffect(scanner) { scan() }
    DisposableEffect(scanner, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                scanner.stop()
                checking = false
                notice = "Stemcontrole gestopt op de achtergrond. Tik op Ververs."
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            scanner.close()
        }
    }

    val preview = when (Locale.forLanguageTag(languageTag).language) {
        "nl" -> "Dit is een voorbeeld van een lokale stem."
        "en" -> "This is an example of a local voice."
        "de" -> "Dies ist eine lokale Stimme."
        "fr" -> "Voici un exemple de voix locale."
        "es" -> "Este es un ejemplo de voz local."
        else -> "Tuddy."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lokale Android-stem · ${languageTag.uppercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Alleen stemmen die Android als geïnstalleerd en niet-netwerkafhankelijk " +
                        "meldt. Een stem kiezen installeert niets en maakt geen " +
                        "spraakherkenning of vertaling mogelijk.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(notice, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { scan() }, enabled = !checking) {
                    Text(if (checking) "Bezig…" else "Ververs geïnstalleerde stemmen")
                }
                if (!checking && voices.isEmpty()) {
                    Text("Geen geschikte stem. Kijk in de Android-tekst-naar-spraakinstellingen.")
                }
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(voices, key = { it.name }) { voice ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(voice.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${voice.languageTag} · Android kwaliteit ${voice.quality}" +
                                    if (savedName == voice.name) " · gekozen" else "",
                                style = MaterialTheme.typography.labelSmall
                            )
                            TextButton(
                                enabled = savedName != voice.name,
                                onClick = {
                                    scanner.stop()
                                    val ok = LocalSpeechVoicePreferences.selectVoice(
                                        context, languageTag, voice.name
                                    )
                                    if (ok) savedName = voice.name
                                    notice = if (ok) "Stem lokaal gekozen. Gebruik Voorbeeld om te luisteren."
                                    else "Stemkeuze kon niet worden bewaard."
                                }
                            ) { Text(if (savedName == voice.name) "Gekozen ✓" else "Kies stem") }
                        }
                    }
                }
                if (savedName != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !checking && voices.any { it.name == savedName },
                            onClick = {
                                scanner.speak(preview, languageTag, LocalSpeechPace.NORMAL) {
                                    notice = it
                                }
                            }
                        ) { Text("▶ Voorbeeld") }
                        TextButton(onClick = {
                            scanner.stop()
                            if (LocalSpeechVoicePreferences.clearVoice(context, languageTag)) {
                                savedName = null
                                notice = "Automatische lokale stemkeuze hersteld."
                            } else notice = "Stemkeuze kon niet worden gewist."
                        }) { Text("Herstel automatisch") }
                    }
                }
                Text(
                    "Androids stemmetadata zijn geen onafhankelijke vliegtuigmodustest. " +
                        "Voorbeelden spelen nooit online terug vanuit Tuddy.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scanner.close()
                onDismiss()
            }) { Text("Gereed") }
        }
    )
}
