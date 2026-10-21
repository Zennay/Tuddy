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
    CSV("text/csv", "Tuddy-mijn-luisteroefening.csv", "CSV")
}

internal data class PendingPrivatePracticeReport(
    val summary: String,
    val format: PrivatePracticeReportFormat,
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
                val first = shiftedCues.indexOfFirst { it.first == span.first }
                val last = shiftedCues.indexOfLast { it.second == span.second }
                val matched = first >= 0 && last >= first &&
                    shiftedCues.subList(first, last + 1).all {
                        it.first >= span.first && it.second <= span.second &&
                            it.second > it.first
                    }
                val preview = if (matched) {
                    val start = clean(cues[first].text, 90)
                    val end = if (first == last) "" else
                        " … " + clean(cues[last].text, 90)
                    start + end
                } else "Alleen handmatig audiotijdvak; geen exacte brontekstmatch"
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
