package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateVocabularyDeck
import com.livelingo.android.ScheduledWordReview
import com.livelingo.android.DuePersonalPhrase
import com.livelingo.android.ScheduledOwnWord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A read-only eight-week look-ahead over three separately consented local
 * study plans. No worker, notification, model, transcript, data mutation or
 * profile. Overdue cards are counted once in TODAY, not shown on past dates.
 */
@Composable
internal fun OfflineStudyCalendarDialog(
    words: PrivateVocabularyDeck,
    phrases: PrivatePhraseDeck,
    ownWords: PrivateOwnWordDeck,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val epochToday = today.toEpochDay()
    var weekOffset by remember { mutableIntStateOf(0) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("nl-NL")) }
    val display: Map<Long, IntArray>? = remember(words, phrases, ownWords, today) {
        val curated: List<ScheduledWordReview> = if (words.reviewsEnabled())
            words.scheduledReviews() else emptyList()
        val personal: List<DuePersonalPhrase>? = if (phrases.reviewsEnabled())
            phrases.scheduledReviews() else emptyList()
        val own: List<ScheduledOwnWord>? = if (ownWords.reviewsEnabled())
            ownWords.scheduledReviews() else emptyList()
        if (personal == null || own == null) null else {
            val byDay = mutableMapOf<Long, IntArray>()
            fun add(day: Long, kind: Int) {
                val effective = day.coerceAtLeast(epochToday)
                byDay.getOrPut(effective) { IntArray(3) }[kind]++
            }
            curated.forEach { add(it.dueDay, 0) }
            personal.forEach { add(it.dueDay, 1) }
            own.forEach { add(it.dueDay, 2) }
            byDay
        }
    }
    val anyPlan = words.reviewsEnabled() ||
        phrases.reviewsEnabled() || ownWords.reviewsEnabled()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn offline studieplanning") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 525.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Bekijk wanneer bestaande privékaarten weer aan de beurt zijn. " +
                        "Eerdere, niet beoordeelde kaarten tellen alleen onder vandaag. " +
                        "Deze kalender stelt niets in en stuurt geen herinneringen.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!anyPlan) {
                    Text("Alle herhaalplanningen staan uit. Kies afzonderlijk toestemming " +
                        "in je privéwoordenlijst of eigen zinnen voordat je een agenda ziet.")
                } else if (display == null) {
                    Text(
                        "Een lokale privéwoorden- of zinnenlijst kon niet veilig worden " +
                            "gelezen. Herstel deze eerst in de eigen lijst. Er is niets gewijzigd.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextButton(
                            enabled = weekOffset > 0,
                            onClick = { weekOffset-- }
                        ) { Text("← Vorige 7 dagen") }
                        TextButton(
                            enabled = weekOffset < 7,
                            onClick = { weekOffset++ }
                        ) { Text("Volgende 7 dagen →") }
                    }
                    Text(
                        "Overzicht ${weekOffset + 1} van 8 · 7 dagen per overzicht",
                        style = MaterialTheme.typography.labelMedium
                    )
                    val first = today.plusDays(weekOffset * 7L)
                    for (dayIndex in 0..6) {
                        val day = first.plusDays(dayIndex.toLong())
                        val counts = display[day.toEpochDay()] ?: intArrayOf(0, 0, 0)
                        val total = counts.sum()
                        HorizontalDivider()
                        Text(
                            if (weekOffset == 0 && dayIndex == 0)
                                "Vandaag · ${day.format(formatter)}"
                            else day.format(formatter),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (total == 0) "Geen kaarten gepland."
                            else "$total kaarten · ${counts[0]} basiswoorden, " +
                                "${counts[1]} eigen zinnen, ${counts[2]} eigen woordbetekenissen",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val beyond = display.filterKeys { it >= epochToday + 56 }
                        .values.sumOf { it.sum() }
                    if (weekOffset == 7 && beyond > 0) {
                        Text(
                            "Nog $beyond kaarten liggen meer dan acht weken vooruit. " +
                                "Deze kalender verandert hun geplande datum niet.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "De aantallen volgen uitsluitend de lokale datum van het toestel. " +
                            "Na bewust beoordelen moet je dit overzicht opnieuw openen " +
                            "om de nieuwste datums te zien.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    PrivateStudyCalendarExportAction(
                        words = words,
                        ownWords = ownWords,
                        phrases = phrases
                    )
                }
                Text(
                    "Eigen woorden en zinnen zijn door jou geschreven, niet " +
                        "onafhankelijk gecontroleerd. Geen officiële leerbeoordeling, " +
                        "geen cloudsynchronisatie en geen automatische meldingen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar studieoverzicht") }
        }
    )
}
