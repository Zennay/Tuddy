package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.livelingo.android.PrivateOwnWordDeck
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateVocabularyDeck
import java.time.LocalDate

/**
 * A single device-only entry point for three separately consented study plans.
 * This screen never enables reviews, starts speech, or persists a transcript.
 */
@Composable
internal fun DailyStudyHub(
    words: PrivateVocabularyDeck,
    phrases: PrivatePhraseDeck,
    ownWords: PrivateOwnWordDeck,
    onManageWords: () -> Unit,
    onManagePhrases: () -> Unit,
    onManageOwnWords: () -> Unit,
    onWordChanged: () -> Unit,
    onOwnWordChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    var wordReviewOpen by remember { mutableStateOf(false) }
    var phraseReviewOpen by remember { mutableStateOf(false) }
    var ownWordReviewOpen by remember { mutableStateOf(false) }
    var dailyBookOpen by remember { mutableStateOf(false) }
    var viewRevision by remember { mutableIntStateOf(0) }

    if (wordReviewOpen) {
        ScheduledWordReviewPanel(
            deck = words,
            onChanged = { viewRevision++; onWordChanged() },
            onDismiss = { wordReviewOpen = false }
        )
        return
    }
    if (phraseReviewOpen) {
        ScheduledPhraseReviewPanel(
            deck = phrases,
            onChanged = { viewRevision++ },
            onDismiss = { phraseReviewOpen = false }
        )
        return
    }

    if (dailyBookOpen) {
        PrivateDueStudyBookDialog(
            words = words,
            phrases = phrases,
            ownWords = ownWords,
            onDismiss = { dailyBookOpen = false }
        )
        return
    }
    if (ownWordReviewOpen) {
        ScheduledOwnWordReviewPanel(
            deck = ownWords,
            onChanged = { viewRevision++; onOwnWordChanged() },
            onDismiss = { ownWordReviewOpen = false }
        )
        return
    }

    // Re-read only local app-private metadata after an intentional evaluation.
    viewRevision
    val today = LocalDate.now().toEpochDay()
    val wordPlanEnabled = words.reviewsEnabled()
    val phrasePlanEnabled = phrases.reviewsEnabled()
    val ownWordPlanEnabled = ownWords.reviewsEnabled()
    val wordReview = if (wordPlanEnabled) words.scheduledReviews() else emptyList()
    val phraseReview = if (phrasePlanEnabled) phrases.scheduledReviews() else emptyList()
    val ownWordReview = if (ownWordPlanEnabled) ownWords.scheduledReviews() else emptyList()
    val wordDue = wordReview.count { it.dueDay <= today }
    val phraseDue = phraseReview.orEmpty().count { it.dueDay <= today }
    val ownWordDue = ownWordReview.orEmpty().count { it.dueDay <= today }
    val totalDue = wordDue + phraseDue + ownWordDue
    val nearest = (wordReview.map { it.dueDay } +
        phraseReview.orEmpty().map { it.dueDay } +
        ownWordReview.orEmpty().map { it.dueDay })
        .filter { it > today }.minOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vandaag offline oefenen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Eén overzicht van drie aparte, vrijwillige herhaalplannen. " +
                        "Geen account, meldingen, taalmodel, opname of cloudsync.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "$totalDue kaarten voor vandaag",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (totalDue > 0) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { dailyBookOpen = true }
                    ) { Text("▤ Bewaar mijn kaarten van vandaag…") }
                    Text(
                        "Alleen na afzonderlijke keuze van kaartgroepen en " +
                            "een externe Android-opslaglocatie.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (totalDue == 0) {
                    Text(
                        if (nearest != null) "Volgende geplande herhaling: " +
                            LocalDate.ofEpochDay(nearest).toString()
                        else "Nog geen kaarten gepland. Je kunt hieronder zelf " +
                            "woorden of zinnen toevoegen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider()
                Text("Bewaarde basiswoorden",
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (wordPlanEnabled)
                        "$wordDue vandaag · ${wordReview.size} lokaal gepland"
                    else "Planning staat uit en wordt niet stilzwijgend ingeschakeld.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = wordDue > 0,
                    onClick = { wordReviewOpen = true }
                ) { Text("Oefen basiswoorden ($wordDue)") }
                TextButton(onClick = onManageWords) {
                    Text(if (wordPlanEnabled) "Beheer basiswoorden en planning"
                        else "Sla basiswoorden op / kies opt-in")
                }
                HorizontalDivider()
                Text("Mijn eigen Kurmanji-, Sorani- en Pasjtoe-zinnen",
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (phraseReview == null)
                        "De lokale lijst is niet leesbaar. Beheer of wis haar in Mijn eigen zinnen."
                    else if (phrasePlanEnabled)
                        "$phraseDue vandaag · ${phraseReview.size} lokaal gepland"
                    else "Planning staat uit en wordt niet stilzwijgend ingeschakeld.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phraseDue > 0 && phraseReview != null,
                    onClick = { phraseReviewOpen = true }
                ) { Text("Oefen eigen zinnen ($phraseDue)") }
                TextButton(onClick = onManagePhrases) {
                    Text(if (phrasePlanEnabled) "Beheer eigen zinnen en planning"
                        else "Voeg eigen zinnen toe / kies opt-in")
                }
                HorizontalDivider()
                Text("Mijn zelfgeschreven woordbetekenissen",
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (ownWordReview == null)
                        "De lokale lijst is niet leesbaar. Beheer deze in Mijn eigen woorden."
                    else if (ownWordPlanEnabled)
                        "$ownWordDue vandaag · ${ownWordReview.size} lokaal gepland"
                    else "Planning staat uit en wordt niet stilzwijgend ingeschakeld.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ownWordDue > 0 && ownWordReview != null,
                    onClick = { ownWordReviewOpen = true }
                ) { Text("Oefen eigen woordbetekenissen ($ownWordDue)") }
                TextButton(onClick = onManageOwnWords) {
                    Text(if (ownWordPlanEnabled)
                        "Beheer eigen woorden en planning"
                    else "Voeg eigen woorden toe / kies opt-in")
                }
                Text(
                    "Eigen zinnen en betekenissen zijn niet onafhankelijk " +
                        "gecontroleerd of automatisch vertaald. Het schema is " +
                        "uitsluitend jouw zelfbeoordeling, geen officiële taaltoets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar gesprek") }
        }
    )
}
