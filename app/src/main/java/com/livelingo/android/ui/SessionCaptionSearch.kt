package com.livelingo.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** Emphasise the literal search hit only; no word/audio timing is inferred. */
@Composable
internal fun SessionCaptionSearchText(
    text: String,
    query: String,
    style: TextStyle
) {
    val term = query.trim().take(120)
    if (term.isEmpty()) {
        Text(text, style = style)
        return
    }
    val hitBackground = MaterialTheme.colorScheme.tertiaryContainer
    val hitForeground = MaterialTheme.colorScheme.onTertiaryContainer
    val marked = buildAnnotatedString {
        var from = 0
        while (from < text.length) {
            val match = text.indexOf(term, startIndex = from, ignoreCase = true)
            if (match < 0) break
            append(text.substring(from, match))
            withStyle(SpanStyle(
                background = hitBackground,
                color = hitForeground
            )) {
                append(text.substring(match, match + term.length))
            }
            from = match + term.length
        }
        append(text.substring(from))
    }
    Text(marked, style = style)
}
