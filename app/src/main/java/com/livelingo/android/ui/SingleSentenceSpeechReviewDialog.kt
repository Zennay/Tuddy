package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.OfflineTypedCaptionPolicy
import com.livelingo.android.OfflineTextModelInventory
import com.livelingo.android.OfflineTextStage
import com.livelingo.android.TargetCatalog

/**
 * One-shot Android on-device result is a PRIVATE DRAFT until explicitly
 * confirmed. Never persist, translate, share or enqueue speech automatically.
 * Alternate hypotheses are unverified Android suggestions, not predictions
 * made or corrected by Tuddy.
 */
@Composable
internal fun SingleSentenceSpeechReviewDialog(
    original: String,
    sourceCode: String,
    targetCode: String,
    alternatives: List<String>,
    lowAndroidConfidencePercent: Int? = null,
    onConfirm: (original: String, reviewedSource: String, reviewedTarget: String) -> Boolean,
    onDiscard: () -> Unit
) {
    var draft by remember(original, sourceCode, targetCode) { mutableStateOf(original) }
    var reviewedSource by remember(original, sourceCode, targetCode) { mutableStateOf(sourceCode) }
    var reviewedTarget by remember(original, sourceCode, targetCode) { mutableStateOf(targetCode) }
    var selection by remember(original, sourceCode, targetCode) { mutableStateOf<String?>(null) }
    var notice by remember(original, sourceCode, targetCode) { mutableStateOf("") }
    var modelNotice by remember(reviewedSource, reviewedTarget) {
        mutableStateOf("Lokale tekstmodellen nog niet gecontroleerd.")
    }
    // Read-only model check: never initiates a download or sends the sentence.
    // New pair checks invalidate previous callbacks; disposal cancels the
    // publication of late receipts into a discarded private review dialog.
    val inventory = remember { OfflineTextModelInventory() }
    LaunchedEffect(reviewedSource, reviewedTarget) {
        inventory.check(reviewedSource, reviewedTarget, TargetCatalog.codes) { receipt ->
            if (receipt.source == reviewedSource && receipt.target == reviewedTarget) {
                modelNotice = when (receipt.stage) {
                    OfflineTextStage.READY -> "Geïnstalleerde tekstmodellen beschikbaar. " + receipt.detail
                    OfflineTextStage.CHECKING -> "Geïnstalleerde offline tekstmodellen controleren…"
                    OfflineTextStage.MISSING -> "Tekstmodel ontbreekt. " + receipt.detail
                    else -> "Lokale modelstatus niet bevestigd. " + receipt.detail
                }
            }
        }
    }
    DisposableEffect(inventory) {
        onDispose { inventory.invalidate() }
    }
    val normalized = OfflineTypedCaptionPolicy.normalize(draft)

    AlertDialog(
        onDismissRequest = onDiscard,
        title = { Text("Controleer je herkende zin") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "De microfoon is gestopt. Dit is alleen een tijdelijke " +
                        "Android-herkenningssuggestie, nog geen gespreksregel. " +
                        "Pas de tekst aan, kies eventueel een ander voorstel, " +
                        "of gooi de zin weg.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (lowAndroidConfidencePercent != null) {
                    Text(
                        "⚠ Android gaf deze herkende zin een lage, niet-gekalibreerde " +
                            "zekerheidsscore (${lowAndroidConfidencePercent}%). " +
                            "Vergelijk zelf met wat er is gezegd voordat je bevestigt. " +
                            "Een hogere score zou evenmin juistheid bewijzen.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        "Android gaf geen lage, bruikbaar gerapporteerde score. " +
                            "Dat is geen bewijs van correcte spraakherkenning.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Voorgestelde tekstrichting: ${sourceCode.uppercase()} → " +
                        "${targetCode.uppercase()}. Controleer de taal van de zin zelf; " +
                        "Android-herkenning en tekstmodellen zijn afzonderlijk.",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selection = "bron" }) {
                        Text("Bron: ${reviewedSource.uppercase()} · kies")
                    }
                    TextButton(onClick = { selection = "doel" }) {
                        Text("Doel: ${reviewedTarget.uppercase()} · kies")
                    }
                }
                if (reviewedSource == reviewedTarget) {
                    Text("Gelijke teksttalen: deze zin wordt zonder vertaling overgenomen.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(modelNotice, style = MaterialTheme.typography.bodySmall)
                }
                if (reviewedSource != sourceCode || reviewedTarget != targetCode) {
                    TextButton(onClick = {
                        reviewedSource = sourceCode
                        reviewedTarget = targetCode
                    }) { Text("Herstel oorspronkelijke taalrichting") }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(OfflineTypedCaptionPolicy.MAX_INPUT_UNITS) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    label = { Text("Jouw definitieve oorspronkelijke tekst") }
                )
                if (alternatives.isNotEmpty()) {
                    Text("Andere niet-geverifieerde Android-voorstellen:",
                        style = MaterialTheme.typography.labelSmall)
                    alternatives.take(3).forEach { candidate ->
                        TextButton(
                            onClick = { draft = candidate },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(candidate, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (draft != original) {
                    TextButton(onClick = { draft = original }) {
                        Text("Herstel eerste herkenningsvoorstel")
                    }
                }
                if (notice.isNotBlank()) Text(notice,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text(
                    "Alleen na Bevestig verschijnt de zin in het tijdelijke gesprek " +
                        "en controleert Tuddy het al aanwezige lokale tekstmodel. " +
                        "Er wordt niets gedownload.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalized != null,
                onClick = {
                    if (!onConfirm(draft, reviewedSource, reviewedTarget)) {
                        notice = "Deze zin kon niet veilig worden toegevoegd. " +
                            "Je tekst blijft hier staan."
                    }
                }
            ) { Text("Bevestig en vertaal lokaal") }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) { Text("Gooi zin weg") }
        }
    )
    selection?.let { which ->
        ReviewedSentenceLanguagePicker(
            which = which,
            selectedCode = if (which == "bron") reviewedSource else reviewedTarget,
            onSelect = { code ->
                if (code in TargetCatalog.codes) {
                    if (which == "bron") reviewedSource = code
                    else reviewedTarget = code
                    notice = ""
                }
            },
            onDismiss = { selection = null }
        )
    }
}
