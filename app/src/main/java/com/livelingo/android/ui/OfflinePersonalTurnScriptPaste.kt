package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateAuthoredConversationScript
import com.livelingo.android.PrivateAuthoredConversationTurn

/** User-pasted, strict private conversation script. Never auto-imports or saves. */
@Composable
internal fun OfflinePersonalTurnScriptPaste(
    current: List<PrivateAuthoredConversationTurn>,
    onImport: (
        expected: List<PrivateAuthoredConversationTurn>,
        imported: List<PrivateAuthoredConversationTurn>,
        replace: Boolean
    ) -> Boolean
) {
    var draftOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var preview by remember {
        mutableStateOf<List<PrivateAuthoredConversationTurn>?>(null)
    }
    var expected by remember {
        mutableStateOf<List<PrivateAuthoredConversationTurn>?>(null)
    }
    var notice by remember { mutableStateOf("") }

    fun apply(replace: Boolean) {
        val incoming = preview ?: return
        val snapshot = expected ?: return
        if (current != snapshot || (!replace && current.size + incoming.size > 12)) {
            notice = "Je gesprek veranderde of de limiet van 12 beurten is bereikt. " +
                "Er is niets geïmporteerd."
        } else {
            notice = if (onImport(snapshot, incoming, replace))
                "Zelf geplakt script alleen in dit tijdelijke gesprek geopend."
            else "Je gesprek wijzigde; niets geïmporteerd."
        }
        preview = null
        expected = null
        draft = ""
    }

    OutlinedButton(
        onClick = {
            draft = ""
            notice = ""
            draftOpen = true
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Plak een zelf gedeeld Tuddy-gesprekscript…") }
    Text("Plakken betekent nog niet importeren. Tuddy controleert " +
        "de volledige tekst lokaal en vraagt afzonderlijk toestemming.",
        style = MaterialTheme.typography.bodySmall)
    if (notice.isNotBlank()) {
        Text(notice, style = MaterialTheme.typography.bodySmall)
    }

    if (draftOpen) {
        AlertDialog(
            onDismissRequest = { draftOpen = false; draft = "" },
            title = { Text("Plak zelf de complete Tuddy-tekst") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 445.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Alleen de exacte tekst die Tuddy zelf bij 'Bewaar' " +
                            "of 'Deel' toont. Geen willekeurige vertaaltekst, " +
                            "ondertiteling of live transcript. Er wordt niets " +
                            "verstuurd of opgeslagen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(8192); notice = "" },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                        label = { Text("Volledig privégesprekscript · max. 8 KiB") },
                        maxLines = 14
                    )
                    if (notice.isNotBlank()) {
                        Text(notice, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val incoming = PrivateAuthoredConversationScript.read(
                        draft.toByteArray(Charsets.UTF_8)
                    )
                    if (incoming == null) {
                        notice = "Geen geldig, volledig Tuddy-gesprekscript " +
                            "(maximaal 8192 bytes en 12 beurten). Niets gewijzigd."
                    } else {
                        expected = current.toList()
                        preview = incoming
                        draftOpen = false
                    }
                }) { Text("Lees volledige controlepreview…") }
            },
            dismissButton = {
                TextButton(onClick = { draftOpen = false; draft = "" }) {
                    Text("Annuleren")
                }
            }
        )
    }
    val rows = preview
    if (rows != null) {
        val canAppend = (expected?.size ?: 12) + rows.size <= 12
        AlertDialog(
            onDismissRequest = { preview = null; expected = null; draft = "" },
            title = { Text("Neem alleen deze zelfgeschreven beurten over?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Deze " + rows.size + " gekozen beurten zijn volledig " +
                        "afkomstig uit de geplakte tekst, niet vertaald of " +
                        "gecontroleerd door Tuddy.",
                        style = MaterialTheme.typography.bodySmall)
                    rows.forEachIndexed { index, row ->
                        val firstCode = if (row.fromMeaning) row.note.meaningLanguage
                            else row.note.language
                        val otherCode = if (row.fromMeaning) row.note.language
                            else row.note.meaningLanguage
                        val first = if (row.fromMeaning) row.note.ownMeaning
                            else row.note.phrase
                        val other = if (row.fromMeaning) row.note.phrase
                            else row.note.ownMeaning
                        Text((index + 1).toString() + ". " + firstCode.uppercase() +
                            " → " + otherCode.uppercase(),
                            style = MaterialTheme.typography.labelMedium)
                        Text(first, style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (firstCode in setOf("ckb", "ps"))
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                        Text(other, style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = if (otherCode in setOf("ckb", "ps"))
                                TextDirection.Rtl else TextDirection.Ltr
                        ))
                    }
                    Text(
                        "Kies zelf vervangen of toevoegen. De geïmporteerde " +
                            "zinnen blijven uitsluitend in RAM en komen nooit " +
                            "automatisch in je persoonlijke woordenlijst.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Column {
                    TextButton(onClick = { apply(true) }) { Text("Vervang mijn gesprek") }
                    TextButton(onClick = { apply(false) }, enabled = canAppend) {
                        Text(if (canAppend) "Voeg tijdelijk toe"
                            else "Niet toevoegen: meer dan 12 beurten")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    preview = null
                    expected = null
                    draft = ""
                }) { Text("Annuleren") }
            }
        )
    }
}
