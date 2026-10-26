package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalSpeechVoiceEntry
import com.livelingo.android.LocalSpeechVoiceInventory
import com.livelingo.android.LocalSpeechVoicePreferences
import com.livelingo.android.LocalSpeechVoicePreview
import java.util.Locale

/**
 * Read-only device-wide catalog. Only explicitly chosen installed voices are
 * persisted by their name per language, and only fixed built-in sample audio
 * can be played. No private captions/recordings or network voice downloads.
 */
@Composable
internal fun InstalledDeviceVoiceCatalogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val inventory = remember(context) { LocalSpeechVoiceInventory(context) }
    val preview = remember(context) { LocalSpeechVoicePreview(context) }
    var voices by remember { mutableStateOf(emptyList<LocalSpeechVoiceEntry>()) }
    var query by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("Androids geïnstalleerde lokale stemmen worden gelezen.") }
    var epoch by remember { mutableIntStateOf(0) }
    var choiceRevision by remember { mutableIntStateOf(0) }

    fun scan() {
        val ticket = ++epoch
        preview.close()
        inventory.close()
        previewing = false
        scanning = true
        voices = emptyList()
        notice = "Alleen op het toestel geïnstalleerde, niet-netwerkafhankelijke stemmen controleren…"
        inventory.inspect { result ->
            if (ticket == epoch) {
                scanning = false
                voices = result.getOrNull().orEmpty()
                notice = result.fold(
                    onSuccess = { found ->
                        if (found.isEmpty()) "Android meldt geen geïnstalleerde offline stemmen."
                        else "${found.size} lokale Android-stemmen gevonden. " +
                            "Taalbeschikbaarheid is geen bewijs van offline ASR of vertaling."
                    },
                    onFailure = { error ->
                        error.message ?: "De lokale stemcontrole is mislukt."
                    }
                )
            }
        }
    }

    LaunchedEffect(inventory) { scan() }
    DisposableEffect(inventory, preview, lifecycle) {
        val listener = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                epoch++
                inventory.close()
                preview.close()
                scanning = false
                previewing = false
                notice = "Stemcontrole gestopt in de achtergrond. Tik op Ververs."
            }
        }
        lifecycle.lifecycle.addObserver(listener)
        onDispose {
            epoch++
            lifecycle.lifecycle.removeObserver(listener)
            inventory.close()
            preview.close()
        }
    }

    val search = query.trim().lowercase(Locale.ROOT)
    val shown = if (search.isBlank()) voices else voices.filter {
        it.languageTag.lowercase(Locale.ROOT).contains(search) ||
            it.displayName.lowercase(Locale.ROOT).contains(search) ||
            it.voiceName.lowercase(Locale.ROOT).contains(search)
    }

    AlertDialog(
        onDismissRequest = {
            epoch++
            inventory.close()
            preview.close()
            onDismiss()
        },
        title = { Text("Alle lokale Android-stemmen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Zoek eerst welke talen Android op dit toestel kan voorlezen. " +
                        "Een gekozen stem geldt ook in het live gesprek en de " +
                        "snelle tekstvertaler. Geen modeldownload, cloudstem of opname.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(notice, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(48) },
                    label = { Text("Zoek taal, code of stem") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { scan() }, enabled = !scanning) {
                    Text(if (scanning) "Stemmen controleren…" else "Ververs vanaf dit toestel")
                }
                if (!scanning && shown.isEmpty()) {
                    Text("Geen passende geïnstalleerde stem. Controleer Androids spraakinstellingen.")
                }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(shown, key = { it.voiceName + ":" + it.languageTag }) { entry ->
                        // Preferences deliberately use the language, not an
                        // invented claim that a dialect-specific ASR model exists.
                        val selected = LocalSpeechVoicePreferences.selectedVoiceName(
                            context, entry.languageTag
                        ) == entry.voiceName
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${entry.displayName} · ${entry.languageTag}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${entry.voiceName} · Android-kwaliteit ${entry.quality}" +
                                    if (selected) " · jouw stem" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    enabled = !scanning && !previewing && !selected,
                                    onClick = {
                                        preview.close()
                                        val saved = LocalSpeechVoicePreferences.selectVoice(
                                            context, entry.languageTag, entry.voiceName
                                        )
                                        if (saved) choiceRevision++
                                        notice = if (saved)
                                            "Lokale stem gekozen voor ${entry.languageTag}. " +
                                                "Dit zegt niets over spraakherkenning."
                                        else "Android kon de lokale stemvoorkeur niet bewaren."
                                    }
                                ) { Text(if (selected) "Gekozen ✓" else "Kies") }
                                TextButton(
                                    enabled = !scanning && !previewing,
                                    onClick = {
                                        val ticket = ++epoch
                                        previewing = true
                                        notice = "Korte ingebouwde voorbeeldzin wordt lokaal afgespeeld…"
                                        preview.preview(entry) { result ->
                                            if (ticket == epoch) {
                                                previewing = false
                                                notice = result.fold(
                                                    onSuccess = {
                                                        "Voorbeeld afgerond met de gekozen geïnstalleerde stem."
                                                    },
                                                    onFailure = {
                                                        it.message ?: "Dit lokale stemvoorbeeld is mislukt."
                                                    }
                                                )
                                            }
                                        }
                                    }
                                ) { Text("▶ Voorbeeld") }
                                if (selected) {
                                    TextButton(onClick = {
                                        preview.close()
                                        val cleared = LocalSpeechVoicePreferences.clearVoice(
                                            context, entry.languageTag
                                        )
                                        if (cleared) choiceRevision++
                                        notice = if (cleared)
                                            "Automatische stemkeuze voor ${entry.languageTag} hersteld."
                                        else "Stemvoorkeur kon niet worden gewist."
                                    }) { Text("Herstel") }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
                Text(
                    "Stemmen zijn door Android als lokaal gemeld; dit is geen fysieke " +
                        "vliegtuigmodusproef en geen bewijs van Koerdische/Pasjtoe ASR+MT.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                epoch++
                inventory.close()
                preview.close()
                onDismiss()
            }) { Text("Sluiten") }
        }
    )
}
