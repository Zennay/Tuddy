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
        val safeMillis = millis.coerceAtLeast(0L)
        val seconds = safeMillis / 1000L
        return String.format(
            Locale.ROOT, "%02d:%02d:%02d.%03d",
            seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L,
            safeMillis % 1000L
        )
    }

    // Hide controls and bidi override codes rather than placing untrusted
    // subtitle content into a copyable, apparently authoritative summary.
    fun excerpt(value: String, maxCodePoints: Int = 90): String {
        // Trim at Unicode code-point boundaries so emoji, Sorani and Pashto
        // never become broken UTF-16 in a private export.
        val safe = value.codePoints().toArray().asSequence()
            .filter { cp -> cp >= 32 && cp != 127 &&
                !Character.isISOControl(cp) &&
                cp !in 0x202A..0x202E && cp !in 0x2066..0x2069 &&
                cp != 0x2028 && cp != 0x2029 && cp != 0xFEFF }
            .take(maxCodePoints + 1).toList()
        val clipped = safe.take(maxCodePoints).joinToString("") {
            String(Character.toChars(it))
        }.trim()
        return clipped + if (safe.size > maxCodePoints) " … [ingekort]" else ""
    }

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
                // A quick first/last excerpt is not sufficient for studying
                // a short passage with several user-authored subtitle lines.
                // Show all when there are <= 8; otherwise show both ends and
                // explicitly disclose the number of omitted intermediate lines.
                val count = last - first + 1
                val indices = if (count <= 8) (first..last).toList() else
                    (first until first + 4).toList() + (last - 3..last).toList()
                appendLine("   Mijn eigen ondertiteltekst per bronregel:")
                indices.forEachIndexed { index, cueIndex ->
                    if (count > 8 && index == 4) {
                        appendLine("   … ${count - 8} tussenliggende regels niet getoond.")
                    }
                    appendLine("   ${cueIndex + 1}. " + excerpt(cues[cueIndex].text, 240))
                }
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
            val rawNote = notes[span].orEmpty()
            val noteEnd = rawNote.offsetByCodePoints(
                0, minOf(240, rawNote.codePointCount(0, rawNote.length))
            )
            val note = rawNote.substring(0, noteEnd).asSequence()
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
        appendLine("MIJN VOLGENDE LUISTERRONDE (GEKOZEN VIA MIJN EIGEN INSCHATTING)")
        val repeat = privatePracticeRouteCandidates(
            bounded, ratings, PrivatePracticeRouteScope.NEEDS_REPEAT
        )
        val unrated = privatePracticeRouteCandidates(
            bounded, ratings, PrivatePracticeRouteScope.UNRATED
        )
        if (repeat.isEmpty() && unrated.isEmpty()) {
            appendLine("Geen fragmenten als lastig gemarkeerd of nog onbeoordeeld.")
            appendLine("Dit is geen bewijs dat de woorden worden begrepen.")
        } else {
            (repeat + unrated).forEachIndexed { index, span ->
                appendLine((index + 1).toString() + ". " + clock(span.first) +
                    " – " + clock(span.second) + ": " +
                    (if (span in repeat) "Zelf gemarkeerd: nog oefenen"
                    else "Zelf nog niet beoordeeld"))
            }
            appendLine("Dit zijn mijn eigen beoordelingen, geen automatische aanbevelingen.")
        }
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
