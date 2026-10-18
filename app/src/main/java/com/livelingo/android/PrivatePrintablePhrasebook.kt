package com.livelingo.android

/**
 * A standalone, script-free HTML booklet of manually authored phrase notes.
 *
 * Does not resolve or invent any translations. Callers must request explicit
 * export consent and let the user choose an Android SAF destination.
 */
internal object PrivatePrintablePhrasebook {
    private const val MAX_ROWS = 80
    private const val MAX_BYTES = 90_000
    private val LANGUAGES = mapOf(
        "kmr" to "Kurmanji",
        "ckb" to "Sorani",
        "ps" to "Pasjtoe"
    )

    private fun escape(value: String): String = buildString(value.length) {
        for (char in value) {
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

    private fun safe(value: String, maxLength: Int): Boolean =
        value.isNotBlank() && value.length <= maxLength && value == value.trim() &&
            value.none { char ->
                Character.isISOControl(char) || Character.isSurrogate(char) ||
                    char == '\u2028' || char == '\u2029' ||
                    (Character.getType(char) == Character.FORMAT.toInt() &&
                        char != '\u200C')
            }

    /**
     * The source deck controls the language and maximum row count. Still
     * validate every field here rather than trusting a caller supplied list.
     * No JavaScript, fonts, remote resources or embedded user-provided markup.
     */
    fun snapshot(notes: List<PersonalPhrase>, selectedLanguage: String?): ByteArray? {
        if (notes.isEmpty() || notes.size > MAX_ROWS ||
            (selectedLanguage != null && selectedLanguage !in LANGUAGES)
        ) return null

        val selected = notes.filter {
            (selectedLanguage == null || it.language == selectedLanguage)
        }
        if (selected.isEmpty()) return null
        if (notes.any { row ->
                row.language !in LANGUAGES ||
                    row.meaningLanguage !in setOf("nl", "en") ||
                    !safe(row.phrase, 120) || !safe(row.ownMeaning, 180)
            }
        ) return null

        val title = if (selectedLanguage == null) "Mijn eigen zinnen"
            else "Mijn eigen zinnen - " + LANGUAGES.getValue(selectedLanguage)
        val doc = StringBuilder(
            "<!doctype html>\n<html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<title>" + escape(title) + "</title>\n" +
                "<style>" +
                "body{font-family:system-ui,sans-serif;margin:0 auto;max-width:850px;" +
                "padding:24px;color:#171923;background:#fff;line-height:1.5}" +
                "h1{font-size:1.8rem}p.note{color:#4a5568}" +
                ".card{border:1px solid #6b7280;border-radius:12px;padding:18px;" +
                "margin:18px 0;break-inside:avoid;page-break-inside:avoid}" +
                ".tag{font-size:.85rem;color:#475569;font-weight:600}" +
                ".phrase{font-size:1.6rem;font-weight:650;overflow-wrap:anywhere;" +
                "unicode-bidi:plaintext;margin:6px 0 14px}" +
                ".meaning{font-size:1.15rem;overflow-wrap:anywhere;unicode-bidi:plaintext}" +
                "@media print{body{padding:0}.card{border-color:#777}}" +
                "</style></head><body><main><h1>" + escape(title) + "</h1>" +
                "<p class=\"note\">Alle zinnen en betekenissen zijn door mijzelf " +
                "geschreven. Niet gecontroleerd of automatisch vertaald door Tuddy. " +
                "Controleer de inhoud voor gebruik. Dit bestand kan buiten Tuddy " +
                "worden bewaard of gedeeld.</p>\n"
        )
        val sorted = selected.sortedWith(compareBy(PersonalPhrase::language, PersonalPhrase::phrase))
        for (record in sorted) {
            val direction = if (record.language == "ckb" || record.language == "ps")
                "rtl" else "ltr"
            doc.append("<article class=\"card\"><div class=\"tag\">")
                .append(escape(LANGUAGES.getValue(record.language)))
                .append(" &middot; eigen notitie</div><div lang=\"")
                .append(record.language)
                .append("\" dir=\"").append(direction)
                .append("\" class=\"phrase\">")
                .append(escape(record.phrase))
                .append("</div><div class=\"tag\">Mijn eigen betekenis (")
                .append(record.meaningLanguage.uppercase())
                .append(")</div><div lang=\"").append(record.meaningLanguage)
                .append("\" dir=\"ltr\" class=\"meaning\">")
                .append(escape(record.ownMeaning)).append("</div></article>\n")
            if (doc.length > MAX_BYTES) return null
        }
        doc.append("</main></body></html>\n")
        val bytes = doc.toString().toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= MAX_BYTES }
    }
}
