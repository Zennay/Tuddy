package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.livelingo.android.model.SafeCommunityPhrasebook

/**
 * Large, explicitly labelled offline display of the FIVE bundled beginner
 * entries, intended for showing a phrase to someone next to the phone.
 *
 * Uses only the already bundled exact-match phrasebook. No ASR, ML Kit,
 * microphone, voice, network, clipboard, share intent or storage.
 */
@Composable
internal fun OfflineStarterPhrasePresenter(
    phrases: List<Pair<String, String>>,
    initialIndex: Int,
    initialSource: String,
    target: String,
    onDismiss: () -> Unit
) {
    val label = when (target) {
        "kmr" -> "Kurmanji"
        "ckb" -> "Sorani"
        "ps" -> "Pasjtoe"
        else -> target.uppercase()
    }
    var index by remember(initialIndex, phrases) {
        mutableIntStateOf(initialIndex.coerceIn(0, (phrases.size - 1).coerceAtLeast(0)))
    }
    var source by remember(initialSource) {
        mutableStateOf(if (initialSource == "en") "en" else "nl")
    }
    var targetFirst by remember { mutableStateOf(true) }
    var larger by remember { mutableStateOf(true) }
    val item = phrases.getOrNull(index)
    val original = item?.let { if (source == "nl") it.first else it.second }.orEmpty()
    val result = if (item == null) null else
        SafeCommunityPhrasebook.translate(source, target, original).getOrNull()
    val isRtl = target == "ckb" || target == "ps"

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.86f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("5 basiswoorden · $label",
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text(
                    "Vijf ingebouwde voorbeeldwoorden, niet onafhankelijk door " +
                        "moedertaalsprekers geverifieerd. Geen vrije vertaling, " +
                        "spraakfunctie of audio-synchronisatie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { source = if (source == "nl") "en" else "nl" }) {
                        Text(if (source == "nl") "Nederlands ⇄" else "English ⇄")
                    }
                    TextButton(onClick = { targetFirst = !targetFirst }) {
                        Text("Wissel volgorde")
                    }
                    TextButton(onClick = { larger = !larger }) {
                        Text(if (larger) "Kleiner" else "Groter")
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (item == null) {
                        Text("Geen voorbeeldkaart beschikbaar.")
                    } else {
                        @Composable
                        fun sourceSide() {
                            Text(
                                if (source == "nl") "Bronwoord · Nederlands" else "Source word · English",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                original,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = if (larger) 35.sp else 25.sp,
                                    lineHeight = if (larger) 44.sp else 34.sp,
                                    textDirection = TextDirection.Ltr
                                )
                            )
                        }
                        @Composable
                        fun targetSide() {
                            Text("Ingebouwd voorbeeld · $label",
                                style = MaterialTheme.typography.labelLarge)
                            Text(
                                result ?: "Geen bekende lokale invoer; niets geraden.",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = if (larger) 47.sp else 32.sp,
                                    lineHeight = if (larger) 58.sp else 41.sp,
                                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        if (targetFirst) {
                            targetSide()
                            HorizontalDivider()
                            sourceSide()
                        } else {
                            sourceSide()
                            HorizontalDivider()
                            targetSide()
                        }
                        Text(
                            "Controleer belangrijke communicatie met een menselijke tolk. " +
                                "Dit voorbeeld is geen volledig offline taalmodel.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        enabled = index > 0,
                        onClick = { index-- }
                    ) { Text("← Vorige") }
                    Text("${if (item == null) 0 else index + 1} / ${phrases.size}",
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        enabled = index < phrases.lastIndex,
                        onClick = { index++ }
                    ) { Text("Volgende →") }
                }
            }
        }
    }
}
