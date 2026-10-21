package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Render at most one small section of the exact UTF-8 export snapshot.
 * Users can inspect every character across pages; the file-writing callback
 * still uses the entire immutable snapshot, never the visible page.
 */
@Composable
internal fun PrivateSubtitlePagedExportPreview(bytes: ByteArray) {
    val source = String(bytes, Charsets.UTF_8)
    val pages = remember(source) {
        val segments = ArrayList<String>()
        var start = 0
        while (start < source.length) {
            var end = minOf(source.length, start + 3_200)
            if (end < source.length) {
                val lineEnd = source.lastIndexOf('\n', end - 1)
                if (lineEnd >= start) {
                    // Preserve original LF or CRLF inside each rendered chunk.
                    end = lineEnd + 1
                } else if (end > start + 1 &&
                    Character.isHighSurrogate(source[end - 1]) &&
                    Character.isLowSurrogate(source[end])
                ) {
                    end--
                }
            }
            segments.add(source.substring(start, end))
            start = end
        }
        segments.ifEmpty { listOf("") }
    }
    var page by remember(source) { mutableIntStateOf(0) }
    val shown = page.coerceIn(0, pages.lastIndex)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            "Werkelijke UTF-8-inhoud · deel ${shown + 1} van ${pages.size}. " +
                "Blader om alle originele regels en tekens te bekijken. " +
                "De volledige kopie wordt opgeslagen, niet alleen dit deel.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = shown > 0, onClick = { page = shown - 1 }) {
                Text("← Vorige")
            }
            TextButton(enabled = shown < pages.lastIndex,
                onClick = { page = shown + 1 }) {
                Text("Volgende →")
            }
        }
        Text(pages[shown],
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall)
    }
}
