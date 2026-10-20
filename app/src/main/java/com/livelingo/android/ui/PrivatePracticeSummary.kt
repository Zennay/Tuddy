package com.livelingo.android.ui

import com.livelingo.android.PrivateSubtitleCue
import java.util.Locale

/**
 * The guided route plays exactly the locally marked personal passage cards.
 * These filters never interpret comprehension or produce a score.
 */
internal enum class PrivatePracticeRouteScope(val title: String) {
    ALL("Alle fragmenten"),
    NEEDS_REPEAT("Nog oefenen"),
    UNRATED("Nog niet beoordeeld")
}

internal fun privatePracticeRouteCandidates(
    passages: List<Pair<Long, Long>>,
    ratings: Map<Pair<Long, Long>, String>,
    scope: PrivatePracticeRouteScope
): List<Pair<Long, Long>> = passages.filter { span ->
    when (scope) {
        PrivatePracticeRouteScope.ALL -> true
        PrivatePracticeRouteScope.NEEDS_REPEAT -> ratings[span] == "repeat"
        PrivatePracticeRouteScope.UNRATED -> ratings[span] !in listOf("repeat", "okay", "easy")
    }
}

/**
 * In-memory personal listening note. This is neither a language evaluation
 * nor evidence of source-audio alignment. Never include the selected SAF URI.
 * The caller alone decides when to present this selectable text.
 */
internal fun privatePracticeSummary(
    cues: List<PrivateSubtitleCue>,
    offsetMs: Long,
    passages: List<Pair<Long, Long>>,
    ratings: Map<Pair<Long, Long>, String>,
    completedRoute: List<Pair<Long, Long>>,
    completedRounds: Int,
    guidedActive: Boolean,
    forExport: Boolean = false,
    notes: Map<Pair<Long, Long>, String> = emptyMap()
): String {
    fun clock(millis: Long): String {
        val seconds = millis.coerceAtLeast(0L) / 1000L
        return String.format(
            Locale.ROOT, "%02d:%02d:%02d",
            seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L
        )
    }

    // Hide controls and bidi override codes rather than placing untrusted
    // subtitle content into a copyable, apparently authoritative summary.
    fun excerpt(value: String): String = value.asSequence()
        .filter { char ->
            char.code >= 32 && char.code != 127 &&
                char !in '\u202a'..'\u202e' &&
                char !in '\u2066'..'\u2069'
        }.take(90).joinToString("").trim()

    val bounded = passages.take(4)
    return buildString {
        appendLine("TUDDY · MIJN PRIVÉ LUISTEROEFENING")
        appendLine("Eigen tijden en beoordelingen; geen automatische spraakherkenning of taalniveauscore.")
        appendLine("Audio-URI en accountgegevens worden niet opgenomen.")
        appendLine("Mijn tijdelijke verschuiving: " + offsetMs + " ms.")
        appendLine()
        appendLine("MIJN HUIDIGE OEFENKAARTJES (" + bounded.size + "/4)")
        bounded.forEachIndexed { index, span ->
            val start = span.first
            val end = span.second
            appendLine((index + 1).toString() + ". " + clock(start) + " – " + clock(end))
            val first = cues.indexOfFirst { it.startMillis + offsetMs == start }
            val last = cues.indexOfLast { it.endMillis + offsetMs == end }
            if (first >= 0 && last >= first &&
                cues.subList(first, last + 1).all {
                    it.startMillis + offsetMs >= start &&
                        it.endMillis + offsetMs <= end
                }) {
                appendLine("   Mijn ondertitelregels " + (first + 1) + "–" + (last + 1) + ": " +
                    excerpt(cues[first].text) +
                    if (first == last) "" else " … " + excerpt(cues[last].text))
            } else {
                appendLine("   Alleen handmatig audiotijdvak; geen exacte brontekstmatch.")
            }
            val assessment = when (ratings[span]) {
                "repeat" -> "Nog oefenen"
                "okay" -> "Gaat goed"
                "easy" -> "Makkelijk"
                else -> "Nog niet zelf beoordeeld"
            }
            appendLine("   Mijn eigen inschatting: " + assessment)
            val note = notes[span].orEmpty().take(240).asSequence()
                .filter { it >= ' ' && it != '\u007f' &&
                    it !in '\u202a'..'\u202e' &&
                    it !in '\u2066'..'\u2069'
                }.joinToString("").trim()
            if (note.isNotBlank()) {
                appendLine("   Mijn eigen volgende oefenstap: " + note)
            }
        }
        if (bounded.isEmpty()) appendLine("Nog geen oefenkaartjes.")
        appendLine()
        appendLine("BEGELEIDE LUISTERROUTE")
        if (completedRoute.isNotEmpty() && completedRoute.size <= 4 &&
            completedRounds in listOf(1, 3, 5)) {
            appendLine("Laatst volledig afgerond in deze audiosessie: " +
                completedRoute.size + " fragmenten × " + completedRounds + " herhalingen.")
            appendLine("Dit beschrijft afspelen, niet begrip of uitspraakkwaliteit.")
        } else {
            appendLine("In deze audiosessie nog geen begeleide route volledig afgerond.")
            if (guidedActive) appendLine("Er loopt een route; deze telt nog niet als voltooid.")
        }
        appendLine()
        if (forExport) {
            appendLine("Dit verslag is alleen na mijn expliciete bestandskeuze bewaard.")
            appendLine("De gekozen Android-provider kan het bestand bewaren of synchroniseren.")
            appendLine("Geen audio, audio-URI, automatische uitspraakanalyse of betrouwbare woordtiming.")
        } else {
            appendLine("Deze tekst wordt uitsluitend in dit tijdelijke scherm getoond.")
            appendLine("Selecteren/kopiëren kan via Android. Daarna kan jouw klembord-app de tekst verwerken.")
            appendLine("Tuddy bewaart niets automatisch en verzendt geen audiodata.")
        }
    }
}
