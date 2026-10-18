package com.livelingo.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var message by remember { mutableStateOf("") }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        val previous = pending
        pending = null
        if (uri != null && previous != null) {
            val current = PrivateStudyCalendar.snapshot(words, ownWords, phrases)
            if (current == null || !current.contentEquals(previous)) {
                message = "De planning is veranderd. Kies opnieuw Bewaar als agenda."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(previous)
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
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val bytes = PrivateStudyCalendar.snapshot(words, ownWords, phrases)
                if (bytes == null) {
                    message = "Geen geldige, leesbare geplande kaarten om te exporteren."
                } else {
                    pending = bytes
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
