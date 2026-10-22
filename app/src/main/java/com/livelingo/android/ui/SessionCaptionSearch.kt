package com.livelingo.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.livelingo.android.PrivateOfflineSearch

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
        // Keep user-authored script unchanged. Fold only the transient search
        // probe, as the live-conversation/archive searches already do.
        val term = PrivateOfflineSearch.fold(
            query.trim().take(120).filterNot { it.isISOControl() }
        )
        return captions.filter { caption ->
            (manualSpeaker == null ||
                (manualSpeaker == "?" && caption.speaker == null) ||
                caption.speaker == manualSpeaker) &&
                (term.isEmpty() ||
                    PrivateOfflineSearch.contains(caption.original, term) ||
                    ((caption.status == "Lokaal vertaald" || caption.status == "Lokaal") &&
                        PrivateOfflineSearch.contains(caption.translated, term)) ||
                    caption.personalMeaning?.let { PrivateOfflineSearch.contains(it, term) } == true)
        }
    }
}

/** Literal, bounded match-highlighting without modifying source word offsets. */
internal fun highlightedCaptionText(
    text: String,
    query: String,
    background: Color,
    foreground: Color,
    activeRange: Pair<Int, Int>? = null,
    activeRangeBackground: Color? = null,
    activeRangeForeground: Color? = null
): AnnotatedString {
    val term = query.trim().take(120)
    return buildAnnotatedString {
        append(text)
        if (term.isNotEmpty()) {
            var from = 0
            while (from < text.length) {
                val match = text.indexOf(term, startIndex = from, ignoreCase = true)
                if (match < 0) break
                addStyle(
                    SpanStyle(background = background, color = foreground),
                    match,
                    match + term.length
                )
                from = match + term.length
            }
        }
        val range = activeRange
        val rangeBackground = activeRangeBackground
        val rangeForeground = activeRangeForeground
        if (range != null && rangeBackground != null && rangeForeground != null &&
            range.first >= 0 && range.second > range.first && range.second <= text.length
        ) {
            addStyle(
                SpanStyle(
                    background = rangeBackground,
                    color = rangeForeground,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                range.first,
                range.second
            )
        }
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
