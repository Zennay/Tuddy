package com.livelingo.android.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivatePhraseTopics
import java.time.LocalDate

private const val NO_TOPIC_FILTER = "\u0000"

/** One day's self-directed, local-only Kurmanji/Sorani/Pashto phrase review. */
@Composable
internal fun ScheduledPhraseReviewPanel(
    deck: PrivatePhraseDeck,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val allDue = remember(deck, today) {
        deck.scheduledReviews().orEmpty().filter { it.dueDay <= today }
    }
    val context = LocalContext.current
    val topicIndex = remember(context) { PrivatePhraseTopics(context) }
    val topicNames = remember(context) { topicIndex.topics() }
    val topicByPhrase = remember(allDue, topicNames) {
        allDue.associate { it.phrase to topicIndex.topicFor(it.phrase) }
    }

    var languageFilter by remember { mutableStateOf<String?>(null) }
    var topicFilter by remember { mutableStateOf<String?>(null) }
    var reviewedPhrases by remember { mutableStateOf(setOf<PersonalPhrase>()) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var writeFirst by remember { mutableStateOf(false) }
    var typedAnswer by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }

    val languageOptions: List<Pair<String?, String>> = buildList {
        add(null to ("Alle talen (" + allDue.size + ")"))
        listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")
            .forEach { (code, label) ->
                val count = allDue.count { it.phrase.language == code }
                if (count > 0) add(code to "$label ($count)")
            }
    }
    val topicOptions: List<Pair<String?, String>> = buildList {
        add(null to ("Alle thema's (" + allDue.size + ")"))
        val withoutTopic = allDue.count { topicByPhrase[it.phrase].isNullOrBlank() }
        if (withoutTopic > 0) add(NO_TOPIC_FILTER to "Zonder thema ($withoutTopic)")
        topicNames.filter { name -> allDue.any { topicByPhrase[it.phrase] == name } }
            .forEach { name ->
                val count = allDue.count { topicByPhrase[it.phrase] == name }
                add(name to "$name ($count)")
            }
    }
    val matchingDue = remember(allDue, languageFilter, topicFilter, topicByPhrase) {
        allDue.filter { item ->
            (languageFilter == null || item.phrase.language == languageFilter) &&
                when (topicFilter) {
                    null -> true
                    NO_TOPIC_FILTER -> topicByPhrase[item.phrase].isNullOrBlank()
                    else -> topicByPhrase[item.phrase] == topicFilter
                }
        }
    }
    // Remove a self-rated card from the live queue. This lets users switch
    // filters mid-session without seeing a card they already rated again.
    val due = remember(matchingDue, reviewedPhrases) {
        matchingDue.filterNot { it.phrase in reviewedPhrases }
    }
    val current = due.getOrNull(index)
    val langNames = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")

    fun resetCurrentCard() {
        index = 0
        revealed = false
        typedAnswer = ""
        notice = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen zinnen · herhalen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Dit zijn alleen jouw eigen, niet onafhankelijk gecontroleerde " +
                        "zinnen en betekenissen. Zelf beoordelen is geen taaltoets. " +
                        "Geen microfoon, model, account of internet.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Filter op taal", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languageOptions.forEach { (code, label) ->
                        FilterChip(
                            selected = languageFilter == code,
                            onClick = {
                                languageFilter = code
                                resetCurrentCard()
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Filter op thema", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topicOptions.forEach { (topic, label) ->
                        FilterChip(
                            selected = topicFilter == topic,
                            onClick = {
                                topicFilter = topic
                                resetCurrentCard()
                            },
                            label = { Text(label) }
                        )
                    }
                }
                if (current == null) {
                    Text(
                        when {
                            allDue.isEmpty() -> "Vandaag zijn er geen eigen zinnen aan de beurt."
                            matchingDue.isEmpty() -> "Geen zinnen in deze selectie zijn vandaag aan de beurt."
                            else -> "Je hebt alle " + matchingDue.size +
                                " zinnen in deze selectie bekeken."
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tuddy bewaart uitsluitend een lokale hash, niveau en herhaaldatum. " +
                            "Open de app later zelf opnieuw: er zijn geen meldingen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val phrase = current.phrase
                    val rtl = phrase.language == "ckb" || phrase.language == "ps"
                    val prompt = if (reverse) phrase.ownMeaning else phrase.phrase
                    val answer = if (reverse) phrase.phrase else phrase.ownMeaning
                    val topic = topicByPhrase[phrase]
                    Text(
                        "Zin " + (matchingDue.size - due.size + 1) + " van " +
                            matchingDue.size + " · " + langNames[phrase.language],
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (!topic.isNullOrBlank()) {
                        Text("Thema · $topic", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = {
                        reverse = !reverse
                        revealed = false
                        typedAnswer = ""
                    }) {
                        Text(if (reverse) "Betekenis → eigen zin ⇄" else "Eigen zin → betekenis ⇄")
                    }
                    TextButton(onClick = {
                        writeFirst = !writeFirst
                        typedAnswer = ""
                        revealed = false
                    }) {
                        Text(if (writeFirst) "✓ Zelf antwoord schrijven" else
                            "✍ Zelf antwoord schrijven")
                    }
                    HorizontalDivider()
                    Text(
                        prompt,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = if (!reverse && rtl) TextDirection.Rtl
                                else TextDirection.Ltr
                        )
                    )
                    if (writeFirst && !revealed) {
                        OutlinedTextField(
                            value = typedAnswer,
                            onValueChange = { typedAnswer = it.take(220) },
                            label = { Text(if (reverse) "Mijn eigen zin" else "Mijn eigen betekenis") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (reverse && rtl) TextDirection.Rtl
                                    else TextDirection.Ltr
                            )
                        )
                        Text("Alleen tijdelijk in dit scherm; geen automatische taalcontrole.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (revealed) {
                        if (writeFirst) {
                            Text("Mijn getypte poging:", style = MaterialTheme.typography.labelMedium)
                            Text(typedAnswer.ifBlank { "(niet ingevuld)" },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (reverse && rtl) TextDirection.Rtl
                                        else TextDirection.Ltr
                                ))
                            Text("Vergelijk zelf met je oorspronkelijke antwoord hieronder.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            answer,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = if (reverse && rtl) TextDirection.Rtl
                                    else TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Je eigen betekenis, niet door Tuddy geverifieerd.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledPhrase(phrase, false)) {
                                        reviewedPhrases = reviewedPhrases + phrase
                                        onChanged()
                                        revealed = false
                                        typedAnswer = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt; je beoordeling is niet onthouden."
                                }
                            ) { Text("Nog oefenen") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (deck.rateScheduledPhrase(phrase, true)) {
                                        reviewedPhrases = reviewedPhrases + phrase
                                        onChanged()
                                        revealed = false
                                        typedAnswer = ""
                                        notice = ""
                                    } else notice = "Opslaan mislukt; je beoordeling is niet onthouden."
                                }
                            ) { Text("Ik wist het") }
                        }
                        Text(
                            "Nog oefenen: morgen. Ik wist het: later na 2, 4, 7, 14 of 30 dagen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { revealed = true }
                        ) { Text("Toon mijn betekenis") }
                    }
                }
                if (notice.isNotBlank()) Text(
                    notice,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar eigen zinnen") }
        }
    )
}
