package com.livelingo.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Local-only lookup in finished caption text. No microphone, translation,
 * persistent index, network request or search analytics.
 */
internal object SessionCaptionSearch {
    fun filter(
        captions: List<FocusCaptionLine>,
        query: String,
        manualSpeaker: String?
    ): List<FocusCaptionLine> {
        val term = query.trim().take(120)
        return captions.filter { caption ->
            (manualSpeaker == null ||
                (manualSpeaker == "?" && caption.speaker == null) ||
                caption.speaker == manualSpeaker) &&
                (term.isEmpty() ||
                    caption.original.contains(term, ignoreCase = true) ||
                    ((caption.status == "Lokaal vertaald" || caption.status == "Lokaal") &&
                        caption.translated.contains(term, ignoreCase = true)) ||
                    caption.personalMeaning?.contains(term, ignoreCase = true) == true)
        }
    }
}

/** Literal, bounded match-highlighting without modifying source word offsets. */
internal fun highlightedCaptionText(
    text: String,
    query: String,
    background: Color,
    foreground: Color
): AnnotatedString {
    val term = query.trim().take(120)
    if (term.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var from = 0
        while (from < text.length) {
            val match = text.indexOf(term, startIndex = from, ignoreCase = true)
            if (match < 0) break
            append(text.substring(from, match))
            withStyle(SpanStyle(background = background, color = foreground)) {
                append(text.substring(match, match + term.length))
            }
            from = match + term.length
        }
        append(text.substring(from))
    }
}

/**
 * Emphasise literal search hits and optionally make finished caption words
 * locally inspectable in fullscreen reading mode; never synthesize definitions.
 */
@Composable
internal fun SessionCaptionSearchText(
    text: String,
    query: String,
    style: TextStyle,
    enableOfflineWordCards: Boolean = false,
    wordLanguage: String? = null,
    allowWordSpeech: Boolean = true
) {
    if (enableOfflineWordCards) {
        OfflineWordText(
            text = text,
            language = wordLanguage,
            style = style,
            allowSpeech = allowWordSpeech,
            highlightQuery = query
        )
        return
    }
    val marked = highlightedCaptionText(
        text,
        query,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer
    )
    Text(marked, style = style)
}
