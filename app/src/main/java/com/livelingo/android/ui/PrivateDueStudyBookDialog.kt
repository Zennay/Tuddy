package com.livelingo.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateCombinedStudyBook
import com.livelingo.android.PrivateDailyPracticeWorksheet
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateVocabularyDeck
import java.time.LocalDate

/** Selected categories and their exact, temporary pre-picker HTML bytes. */
private data class PendingDailyStudyBook(
    val groups: Set<String>, val worksheet: Boolean, val snapshot: ByteArray
)

/**
 * Explicitly export only due-today cards from separately consented schedules.
 * Never exports a transcript or enables any plan. Uses the already bounded,
 * escaping, script-free combined study booklet renderer.
 */
@Composable
internal fun PrivateDueStudyBookDialog(
    words: PrivateVocabularyDeck,
    phrases: PrivatePhraseDeck,
    ownWords: PrivateOwnWordDeck,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val today = LocalDate.now().toEpochDay()
    val enabled = buildSet {
        if (words.reviewsEnabled()) add("curated")
        if (phrases.reviewsEnabled()) add("phrases")
        if (ownWords.reviewsEnabled()) add("own")
    }
    var selected by remember { mutableStateOf(enabled) }
    var worksheetMode by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingDailyStudyBook?>(null) }
    var notice by remember { mutableStateOf("") }

    fun dueCounts(group: String): Int? = when (group) {
        "curated" -> words.scheduledReviews().count { it.dueDay <= today }
        "own" -> ownWords.scheduledReviews()?.count { it.dueDay <= today }
        "phrases" -> phrases.scheduledReviews()?.count { it.dueDay <= today }
        else -> null
    }

    fun produceSnapshot(chosen: Set<String>, asWorksheet: Boolean): ByteArray? {
        if (chosen.isEmpty() || chosen.any { it !in enabled }) return null
        val currentDay = LocalDate.now().toEpochDay()
        val curated = if ("curated" in chosen)
            words.scheduledReviews().filter { it.dueDay <= currentDay }.map { it.saved }
        else emptyList()
        val owned = if ("own" in chosen)
            ownWords.scheduledReviews()?.filter { it.dueDay <= currentDay }?.map { it.entry }
        else emptyList()
        val personal = if ("phrases" in chosen)
            phrases.scheduledReviews()?.filter { it.dueDay <= currentDay }?.map { it.phrase }
        else emptyList()
        return if (asWorksheet) PrivateDailyPracticeWorksheet.snapshot(
            curated, owned, personal
        ) else PrivateCombinedStudyBook.snapshot(curated, owned, personal)
    }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val original = pending
        pending = null
        if (uri != null && original != null) {
            val fresh = produceSnapshot(original.groups, original.worksheet)
            if (fresh == null || !fresh.contentEquals(original.snapshot)) {
                notice = "De selectie of herhaaldatum is gewijzigd. Maak een nieuw studieblad."
            } else {
                val written = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(original.snapshot)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                notice = if (written) "Studieblad opgeslagen op jouw gekozen locatie."
                    else "Opslaan mislukt; je eigen woorden en planning blijven intact."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bewaar mijn kaarten van vandaag") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Maak een zelfstandig afdrukbaar HTML-blad van alleen de kaarten " +
                        "die op dit toestel vandaag aan de beurt zijn. Niet-geplande " +
                        "woorden of zinnen blijven buiten het bestand.",
                    style = MaterialTheme.typography.bodySmall
                )
                listOf(
                    Triple("curated", "Bewaarde basiswoorden", "Kleine ingebouwde betekenissen"),
                    Triple("own", "Mijn eigen woordbetekenissen", "Zelfgeschreven, niet geverifieerd"),
                    Triple("phrases", "Mijn eigen gesprekszinnen", "Zelfgeschreven, niet geverifieerd")
                ).forEach { (group, label, detail) ->
                    val count = if (group in enabled) dueCounts(group) else null
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = group in enabled && count != null && count > 0,
                        onClick = {
                            selected = if (group in selected) selected - group
                                else selected + group
                        }
                    ) {
                        Text(
                            (if (group in selected) "☑ " else "☐ ") +
                                "$label · " + (count?.toString() ?: "uit/niet leesbaar")
                        )
                    }
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
                Text("Kies leesboek of oefenblad",
                    style = MaterialTheme.typography.labelMedium)
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { worksheetMode = !worksheetMode }
                ) {
                    Text(if (worksheetMode) "✓ Vragen en antwoorden op aparte pagina's"
                        else "✓ Leesboek: vraag en betekenis naast elkaar")
                }
                Text(
                    if (worksheetMode)
                        "Op het oefenblad staan de vragen eerst met schrijfruimte; " +
                            "de eigen betekenissen staan op een aparte antwoordpagina."
                    else "Het leesboek toont elke bewaarde vraag direct met betekenis.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Alleen zelf bewaarde gegevens. Voor eigen woorden en zinnen " +
                        "zijn dit jouw betekenissen, zonder native-speakercontrole. " +
                        "Er worden geen scores of herhaaldatums geëxporteerd.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Let op: je kiest een externe Android-bestandsprovider. " +
                        "Die kan jouw teksten synchroniseren of delen. Een extern " +
                        "bestand verdwijnt niet wanneer je Tuddy-gegevens wist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    enabled = selected.isNotEmpty() && selected.any {
                        (dueCounts(it) ?: 0) > 0
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val snapshot = produceSnapshot(selected, worksheetMode)
                        if (snapshot == null) {
                            notice = "De selectie is leeg, gewijzigd of niet veilig leesbaar."
                        } else {
                            pending = PendingDailyStudyBook(
                                selected.toSet(), worksheetMode, snapshot
                            )
                            notice = ""
                            createDocument.launch(if (worksheetMode)
                                "Tuddy-oefenblad-vandaag.html"
                            else "Tuddy-kaarten-vandaag.html")
                        }
                    }
                ) { Text("Ik begrijp dit, kies opslag…") }
                if (notice.isNotBlank()) Text(notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
