package com.livelingo.android

/**
 * Script-free, fully offline bilingual fill-in-the-gap workbook. The source
 * sentence is preserved; blanks are only displayed in the export.
 */
internal enum class PinnedClozeDifficulty(val title: String) {
    EASY("Licht · 1 van 3 woorden ontbreekt"),
    NORMAL("Normaal · ongeveer de helft ontbreekt"),
    CHALLENGE("Uitdaging · 2 van 3 woorden ontbreken")
}

internal object PrivatePinnedCaptionClozeHtml {
    private const val MAX_BYTES = 262_144

    private fun escape(text: String): String = buildString(text.length) {
        text.forEach { ch ->
            append(when (ch) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> ch.toString()
            })
        }
    }

    private fun missingWords(value: String, difficulty: PinnedClozeDifficulty): String {
        val words = value.trim().split(Regex("\\s+"))
        // Do not invent word boundaries in unsegmented scripts.
        if (words.size < 2) return "________"
        return words.mapIndexed { index, word ->
            val hide = if (words.size < 3) index == words.lastIndex
                else when (difficulty) {
                    PinnedClozeDifficulty.EASY -> index % 3 == 2
                    PinnedClozeDifficulty.NORMAL -> index % 2 == 1
                    PinnedClozeDifficulty.CHALLENGE -> index % 3 != 0
                }
            if (hide) "________" else word
        }.joinToString(" ")
    }

    /** Same gaps as the exported HTML, for the Android consent preview. */
    fun previewHint(
        text: String,
        languageCode: String? = null,
        difficulty: PinnedClozeDifficulty = PinnedClozeDifficulty.NORMAL
    ): String {
        val words = missingWords(
            PrivatePinnedCaptionStudyTsv.field(text, spreadsheetSafe = false), difficulty
        )
        return if (languageCode == null) words else "[" + languageCode + "] " + words
    }

    fun snapshot(
        rows: List<TranscriptExportRow>,
        direction: PinnedStudyDirection,
        showLanguageTags: Boolean = false,
        difficulty: PinnedClozeDifficulty = PinnedClozeDifficulty.NORMAL
    ): ByteArray? {
        // Same exact source-data and language-label validation as TSV.
        // Printed text must not gain spreadsheet formula-safety apostrophes.
        if (PrivatePinnedCaptionStudyTsv.snapshot(rows, direction, showLanguageTags) == null)
            return null
        // Prompt, exact complete answer, and what is visible before revealing it.
        val cards = ArrayList<Triple<String, String, String>>()
        rows.forEach { row ->
            val source = PrivatePinnedCaptionStudyTsv.field(row.original, spreadsheetSafe = false)
            val target = PrivatePinnedCaptionStudyTsv.field(row.translated, spreadsheetSafe = false)
            if (source.isBlank() || target.isBlank()) return null
            val front = if (showLanguageTags) "[" + row.sourceCode + "] " + source else source
            val back = if (showLanguageTags) "[" + row.targetCode + "] " + target else target
            if (direction != PinnedStudyDirection.TARGET_TO_SOURCE)
                cards.add(Triple(front, back, previewHint(target,
                    if (showLanguageTags) row.targetCode else null, difficulty)))
            if (direction != PinnedStudyDirection.SOURCE_TO_TARGET)
                cards.add(Triple(back, front, previewHint(source,
                    if (showLanguageTags) row.sourceCode else null, difficulty)))
        }
        if (cards.isEmpty() || cards.size > SessionCaptionPins.MAX_PINS * 2) return null

        val out = StringBuilder(
            "<!doctype html><html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<meta http-equiv=\"Content-Security-Policy\" " +
                "content=\"default-src 'none'; style-src 'unsafe-inline';" +
                " form-action 'none'; base-uri 'none'\">" +
                "<title>Tuddy · Tweetalige aanvuloefening</title>" +
                "<style>body{font:16px system-ui,sans-serif;max-width:860px;margin:auto;" +
                "padding:20px;color:#182435;line-height:1.55}" +
                ".card{border:1px solid #8c98a8;padding:15px;margin:14px 0;" +
                "break-inside:avoid;page-break-inside:avoid}" +
                ".phrase,.hint{unicode-bidi:plaintext;overflow-wrap:anywhere}" +
                ".hint{background:#f2f5f8;padding:10px;border-radius:6px}" +
                "summary{cursor:pointer;padding:8px 0}" +
                ".write{border-bottom:1px solid #64748b;min-height:26px;margin-top:12px}" +
                ".answers{break-before:page;page-break-before:always}" +
                "@media print{@page{margin:14mm}body{padding:0}" +
                "details{display:none}.card{break-inside:avoid}}" +
                "</style></head><body><main><h1>Tweetalige aanvuloefening</h1>" +
                "<p>Moeilijkheid: " + escape(difficulty.title) + ". " +
                "Vul de ontbrekende woorden in. Dit zijn uitsluitend " +
                "zelfgekozen lokale zinnen, ongeverifieerde modelvertalingen " +
                "of eigen correcties. Geen automatische taalcontrole." +
                " De antwoorden staan op een nieuwe afdrukpagina.</p>" +
                "<section aria-label=\"Oefenvragen\"><h2>Vragen</h2>"
        )
        cards.forEachIndexed { index, (question, answer, hint) ->
            out.append("<article class=\"card\"><b>Kaart ").append(index + 1)
                .append("</b><p class=\"phrase\"><bdi dir=\"auto\">")
                .append(escape(question)).append("</bdi></p>")
                .append("<p class=\"hint\"><bdi dir=\"auto\">")
                .append(escape(hint)).append("</bdi></p>")
                .append("<div class=\"write\" aria-hidden=\"true\"></div>")
                .append("<details><summary>Bekijk volledig antwoord</summary>")
                .append("<p class=\"phrase\"><bdi dir=\"auto\">")
                .append(escape(answer)).append("</bdi></p></details></article>")
            if (out.length > MAX_BYTES) return null
        }
        out.append("</section><section class=\"answers\"><h2>Antwoordblad</h2>")
        cards.forEachIndexed { index, (_, answer, _) ->
            out.append("<p class=\"phrase\"><b>").append(index + 1)
                .append(".</b> <bdi dir=\"auto\">").append(escape(answer))
                .append("</bdi></p>")
            if (out.length > MAX_BYTES) return null
        }
        out.append("</section><p>Dit lokale HTML-bestand bevat geen " +
            "scripts, audio of netwerkbronnen. Je gekozen Android-bestandsprovider " +
            "kan het bestand wel extern opslaan of delen.</p></main></body></html>")
        return out.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
