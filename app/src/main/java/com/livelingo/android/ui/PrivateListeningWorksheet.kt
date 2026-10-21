package com.livelingo.android.ui

import com.livelingo.android.PrivateSubtitleCue
import java.util.Locale

/**
 * Explicit, user-saved printable listening worksheet for the A–B passages
 * chosen in the local session. All clues come solely from user-provided
 * subtitle timing; nothing aligns audio or assesses comprehension.
 *
 * Never receives or embeds audio URIs, files, permissions, or accounts.
 */
/** Decide up front whether the recipient may receive the underlying subtitle text. */
internal enum class PrivateListeningWorksheetAudience(val title: String) {
    SELF_STUDY("Zelfstudie · bronregels zelf onthullen"),
    STUDENT_HANDOUT("Leerlingenblad · zonder bronregels of privébeoordeling")
}

internal object PrivateListeningWorksheet {
    private const val MAX_BYTES = 131_072
    private const val MAX_CUES = 5_000
    private const val MAX_VISIBLE_LINES = 18
    private const val MAX_LINE_CODEPOINTS = 600

    private fun time(ms: Long): String {
        val millis = ms.coerceAtLeast(0L)
        val seconds = millis / 1000L
        return String.format(
            Locale.ROOT, "%02d:%02d:%02d.%03d",
            seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L,
            millis % 1000L
        )
    }

    private fun clean(value: String, maxCodePoints: Int): String? {
        if (value.length > 8_192) return null
        val safe = ArrayList<Int>(maxCodePoints + 1)
        var index = 0
        while (index < value.length && safe.size <= maxCodePoints) {
            val first = value[index]
            val cp: Int
            if (Character.isHighSurrogate(first)) {
                if (index + 1 >= value.length || !Character.isLowSurrogate(value[index + 1]))
                    return null
                cp = Character.toCodePoint(first, value[index + 1])
                index += 2
            } else {
                if (Character.isLowSurrogate(first)) return null
                cp = first.code
                index++
            }
            if (cp in 0x202A..0x202E || cp in 0x2066..0x2069 ||
                cp == 0xFEFF || (Character.isISOControl(cp) &&
                    cp != '\n'.code && cp != '\r'.code && cp != '\t'.code)
            ) continue
            safe.add(if (cp == '\n'.code || cp == '\r'.code || cp == '\t'.code ||
                cp == 0x2028 || cp == 0x2029) ' '.code else cp)
        }
        val out = safe.take(maxCodePoints).joinToString("") {
            String(Character.toChars(it))
        }.trim()
        return out + if (index < value.length || safe.size > maxCodePoints)
            " … [tekst ingekort]" else ""
    }

    private fun html(value: String): String = buildString(value.length) {
        for (char in value) append(when (char) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            '\'' -> "&#39;"
            else -> char.toString()
        })
    }

    private fun rating(value: String?): String = when (value) {
        "repeat" -> "Nog oefenen"
        "okay" -> "Gaat goed"
        "easy" -> "Makkelijk"
        else -> "Nog niet beoordeeld"
    }

    private data class Passage(
        val number: Int,
        val start: Long,
        val end: Long,
        val rating: String,
        val note: String,
        val lines: List<Pair<Int, String>>,
        val omitted: Int
    )

    fun snapshot(
        cues: List<PrivateSubtitleCue>,
        offsetMs: Long,
        passages: List<Pair<Long, Long>>,
        ratings: Map<Pair<Long, Long>, String>,
        notes: Map<Pair<Long, Long>, String>,
        scope: PrivatePracticeRouteScope = PrivatePracticeRouteScope.ALL,
        audience: PrivateListeningWorksheetAudience = PrivateListeningWorksheetAudience.SELF_STUDY
    ): ByteArray? {
        if (passages.isEmpty() || passages.size > 4 ||
            passages.distinct().size != passages.size ||
            cues.size > MAX_CUES) return null

        val selected = privatePracticeRouteCandidates(passages, ratings, scope)
        if (selected.isEmpty()) return null
        val sections = ArrayList<Passage>()
        for ((index, bounds) in selected.withIndex()) {
            val start = bounds.first
            val end = bounds.second
            if (start < 0L || end <= start || end - start > 300_000L) return null
            // A student handout never needs the subtitle or private-note
            // content at all: avoid traversing it or rejecting this redacted
            // document due to unrelated malformed source text.
            val note = if (audience == PrivateListeningWorksheetAudience.SELF_STUDY)
                clean(notes[bounds].orEmpty(), 240) ?: return null else ""
            val allLines = ArrayList<Pair<Int, String>>()
            if (audience == PrivateListeningWorksheetAudience.SELF_STUDY) {
                // Match complete subtitle cues only. Hand-picked A–B times
                // never imply that audio and SRT/VTT have been aligned.
                for ((cueIndex, cue) in cues.withIndex()) {
                    val cueStart = runCatching { Math.addExact(cue.startMillis, offsetMs) }
                        .getOrNull() ?: return null
                    val cueEnd = runCatching { Math.addExact(cue.endMillis, offsetMs) }
                        .getOrNull() ?: return null
                    if (cueStart >= start && cueEnd <= end && cueEnd > cueStart) {
                        val line = clean(cue.text, MAX_LINE_CODEPOINTS) ?: return null
                        if (line.isNotBlank()) allLines.add(cueIndex + 1 to line)
                    }
                }
            }
            // When a longer passage is selected, disclose any omitted middle
            // lines instead of silently claiming a complete transcription.
            val visible = if (allLines.size <= MAX_VISIBLE_LINES) allLines
                else allLines.take(MAX_VISIBLE_LINES / 2) +
                    allLines.takeLast(MAX_VISIBLE_LINES / 2)
            sections.add(Passage(
                number = index + 1, start = start, end = end,
                rating = rating(ratings[bounds]), note = note,
                lines = visible,
                omitted = allLines.size - visible.size
            ))
        }

        val out = StringBuilder(
            "<!doctype html><html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<meta http-equiv=\"Content-Security-Policy\" " +
                "content=\"default-src 'none'; style-src 'unsafe-inline';" +
                " form-action 'none'; base-uri 'none'\">" +
                "<title>" +
                (if (audience == PrivateListeningWorksheetAudience.STUDENT_HANDOUT)
                    "Tuddy · Luisterleerlingenblad zonder bronregels"
                else "Tuddy · Mijn luisterwerkblad") + "</title>" +
                "<style>body{font:16px system-ui,sans-serif;line-height:1.55;" +
                "max-width:850px;margin:auto;padding:22px;color:#202332}" +
                "article{border:1px solid #999;border-radius:9px;padding:12px;" +
                "margin:18px 0;break-inside:avoid}li{margin:12px 0}" +
                "bdi{overflow-wrap:anywhere}summary{cursor:pointer}" +
                "textarea{width:100%;min-height:88px;box-sizing:border-box;" +
                "padding:10px;font:inherit;resize:vertical;unicode-bidi:plaintext}" +
                ".rating{border:0;margin:8px 0;padding:0}" +
                ".rating label{display:inline-block;margin:4px 14px 4px 0}" +
                ".print-rating{display:none}" +
                ".reset{margin:8px 0;padding:6px 10px;cursor:pointer}" +
                ".answer-key{display:none;break-before:page;page-break-before:always}" +
                ".answer-key:target{display:block}" +
                ".answerlink{display:inline-block;margin:10px 0}" +
                ".printcontrol{display:inline-block;margin:12px 0;font-weight:600;" +
                    "cursor:pointer}.printkey{vertical-align:middle;margin-right:8px}" +
                ".line{display:none;border-bottom:1px solid #555;height:24px}" +
                "p{overflow-wrap:anywhere}" +
                "@media print{body{padding:0}details,form,.answerlink," +
                    ".printkey,.printcontrol{display:none}" +
                ".print-rating{display:block}" +
                ".line{display:block}" +
                ".answer-key,.answer-key:target{display:none}" +
                ".printkey:checked ~ .answer-key," +
                    ".printkey:checked ~ .answer-key:target{display:block}" +
                "article{border:0;border-bottom:1px solid #999}}" +
                "</style></head><body><main id=\"top\"><h1>" +
                (if (audience == PrivateListeningWorksheetAudience.STUDENT_HANDOUT)
                    "Mijn luisterleerlingenblad"
                else "Mijn privé luisterwerkblad") + "</h1>" +
                "<p>Luister eerst naar je eigen A–B-fragment. Schrijf op wat je " +
                "hoort of begrijpt. " +
                (if (audience == PrivateListeningWorksheetAudience.SELF_STUDY)
                    "Open de eigen bronregels daarna bewust om te vergelijken. " +
                        "Bij het afdrukken verschijnt het complete bronregelblad " +
                        "pas als je zelf het bijbehorende afdrukvakje aanvinkt. "
                else "Dit leerlingenblad bevat geen oorspronkelijke bronregels, " +
                    "persoonlijke inschattingen of eigen notities. ") +
                "Dit zijn handmatig gekozen fragmenttijden, " +
                "geen ASR, uitspraakmeting of geverifieerde vertaling.</p>" +
                (if (audience == PrivateListeningWorksheetAudience.SELF_STUDY)
                    "<p>Mijn bewust gekozen selectie: " + html(scope.title) + " (" +
                        selected.size + " van " + passages.size + " eigen fragmenten).</p>"
                else "<p>Oefenfragmenten: " + selected.size + ".</p>") +
                "<p>Bestemming: " + html(audience.title) + ".</p>"
        )
        for (part in sections) {
            out.append("<article><h2>Fragment ").append(part.number).append("</h2><p>")
                .append(time(part.start)).append(" – ").append(time(part.end))
                .append("</p>")
            if (audience == PrivateListeningWorksheetAudience.SELF_STUDY) {
                out.append("<p>Eigen inschatting: ").append(html(part.rating)).append("</p>")
            }
            out.append("<p>Wat hoor ik? Schrijf mijn eigen antwoord op:</p>")
                .append("<form autocomplete=\"off\"><label for=\"answer-")
                .append(part.number).append("\">Mijn eigen antwoord (alleen deze browser)</label>")
                .append("<textarea id=\"answer-").append(part.number)
                .append("\" rows=\"4\" dir=\"auto\" spellcheck=\"false\" " +
                    "autocomplete=\"off\" autocorrect=\"off\"></textarea>")
                .append("<fieldset class=\"rating\"><legend>Mijn eigen luisterinschatting " +
                    "(optioneel, niet opgeslagen)</legend>")
                .append("<label><input type=\"radio\" name=\"rating-").append(part.number)
                .append("\" value=\"repeat\"> Nog oefenen</label>")
                .append("<label><input type=\"radio\" name=\"rating-").append(part.number)
                .append("\" value=\"okay\"> Ging goed</label>")
                .append("<label><input type=\"radio\" name=\"rating-").append(part.number)
                .append("\" value=\"easy\"> Makkelijk</label></fieldset>")
                .append("<button class=\"reset\" type=\"reset\">Wis mijn antwoord " +
                    "en inschatting</button></form>")
                .append("<p class=\"print-rating\">Eigen luisterinschatting: " +
                    "&#9633; Nog oefenen &nbsp; &#9633; Ging goed &nbsp; " +
                    "&#9633; Makkelijk</p>")
                .append("<div class=\"line\"></div><div class=\"line\"></div>")
            if (audience == PrivateListeningWorksheetAudience.SELF_STUDY) {
                out.append("<details><summary>Toon zelf gekozen bronregels (")
                    .append(part.lines.size).append(" getoond)</summary>")
            if (part.lines.isEmpty()) {
                out.append("<p>Geen volledig passende ondertitelregel; " +
                    "dit is alleen mijn handmatig gekozen audiotijdvak.</p>")
            } else {
                out.append("<ol>")
                for ((number, line) in part.lines) {
                    out.append("<li>Bronregel ").append(number).append(": <bdi dir=\"auto\">")
                        .append(html(line)).append("</bdi></li>")
                }
                out.append("</ol>")
            }
            if (part.omitted > 0) out.append("<p>")
                .append(part.omitted)
                .append(" tussenliggende bronregels niet getoond.</p>")
                out.append("</details>")
                if (part.note.isNotBlank()) out.append(
                    "<p>Mijn volgende oefenstap: <bdi dir=\"auto\">"
                ).append(html(part.note)).append("</bdi></p>")
            }
            out.append("</article>")
            if (out.length > MAX_BYTES) return null
        }
        if (audience == PrivateListeningWorksheetAudience.SELF_STUDY) {
            out.append("<input id=\"print-listening-key\" class=\"printkey\" " +
                    "type=\"checkbox\" aria-label=\"Print ook mijn bronregelblad\">" +
                    "<label class=\"printcontrol\" for=\"print-listening-key\">" +
                    "Print ook mijn bronregelblad (bewuste keuze)</label>")
            out.append("<a class=\"answerlink\" href=\"#bronregels\">" +
                "Open compleet bronregelblad (bewuste keuze)</a>")
                .append("<section class=\"answer-key\" id=\"bronregels\">" +
                    "<h2>Bronregels voor zelfcontrole</h2>" +
                    "<a class=\"answerlink\" href=\"#top\">Terug naar start</a>" +
                    "<p>De bronregels komen uit mijn eigen SRT/VTT; ze zijn niet " +
                    "automatisch naar het geluidsbestand uitgelijnd.</p>")
        for (part in sections) {
            out.append("<h3>Fragment ").append(part.number).append(" · ")
                .append(time(part.start)).append(" – ").append(time(part.end)).append("</h3>")
            if (part.lines.isEmpty()) out.append("<p>Geen exacte cue-koppeling.</p>")
            else {
                out.append("<ol>")
                for ((number, line) in part.lines) out.append("<li>")
                    .append(number).append(". <bdi dir=\"auto\">")
                    .append(html(line)).append("</bdi></li>")
                out.append("</ol>")
            }
            if (part.omitted > 0) out.append("<p>")
                .append(part.omitted).append(" middenregels overgeslagen.</p>")
            if (out.length > MAX_BYTES) return null
        }
            out.append("</section>")
        }
        out.append("<p>Alleen na expliciete keuze via Android bewaard. " +
            "Een externe bestandsprovider kan de tekst synchroniseren of delen. " +
            "Geen audio, URI, account of netwerkaanvraag in dit HTML-bestand." +
            "</p></main></body></html>")
        return out.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
