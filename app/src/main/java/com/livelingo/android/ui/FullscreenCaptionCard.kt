package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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

/** Read-only, ephemeral card for finished live captions. No network or export. */
private enum class CaptionCardSide { BOTH, ORIGINAL, TRANSLATION }

@Composable
private fun CaptionSide(text: String, code: String, large: Boolean) {
    val language = code.substringBefore('-')
    val rtl = language in setOf("ar", "fa", "ur", "he", "ps", "ckb")
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = if (large) 38.sp else 26.sp,
            lineHeight = if (large) 48.sp else 36.sp,
            fontWeight = FontWeight.Medium,
            textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr
        )
    )
}

/**
 * Shows only captions with locally finished text, as supplied by the caller.
 * A sentence can be shown across a table without Android share/copy or
 * persistence. Translation quality is not verified or implied.
 */
@Composable
internal fun FullscreenCaptionCard(
    captions: List<FocusCaptionLine>,
    initialId: Long,
    onDismiss: () -> Unit
) {
    val finished = captions.filter {
        (it.status == "Lokaal" || it.status == "Lokaal vertaald") &&
            it.original.isNotBlank()
    }
    var index by remember(initialId) {
        mutableIntStateOf(finished.indexOfFirst { it.id == initialId }.coerceAtLeast(0))
    }
    var side by remember { mutableStateOf(CaptionCardSide.BOTH) }
    var large by remember { mutableStateOf(true) }
    val safeIndex = index.coerceIn(0, (finished.size - 1).coerceAtLeast(0))
    val card = finished.getOrNull(safeIndex)
    val translation = card?.translated?.takeIf { it.isNotBlank() && it != card.original }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tuddy · Toon gesprekszin",
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text(
                    "Alleen deze tijdelijke sessie · geen kopie of verzending. " +
                        "Een lokale vertaling kan fouten bevatten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                if (card == null) {
                    Text("Geen afgeronde gesprekszin beschikbaar.")
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(onClick = { side = CaptionCardSide.BOTH }) {
                            Text(if (side == CaptionCardSide.BOTH) "Beide ✓" else "Beide")
                        }
                        TextButton(onClick = { side = CaptionCardSide.ORIGINAL }) {
                            Text(if (side == CaptionCardSide.ORIGINAL)
                                "Origineel ✓" else "Origineel")
                        }
                        TextButton(
                            enabled = translation != null,
                            onClick = { side = CaptionCardSide.TRANSLATION }
                        ) {
                            Text(if (side == CaptionCardSide.TRANSLATION)
                                "Vertaling ✓" else "Vertaling")
                        }
                    }
                    TextButton(onClick = { large = !large }) {
                        Text(if (large) "Kleinere letters" else "Grotere letters")
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        if (side != CaptionCardSide.TRANSLATION || translation == null) {
                            Text("Origineel · " + card.sourceCode.uppercase(),
                                style = MaterialTheme.typography.labelLarge)
                            CaptionSide(card.original, card.sourceCode, large)
                        }
                        if (side != CaptionCardSide.ORIGINAL) {
                            HorizontalDivider()
                            if (translation != null) {
                                Text("Lokaal vertaald · " + card.targetCode.uppercase(),
                                    style = MaterialTheme.typography.labelLarge)
                                CaptionSide(translation, card.targetCode, large)
                            } else {
                                Text("Voor deze regel is geen afzonderlijke vertaling beschikbaar.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        card.personalMeaning?.let { own ->
                            HorizontalDivider()
                            Text("Mijn eigen uitleg · niet geverifieerd, niet gedeeld",
                                style = MaterialTheme.typography.labelLarge)
                            CaptionSide(own, card.targetCode, large)
                        }
                        if (card.speaker == "A" || card.speaker == "B") {
                            Text("Spreker " + card.speaker + " · handmatig gelabeld",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(enabled = safeIndex > 0, onClick = {
                        index = (safeIndex - 1).coerceAtLeast(0)
                        side = CaptionCardSide.BOTH
                    }) { Text("← Vorige") }
                    Text((if (card == null) 0 else safeIndex + 1).toString() +
                        " / " + finished.size,
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        enabled = safeIndex < finished.lastIndex,
                        onClick = {
                            index = safeIndex + 1
                            side = CaptionCardSide.BOTH
                        }
                    ) { Text("Volgende →") }
                }
            }
        }
    }
}
