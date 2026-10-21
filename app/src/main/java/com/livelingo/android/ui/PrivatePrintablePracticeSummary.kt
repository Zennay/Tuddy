package com.livelingo.android.ui

/**
 * Explicit export of the already rendered, user-authored practice overview.
 * The caller supplies only privatePracticeSummary(..., forExport = true):
 * neither audio bytes nor the Android audio URI are accepted.
 * No external fonts, scripts, links, fetches or persisted state.
 */
internal enum class PrivatePracticeReportFormat(
    val mime: String, val filename: String, val title: String
) {
    HTML("text/html", "Tuddy-mijn-luisteroefening.html", "HTML"),
    WORKSHEET_HTML("text/html", "Tuddy-mijn-luisterwerkblad.html", "HTML-oefenblad"),
    TXT("text/plain", "Tuddy-mijn-luisteroefening.txt", "TXT"),
    CSV("text/csv", "Tuddy-mijn-luisteroefening.csv", "CSV"),
    DETAILED_CSV("text/csv", "Tuddy-mijn-bronregels-per-fragment.csv",
        "CSV · bronregels per fragment")
}

internal data class PendingPrivatePracticeReport(
    val summary: String,
    val format: PrivatePracticeReportFormat,
    val selectionScope: PrivatePracticeRouteScope,
    val worksheetAudience: PrivateListeningWorksheetAudience,
    val worksheetPrompt: PrivateListeningWorksheetPrompt,
    val perFragmentPrompts: Map<Pair<Long, Long>, PrivateListeningWorksheetPrompt>,
    val bytes: ByteArray
)

internal object PrivatePrintablePracticeSummary {
    private const val MAX_CHARS = 16_384
    private const val MAX_BYTES = 65_536

    private fun valid(summary: String): Boolean {
        if (summary.isBlank() || summary.length > MAX_CHARS ||
            !summary.startsWith("TUDDY · MIJN PRIVÉ LUISTEROEFENING\n")
        ) return false
        var index = 0
        while (index < summary.length) {
            val char = summary[index]
            if (Character.isHighSurrogate(char)) {
                if (index + 1 >= summary.length ||
                    !Character.isLowSurrogate(summary[index + 1])) return false
                index += 2
                continue
            }
            if (Character.isLowSurrogate(char) ||
                (char.isISOControl() && char != '\n' && char != '\t')
            ) return false
            index++
        }
        return true
    }


    /**
     * Explicit, standalone CSV for a person's own fragment ratings and
     * next-step notes. This is not a speech model output or verified alignment.
     * Never accept the audio URI, media bytes, or account metadata.
     */
    fun snapshotCsv(
        cues: List<com.livelingo.android.PrivateSubtitleCue>,
        offsetMs: Long,
        passages: List<Pair<Long, Long>>,
        ratings: Map<Pair<Long, Long>, String>,
        notes: Map<Pair<Long, Long>, String>,
        scope: PrivatePracticeRouteScope = PrivatePracticeRouteScope.ALL
    ): ByteArray? {
        if (passages.isEmpty() || passages.size > 4 ||
            passages.distinct().size != passages.size || cues.size > 5_000) return null
        val selected = privatePracticeRouteCandidates(passages, ratings, scope)
        if (selected.isEmpty() || selected.any {
                it.first < 0L || it.second <= it.first ||
                    it.second - it.first > 300_000L
            }) return null
        // Reject arithmetic overflow instead of showing unrelated source lines
        // when a user shifts a long subtitle document.
        val shiftedCues = ArrayList<Pair<Long, Long>>(cues.size)
        for (cue in cues) {
            val first = runCatching { Math.addExact(cue.startMillis, offsetMs) }
                .getOrNull() ?: return null
            val last = runCatching { Math.addExact(cue.endMillis, offsetMs) }
                .getOrNull() ?: return null
            shiftedCues.add(first to last)
        }
        fun clean(value: String, limit: Int): String {
            val readable = value.codePoints().toArray().asSequence()
                .filter { point ->
                    point >= 32 && point != 127 &&
                        point !in 0xD800..0xDFFF &&
                        !Character.isISOControl(point) &&
                        point !in 0x202A..0x202E && point !in 0x2066..0x2069 &&
                        point != 0xFEFF
                }
                .take(limit).joinToString("") { point ->
                    String(Character.toChars(point))
                }.trim()
            // All fields are quoted, but quoting alone does NOT prevent a
            // spreadsheet treating a user-authored field as a formula.
            val firstVisible = readable.firstOrNull { !it.isWhitespace() &&
                Character.getType(it) != Character.FORMAT.toInt() }
            return if ((firstVisible != null && firstVisible in "=+-@") ||
                (readable.firstOrNull()?.let { it in "=+-@" } == true)
            ) "'" + readable else readable
        }
        fun time(ms: Long): String {
            val safeMs = ms.coerceAtLeast(0L)
            val whole = safeMs / 1000L
            return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d.%03d",
                whole / 3600L, (whole / 60L) % 60L, whole % 60L,
                safeMs % 1000L)
        }
        fun row(vararg columns: String): String = columns.joinToString(",") {
            "\"" + it.replace("\"", "\"\"") + "\""
        } + "\r\n"
        val output = buildString {
            append(row("Fragment", "Begin (eigen tijd)", "Einde (eigen tijd)",
                "Mijn beoordeling", "Mijn volgende oefenstap", "Ondertitelvoorbeeld"))
            selected.forEachIndexed { index, span ->
                // A–B markers are picked freely by the listener and rarely
                // equal subtitle boundaries. Include whole cues that fall
                // inside the span, exactly like the HTML worksheet does.
                // Never infer audio alignment from partial cue overlap.
                val matchingIndices = shiftedCues.indices.filter { cueIndex ->
                    val cue = shiftedCues[cueIndex]
                    cue.first >= span.first && cue.second <= span.second &&
                        cue.second > cue.first
                }
                val preview = when (matchingIndices.size) {
                    0 -> "Geen volledig passende ondertitelregel; alleen eigen audiotijdvak"
                    1 -> "1 volledig passende bronregel: " +
                        clean(cues[matchingIndices.single()].text, 90)
                    else -> {
                        val firstText = clean(cues[matchingIndices.first()].text, 90)
                        val lastText = clean(cues[matchingIndices.last()].text, 90)
                        val omitted = matchingIndices.size - 2
                        matchingIndices.size.toString() + " volledig passende bronregels: " +
                            firstText + (if (omitted == 0) " / " else
                                " … [" + omitted + " middenregels niet getoond] … ") +
                            lastText
                    }
                }
                val rating = when (ratings[span]) {
                    "repeat" -> "Nog oefenen"
                    "okay" -> "Gaat goed"
                    "easy" -> "Makkelijk"
                    else -> "Nog niet beoordeeld"
                }
                append(row((index + 1).toString(), time(span.first), time(span.second),
                    rating, clean(notes[span].orEmpty(), 240), preview))
            }
        }
        return output.toByteArray(Charsets.UTF_8).takeIf { it.size <= MAX_BYTES }
    }

    /**
     * A source-row-per-line CSV for explicitly chosen listening fragments.
     * The A–B marks are user-authored and NEVER assert audio alignment:
     * include only complete SRT/VTT cues contained in each chosen interval.
     *
     * Unlike the compact six-column CSV, this preserves every matching
     * authored subtitle line (up to the existing bounded UTF-8 document
     * limit). Source and notes are treated as untrusted spreadsheet text.
     */
    fun snapshotDetailedCsv(
        cues: List<com.livelingo.android.PrivateSubtitleCue>,
        offsetMs: Long,
        passages: List<Pair<Long, Long>>,
        ratings: Map<Pair<Long, Long>, String>,
        notes: Map<Pair<Long, Long>, String>,
        scope: PrivatePracticeRouteScope = PrivatePracticeRouteScope.ALL
    ): ByteArray? {
        if (passages.isEmpty() || passages.size > 4 ||
            passages.distinct().size != passages.size || cues.size > 5_000) return null
        val selected = privatePracticeRouteCandidates(passages, ratings, scope)
        if (selected.isEmpty() || selected.any { span ->
                span.first < 0L || span.second <= span.first ||
                    span.second - span.first > 300_000L
            }) return null

        // Validate all shifted timestamps, not only the visible excerpt.
        val shifted = ArrayList<Pair<Long, Long>>(cues.size)
        for (cue in cues) {
            val start = runCatching { Math.addExact(cue.startMillis, offsetMs) }
                .getOrNull() ?: return null
            val end = runCatching { Math.addExact(cue.endMillis, offsetMs) }
                .getOrNull() ?: return null
            shifted.add(start to end)
        }
        fun clock(ms: Long): String {
            val whole = ms / 1_000L
            return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d.%03d",
                whole / 3_600L, (whole / 60L) % 60L, whole % 60L,
                ms % 1_000L)
        }
        fun safeCell(input: String): String? {
            // Prevent a very long or malformed phrase from silently
            // becoming an apparently complete source-line export.
            if (input.length > 8_192) return null
            val result = StringBuilder(input.length)
            var index = 0
            while (index < input.length) {
                val cp = input.codePointAt(index)
                if (cp in 0xD800..0xDFFF) return null
                index += Character.charCount(cp)
                if (cp in 0x202A..0x202E || cp in 0x2066..0x2069 ||
                    cp == 0xFEFF || Character.isISOControl(cp)) {
                    if (cp == '\n'.code || cp == '\r'.code || cp == '\t'.code)
                        result.append(' ')
                    continue
                }
                if (cp == 0x2028 || cp == 0x2029) result.append(' ')
                else result.appendCodePoint(cp)
            }
            val clean = result.toString().trim()
            val firstVisible = clean.codePoints().toArray().firstOrNull { cp ->
                !Character.isWhitespace(cp) && !Character.isSpaceChar(cp) &&
                    Character.getType(cp) != Character.FORMAT.toInt()
            }
            // Quoting CSV alone does not neutralize spreadsheet formulae;
            // also protect fields prefixed with invisible format marks.
            return if (firstVisible != null &&
                firstVisible in listOf('='.code, '+'.code, '-'.code, '@'.code))
                "'" + clean else clean
        }
        fun row(vararg cells: String): String = cells.joinToString(",") { cell ->
            "\"" + cell.replace("\"", "\"\"") + "\""
        } + "\r\n"
        val out = StringBuilder()
        out.append(row("Fragment", "Mijn A-B begin", "Mijn A-B einde",
            "Mijn beoordeling", "Mijn volgende oefenstap",
            "Oorspronkelijke bronregel", "Ondertitel begin", "Ondertitel einde",
            "Volledige eigen ondertiteltekst"))
        for ((passIndex, passage) in selected.withIndex()) {
            val rating = when (ratings[passage]) {
                "repeat" -> "Nog oefenen"
                "okay" -> "Gaat goed"
                "easy" -> "Makkelijk"
                else -> "Nog niet beoordeeld"
            }
            val note = safeCell(notes[passage].orEmpty()) ?: return null
            val meta = arrayOf((passIndex + 1).toString(), clock(passage.first),
                clock(passage.second), rating, note)
            var found = 0
            for ((cueIndex, cue) in cues.withIndex()) {
                val times = shifted[cueIndex]
                if (times.first < passage.first || times.second > passage.second ||
                    times.second <= times.first) continue
                val sourceText = safeCell(cue.text) ?: return null
                if (sourceText.isBlank()) continue
                out.append(row(*meta, (cueIndex + 1).toString(),
                    clock(times.first), clock(times.second), sourceText))
                found++
                if (out.length > 131_072) return null
            }
            if (found == 0) {
                // Do not invent an alignment or pretend that a manually
                // chosen time span contains actual detected speech.
                out.append(row(*meta, "", "", "",
                    "Geen volledig passende ondertitelregel (eigen A-B-tijdvak)"))
            }
            if (out.length > 131_072) return null
        }
        return out.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= 131_072 }
    }

    fun snapshotText(summary: String): ByteArray? =
        if (valid(summary)) summary.toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES } else null

    fun snapshot(summary: String): ByteArray? {
        if (!valid(summary)) return null
        val escaped = buildString(summary.length) {
            summary.forEach { char ->
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(char)
                }
            }
        }
        val html = """
            <!doctype html>
            <html lang="nl"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <meta name="referrer" content="no-referrer">
            <title>Mijn privé Tuddy-luisteroefening</title>
            <style>
            body{font-family:system-ui,sans-serif;max-width:800px;margin:auto;padding:24px;
              line-height:1.6;color:#202332;background:#fff}
            h1{font-size:1.5rem}
            .report{white-space:pre-wrap;overflow-wrap:anywhere;unicode-bidi:plaintext;
              border:1px solid #555;padding:20px;border-radius:10px}
            .note{font-size:.9rem;color:#414854}
            @media print{body{padding:0}.report{border:0;padding:0}}
            </style></head><body>
            <main><h1>Mijn privé luisteroefening</h1>
            <p class="note">Eigen tijdcodes en beoordelingen, niet automatisch gecontroleerd.
            Audio en bestandslocaties zijn niet opgenomen. Dit bestand is bewust door mij
            bewaard en kan door mijn gekozen bestandsprovider gesynchroniseerd worden.</p>
            <div class="report">$escaped</div>
            </main></body></html>
        """.trimIndent()
        return html.toByteArray(Charsets.UTF_8).takeIf { it.size <= MAX_BYTES }
    }
}
