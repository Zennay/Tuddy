package com.livelingo.android.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
    saveOwnMeaning: ((String, String, String) -> Boolean)? = null,
    allowSpeech: Boolean = true,
    highlightQuery: String = ""
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    var dialogOpen by remember(text, language) { mutableStateOf(false) }
    var selectedWord by remember(text, language) { mutableStateOf("") }
    // Held peek is transient and never modifies the stored glossary/deck.
    var heldWord by remember(text, language) { mutableStateOf<String?>(null) }
    var meaningDraft by remember(text, language) { mutableStateOf("") }
    var noteMessage by remember(text, language) { mutableStateOf("") }

    // Styled match spans retain exactly the same character offsets, so wordAt()
    // still selects the visible source word, even inside a search highlight.
    val markedText = highlightedCaptionText(
        text,
        highlightQuery,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer
    )
    Text(
        text = markedText,
        style = style,
        onTextLayout = { layout = it },
        modifier = Modifier
            .pointerInput(text) {
                detectTapGestures(
                    onPress = {
                        // Both release and scroll cancellation clear the hover
                        // preview. An ordinary tap still opens the full editor.
                        tryAwaitRelease()
                        heldWord = null
                    },
                    onLongPress = { point ->
                        val offset = layout?.getOffsetForPosition(point)
                        val word = offset?.let { wordAt(text, it) }
                        heldWord = word
                    },
                    onTap = { point ->
                        val offset = layout?.getOffsetForPosition(point)
                        val word = offset?.let { wordAt(text, it) }
                        if (word != null) {
                            heldWord = null
                            selectedWord = word
                            meaningDraft = ""
                            noteMessage = ""
                            dialogOpen = true
                        }
                    }
                )
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

    val peek = heldWord
    if (peek != null && !dialogOpen) {
        val local = LocalGlossary.lookup(language, peek)
        val authored = language?.let { readOwnMeaning?.invoke(it, peek) }
        Popup(
            alignment = Alignment.TopCenter,
            onDismissRequest = { heldWord = null },
            properties = PopupProperties(focusable = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 280.dp).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Woord · ${language?.uppercase() ?: "onbekend"}: $peek",
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDirection = TextDirection.ContentOrRtl))
                    Text(
                        if (local != null) "${local.illustration} ${local.definition}"
                        else "Geen bekende lokale woordenboekbetekenis.",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                    if (!authored.isNullOrBlank()) {
                        Text("Mijn eigen uitleg · niet geverifieerd: $authored",
                            style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                    Text("Laat los om te sluiten · tik kort om te bewerken.",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

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
                            if (it.length <= 40) {
                                // Historical bounded-input contract: under
                                // the guard take(40) can never cut a surrogate.
                                selectedWord = it.take(40)
                                meaningDraft = ""
                                noteMessage = ""
                            } else {
                                noteMessage = "Maximaal 40 tekens; een woord wordt niet afgekapt."
                            }
                        },
                        label = { Text("Woord uit de zin") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl),
                        singleLine = true
                    )
                    Text(
                        if (definition != null) "${definition.illustration} ${definition.definition}"
                        else "Geen gecontroleerde lokale betekenis voor dit woord of deze taal.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OfflineWordPronunciation(
                        word = selectedWord,
                        language = language,
                        enabled = allowSpeech
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
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = TextDirection.ContentOrRtl),
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
                        "Alleen enkele gecontroleerde basiswoorden in NL, EN, DE, FR en ES. " +
                            "Ook Sorani/Pasjtoe-woorden kun je hier zelf selecteren " +
                            "en van een eigen, niet geverifieerde betekenis voorzien " +
                            "als de eigen-woordenfunctie beschikbaar is. " +
                            "Geen automatische vertaling; geen AI-gok of online woordenboek; " +
                            "niet gebruiken als medisch of juridisch advies.",
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
