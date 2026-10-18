package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleRetiming
import com.livelingo.android.PrivateSubtitleTimelineInspector
import com.livelingo.android.SubtitleTimelineFindingKind

/** Read-only inspector; selecting a finding navigates to the existing editor. */
@Composable
internal fun SubtitleTimelineInspectorDialog(
    cues: List<PrivateSubtitleCue>,
    onSelectCue: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val overview = remember(cues) { PrivateSubtitleTimelineInspector.analyze(cues) }
    var filter by remember { mutableStateOf<SubtitleTimelineFindingKind?>(null) }
    val matching = overview.findings.filter { filter == null || it.kind == filter }
    val visible = matching.take(160)
    val groups = listOf(
        null to "Alles",
        SubtitleTimelineFindingKind.OVERLAP to "Overlap",
        SubtitleTimelineFindingKind.SAME_TIMESPAN to "Zelfde tijd",
        SubtitleTimelineFindingKind.SHORT_GAP to "Kort gat",
        SubtitleTimelineFindingKind.LONG_GAP to "Lang gat",
        SubtitleTimelineFindingKind.BRIEF_CUE to "Korte cue",
        SubtitleTimelineFindingKind.FAST_READING to "Snel lezen",
        SubtitleTimelineFindingKind.LONG_TEXT_LINE to "Lange regel"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bekijk de ondertiteltijdlijn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alleen informatie uit je tijdelijke SRT/VTT. De signalen " +
                    "kunnen opzettelijke montage zijn: Tuddy vergelijkt niets " +
                    "met audio of video en wijzigt hier geen bestanden.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Overlappingen: " + overview.overlapCount +
                    " · zelfde tijdvak: " + overview.sameSpanCount)
                Text("Korte tussenruimtes: " + overview.shortGapCount +
                    " · lange tussenruimtes: " + overview.longGapCount +
                    " · heel korte regels: " + overview.briefCueCount)
                Text("Mogelijk te snel leesbaar: " + overview.fastReadingCount +
                    " · tekstregels langer dan 42 tekens: " + overview.longTextLineCount)
                Text("Leesbaarheid is een ruwe tekst/tijdschatting (22 tekens/s), " +
                    "geen vastgestelde taal-, vertaal- of toegankelijkheidsscore. " +
                    "Tweetalige regels kunnen bewust meer tekst bevatten.",
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                LazyColumn(modifier = Modifier.heightIn(max = 90.dp)) {
                    items(groups) { pair ->
                        TextButton(onClick = { filter = pair.first }) {
                            Text((if (filter == pair.first) "✓ " else "") + pair.second)
                        }
                    }
                }
                if (visible.isEmpty()) {
                    Text("Geen overeenkomstige aandachtspunten in dit document.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Tik een regel om hem in de editor te openen.",
                        style = MaterialTheme.typography.labelSmall)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(visible) { item ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onSelectCue(item.cueIndex) }
                            ) {
                                Column {
                                    Text("Regel " + (item.cueIndex + 1) + " · " +
                                        PrivateSubtitleRetiming.label(item.timeMillis),
                                        style = MaterialTheme.typography.labelLarge)
                                    Text(item.explanation,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                if (matching.size > visible.size) {
                    Text("De eerste " + visible.size + " signalen van deze selectie " +
                        "worden getoond; de telling bevat ook de overige.",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
