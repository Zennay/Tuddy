package com.livelingo.android

import com.livelingo.android.model.LocalGlossary
import java.util.Locale

/**
 * Explicitly consented offline HTML study bundle, not a transcript archive.
 * Only three manually curated/user-saved decks are admitted. No network,
 * scripts, audio, conversation history, external style or font dependencies.
 */
internal object PrivateCombinedStudyBook {
    private const val MAX_BYTES = 250_000
    private val PERSONAL_LANGUAGES = setOf("kmr", "ckb", "ps")
    private val RTL = setOf("ar", "fa", "ur", "he", "ckb", "ps")
    private fun escape(s: String): String = buildString(s.length) {
        s.forEach { ch ->
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
    private fun safe(s: String, max: Int): Boolean =
        s.isNotBlank() && s.length <= max && s == s.trim() &&
            s.none { ch ->
                Character.isISOControl(ch) || Character.isSurrogate(ch) ||
                    ch == '\u2028' || ch == '\u2029' ||
                    (Character.getType(ch) == Character.FORMAT.toInt() &&
                        ch != '\u200C')
            }
    private fun dir(lang: String): String = if (lang in RTL) "rtl" else "ltr"

    fun snapshot(
        curated: List<SavedWord>,
        ownWords: List<OwnWord>?,
        phrases: List<PersonalPhrase>?
    ): ByteArray? {
        // Null means unreadable private storage. Never silently omit records.
        if (ownWords == null || phrases == null) return null
        if (curated.size > 120 || ownWords.size > 120 || phrases.size > 80 ||
            curated.size + ownWords.size + phrases.size == 0) return null
        val curatedKeys = HashSet<String>()
        val curatedRows = curated.map { row ->
            if (row.language !in setOf("nl", "en", "de", "fr", "es") ||
                !safe(row.word, 40) ||
                !curatedKeys.add(row.language + "|" + row.word.lowercase(Locale.ROOT)))
                return null
            val entry = LocalGlossary.lookup(row.language, row.word) ?: return null
            if (!safe(entry.definition, 250) || entry.illustration.length > 16)
                return null
            row to entry
        }
        val ownKeys = HashSet<String>()
        if (ownWords.any {
            it.language !in PrivateOwnWordDeck.SUPPORTED_CODES ||
                !safe(it.word, 48) || it.word.any(Char::isWhitespace) ||
                !safe(it.ownMeaning, 180) ||
                !ownKeys.add(it.language + "|" + it.word.lowercase(Locale.ROOT))
        }) return null
        val phraseKeys = HashSet<String>()
        if (phrases.any {
            it.language !in PERSONAL_LANGUAGES ||
                it.meaningLanguage !in setOf("nl", "en") ||
                !safe(it.phrase, 120) || !safe(it.ownMeaning, 180) ||
                !phraseKeys.add(it.language + "|" + it.phrase.lowercase(Locale.ROOT))
        }) return null

        val html = StringBuilder(
            "<!doctype html>\n<html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<title>Tuddy · Mijn offline studieboek</title><style>" +
                "body{font-family:system-ui,sans-serif;background:#fff;color:#161923;" +
                "max-width:850px;margin:auto;padding:24px;line-height:1.6}" +
                "h1{font-size:1.9rem}h2{font-size:1.4rem;break-after:avoid}" +
                ".note{color:#475569}.item{border:1px solid #64748b;border-radius:11px;" +
                "padding:14px 18px;margin:14px 0;break-inside:avoid}" +
                ".tag{font-size:.84rem;color:#475569;font-weight:600}" +
                ".word{font-size:1.45rem;font-weight:650;overflow-wrap:anywhere}" +
                ".meaning{font-size:1.1rem;overflow-wrap:anywhere;unicode-bidi:plaintext}" +
                "@media print{body{padding:0}.item{border-color:#888}}" +
                "</style></head><body><main>" +
                "<h1>Tuddy · Mijn offline studieboek</h1>" +
                "<p class=\"note\">Alleen door mij bewust opgeslagen leermateriaal." +
                " Dit boek bevat geen gesprekken, audio of automatisch opgeslagen vertalingen." +
                " Controleer teksten voor gebruik; een gekozen bestandsprovider kan het" +
                " document extern synchroniseren.</p>\n"
        )
        fun section(title: String, description: String) {
            html.append("<section><h2>").append(escape(title))
                .append("</h2><p class=\"note\">")
                .append(escape(description)).append("</p>\n")
        }
        fun item(code: String, title: String, meaning: String, note: String) {
            html.append("<article class=\"item\"><div class=\"tag\">")
                .append(escape(code.uppercase(Locale.ROOT))).append(" · ")
                .append(escape(note))
                .append("</div><div class=\"word\" lang=\"")
                .append(escape(code)).append("\" dir=\"").append(dir(code))
                .append("\">").append(escape(title))
                .append("</div><div class=\"meaning\" dir=\"auto\">")
                .append(escape(meaning)).append("</div></article>\n")
        }
        if (curatedRows.isNotEmpty()) {
            section("Mijn bewaarde basiswoorden",
                "Kleine ingebouwde woordenlijst. Niet uit een live gesprek of externe vertaler.")
            curatedRows.sortedWith(compareBy({ it.first.language }, { it.first.word }))
                .forEach { (row, entry) ->
                    item(row.language, row.word + " " + entry.illustration,
                        entry.definition, "ingebouwde basisbetekenis")
                }
            html.append("</section>\n")
        }
        if (ownWords.isNotEmpty()) {
            section("Mijn eigen woordbetekenissen",
                "Zelfgeschreven betekenissen, niet onafhankelijk geverifieerd.")
            ownWords.sortedWith(compareBy(OwnWord::language, OwnWord::word))
                .forEach { item(it.language, it.word, it.ownMeaning, "eigen, ongecontroleerd woord") }
            html.append("</section>\n")
        }
        if (phrases.isNotEmpty()) {
            section("Mijn eigen oefenzinnen",
                "Eigen betekenis in Nederlands of Engels, niet door Tuddy vertaald of gecontroleerd.")
            phrases.sortedWith(compareBy(PersonalPhrase::language, PersonalPhrase::phrase))
                .forEach {
                    item(it.language, it.phrase, it.ownMeaning,
                        "eigen zin · betekenis in " + it.meaningLanguage.uppercase(Locale.ROOT))
                }
            html.append("</section>\n")
        }
        html.append("</main></body></html>\n")
        return html.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
