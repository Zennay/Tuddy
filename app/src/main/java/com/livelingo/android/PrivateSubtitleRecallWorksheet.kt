package com.livelingo.android

/**
 * Printable, script-free and fully offline worksheet from an explicitly
 * opened and filtered SRT/WebVTT document. No implicit export or storage.
 */
internal object PrivateSubtitleRecallWorksheet {
    private const val MAX_CARDS = 24
    private const val MAX_BYTES = 100_000

    private fun escape(value: String) = buildString(value.length) {
        value.forEach { char ->
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

    fun snapshot(selected: List<PrivateSubtitleCue>): ByteArray? {
        if (selected.isEmpty() || selected.size > MAX_CARDS ||
            PrivateSubtitleDraftEditor.snapshot(selected, SubtitleDraftFormat.SRT) == null
        ) return null
        val out = StringBuilder(
            "<!doctype html>\n<html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<meta http-equiv=\"Content-Security-Policy\" " +
                "content=\"default-src 'none'; style-src 'unsafe-inline'; img-src 'none';" +
                " connect-src 'none'; form-action 'none'\">" +
                "<title>Tuddy - mijn offline ondertiteloefening</title>" +
                "<style>body{font-family:system-ui,sans-serif;max-width:840px;" +
                "margin:auto;padding:22px;color:#19202a;line-height:1.6}" +
                ".note{color:#475569}.card{border:1px solid #7c8794;" +
                "padding:14px;margin:12px 0;break-inside:avoid}" +
                ".hint{font-size:1.35rem;unicode-bidi:plaintext;overflow-wrap:anywhere}" +
                ".answer{unicode-bidi:plaintext;overflow-wrap:anywhere}" +
                ".space{height:38px;border-bottom:1px solid #94a3b8}" +
                ".key{break-before:page;page-break-before:always}" +
                "@media print{body{padding:0}.card{break-inside:avoid}}" +
                "</style></head><body><main>" +
                "<h1>Tuddy · Ondertitels oefenen</h1>" +
                "<p class=\"note\">Zelfgekozen lokale ondertiteltekst, niet door Tuddy " +
                "geverifieerd. Tijdcodes zijn geen audio-uitlijning. " +
                "Geen gesprek of spraakopname automatisch toegevoegd. " +
                "Een externe bestandsprovider kan dit document synchroniseren.</p>" +
                "<section><h2>Schrijf je antwoord</h2>"
        )
        selected.forEachIndexed { index, cue ->
            val words = cue.text.trim().split(Regex("\\s+"))
            val hint = if (words.size >= 3)
                words.take((words.size / 3).coerceAtLeast(1)).joinToString(" ") + " …"
            else "Wat stond op deze tijdcode?"
            out.append("<article class=\"card\"><b>Kaart ").append(index + 1)
                .append(" · ").append(escape(cue.startLabel)).append(" – ")
                .append(escape(cue.endLabel)).append("</b>")
                .append("<p class=\"hint\" dir=\"auto\">").append(escape(hint))
                .append("</p><div class=\"space\"></div></article>")
        }
        out.append("</section><section class=\"key\"><h2>Antwoordblad</h2>")
        selected.forEachIndexed { index, cue ->
            out.append("<article class=\"card\"><b>Kaart ")
                .append(index + 1).append(" · ")
                .append(escape(cue.startLabel)).append("</b>")
                .append("<p class=\"answer\" dir=\"auto\">")
                .append(escape(cue.text).replace("\n", "<br>"))
                .append("</p></article>")
        }
        out.append("</section></main></body></html>\n")
        return out.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
