package com.livelingo.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Follows the text already emitted by Android's on-device recognizer.
 * Highlights only the last non-whitespace token in a provisional original.
 *
 * This is visual text focus, NOT an audio timestamp, diarization or
 * word-aligned translation. No new microphone, inference or network calls.
 */
@Composable
internal fun LiveInterimWordFocus(text: String, fontScale: Float) {
    LiveInterimWordFocus(text, fontScale, compact = false)
}

@Composable
internal fun LiveInterimWordFocus(text: String, fontScale: Float, compact: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    val annotated = remember(text, color) {
        buildAnnotatedString {
            append(text)
            val end = text.trimEnd().length
            if (end > 0) {
                val start = text.substring(0, end).indexOfLast { it.isWhitespace() } + 1
                if (start < end) {
                    addStyle(
                        SpanStyle(color = color, fontWeight = FontWeight.Bold),
                        start,
                        end
                    )
                }
            }
        }
    }
    val baseStyle = if (compact) MaterialTheme.typography.bodyLarge
        else MaterialTheme.typography.headlineSmall
    Text(
        annotated,
        style = baseStyle.copy(fontSize = baseStyle.fontSize * fontScale)
    )
}
