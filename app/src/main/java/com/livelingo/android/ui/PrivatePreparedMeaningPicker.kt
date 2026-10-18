package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseDeck

/**
 * Deliberately copies only the user's *own NL/EN meaning* to an UNSENT editor
 * draft. Never presents Kurdish/Pashto source text as supported ML Kit input.
 * Does not persist any new content, translate, send, or access a microphone.
 */
@Composable
internal fun PrivatePreparedMeaningPicker(
    deck: PrivatePhraseDeck,
    hasUnsentDraft: Boolean,
    onChosen: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val stored = remember { deck.entries() }
    var search by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("all") }
    var shownCount by remember { mutableIntStateOf(20) }
    var pendingReplace by remember { mutableStateOf<PersonalPhrase?>(null) }
    val match = stored.orEmpty().filter { phrase ->
        (language == "all" || phrase.language == language) &&
            (search.isBlank() || phrase.phrase.contains(search.trim(), ignoreCase = true) ||
                phrase.ownMeaning.contains(search.trim(), ignoreCase = true))
    }
    val selected = pendingReplace
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { pendingReplace = null },
            title = { Text("Onverstuurd concept vervangen?") },
            text = { Text(
                "Je huidige getypte concept is nog niet verstuurd of opgeslagen. " +
                    "Wil je dat concept bewust vervangen met jouw eigen " +
                    "${if (selected.meaningLanguage == "nl") "Nederlandse" else "Engelse"} " +
                    "betekenis? De Kurmanji/Sorani/Pasjtoe-zin wordt NIET " +
                    "door het tekstmodel vertaald en niets wordt automatisch verzonden."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    pendingReplace = null
                    onChosen(selected.ownMeaning, selected.meaningLanguage)
                }) { Text("Vervang alleen concept") }
            },
            dismissButton = {
                TextButton(onClick = { pendingReplace = null }) {
                    Text("Bewaar mijn huidige concept")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn voorbereide tekst · privé") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Kies uitsluitend een eigen Nederlandse/Engelse betekenis " +
                        "als nieuw, ONVERSTUURD tekstconcept. Eigen betekenissen " +
                        "zijn niet geverifieerd. Dit biedt geen automatische " +
                        "Kurmanji-, Sorani- of Pasjtoe-vertaling.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (stored == null) {
                    Text(
                        "De lokale zinnenlijst kon niet veilig worden gelezen. " +
                            "Het bestaande concept blijft behouden.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (stored.isEmpty()) {
                    Text("Nog geen zelfgeschreven zinnen. Voeg die eerst toe in Mijn eigen zinnen.")
                } else {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it.take(60); shownCount = 20 },
                        label = { Text("Zoek mijn eigen zin of betekenis") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    listOf(
                        "all" to "Alle talen",
                        "kmr" to "Kurmanji",
                        "ckb" to "Sorani",
                        "ps" to "Pasjtoe"
                    ).forEach { (code, label) ->
                        TextButton(onClick = { language = code; shownCount = 20 }) {
                            Text((if (language == code) "✓ " else "") + label)
                        }
                    }
                    Text("${match.size} zelfgeschreven zinnen gevonden · " +
                        "maximaal ${minOf(match.size, shownCount)} zichtbaar",
                        style = MaterialTheme.typography.labelSmall)
                    match.take(shownCount).forEach { item ->
                        Text(
                            item.phrase,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (item.language == "ps" ||
                                    item.language == "ckb") TextDirection.Rtl
                                else TextDirection.Ltr
                            )
                        )
                        Text("Mijn eigen betekenis (${item.meaningLanguage.uppercase()}): " +
                            item.ownMeaning,
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            if (hasUnsentDraft) pendingReplace = item else {
                                onChosen(item.ownMeaning, item.meaningLanguage)
                            }
                        }) { Text("Gebruik alleen mijn eigen betekenis als concept") }
                    }
                    if (shownCount < match.size) {
                        TextButton(onClick = { shownCount += 20 }) {
                            Text("Toon volgende eigen zinnen")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Terug naar tekstgesprek") } }
    )
}
