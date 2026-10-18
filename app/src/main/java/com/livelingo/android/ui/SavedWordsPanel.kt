package com.livelingo.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateVocabularyDeck
import java.io.ByteArrayOutputStream

/**
 * Offline review/delete screen; only curated language+word keys ever persist.
 * A full conversation is never loaded from or saved to device storage.
 */
@Composable
internal fun SavedWordsPanel(
    deck: PrivateVocabularyDeck,
    revision: Int,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val allWords = remember(revision) { deck.entries() }
    var search by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(emptySet<String>()) }
    val searchTerm = search.trim()
    val words = if (searchTerm.isBlank()) allWords else allWords.filter {
        it.word.contains(searchTerm, ignoreCase = true) ||
            it.language.contains(searchTerm, ignoreCase = true)
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var practiceOpen by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingImport by remember { mutableStateOf<List<String>?>(null) }
    var transferStatus by remember { mutableStateOf("") }
    val context = LocalContext.current
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val snapshot = pendingExport
        pendingExport = null
        if (uri != null && snapshot != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(snapshot)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            transferStatus = if (saved) "Woordenback-up opgeslagen op jouw gekozen locatie."
                else "Opslaan mislukt; alle lokaal bewaarde woorden blijven behouden."
        }
    }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImport = null
        if (uri != null) {
            val candidate = runCatching {
                val output = ByteArrayOutputStream()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val chunk = ByteArray(4096)
                    while (true) {
                        val count = input.read(chunk)
                        if (count < 0) break
                        if (count == 0 || output.size() + count > 16_384) {
                            throw IllegalArgumentException("Invalid backup size")
                        }
                        output.write(chunk, 0, count)
                    }
                } ?: throw IllegalArgumentException("Unreadable provider")
                deck.previewImport(output.toByteArray())
            }.getOrNull()
            if (candidate == null) {
                transferStatus = "Bestand geweigerd: onbekend formaat, te groot of niet-erkende woorden. " +
                    "Je bestaande woorden zijn behouden."
            } else {
                pendingImport = candidate
                transferStatus = "Bestand gecontroleerd. Vervangen gebeurt pas na jouw bevestiging."
            }
        }
    }

    if (practiceOpen) {
        WordPracticePanel(
            savedWords = allWords,
            onDismiss = { practiceOpen = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn opgeslagen woorden") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Alleen zelf bewaarde woorden uit de kleine gecontroleerde woordenlijst. " +
                        "Je gesprekken worden niet opgeslagen en niets gaat naar een server.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    enabled = allWords.isNotEmpty(),
                    onClick = { practiceOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Oefen opgeslagen woorden") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = allWords.isNotEmpty(),
                        onClick = {
                            val snapshot = deck.exportSnapshot()
                            if (snapshot == null) {
                                transferStatus = "Geen geldige opgeslagen woorden om te bewaren."
                            } else {
                                pendingExport = snapshot
                                createBackup.launch("tuddy-mijn-woorden.json")
                            }
                        }
                    ) { Text("Bewaar back-up…") }
                    OutlinedButton(onClick = {
                        pendingImport = null
                        openBackup.launch(arrayOf("application/json", "text/plain"))
                    }) { Text("Herstel back-up…") }
                }
                Text(
                    "Alleen gecontroleerde woorden én taalcodes, nooit gesprekstekst of definities. " +
                        "Een gekozen cloudbestandsprovider kan de back-up extern synchroniseren.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (transferStatus.isNotBlank()) Text(
                    transferStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(40) },
                    label = { Text("Zoek opgeslagen woord of taal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("${words.size} van ${allWords.size} woorden · betekenissen verborgen om te oefenen",
                    style = MaterialTheme.typography.bodySmall)
                if (words.isEmpty()) {
                    Text(if (allWords.isEmpty())
                        "Nog geen woorden. Tik op een woord in een ondertitel en kies Bewaar woord."
                        else "Geen opgeslagen woorden met deze zoekterm.")
                }
                words.forEach { word ->
                    HorizontalDivider()
                    val key = "${word.language}|${word.word}"
                    Column {
                        Text("${word.entry.illustration} ${word.word} · ${word.language}",
                            style = MaterialTheme.typography.titleSmall)
                        if (key in revealed) {
                            Text(word.entry.definition, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = {
                                revealed = if (key in revealed) revealed - key else revealed + key
                            }) { Text(if (key in revealed) "Verberg betekenis" else "Toon betekenis") }
                            TextButton(onClick = {
                                if (deck.remove(word.language, word.word)) {
                                    revealed = revealed - key
                                    onChanged()
                                }
                            }) { Text("Verwijder") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        },
        dismissButton = {
            if (allWords.isNotEmpty()) {
                TextButton(onClick = { confirmDelete = true }) { Text("Wis alle woorden") }
            }
        }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Alle opgeslagen woorden wissen?") },
            text = { Text("Dit wist de lokale woordenlijst definitief, niet je tijdelijke gesprek.") },
            confirmButton = {
                TextButton(onClick = {
                    if (deck.clear()) onChanged()
                    confirmDelete = false
                }) { Text("Wis woorden") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") }
            }
        )
    }

    val importCandidate = pendingImport
    if (importCandidate != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Opgeslagen woorden vervangen?") },
            text = {
                Text("Deze back-up bevat ${importCandidate.size} herkende woorden uit Tuddy's " +
                    "kleine lokale woordenlijst. Hiermee vervang je je huidige woorden. " +
                    "Gesprekken of eigen zinnen worden niet geïmporteerd.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val restored = deck.replaceImported(importCandidate)
                    if (restored) onChanged()
                    transferStatus = if (restored) "Woordenlijst lokaal hersteld."
                        else "Herstel mislukt; de oorspronkelijke woorden blijven behouden."
                    pendingImport = null
                }) { Text("Vervang woorden") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImport = null
                    transferStatus = "Herstel geannuleerd; niets vervangen."
                }) { Text("Annuleren") }
            }
        )
    }
}
