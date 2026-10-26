package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.OfflineTextReceipt
import com.livelingo.android.OfflineTextStage
import com.livelingo.android.TargetCatalog
import com.livelingo.android.TypedFavoriteModelBatchController
import com.livelingo.android.TypedLanguagePair

/**
 * User-directed offline readiness dashboard for saved TEXT pairs. Reading
 * installed model inventory is automatic and read-only; the Wi-Fi button is
 * behind a separate explicit consent with the actual immutable pair list.
 */
@Composable
internal fun TypedFavoriteOfflineModelsDialog(
    savedPairs: List<TypedLanguagePair>,
    onDismiss: () -> Unit
) {
    val pairs = remember(savedPairs) { savedPairs.take(6).toList() }
    val modelBatch = remember { TypedFavoriteModelBatchController() }
    val receipts = remember { mutableStateMapOf<TypedLanguagePair, OfflineTextReceipt>() }
    var busy by remember { mutableStateOf(false) }
    var confirmWifi by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    DisposableEffect(modelBatch) {
        onDispose { modelBatch.cancel() }
    }

    fun inspectAll() {
        if (busy) return
        busy = true
        notice = "Alleen lokaal opgeslagen tekstmodelinformatie bekijken…"
        receipts.clear()
        modelBatch.inspect(
            pairs = pairs,
            onReceipt = { pair, receipt -> receipts[pair] = receipt },
            onFinished = { message ->
                busy = false
                notice = message
            }
        )
    }
    LaunchedEffect(pairs) { inspectAll() }

    fun close() {
        modelBatch.cancel()
        busy = false
        confirmWifi = false
        onDismiss()
    }
    if (confirmWifi) {
        AlertDialog(
            onDismissRequest = { confirmWifi = false },
            title = { Text("Ontbrekende tekstmodellen via wifi voorbereiden?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Je kiest zelf om alle ${pairs.size} bewaarde teksttaalparen " +
                        "achtereenvolgens te controleren. Alleen ontbrekende " +
                        "taalmodellen worden zo nodig via wifi gedownload. " +
                        "Deze download kan netwerkverkeer en opslagruimte kosten.")
                    pairs.forEach { pair ->
                        Text("${pair.from.uppercase()} → ${pair.to.uppercase()}",
                            style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Een modelcontrole heeft maximaal 12 seconden voor een antwoord. " +
                        "Na twee minuten wachten op een download meldt Tuddy de " +
                        "status als onbekend en stopt de reeks. De al door ML Kit " +
                        "gestarte download kan nog doorgaan. Tuddy start geen tweede " +
                        "download totdat de echte vorige Android-taak is afgerond. " +
                        "Sluiten stopt alleen nieuwe voorbereidingen. " +
                        "Dit installeert GEEN offline spraakherkenning.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirmWifi = false
                    busy = true
                    receipts.clear()
                    notice = "Alleen na jouw toestemming: modellen via wifi voorbereiden…"
                    modelBatch.prepareMissingViaWifi(
                        pairs = pairs,
                        onReceipt = { pair, receipt -> receipts[pair] = receipt },
                        onFinished = { message ->
                            busy = false
                            notice = message
                        }
                    )
                }) { Text("Ja, bereid ontbrekende modellen via wifi voor") }
            },
            dismissButton = {
                TextButton(onClick = { confirmWifi = false }) {
                    Text("Annuleren")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { close() },
        title = { Text("Mijn favoriete tekstrichtingen · offline") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Controleer of beide lokale ML Kit-teksttalen voor elk van " +
                    "je bewaarde richtingen aanwezig zijn. Favoriet betekent " +
                    "nooit dat offline modellen of spraakpakketten klaarstaan.",
                    style = MaterialTheme.typography.bodySmall)
                if (pairs.isEmpty()) {
                    Text("Nog geen favoriete tekstrichtingen. Bewaar eerst een " +
                        "paar in het tweeweg-tekstgesprek.")
                } else {
                    pairs.forEach { pair ->
                        val receipt = receipts[pair]
                        val stage = receipt?.stage
                        Text("${pair.from.uppercase()} → ${pair.to.uppercase()}",
                            style = MaterialTheme.typography.titleSmall)
                        Text(when (stage) {
                            OfflineTextStage.READY ->
                                "✓ Beide tekstmodellen lokaal beschikbaar"
                            OfflineTextStage.MISSING ->
                                "Ontbrekend: " + receipt.detail
                            OfflineTextStage.CHECKING ->
                                "Controleren… geen download"
                            OfflineTextStage.UNSUPPORTED ->
                                "Dit tekstpaar wordt niet ondersteund"
                            OfflineTextStage.UNKNOWN ->
                                "Lokale beschikbaarheid kon niet worden bevestigd"
                            else ->
                                "Nog niet gecontroleerd"
                        }, style = MaterialTheme.typography.bodySmall,
                            color = if (stage == OfflineTextStage.MISSING ||
                                stage == OfflineTextStage.UNSUPPORTED ||
                                stage == OfflineTextStage.UNKNOWN)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                    }
                }
                Text(notice, style = MaterialTheme.typography.bodySmall)
                TextButton(
                    enabled = !busy && pairs.isNotEmpty(),
                    onClick = { inspectAll() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Controleren…" else "↻ Controleer alle favorieten opnieuw") }
                Button(
                    enabled = !busy && pairs.isNotEmpty(),
                    onClick = { confirmWifi = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Bereid ontbrekende modellen via wifi voor…") }
                Text("Modelcontrole leest alleen dit toestel. Downloads vereisen " +
                    "de aparte bevestiging hierboven. Werkt een taal niet, dan " +
                    "is er geen online fallback en wordt een mislukte voorbereiding " +
                    "niet als geslaagd gemeld. Deze status wordt niet opgeslagen.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Kurmanji, Sorani en Pasjtoe zijn geen ondersteunde vrije " +
                    "ML Kit-teksttalen. Fysieke offline spraak- en taalacceptatie " +
                    "is een afzonderlijk releasevereiste.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { close() }) { Text("Terug naar tweeweg-gesprek") }
        }
    )
}
