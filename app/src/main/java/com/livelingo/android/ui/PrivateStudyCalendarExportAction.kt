package com.livelingo.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateStudyCalendar
import com.livelingo.android.PrivateVocabularyDeck

private data class PendingCalendar(val groups: Set<String>, val bytes: ByteArray)

/**
 * Explicit SAF export for the existing read-only eight-week study calendar.
 * Calendar entries include counts only; this does not register reminders or
 * request Calendar permissions. An edited schedule invalidates pending bytes.
 */
@Composable
internal fun PrivateStudyCalendarExportAction(
    words: PrivateVocabularyDeck,
    ownWords: PrivateOwnWordDeck,
    phrases: PrivatePhraseDeck
) {
    val context = LocalContext.current
    val available = buildSet {
        if (words.reviewsEnabled()) add("curated")
        if (ownWords.reviewsEnabled()) add("own")
        if (phrases.reviewsEnabled()) add("phrases")
    }
    var selected by remember { mutableStateOf(available) }
    var pending by remember { mutableStateOf<PendingCalendar?>(null) }
    var message by remember { mutableStateOf("") }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        val previous = pending
        pending = null
        if (uri != null && previous != null) {
            val current = PrivateStudyCalendar.snapshot(
                words, ownWords, phrases, previous.groups
            )
            if (selected != previous.groups ||
                current == null || !current.contentEquals(previous.bytes)) {
                message = "De planning is veranderd. Kies opnieuw Bewaar als agenda."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(previous.bytes)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                message = if (saved) "Het agendabestand is bewust opgeslagen."
                    else "Opslaan mislukt; de lokale planning is intact."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            "Selecteer zelf welke al ingeschakelde kaartgroepen in het " +
                "agendabestand mogen meetellen:",
            style = MaterialTheme.typography.bodySmall
        )
        listOf(
            "curated" to "Bewaarde basiswoorden",
            "own" to "Mijn eigen woorden",
            "phrases" to "Mijn eigen zinnen"
        ).forEach { (group, title) ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = group in available,
                onClick = {
                    selected = if (group in selected) selected - group
                        else selected + group
                }
            ) {
                Text((if (group in selected) "☑ " else "☐ ") + title +
                    if (group !in available) " (planning uit)" else "")
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = selected.isNotEmpty(),
            onClick = {
                val bytes = PrivateStudyCalendar.snapshot(
                    words, ownWords, phrases, selected
                )
                if (bytes == null) {
                    message = "Geen geldige, leesbare geplande kaarten om te exporteren."
                } else {
                    pending = PendingCalendar(selected.toSet(), bytes)
                    message = ""
                    createDocument.launch("Tuddy-studieplanning-8-weken.ics")
                }
            }
        ) { Text("Bewaar deze 8 weken als agenda (.ics)…") }
        Text(
            "Alleen datums en aantallen, zonder woorden of zinnen. " +
                "Na import werkt de externe agenda niet automatisch bij. " +
                "Je gekozen provider of agenda-app kan het bestand synchroniseren.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (message.isNotBlank()) Text(
            message, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
