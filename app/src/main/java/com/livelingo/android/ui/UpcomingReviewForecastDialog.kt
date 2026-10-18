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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.PrivateStudyCalendar
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateVocabularyDeck
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Live, read-only forecast from existing opted-in, app-private schedules.
 * A rating may shift the forecast; it is never a notification or a commitment.
 */
private data class ForecastDay(
    val date: LocalDate,
    val curated: Int,
    val own: Int,
    val phrases: Int
) {
    val total: Int get() = curated + own + phrases
}

private data class Forecast(
    val days: List<ForecastDay>,
    val outsideWindow: Int,
    val curatedEnabled: Boolean,
    val ownEnabled: Boolean,
    val phraseEnabled: Boolean
)

private fun forecast(
    words: PrivateVocabularyDeck,
    own: PrivateOwnWordDeck,
    phrases: PrivatePhraseDeck
): Forecast? {
    val curatedEnabled = words.reviewsEnabled()
    val ownEnabled = own.reviewsEnabled()
    val phraseEnabled = phrases.reviewsEnabled()
    val curated = if (curatedEnabled) words.scheduledReviews().map { it.dueDay }
        else emptyList()
    val personalWords = if (ownEnabled) own.scheduledReviews()?.map { it.dueDay }
        else emptyList()
    val personalPhrases = if (phraseEnabled) phrases.scheduledReviews()?.map { it.dueDay }
        else emptyList()
    // Missing opted-in storage cannot be shown as an empty success state.
    if (personalWords == null || personalPhrases == null) return null
    val today = LocalDate.now()
    val start = today.toEpochDay()
    val days = (0L..13L).map { offset ->
        val day = start + offset
        fun match(due: Long): Boolean =
            if (offset == 0L) due <= day else due == day
        ForecastDay(
            today.plusDays(offset),
            curated.count(::match),
            personalWords.count(::match),
            personalPhrases.count(::match)
        )
    }
    val end = start + 13
    return Forecast(
        days,
        curated.count { it > end } + personalWords.count { it > end } +
            personalPhrases.count { it > end },
        curatedEnabled, ownEnabled, phraseEnabled
    )
}

/** Shows real due dates only; it never opens or changes a review plan. */
@Composable
internal fun UpcomingReviewForecastDialog(
    words: PrivateVocabularyDeck,
    ownWords: PrivateOwnWordDeck,
    phrases: PrivatePhraseDeck,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var exportNotice by remember { mutableStateOf("") }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        val previous = pending
        pending = null
        if (uri != null && previous != null) {
            val current = PrivateStudyCalendar.snapshot(words, ownWords, phrases)
            if (current == null || !current.contentEquals(previous)) {
                exportNotice = "Herhaaldatums of kaarten zijn gewijzigd. Kies opnieuw exporteren."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(previous)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                exportNotice = if (saved) "Agenda-export opgeslagen."
                    else "Opslaan mislukt; je herhaalplanning is niet veranderd."
            }
        }
    }
    val data = forecast(words, ownWords, phrases)
    val format = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("nl-NL"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn offline herhaalplanning · 14 dagen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 530.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Vooruitblik op kaarten die volgens jouw huidige lokale " +
                        "herhaalplannen aan de beurt komen. Niet automatisch aangezet; " +
                        "de dag kan veranderen zodra je jezelf beoordeelt.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (data == null) {
                    Text(
                        "Een ingeschakelde privéwoorden- of zinnenlijst kon niet worden " +
                            "gelezen. Er is niets veranderd en de planning wordt niet gegokt.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    val any = data.curatedEnabled || data.ownEnabled || data.phraseEnabled
                    if (!any) Text(
                        "Er staat nog geen herhaalplanning aan. Je kunt elk plan " +
                            "afzonderlijk inschakelen bij je eigen woorden of zinnen.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val total = data.days.sumOf { it.total }
                    if (any) {
                        Text(
                            "In deze twee weken: ${total} kaarten (inclusief achterstand).",
                            style = MaterialTheme.typography.titleMedium
                        )
                        data.days.forEachIndexed { index, day ->
                            HorizontalDivider()
                            Text(
                                (if (index == 0) "Vandaag en achterstand · " else "") +
                                    day.date.format(format) + " · ${day.total}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Basis ${day.curated} · Eigen woorden ${day.own} · " +
                                    "Eigen zinnen ${day.phrases}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = total > 0,
                            onClick = {
                                val snapshot = PrivateStudyCalendar.snapshot(
                                    words, ownWords, phrases
                                )
                                if (snapshot == null) {
                                    exportNotice = "Geen geldige vervaldatums om te exporteren."
                                } else {
                                    pending = snapshot
                                    exportNotice = ""
                                    documentPicker.launch("Tuddy-studieplanning.ics")
                                }
                            }
                        ) { Text("Bewaar alleen datums en aantallen als agenda…") }
                        Text(
                            "Je kiest zelf een externe Android-bestandsprovider; " +
                                "die kan dit kalenderbestand synchroniseren. " +
                                "Er staan geen woorden, zinnen of betekenissen in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (data.outsideWindow > 0) {
                            HorizontalDivider()
                            Text(
                                "Na deze twee weken: ${data.outsideWindow} kaart(en).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (exportNotice.isNotBlank()) Text(
                    exportNotice, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Alleen de datum en kaartgroep worden tijdelijk getoond. " +
                        "Dit is geen automatische herinnering, taaldiagnose of cloudsync. " +
                        "Eigen betekenissen zijn niet onafhankelijk geverifieerd.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
