package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleMultiAnchor
import com.livelingo.android.PrivateSubtitleManualPairSelection
import com.livelingo.android.PrivateSubtitleTrackMultiAnchorAlignment

/**
 * Every matching cue is selected by the user; browsing never commits a match.
 * Only the immutable original source cues are shown here, not the retimed copy.
 */
@Composable
internal fun SubtitleTrackMultiAnchorSelectorDialog(
    primary: List<PrivateSubtitleCue>,
    secondary: List<PrivateSubtitleCue>,
    initial: List<PrivateSubtitleMultiAnchor>?,
    onChoose: (List<PrivateSubtitleMultiAnchor>) -> Unit,
    onDismiss: () -> Unit
) {
    var anchors by remember(primary, secondary) {
        mutableStateOf(initial ?: listOf(
            PrivateSubtitleMultiAnchor(1, 1),
            PrivateSubtitleMultiAnchor(primary.size, secondary.size)
        ))
    }
    var primaryInput by remember(primary, secondary) { mutableStateOf("") }
    var anchorExpression by remember(primary, secondary) { mutableStateOf("") }
    var editingIndex by remember(primary, secondary) { mutableStateOf<Int?>(null) }
    var secondaryInput by remember(primary, secondary) { mutableStateOf("") }

    val firstRow = primaryInput.toIntOrNull()
    val secondRow = secondaryInput.toIntOrNull()
    val candidate = if (firstRow != null && secondRow != null &&
        firstRow in 1..primary.size && secondRow in 1..secondary.size
    ) PrivateSubtitleMultiAnchor(firstRow, secondRow) else null
    val editAt = editingIndex
    val candidateAnchors = candidate?.let { pair ->
        (if (editAt != null && editAt in anchors.indices) {
            anchors.mapIndexed { index, saved ->
                if (index == editAt) pair else saved
            }
        } else anchors + pair).sortedBy { it.primaryRow }
    }
    // The full 500-cue dual-format snapshots should never be recomputed
    // just because the user scrolls, opens a source row or pages results.
    val additionalPreview = remember(primary, secondary, candidateAnchors) {
        if (candidateAnchors != null &&
            candidateAnchors.size <= PrivateSubtitleTrackMultiAnchorAlignment.MAX_ANCHORS
        ) PrivateSubtitleTrackMultiAnchorAlignment.preview(
            primary, secondary, candidateAnchors) else null
    }
    val preview = remember(primary, secondary, anchors) {
        PrivateSubtitleTrackMultiAnchorAlignment.preview(
            primary, secondary, anchors)
    }
    val mappedBulkRows = remember(anchorExpression, primary.size, secondary.size) {
        PrivateSubtitleManualPairSelection.parse(
            anchorExpression, primary.size, secondary.size)
            ?.takeIf {
                it.size in 2..PrivateSubtitleTrackMultiAnchorAlignment.MAX_ANCHORS
            }
            ?.map { PrivateSubtitleMultiAnchor(it.primaryRow, it.secondaryRow) }
    }
    val bulkPreview = remember(primary, secondary, mappedBulkRows) {
        mappedBulkRows?.let {
            PrivateSubtitleTrackMultiAnchorAlignment.preview(primary, secondary, it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tweede spoor · meerpunts tijdcorrectie") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Tuddy past het tweede, door jou gekozen spoor " +
                    "stuk voor stuk aan tussen 2–8 zelf aangewezen bij " +
                    "elkaar horende bronregelparen. Buiten de eerste " +
                    "en laatste ankers geldt het dichtstbijzijnde tempo. " +
                    "Dit is geen audio-uitlijning of taalherkenning.",
                    style = MaterialTheme.typography.bodySmall)
                Text("${anchors.size} gekozen ankers (maximaal 8). " +
                    "Kies ze zelf in beide originele sporen. " +
                    "De begin-/eindtijden worden pas na aparte " +
                    "definitieve toestemming in RAM toegepast.",
                    style = MaterialTheme.typography.bodySmall)

                anchors.forEachIndexed { index, pair ->
                    val firstCue = primary.getOrNull(pair.primaryRow - 1)
                    val secondCue = secondary.getOrNull(pair.secondaryRow - 1)
                    if (firstCue != null && secondCue != null) {
                        Text("Anker ${index + 1} · spoor 1, bronregel " +
                            "${pair.primaryRow} · ${firstCue.startLabel}\n" +
                            firstCue.text + "\nSpoor 2, bronregel " +
                            "${pair.secondaryRow} · ${secondCue.startLabel}\n" +
                            secondCue.text,
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                editingIndex = index
                                primaryInput = pair.primaryRow.toString()
                                secondaryInput = pair.secondaryRow.toString()
                            }
                        ) { Text("Wijzig beide oorspronkelijke rijen van anker ${index + 1}") }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = anchors.size > 2,
                            onClick = {
                                anchors = anchors.filterIndexed { i, _ -> i != index }
                                editingIndex = null
                                primaryInput = ""
                                secondaryInput = ""
                            }
                        ) { Text("Verwijder alleen anker ${index + 1}") }
                    }
                }
                Text("Voor lange sporen: je kunt ook alle ankers in één " +
                    "bewust getypte reeks opgeven, bijvoorbeeld " +
                    "1:1,100:99,240:237,500:497. Dit zijn exact " +
                    "de oorspronkelijke bronnummers, geen tijdcodes.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = anchorExpression,
                    onValueChange = { anchorExpression = it.take(513) },
                    label = { Text("Vervang ankerreeks · spoor1:spoor2, …") },
                    minLines = 1, maxLines = 3
                )
                if (anchorExpression.isNotBlank() && bulkPreview == null) {
                    Text("Ongeldige ankerreeks. Kies 2–8 strikt " +
                        "oplopende, unieke, bestaande bronparen. Elk " +
                        "segment moet binnen tijd- en schaalgrenzen " +
                        "vallen en het hele tweede spoor exporteerbaar maken.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (bulkPreview != null) {
                    Text("Volledige oorspronkelijke ankervoorstel: " +
                        "${bulkPreview.anchors.size} bronparen. " +
                        "Alle eigen teksten hieronder vooraf beoordelen:",
                        style = MaterialTheme.typography.titleSmall)
                    bulkPreview.anchors.forEachIndexed { index, pair ->
                        val original = primary[pair.primaryRow - 1]
                        val incoming = secondary[pair.secondaryRow - 1]
                        Text("Nieuw anker ${index + 1}: spoor 1 " +
                            "bronregel ${pair.primaryRow} " +
                            "${original.startLabel}\n${original.text}\n" +
                            "Spoor 2 bronregel ${pair.secondaryRow} " +
                            "${incoming.startLabel}\n${incoming.text}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val freshRows = PrivateSubtitleManualPairSelection.parse(
                                anchorExpression, primary.size, secondary.size)
                                ?.takeIf { rows ->
                                    rows.size in 2..
                                        PrivateSubtitleTrackMultiAnchorAlignment.MAX_ANCHORS
                                }
                                ?.map {
                                    PrivateSubtitleMultiAnchor(it.primaryRow, it.secondaryRow)
                                }
                            val fresh = freshRows?.let {
                                PrivateSubtitleTrackMultiAnchorAlignment.preview(
                                    primary, secondary, it)
                            }
                            if (fresh != null && fresh == bulkPreview &&
                                freshRows != null) {
                                anchors = freshRows
                                editingIndex = null
                                primaryInput = ""
                                secondaryInput = ""
                                anchorExpression = ""
                            }
                        }
                    ) { Text("Vervang alle ankers bewust door deze reeks") }
                }
                Text("Voeg een extra overeenkomst tussen bestaande " +
                    "ankers toe, of tik bij een bestaand anker op Wijzig. " +
                    "Alles vereist stijgende originele bronvolgorde.",
                    style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = primaryInput,
                        onValueChange = {
                            primaryInput = it.filter { char ->
                                char in '0'..'9' }.take(4)
                        },
                        label = { Text("Spoor 1 · rij") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = secondaryInput,
                        onValueChange = {
                            secondaryInput = it.filter { char ->
                                char in '0'..'9' }.take(4)
                        },
                        label = { Text("Spoor 2 · rij") },
                        singleLine = true
                    )
                }
                MultiAnchorSourceBrowser(
                    label = "Origineel spoor 1",
                    cues = primary,
                    onChoose = { primaryInput = it.toString() }
                )
                MultiAnchorSourceBrowser(
                    label = "Origineel spoor 2",
                    cues = secondary,
                    onChoose = { secondaryInput = it.toString() }
                )
                if (candidate != null && additionalPreview != null) {
                    val a = primary[candidate.primaryRow - 1]
                    val b = secondary[candidate.secondaryRow - 1]
                    Text("Nieuw gekozen paar, nog niet toegevoegd:\n" +
                        "Spoor 1 · ${a.startLabel}\n${a.text}\n" +
                        "Spoor 2 · ${b.startLabel}\n${b.text}",
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = additionalPreview != null,
                    onClick = {
                        if (candidateAnchors != null &&
                            PrivateSubtitleTrackMultiAnchorAlignment.preview(
                                primary, secondary, candidateAnchors) != null
                        ) {
                            anchors = candidateAnchors
                            editingIndex = null
                            primaryInput = ""
                            secondaryInput = ""
                        }
                    }
                ) {
                    Text(if (editingIndex != null)
                        "Vervang alleen dit anker na controle"
                    else "Voeg dit bronankerpaar bewust toe")
                }

                if (preview == null) {
                    Text("Tijdlijn ongeldig. Elk anker moet in beide bronnen " +
                        "verderop liggen, met per anker maximaal ±600 s verschil " +
                        "en per segment tempo tussen 0,5× en 2×. " +
                        "De hele uitkomst moet als SRT én VTT geldig zijn.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    preview.segmentSourceSpansMs.indices.forEach { index ->
                        Text("Segment ${index + 1} → ${index + 2}: " +
                            "tweede spoor ${preview.segmentSourceSpansMs[index]} ms " +
                            "wordt eerste spoor " +
                            "${preview.segmentTargetSpansMs[index]} ms.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Na overnemen volgt de volledige gecombineerde " +
                        "preview met alle losse bronregels. Pas de aparte " +
                        "bevestiging verandert het tijdelijke concept.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null, onClick = {
                val fresh = PrivateSubtitleTrackMultiAnchorAlignment.preview(
                    primary, secondary, anchors)
                if (fresh != null && preview != null &&
                    fresh.adjustedSecondary == preview.adjustedSecondary &&
                    fresh.anchors == preview.anchors) onChoose(anchors.toList())
            }) { Text("Neem alle tijdankers over") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

@Composable
private fun MultiAnchorSourceBrowser(
    label: String,
    cues: List<PrivateSubtitleCue>,
    onChoose: (Int) -> Unit
) {
    var search by remember(cues) { mutableStateOf("") }
    var page by remember(cues) { mutableIntStateOf(0) }
    var expanded by remember(cues) { mutableStateOf(emptySet<Int>()) }
    val found = cues.withIndex().filter {
        search.isBlank() || it.value.text.contains(search.trim(), ignoreCase = true)
    }
    val pages = maxOf(1, (found.size + 2) / 3)
    val currentPage = page.coerceIn(0, pages - 1)
    Text("$label · ${found.size} van ${cues.size} regels · " +
        "pagina ${currentPage + 1}/$pages",
        style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(), value = search,
        onValueChange = { search = it.take(100); page = 0 },
        label = { Text("Zoek eigen originele zinsdelen") },
        singleLine = true
    )
    if (found.isEmpty()) Text("Geen originele bronregels gevonden.",
        style = MaterialTheme.typography.bodySmall)
    found.drop(currentPage * 3).take(3).forEach { item ->
        val row = item.index + 1
        val cue = item.value
        val showAll = row in expanded
        Text("Originele rij $row · ${cue.startLabel} → ${cue.endLabel}\n" +
            (if (showAll) cue.text else cue.text.replace('\n', ' ').take(95)),
            style = MaterialTheme.typography.bodySmall)
        if (cue.text.length > 95 || cue.text.contains('\n')) {
            TextButton(onClick = {
                expanded = if (showAll) expanded - row else expanded + row
            }) { Text(if (showAll) "Verkort bronregel" else "Lees volledige bronregel") }
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(),
            onClick = { onChoose(row) }) {
            Text("Neem oorspronkelijke regel $row over (nog niet koppelen)")
        }
    }
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(enabled = currentPage > 0,
            onClick = { page = currentPage - 1 }) { Text("← Vorige $label") }
        TextButton(enabled = currentPage < pages - 1,
            onClick = { page = currentPage + 1 }) { Text("Volgende →") }
    }
}
