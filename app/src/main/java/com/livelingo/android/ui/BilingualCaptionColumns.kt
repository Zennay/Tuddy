package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

/**
 * A readable side-by-side original/translation layout for wide screens.
 * Uses only already-present words and their actual local translation state;
 * never invents translation or audio-aligned timings.
 */
@Composable
internal fun BilingualCaptionColumns(
    original: String,
    translation: String,
    sourceCode: String,
    targetCode: String,
    status: String,
    fontScale: Float,
    isInterim: Boolean,
    highlightLatestFinal: Boolean = false,
    searchQuery: String = "",
    enableOfflineWordCards: Boolean = false,
    allowWordSpeech: Boolean = true
) {
    val translated = status == "Lokaal vertaald" || status == "Lokaal"
    val originalStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = MaterialTheme.typography.titleLarge.fontSize * fontScale,
        // Keep physical line height proportional when Android system text
        // scaling and the user-selected reader zoom are both enlarged.
        lineHeight = MaterialTheme.typography.titleLarge.fontSize * fontScale * 1.35f,
        textDirection = TextDirection.ContentOrRtl
    )
    val translatedStyle = originalStyle.copy(color = MaterialTheme.colorScheme.primary)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isInterim) "Live · herkende tekst" else "Origineel · ${sourceCode.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isInterim) {
                LiveInterimWordFocus(text = original, fontScale = fontScale * 0.88f, compact = true)
                Text("Laatste tekstwoord · geen audio-woordtiming",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SessionCaptionSearchText(
                    original, searchQuery, originalStyle,
                    enableOfflineWordCards = enableOfflineWordCards,
                    wordLanguage = sourceCode,
                    allowWordSpeech = allowWordSpeech,
                    activeSpeechText = if (highlightLatestFinal) normalizedLiveFocusText(original) else null,
                    activeSpeechRange = if (highlightLatestFinal) lastTextWordRange(original) else null
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isInterim) "Voorlopige lokale vertaling" else "Vertaling · ${targetCode.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isInterim) {
                if (translation.isNotBlank()) {
                    LiveInterimWordFocus(
                        text = translation, fontScale = fontScale * 0.88f, compact = true
                    )
                    Text("Laatste vertaald tekstwoord · geen audio-uitlijning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else
                    Text("Wacht op lokaal resultaat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (translated) {
                SessionCaptionSearchText(
                    translation, searchQuery, translatedStyle,
                    enableOfflineWordCards = enableOfflineWordCards,
                    wordLanguage = targetCode,
                    allowWordSpeech = allowWordSpeech,
                    activeSpeechText = if (highlightLatestFinal) normalizedLiveFocusText(translation) else null,
                    activeSpeechRange = if (highlightLatestFinal) lastTextWordRange(translation) else null
                )
            } else {
                Text("[Vertaling niet voltooid: $status]",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
