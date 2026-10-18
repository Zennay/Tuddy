package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.livelingo.android.SubtitleDraftFormat
import com.livelingo.android.SubtitleDraftMode
import com.livelingo.android.SubtitleDraftOptions

/**
 * Manual editing assumptions; selecting a preset does not synchronize
 * captions with an audio or video recording. No hidden user profile.
 */
@Composable
internal fun SubtitleDraftControls(
    options: SubtitleDraftOptions,
    onChange: (SubtitleDraftOptions) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Bestandsformaat van het ruwe ondertitelconcept",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(
                SubtitleDraftFormat.SRT to "SRT",
                SubtitleDraftFormat.WEBVTT to "WebVTT"
            ).forEach { (format, label) ->
                TextButton(onClick = { onChange(options.copy(format = format)) }) {
                    Text(if (options.format == format) label + " ✓" else label)
                }
            }
        }
        Text("Welke tekst komt in het ondertitelconcept?",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(
                SubtitleDraftMode.BILINGUAL to "Beide",
                SubtitleDraftMode.ORIGINAL_ONLY to "Origineel",
                SubtitleDraftMode.TRANSLATION_ONLY to "Vertaling"
            ).forEach { (choice, label) ->
                TextButton(onClick = { onChange(options.copy(mode = choice)) }) {
                    Text(if (options.mode == choice) label + " ✓" else label)
                }
            }
        }
        Text("Begin van eerste zin: extra ruwe verschuiving",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(0L, 5000L, 10000L).forEach { offset ->
                TextButton(onClick = { onChange(options.copy(startOffsetMs = offset)) }) {
                    val label = (offset / 1000).toString() + " s"
                    Text(if (options.startOffsetMs == offset) label + " ✓" else label)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(30000L, 60000L).forEach { offset ->
                TextButton(onClick = { onChange(options.copy(startOffsetMs = offset)) }) {
                    val label = (offset / 1000).toString() + " s"
                    Text(if (options.startOffsetMs == offset) label + " ✓" else label)
                }
            }
        }
        Text("Maximale duur van een conceptregel",
            style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(2000L, 3500L, 5000L, 7000L).forEach { duration ->
                TextButton(onClick = { onChange(options.copy(cueDurationMs = duration)) }) {
                    val label = when (duration) {
                        3500L -> "3,5 s"
                        else -> (duration / 1000).toString() + " s"
                    }
                    Text(if (options.cueDurationMs == duration) label + " ✓" else label)
                }
            }
        }
        Text(
            "Dit is alleen een monteerhulp: opnamestart, woord- en zinstijden " +
                "zijn niet gemeten. Controleer en corrigeer alles in je video-editor. " +
                "Zonder voltooide vertaling valt de regel weg uit 'Vertaling'.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
