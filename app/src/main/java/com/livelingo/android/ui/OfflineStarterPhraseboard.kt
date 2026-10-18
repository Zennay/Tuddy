package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.model.SafeCommunityPhrasebook

/**
 * Immediate, exact-match, offline starter words. Only the existing bundled
 * five-phrase preview is used; no free-form language model, microphone,
 * remote dictionary, lookup download or voice capability is implied.
 */
@Composable
internal fun OfflineStarterPhraseboard(
    initialTarget: String = "kmr",
    onOpenPersonalPhrases: () -> Unit,
    onDismiss: () -> Unit
) {
    var target by remember(initialTarget) {
        mutableStateOf(initialTarget.takeIf { it in setOf("kmr", "ckb", "ps") } ?: "kmr")
    }
    var source by remember { mutableStateOf("nl") }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    val choices = listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
    val starter = listOf(
        "hallo" to "hello",
        "bedankt" to "thank you",
        "ja" to "yes",
        "nee" to "no",
        "water" to "water"
    )

    // The large-reader is a separate deliberate mode, not a new model or
    // hidden side effect of choosing a language.
    expandedIndex?.let { position ->
        OfflineStarterPhrasePresenter(
            phrases = starter,
            initialIndex = position,
            initialSource = source,
            target = target,
            onDismiss = { expandedIndex = null }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("5 basisuitdrukkingen · zonder internet") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Uitsluitend vijf vast opgenomen begrippen, niet vrije " +
                    "zinnen, spraakherkenning of een automatisch vertaalmodel.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    choices.forEach { (code, label) ->
                        TextButton(onClick = { target = code }) {
                            Text(if (target == code) "$label ✓" else label,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                TextButton(onClick = { source = if (source == "nl") "en" else "nl" }) {
                    Text(if (source == "nl") "Bronwoorden: Nederlands ⇄"
                        else "Source words: English ⇄")
                }
                starter.forEachIndexed { position, (dutch, english) ->
                    val phrase = if (source == "nl") dutch else english
                    val translated = SafeCommunityPhrasebook.translate(source, target, phrase)
                        .getOrNull() ?: "Geen gecontroleerde invoer beschikbaar."
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(phrase, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            translated,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDirection = if (target == "ckb" || target == "ps")
                                    TextDirection.Rtl else TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(onClick = { expandedIndex = position }) {
                            Text("▣ Toon dit woord groot")
                        }
                        HorizontalDivider()
                    }
                }
                Text("De bestaande zinnen zijn niet onafhankelijk door " +
                    "moedertaalsprekers gecontroleerd. Vermijd gebruik bij " +
                    "medische, juridische of urgente beslissingen zonder tolk.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenPersonalPhrases) { Text("Mijn eigen zinnen…") }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
