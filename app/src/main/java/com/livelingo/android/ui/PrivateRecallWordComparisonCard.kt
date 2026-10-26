package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateRecallWordComparison

/**
 * A strictly opt-in *reading aid* shown only after the reference is revealed.
 * All comparison data is derived from the current RAM strings on the screen.
 * It never produces an automatic pass/fail or changes the user's manual mark.
 */
@Composable
internal fun PrivateRecallWordComparisonCard(
    learnerAttempt: String,
    revealedReference: String,
    attemptLabel: String
) {
    val comparison = remember(learnerAttempt, revealedReference) {
        PrivateRecallWordComparison.compare(learnerAttempt, revealedReference)
    }
    Column(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "Lokale woordvergelijking van $attemptLabel; geen taalbeoordeling"
        },
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Woordvergelijking · $attemptLabel",
            style = MaterialTheme.typography.titleSmall)
        if (comparison == null) {
            Text("Vergelijken niet beschikbaar: lege, te lange of ongeschikte tekst. " +
                "Maximaal 400 tekens eigen antwoord, 800 tekens referentie en " +
                "80 woorden per tekst.",
                style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "${comparison.sharedInOrder.size} woorden in dezelfde volgorde " +
                    "teruggevonden (van ${comparison.referenceWords} referentiewoorden).",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Overeenkomend: " + comparison.sharedInOrder.take(24)
                    .joinToString(" · ").ifBlank { "Geen" } +
                    if (comparison.sharedInOrder.size > 24) " · …" else "",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Alleen in referentie: " + comparison.onlyInReference.take(24)
                    .joinToString(" · ").ifBlank { "Geen" } +
                    if (comparison.onlyInReference.size > 24) " · …" else "",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Alleen in jouw poging: " + comparison.onlyInAttempt.take(24)
                    .joinToString(" · ").ifBlank { "Geen" } +
                    if (comparison.onlyInAttempt.size > 24) " · …" else "",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "Dit is alleen letterlijke woordoverlap in volgorde, zonder " +
                "hoofdletterverschil. Synoniemen, grammatica, betekenis, " +
                "uitspraak en mogelijke fouten in de lokale vertaling worden " +
                "niet beoordeeld. Jij kiest nog steeds Zelf goed of Nog oefenen. " +
                "Niets wordt opgeslagen of gedeeld.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
