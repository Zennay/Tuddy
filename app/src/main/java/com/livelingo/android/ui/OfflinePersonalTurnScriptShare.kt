package com.livelingo.android.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateAuthoredConversationScript
import com.livelingo.android.PrivateAuthoredConversationTurn

/**
 * Deliberately share only the exact visible selected private dialogue script.
 * No implicit clipboard, file, analytics, permissions or network transport.
 */
@Composable
internal fun OfflinePersonalTurnScriptShare(
    turns: List<PrivateAuthoredConversationTurn>
) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<ByteArray?>(null) }
    var feedback by remember { mutableStateOf("") }
    // Explicitly scoped turn selection: never default to sharing the whole session.
    var selectedIndices by remember(turns) { mutableStateOf<Set<Int>>(emptySet()) }
    fun fresh(): ByteArray? = PrivateAuthoredConversationScript.render(
        turns.filterIndexed { index, _ -> index in selectedIndices }
    )

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Kies de beurten die je afzonderlijk extern wilt delen:",
            style = MaterialTheme.typography.labelMedium)
        Row {
            TextButton(onClick = { selectedIndices = turns.indices.toSet() }) {
                Text("Kies alle " + turns.size + " beurten")
            }
            TextButton(onClick = { selectedIndices = emptySet() }) {
                Text("Wis selectie")
            }
        }
        turns.forEachIndexed { index, turn ->
            val note = turn.note
            val source = if (turn.fromMeaning) note.ownMeaning else note.phrase
            TextButton(
                onClick = {
                    selectedIndices = if (index in selectedIndices)
                        selectedIndices - index else selectedIndices + index
                    feedback = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text((if (index in selectedIndices) "☑ " else "☐ ") +
                    "Beurt " + (index + 1) + ": " + source.take(70))
            }
        }
        OutlinedButton(
            enabled = selectedIndices.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val bytes = fresh()
                feedback = if (bytes == null)
                    "Kies maximaal twaalf geldige eigen gespreksbeurten."
                else ""
                if (bytes != null) preview = bytes.copyOf()
            }
        ) { Text("Deel alleen mijn " + selectedIndices.size + " geselecteerde beurt(en)…") }
        Text(
            "Delen is optioneel: selecteer de gewenste beurten, bekijk " +
                "alleen die volledige tekst en kies daarna " +
                "zelf een Android-app. Niets wordt automatisch verzonden.",
            style = MaterialTheme.typography.bodySmall
        )
        if (feedback.isNotBlank()) Text(feedback,
            style = MaterialTheme.typography.bodySmall)
    }

    val selected = preview
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Deze exacte privézinnen extern delen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Alleen dit expliciet geselecteerde tekstfragment " +
                            "van maximaal 12 beurten / 8192 bytes gaat naar " +
                            "een Android-deelvenster; geen audio, geschiedenis " +
                            "of andere persoonlijke zinnen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(selected.toString(Charsets.UTF_8),
                        style = MaterialTheme.typography.bodySmall)
                    Text(
                        "De ontvanger kan de tekst opslaan, doorsturen, " +
                            "vertalen of uploaden. Tuddy kan dat niet terugdraaien. " +
                            "Zelfgeschreven betekenissen zijn niet geverifieerd. " +
                            "Alleen volledig vertrouwde gesprekken delen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val current = fresh()
                    preview = null
                    if (current == null || !current.contentEquals(selected)) {
                        feedback = "Je gesprek is gewijzigd. Bekijk de " +
                            "huidige volledige tekst opnieuw voordat je deelt."
                    } else {
                        val launched = runCatching {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT,
                                    selected.toString(Charsets.UTF_8))
                            }
                            context.startActivity(Intent.createChooser(
                                intent, "Kies zelf waar je het Tuddy-gesprek deelt"
                            ))
                            true
                        }.getOrDefault(false)
                        feedback = if (launched)
                            "Android-deelvenster geopend. Alleen jij " +
                                "kiest een ontvanger of app."
                        else "Android-delen is niet beschikbaar. Niets verzonden."
                    }
                }) { Text("Open Android-deelvenster…") }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }) { Text("Annuleren") }
            }
        )
    }
}
