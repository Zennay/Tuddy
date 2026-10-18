package com.livelingo.android

import com.livelingo.android.model.LocalGlossary

/**
 * Human-invoked, printable offline worksheet. Source material is first
 * admitted by the same strict privacy/content boundary as the combined
 * offline book. The answer key is printed on a separate page, never hidden
 * behind a network call, dynamic script, or user account.
 */
internal object PrivateDailyPracticeWorksheet {
    private const val MAX_BYTES = 250_000

    private data class Entry(
        val language: String,
        val source: String,
        val answer: String,
        val kind: String
    )

    private fun escape(raw: String): String = buildString(raw.length) {
        raw.forEach { ch ->
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

    private fun direction(language: String): String =
        if (language in setOf("ckb", "ps", "ar", "fa", "ur", "he", "ug"))
            "rtl" else "ltr"

    fun snapshot(
        curated: List<SavedWord>,
        ownWords: List<OwnWord>?,
        phrases: List<PersonalPhrase>?
    ): ByteArray? {
        // Reuse exactly the existing human-content limits, canonical glossary
        // lookup, language allowlists, size rules and duplicate guards.
        if (PrivateCombinedStudyBook.snapshot(curated, ownWords, phrases) == null)
            return null
        val personal = ownWords ?: return null
        val sentences = phrases ?: return null
        val entries = mutableListOf<Entry>()
        for (row in curated.sortedWith(compareBy(SavedWord::language, SavedWord::word))) {
            val checked = LocalGlossary.lookup(row.language, row.word) ?: return null
            entries.add(Entry(row.language, row.word, checked.definition, "Ingebouwd basiswoord"))
        }
        for (row in personal.sortedWith(compareBy(OwnWord::language, OwnWord::word))) {
            entries.add(Entry(row.language, row.word, row.ownMeaning,
                "Eigen woord · niet geverifieerd"))
        }
        for (row in sentences.sortedWith(compareBy(PersonalPhrase::language, PersonalPhrase::phrase))) {
            entries.add(Entry(row.language, row.phrase, row.ownMeaning,
                "Eigen zin · niet geverifieerd"))
        }
        if (entries.isEmpty()) return null

        val html = StringBuilder(
            "<!doctype html><html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<title>Tuddy · Mijn oefenblad van vandaag</title><style>" +
                "body{font-family:system-ui,sans-serif;max-width:850px;margin:0 auto;" +
                "padding:24px;background:#fff;color:#111827;line-height:1.5}" +
                "h1{font-size:1.75rem}h2{font-size:1.3rem}" +
                ".note{color:#475569}.card{border-bottom:1px solid #9ca3af;" +
                "padding:14px 0;break-inside:avoid}" +
                ".prompt{font-size:1.35rem;font-weight:600;overflow-wrap:anywhere;" +
                "unicode-bidi:plaintext}.tag{font-size:.8rem;color:#475569}" +
                ".answer-space{border-bottom:1px dotted #9ca3af;height:35px}" +
                ".key{break-before:page;page-break-before:always}" +
                ".answer{font-size:1.15rem;overflow-wrap:anywhere;unicode-bidi:plaintext}" +
                "@media print{body{padding:0}.key{break-before:page}}" +
                "</style></head><body><main>" +
                "<h1>Mijn offline oefenblad van vandaag</h1>" +
                "<p class=\"note\">Alleen bewust gekozen privéwoorden en zelfgeschreven " +
                "zinnen. Vul je eigen betekenis op papier in. Controleer later " +
                "de antwoorden op de aparte sleutelpagina. Eigen betekenissen " +
                "zijn niet door Tuddy of een taalexpert geverifieerd. " +
                "Dit bestand valt buiten het wissen van appgegevens.</p>"
        )
        entries.forEachIndexed { index, entry ->
            html.append("<article class=\"card\"><div class=\"tag\">")
                .append(index + 1).append(" · ")
                .append(escape(entry.kind)).append(" · ")
                .append(escape(entry.language.uppercase()))
                .append("</div><div class=\"prompt\" lang=\"")
                .append(escape(entry.language)).append("\" dir=\"")
                .append(direction(entry.language)).append("\">")
                .append(escape(entry.source))
                .append("</div><div class=\"answer-space\"></div></article>")
            if (html.length > MAX_BYTES) return null
        }
        html.append("<section class=\"key\"><h2>Mijn antwoorden · controleer zelf</h2>")
        entries.forEachIndexed { index, entry ->
            html.append("<article class=\"card\"><div class=\"tag\">")
                .append(index + 1).append(" · ")
                .append(escape(entry.kind)).append("</div>")
                .append("<div class=\"answer\" dir=\"auto\">")
                .append(escape(entry.answer)).append("</div></article>")
            if (html.length > MAX_BYTES) return null
        }
        html.append("</section></main></body></html>\n")
        return html.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
