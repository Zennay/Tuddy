package com.livelingo.android

/**
 * A self-contained, script-free printable worksheet of explicitly selected
 * HUMAN-written translation suggestions. No speech, models, network or storage.
 * The Android document picker owns the later consent and destination.
 */
internal object SessionCorrectionPrintableWorksheet {
    private const val MAX_BYTES = 80_000

    private fun escape(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

    fun create(entries: List<SessionTranslationCorrection>, answerKey: Boolean): ByteArray? {
        if (entries.isEmpty() || !SessionTranslationCorrections.validEntries(entries)) return null
        val title = if (answerKey) "Mijn eigen vertaalcorrecties" else "Mijn invulblad"
        val doc = StringBuilder(
            "<!doctype html>\n<html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<meta http-equiv=\"Content-Security-Policy\" " +
                "content=\"default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; " +
                "form-action 'none'\">" +
                "<title>" + escape(title) + "</title>" +
                "<style>" +
                "body{font-family:system-ui,sans-serif;color:#111827;background:#fff;" +
                "max-width:850px;margin:auto;padding:24px;line-height:1.5}" +
                ".note{color:#475569}.card{padding:14px 18px;margin:16px 0;" +
                "border:1px solid #9ca3af;border-radius:10px;break-inside:avoid}" +
                ".pair{font-size:.85rem;font-weight:700;color:#334155}" +
                ".text{font-size:1.25rem;white-space:pre-wrap;overflow-wrap:anywhere;" +
                "unicode-bidi:plaintext;margin:10px 0}" +
                ".answer{font-size:1.1rem;white-space:pre-wrap;overflow-wrap:anywhere;" +
                "unicode-bidi:plaintext}" +
                ".lines{height:55px;border-bottom:1px dashed #64748b}" +
                "@media print{body{padding:0}.card{border-color:#666}}" +
                "</style></head><body><main><h1>" + escape(title) + "</h1>" +
                "<p class=\"note\">Uitsluitend eigen, niet onafhankelijk gecontroleerde " +
                "vertaalvoorstellen. Geen modeluitvoer of bewijs voor ondersteunde " +
                "offline talen. Dit HTML-bestand wordt alleen op jouw bewuste verzoek " +
                "bij een gekozen bestandsprovider opgeslagen en kan buiten Tuddy " +
                "worden gedeeld.</p>"
        )
        for ((index, item) in entries.withIndex()) {
            doc.append("<article class=\"card\"><div class=\"pair\">")
                .append(index + 1).append(". ")
                .append(escape(item.sourceCode)).append(" &rarr; ")
                .append(escape(item.targetCode)).append("</div><div class=\"text\" dir=\"auto\">")
                .append(escape(item.original)).append("</div>")
            if (answerKey) {
                doc.append("<div class=\"pair\">Mijn eigen vertaling</div>")
                    .append("<div class=\"answer\" dir=\"auto\">")
                    .append(escape(item.translation)).append("</div>")
            } else {
                doc.append("<div class=\"pair\">Mijn antwoord:</div>")
                    .append("<div class=\"lines\"></div>")
            }
            doc.append("</article>\n")
            if (doc.length > MAX_BYTES) return null
        }
        doc.append("</main></body></html>\n")
        return doc.toString().toByteArray(Charsets.UTF_8).takeIf { it.size <= MAX_BYTES }
    }
}
