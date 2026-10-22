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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.OwnWord
import com.livelingo.android.PrivateOwnWordQuiz

/**
 * An opt-in quiz of the user's own saved words; no score, microphone,
 * transcript or word choice is persisted or sent anywhere.
 */
@Composable
internal fun OwnWordQuizDialog(
    notes: List<OwnWord>,
    curated: Boolean = false,
    onDismiss: () -> Unit
) {
    var reverse by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    var focus by remember { mutableStateOf<List<OwnWord>?>(null) }
    val cards = remember(notes, reverse, focus, round) {
        PrivateOwnWordQuiz.create(
            pool = notes,
            focus = focus ?: notes,
            reverse = reverse,
            seed = round + 1031
        )
    }
    var index by remember(cards) { mutableIntStateOf(0) }
    var correct by remember(cards) { mutableIntStateOf(0) }
    var mistakes by remember(cards) { mutableStateOf<List<OwnWord>>(emptyList()) }
    var selected by remember(cards, index) { mutableStateOf<String?>(null) }
    val card = cards.getOrNull(index)
    val rtl = setOf("ckb", "ps", "ar", "fa", "ur", "sd", "ug")
    val names = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (curated) "Quiz · bewaarde basiswoorden" else "Kies de betekenis · privéquiz") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (curated) "Kies bij je bewust bewaarde basiswoorden de kleine " +
                        "ingebouwde woordenboekbetekenis. Dit is geen vrije vertaling " +
                        "en bewijst geen dialectkwaliteit. Geen microfoon, internet " +
                        "of opgeslagen score."
                    else "Oefen alleen woorden die jij zelf hebt opgeslagen. De quiz " +
                        "controleert uitsluitend of je jouw eigen betekenis terugvindt: " +
                        "die is niet door Tuddy of een native speaker geverifieerd. " +
                        "Geen microfoon, internet, opgeslagen score of automatische vertaling.",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = {
                    reverse = !reverse
                    focus = null
                    round++
                }) {
                    Text(if (reverse) "Betekenis → woord ⇄ (wissel)"
                        else "Woord → betekenis ⇄ (wissel)")
                }
                if (focus != null) {
                    Text("Herhaalronde: alleen eerder verkeerd gekozen woorden.",
                        style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                if (card == null) {
                    if (cards.isEmpty()) {
                        Text("Deze selectie heeft minimaal twee woorden met " +
                            "verschillende betekenissen in dezelfde taal nodig. " +
                            "Woorden met een identieke betekenis worden bewust " +
                            "overgeslagen, zodat er geen dubbel juist antwoord ontstaat.")
                    } else {
                        Text("Ronde afgerond · $correct / ${cards.size} gekozen zoals opgeslagen.",
                            style = MaterialTheme.typography.titleMedium)
                        if (mistakes.isNotEmpty()) {
                            Text("${mistakes.size} woorden nog eens bekijken.",
                                style = MaterialTheme.typography.bodySmall)
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    focus = mistakes.toList()
                                    round++
                                }
                            ) { Text("Herhaal de ${mistakes.size} lastige woorden") }
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { focus = null; round++ }
                        ) { Text("Nieuwe ronde met mijn woordselectie") }
                    }
                } else {
                    Text("Vraag ${index + 1} / ${cards.size} · " +
                        (names[card.entry.language] ?: card.entry.language.uppercase()),
                        style = MaterialTheme.typography.labelMedium)
                    val prompt = if (reverse) card.entry.ownMeaning else card.entry.word
                    Text(
                        prompt,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            textDirection = if (!reverse && card.entry.language in rtl)
                                TextDirection.Rtl else TextDirection.ContentOrLtr
                        )
                    )
                    Text(if (curated) {
                        "Welke ${if (reverse) "woordvorm" else "ingebouwde basisbetekenis"} " +
                            "hoort volgens Tuddy's kleine woordenlijst bij deze kaart?"
                    } else {
                        "Welke ${if (reverse) "woordvorm" else "eigen betekenis"} " +
                            "heb je zelf bij deze kaart opgeslagen?"
                    },
                        style = MaterialTheme.typography.bodySmall)
                    card.choices.forEach { choice ->
                        val revealed = selected != null
                        val isCorrect = choice == card.correctAnswer
                        val label = when {
                            !revealed -> choice
                            isCorrect -> "✓ $choice"
                            choice == selected -> "✗ $choice"
                            else -> choice
                        }
                        OutlinedButton(
                            enabled = !revealed,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (selected == null) {
                                    selected = choice
                                    if (isCorrect) correct++
                                    else mistakes = mistakes + card.entry
                                }
                            }
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDirection = if (reverse && card.entry.language in rtl)
                                        TextDirection.Rtl else TextDirection.ContentOrLtr
                                )
                            )
                        }
                    }
                    if (selected != null) {
                        Text(if (selected == card.correctAnswer) {
                            if (curated) "Goed onthouden volgens de ingebouwde basiswoordenlijst."
                            else "Goed onthouden volgens je eigen woordenlijst."
                        } else {
                            if (curated) "Ingebouwde betekenis: ${card.correctAnswer}"
                            else "Je had zelf opgeslagen: ${card.correctAnswer}"
                        },
                            color = MaterialTheme.colorScheme.primary)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { index++ }
                        ) { Text(if (index + 1 == cards.size) "Bekijk ronde-uitkomst"
                            else "Volgende woord") }
                    }
                    Text("Goede keuzes: $correct · afgehandeld: " +
                        "${index + if (selected == null) 0 else 1} / ${cards.size}",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "De maximaal 20 vragen en alle antwoorden verdwijnen wanneer " +
                        "je dit scherm sluit. Je opgeslagen woorden, privénotities " +
                        "en herhaalplanning worden niet gewijzigd.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar eigen woorden") }
        }
    )
}
