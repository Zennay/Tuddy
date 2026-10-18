package com.livelingo.android

import java.util.Locale

/**
 * Script-free printable booklet containing ONLY intentionally saved personal
 * word meanings. No transcript, model output, remote font or network requests.
 * Android SAF consent and file destination are owned by the caller.
 */
internal object PrivatePrintableOwnWordbook {
    private const val MAX_ROWS = 120
    private const val MAX_BYTES = 120_000
    private val CODES = PrivateOwnWordDeck.SUPPORTED_CODES
    private val RTL = setOf("ar", "fa", "ur", "ckb", "ps")

    private fun escape(value: String): String = buildString(value.length) {
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

    private fun safe(value: String, max: Int): Boolean =
        value.isNotBlank() && value.length <= max && value == value.trim() &&
            value.none { char ->
                Character.isISOControl(char) || Character.isSurrogate(char) ||
                    char == '\u2028' || char == '\u2029' ||
                    (Character.getType(char) == Character.FORMAT.toInt() && char != '\u200C')
            }

    fun snapshot(notes: List<OwnWord>, selectedLanguage: String?): ByteArray? {
        if (notes.isEmpty() || notes.size > MAX_ROWS ||
            (selectedLanguage != null && selectedLanguage !in CODES)) return null

        val seen = HashSet<String>()
        if (notes.any {
            it.language !in CODES || !safe(it.word, 48) ||
                it.word.any(Char::isWhitespace) || !safe(it.ownMeaning, 180) ||
                !seen.add(it.language + "|" + it.word.lowercase(Locale.ROOT))
        }) return null
        val selected = notes.filter {
            selectedLanguage == null || it.language == selectedLanguage
        }.sortedWith(compareBy(OwnWord::language, OwnWord::word))
        if (selected.isEmpty()) return null

        val title = "Mijn eigen woorden" +
            (if (selectedLanguage == null) "" else " · " + selectedLanguage.uppercase(Locale.ROOT))
        val html = StringBuilder(
            "<!doctype html>\n<html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<title>" + escape(title) + "</title>" +
                "<style>body{font-family:system-ui,sans-serif;max-width:830px;" +
                "margin:0 auto;padding:24px;color:#171923;background:white;line-height:1.5}" +
                "h1{font-size:1.8rem}p.note{color:#475569}" +
                ".card{padding:14px 18px;border:1px solid #64748b;" +
                "border-radius:12px;margin:14px 0;break-inside:avoid;page-break-inside:avoid}" +
                ".tag{color:#475569;font-size:.86rem;font-weight:600}" +
                ".word{font-size:1.65rem;font-weight:700;margin:5px 0;" +
                "unicode-bidi:plaintext;overflow-wrap:anywhere}" +
                ".meaning{font-size:1.15rem;white-space:pre-wrap;overflow-wrap:anywhere;" +
                "unicode-bidi:plaintext}" +
                "@media print{body{padding:0}.card{border-color:#777}}</style>" +
                "</head><body><main><h1>" + escape(title) + "</h1>" +
                "<p class=\"note\">Dit zijn door mij zelf geschreven woordbetekenissen, " +
                "niet gecontroleerd of automatisch vertaald door Tuddy. " +
                "Controleer de tekst voor gebruik. Het gekozen bestand kan buiten " +
                "de app worden bewaard of gedeeld.</p>\n"
        )
        selected.forEach { note ->
            val dir = if (note.language in RTL) "rtl" else "ltr"
            html.append("<article class=\"card\"><div class=\"tag\">")
                .append(note.language.uppercase(Locale.ROOT))
                .append(" · eigen notitie</div><div class=\"word\" lang=\"")
                .append(note.language).append("\" dir=\"").append(dir).append("\">")
                .append(escape(note.word))
                .append("</div><div class=\"tag\">Mijn eigen betekenis</div>")
                .append("<div class=\"meaning\" dir=\"auto\">")
                .append(escape(note.ownMeaning)).append("</div></article>\n")
            if (html.length > MAX_BYTES) return null
        }
        html.append("</main></body></html>\n")
        return html.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
