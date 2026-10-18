package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseTopics

/** Theme management for human-authored notes, never model-derived categories. */
@Composable
internal fun PrivatePhraseTopicsDialog(
    storage: PrivatePhraseTopics,
    revision: Int,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val topics = remember(revision) { storage.topics() }
    var editTarget by remember { mutableStateOf<String?>(null) }
    var proposed by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }

    if (editTarget != null) {
        val creating = editTarget == "__new__"
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(if (creating) "Nieuw privéthema" else "Thema hernoemen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kies zelf een korte naam. Geen automatische indeling, " +
                        "vertaling of synchronisatie.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = proposed,
                        onValueChange = { proposed = it.take(30) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Themanaam (max. 30 tekens)") }
                    )
                    if (notice.isNotBlank()) Text(notice,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val done = if (creating) storage.add(proposed) else
                        storage.rename(editTarget!!, proposed)
                    if (done) {
                        onChanged()
                        editTarget = null
                        proposed = ""
                        notice = ""
                    } else notice = "Naam geweigerd: al in gebruik, ongeldig of maximaal 12 thema’s."
                }) { Text(if (creating) "Maak thema" else "Bewaar naam") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null; notice = "" }) {
                    Text("Annuleren")
                }
            }
        )
        return
    }

    val deleting = pendingDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Thema ‘$deleting’ verwijderen?") },
            text = { Text("Dit verwijdert uitsluitend het thema en de indeling. " +
                "Alle eigen zinnen, betekenissen, favorieten en herhaaldatums blijven behouden.") },
            confirmButton = {
                TextButton(onClick = {
                    val removed = storage.delete(deleting)
                    if (removed) onChanged()
                    notice = if (removed) "" else "Verwijderen mislukt; probeer opnieuw."
                    pendingDelete = null
                }) { Text("Verwijder thema") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuleren") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn privéthema’s") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("Deel zelf je eigen zinnen in bij maximaal twaalf thema’s. " +
                    "Alles blijft op dit toestel. De oorspronkelijke zinnen en betekenissen " +
                    "veranderen niet.", style = MaterialTheme.typography.bodySmall)
                TextButton(
                    enabled = topics.size < PrivatePhraseTopics.MAX_TOPICS,
                    onClick = { proposed = ""; notice = ""; editTarget = "__new__" }
                ) { Text("+ Nieuw thema") }
                topics.forEach { name ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            proposed = name
                            notice = ""
                            editTarget = name
                        }) { Text("Hernoem") }
                        TextButton(onClick = { pendingDelete = name }) { Text("Wis") }
                    }
                }
                if (topics.isEmpty()) Text("Je hebt nog geen thema’s.")
                if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.error)
                Text("Een JSON-/TSV-zinnenback-up bevat alleen jouw zinnen, " +
                    "niet deze privé-indeling.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Terug") } }
    )
}

@Composable
internal fun AssignPrivatePhraseTopicDialog(
    note: PersonalPhrase,
    storage: PrivatePhraseTopics,
    topics: List<String>,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    val selected = storage.topicFor(note)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deel mijn eigen zin in") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Je kiest zelf één thema. Dit controleert of vertaalt " +
                    "de zin niet.", style = MaterialTheme.typography.bodySmall)
                (listOf<String?>(null) + topics).forEach { candidate ->
                    TextButton(onClick = {
                        if (storage.assign(note, candidate)) {
                            onChanged()
                            onDismiss()
                        } else message = "Indelen mislukt: de zin of het thema is gewijzigd."
                    }) {
                        val label = candidate ?: "Zonder thema"
                        Text(if (candidate == selected) "$label ✓" else label)
                    }
                }
                if (topics.isEmpty()) Text("Maak eerst een eigen thema aan.")
                if (message.isNotBlank()) Text(message,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}
