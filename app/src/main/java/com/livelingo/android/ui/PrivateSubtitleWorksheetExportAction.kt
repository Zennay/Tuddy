package com.livelingo.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleRecallWorksheet

private data class PendingSubtitleWorksheet(
    val cues: List<PrivateSubtitleCue>,
    val bytes: ByteArray
)

/** Requires two explicit decisions: export button and Android document location. */
@Composable
internal fun PrivateSubtitleWorksheetExportAction(
    selected: List<PrivateSubtitleCue>
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<PendingSubtitleWorksheet?>(null) }
    var notice by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val previous = pending
        pending = null
        if (uri != null && previous != null) {
            val fresh = PrivateSubtitleRecallWorksheet.snapshot(selected.take(24))
            if (previous.cues != selected.take(24) || fresh == null ||
                !fresh.contentEquals(previous.bytes)
            ) {
                notice = "De ondertitelselectie is gewijzigd. Kies opnieuw exporteren."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(previous.bytes)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                notice = if (saved) "Het offline oefenblad is opgeslagen."
                    else "Opslaan mislukt; je ondertitelbestand bleef ongewijzigd."
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = selected.isNotEmpty(),
            onClick = {
                val chosen = selected.take(24).toList()
                val bytes = PrivateSubtitleRecallWorksheet.snapshot(chosen)
                if (bytes == null) {
                    notice = "Ongeldige ondertitelregels; kan geen veilig oefenblad maken."
                } else {
                    pending = PendingSubtitleWorksheet(chosen, bytes)
                    notice = ""
                    create.launch("Tuddy-ondertitels-oefenblad.html")
                }
            }
        ) { Text("▤ Bewaar geselecteerde regels als oefenblad…") }
        Text(
            "Maximaal 24 gevonden regels: oefenvragen en een aparte " +
                "antwoordpagina. Alleen na bewuste Android-bestandskeuze. " +
                "De provider kan het document extern synchroniseren.",
            style = MaterialTheme.typography.bodySmall
        )
        if (notice.isNotBlank()) Text(notice,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary)
    }
}
