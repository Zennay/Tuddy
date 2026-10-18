package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PersonalPhrase

@Composable
private fun PhraseSide(text: String, code: String, label: String, large: Boolean) {
    val rtl = code in setOf("ckb", "ps", "ar", "fa", "ur")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (large) 34.sp else 25.sp,
                lineHeight = if (large) 44.sp else 34.sp,
                textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Deliberately visible, all-local presentation of the user's OWN notes.
 * No ASR, ML Kit, TTS, cloud service or Android share API.
 */
@Composable
internal fun OfflinePhrasePresenter(
    notes: List<PersonalPhrase>,
    initial: PersonalPhrase,
    onDismiss: () -> Unit
) {
    val cards = remember(notes, initial) {
        notes.filter { it.language == initial.language }
            .sortedWith(compareBy(PersonalPhrase::phrase))
    }
    var selectedIndex by remember(cards, initial) {
        mutableIntStateOf(cards.indexOfFirst { it == initial }.coerceAtLeast(0))
    }
    var large by remember { mutableStateOf(true) }
    var showMeaningFirst by remember { mutableStateOf(false) }
    val selected = cards.getOrNull(selectedIndex)
    val label = when (initial.language) {
        "kmr" -> "Kurmanji"
        "ckb" -> "Sorani"
        "ps" -> "Pasjtoe"
        else -> initial.language.uppercase()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.86f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mijn gesprekskaart · " + label,
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text(
                    "Door jou geschreven · geen geverifieerde vertaling. " +
                        "Toon alleen zinnen die je met iemand wilt delen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                if (selected == null) {
                    Text("Deze opgeslagen zin is niet meer beschikbaar.")
                    Spacer(Modifier.weight(1f))
                } else {
                    Column(
                        modifier = Modifier.weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        if (showMeaningFirst) {
                            PhraseSide(selected.ownMeaning, selected.meaningLanguage,
                                "Eigen betekenis (" + selected.meaningLanguage.uppercase() + ")", large)
                            HorizontalDivider()
                            PhraseSide(selected.phrase, selected.language,
                                "Eigen zin (" + label + ")", large)
                        } else {
                            PhraseSide(selected.phrase, selected.language,
                                "Eigen zin (" + label + ")", large)
                            HorizontalDivider()
                            PhraseSide(selected.ownMeaning, selected.meaningLanguage,
                                "Eigen betekenis (" + selected.meaningLanguage.uppercase() + ")", large)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = { showMeaningFirst = !showMeaningFirst }) {
                        Text("Wissel volgorde")
                    }
                    TextButton(onClick = { large = !large }) {
                        Text(if (large) "Kleiner" else "Groter")
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        enabled = selectedIndex > 0,
                        onClick = { selectedIndex-- }
                    ) { Text("← Vorige") }
                    Text((if (selected == null) 0 else selectedIndex + 1).toString() +
                        " / " + cards.size,
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        enabled = selectedIndex < cards.lastIndex,
                        onClick = { selectedIndex++ }
                    ) { Text("Volgende →") }
                }
            }
        }
    }
}
