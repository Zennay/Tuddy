package com.livelingo.android.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.livelingo.android.model.LocalGlossary
import com.livelingo.android.model.wordAt

/**
 * Tap a displayed word for a tiny, fully offline dictionary card.
 * A semantic activation opens an editable field for keyboard/TalkBack users.
 * The app never submits user text to an external definition service.
 */
@Composable
internal fun OfflineWordText(
    text: String,
    language: String?,
    style: TextStyle,
    onSaveWord: ((String, String) -> Boolean)? = null,
    readOwnMeaning: ((String, String) -> String?)? = null,
    saveOwnMeaning: ((String, String, String) -> Boolean)? = null
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    var dialogOpen by remember(text, language) { mutableStateOf(false) }
    var selectedWord by remember(text, language) { mutableStateOf("") }
    var meaningDraft by remember(text, language) { mutableStateOf("") }
    var noteMessage by remember(text, language) { mutableStateOf("") }

    Text(
        text = text,
        style = style,
        onTextLayout = { layout = it },
        modifier = Modifier
            .pointerInput(text) {
                detectTapGestures { point ->
                    val offset = layout?.getOffsetForPosition(point)
                    val word = offset?.let { wordAt(text, it) } ?: return@detectTapGestures
                    selectedWord = word.take(48)
                    meaningDraft = ""
                    noteMessage = ""
                    dialogOpen = true
                }
            }
            .semantics {
                onClick(label = "Open lokale woordkaart") {
                    selectedWord = ""
                    meaningDraft = ""
                    noteMessage = ""
                    dialogOpen = true
                    true
                }
            }
    )

    if (dialogOpen) {
        val definition = LocalGlossary.lookup(language, selectedWord)
        val ownMeaning = if (language != null) readOwnMeaning?.invoke(language, selectedWord) else null
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text("Lokale woordkaart") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedWord,
                        onValueChange = {
                            selectedWord = it.take(48)
                            meaningDraft = ""
                            noteMessage = ""
                        },
                        label = { Text("Woord uit de zin") },
                        singleLine = true
                    )
                    Text(
                        if (definition != null) "${definition.illustration} ${definition.definition}"
                        else "Geen gecontroleerde lokale betekenis voor dit woord of deze taal.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (ownMeaning != null) {
                        Text("Mijn eigen notitie (niet geverifieerd): " + ownMeaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    if (saveOwnMeaning != null && language != null) {
                        OutlinedTextField(
                            value = meaningDraft,
                            onValueChange = { meaningDraft = it.take(180) },
                            label = { Text("Mijn eigen betekenis · niet gecontroleerd") },
                            maxLines = 3
                        )
                        TextButton(
                            enabled = selectedWord.isNotBlank() && meaningDraft.isNotBlank(),
                            onClick = {
                                val saved = saveOwnMeaning(language, selectedWord, meaningDraft)
                                noteMessage = if (saved)
                                    "Eigen betekenis privé opgeslagen; niet geverifieerd."
                                else "Niet opgeslagen. Controleer de woordtaal en invoer."
                                if (saved) meaningDraft = ""
                            }
                        ) { Text("Bewaar mijn eigen betekenis") }
                        if (noteMessage.isNotBlank()) {
                            Text(noteMessage, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        "Alleen enkele basiswoorden in NL, EN, DE, FR en ES. " +
                            "Geen AI-gok of online woordenboek; niet gebruiken als medisch of juridisch advies.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Sluiten") }
            },
            dismissButton = {
                if (definition != null && onSaveWord != null && language != null) {
                    TextButton(onClick = {
                        if (onSaveWord(language, selectedWord)) dialogOpen = false
                    }) { Text("Bewaar woord") }
                }
            }
        )
    }
}
