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

/** The learner chooses how much of each hidden whitespace-separated term to see. */
internal enum class PinnedClozeHintMode(val title: String) {
    BLANK("Alleen invulstrepen"),
    FIRST_LETTER("Eerste teken van verborgen woorden"),
    LENGTH("Woordlengte als invulstrepen")
}

/** Space is applied to both the browser answer field and printed writing lines. */
internal enum class PinnedClozeAnswerSpace(
    val title: String, val printHeightPx: Int, val browserRows: Int
) {
    STANDARD("Normaal · twee schrijflijnen", 66, 3),
    SPACIOUS("Ruim · vier schrijflijnen", 132, 5)
}

/** An explicit content audience; a student handout must not embed the answer. */
internal enum class PinnedClozeAudience(val title: String) {
    SELF_STUDY("Zelfstudie · antwoorden na bewuste keuze"),
    STUDENT_HANDOUT("Leerlingenblad · geen antwoorden in bestand")
}

/** Reproducible alternative missing-word placements for the same chosen sentences. */
internal enum class PinnedClozePass(val title: String, val indices: List<Int>) {
    FIRST("Ronde 1 · eerste invulpatroon", listOf(0)),
    SECOND("Ronde 2 · andere ontbrekende woorden", listOf(1)),
    THIRD("Ronde 3 · aanvullend invulpatroon", listOf(2)),
    ALL_THREE("Alle drie rondes · volledig oefenboek", listOf(0, 1, 2))
}

internal object PrivatePinnedCaptionClozeHtml {
    private data class PracticeCard(
        val question: String,
        val answer: String,
        val hint: String,
        val roundIndex: Int
    )
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

    private fun partOfTerm(codePoint: Int): Boolean =
        Character.isLetterOrDigit(codePoint) || when (Character.getType(codePoint)) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt() -> true
            else -> false
        }

    /** Keep authored surrounding punctuation visible, but never reveal the answer term. */
    private fun hiddenWord(word: String, hintMode: PinnedClozeHintMode): String {
        var begin = 0
        while (begin < word.length) {
            val point = word.codePointAt(begin)
            if (partOfTerm(point)) break
            begin += Character.charCount(point)
        }
        var end = word.length
        while (end > begin) {
            val point = word.codePointBefore(end)
            if (partOfTerm(point)) break
            end -= Character.charCount(point)
        }
        if (begin >= end) return "________"
        val term = word.substring(begin, end)
        val replacement = when (hintMode) {
            PinnedClozeHintMode.BLANK -> "________"
            PinnedClozeHintMode.FIRST_LETTER -> {
                val first = term.codePointAt(0)
                if (Character.isLetterOrDigit(first) && term.codePointCount(0, term.length) > 1)
                    String(Character.toChars(first)) + "____"
                else "________"
            }
            PinnedClozeHintMode.LENGTH ->
                "_".repeat(term.codePointCount(0, term.length).coerceIn(1, 16))
        }
        return word.substring(0, begin) + replacement + word.substring(end)
    }

    private fun missingWords(
        value: String,
        difficulty: PinnedClozeDifficulty,
        hintMode: PinnedClozeHintMode,
        roundIndex: Int
    ): String {
        val words = value.trim().split(Regex("\\s+"))
        // Do not invent word boundaries in unsegmented scripts.
        if (words.size < 2) return "________"
        val pass = roundIndex.coerceIn(0, 2)
        return words.mapIndexed { index, word ->
            val hide = if (words.size < 3) index == (1 - pass % 2)
                else when (difficulty) {
                    PinnedClozeDifficulty.EASY -> (index + pass) % 3 == 2
                    PinnedClozeDifficulty.NORMAL -> when (pass) {
                        0 -> index % 2 == 1
                        1 -> index % 2 == 0
                        else -> index % 6 in 1..3
                    }
                    PinnedClozeDifficulty.CHALLENGE -> (index + pass) % 3 != 0
                }
            if (hide) hiddenWord(word, hintMode) else word
        }.joinToString(" ")
    }

    /** Same gaps as the exported HTML, for the Android consent preview. */
    fun previewHint(
        text: String,
        languageCode: String? = null,
        difficulty: PinnedClozeDifficulty = PinnedClozeDifficulty.NORMAL,
        hintMode: PinnedClozeHintMode = PinnedClozeHintMode.BLANK,
        roundIndex: Int = 0
    ): String {
        val words = missingWords(
            PrivatePinnedCaptionStudyTsv.field(text, spreadsheetSafe = false),
            difficulty, hintMode, roundIndex
        )
        return if (languageCode == null) words else "[" + languageCode + "] " + words
    }

    fun snapshot(
        rows: List<TranscriptExportRow>,
        direction: PinnedStudyDirection,
        showLanguageTags: Boolean = false,
        difficulty: PinnedClozeDifficulty = PinnedClozeDifficulty.NORMAL,
        hintMode: PinnedClozeHintMode = PinnedClozeHintMode.BLANK,
        answerSpace: PinnedClozeAnswerSpace = PinnedClozeAnswerSpace.STANDARD,
        audience: PinnedClozeAudience = PinnedClozeAudience.SELF_STUDY,
        pass: PinnedClozePass = PinnedClozePass.FIRST
    ): ByteArray? {
        // Same exact source-data and language-label validation as TSV.
        // Printed text must not gain spreadsheet formula-safety apostrophes.
        if (PrivatePinnedCaptionStudyTsv.snapshot(rows, direction, showLanguageTags) == null)
            return null
        // Preserve each answer exactly while deliberately varying gap placements.
        // No randomness, remote model or inferred word timings.
        val cards = ArrayList<PracticeCard>()
        pass.indices.forEach { round ->
            rows.forEach { row ->
                val source = PrivatePinnedCaptionStudyTsv.field(
                    row.original, spreadsheetSafe = false
                )
                val target = PrivatePinnedCaptionStudyTsv.field(
                    row.translated, spreadsheetSafe = false
                )
                if (source.isBlank() || target.isBlank()) return null
                val front = if (showLanguageTags) "[" + row.sourceCode + "] " + source else source
                val back = if (showLanguageTags) "[" + row.targetCode + "] " + target else target
                if (direction != PinnedStudyDirection.TARGET_TO_SOURCE)
                    cards.add(PracticeCard(front, back, previewHint(target,
                        if (showLanguageTags) row.targetCode else null,
                        difficulty, hintMode, round), round))
                if (direction != PinnedStudyDirection.SOURCE_TO_TARGET)
                    cards.add(PracticeCard(back, front, previewHint(source,
                        if (showLanguageTags) row.sourceCode else null,
                        difficulty, hintMode, round), round))
            }
        }
        if (cards.isEmpty() || cards.size > SessionCaptionPins.MAX_PINS * 2 * 3) return null
        val cardsPerRound = cards.size / pass.indices.size

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
                ".work{display:block;font-weight:600;margin:12px 0 4px}" +
                "textarea{display:block;box-sizing:border-box;width:100%;min-height:84px;" +
                "border:1px solid #64748b;border-radius:6px;padding:8px;" +
                "font:inherit;resize:vertical;unicode-bidi:plaintext}" +
                ".grade{border:0;padding:0;margin:10px 0}" +
                ".grade label{display:inline-block;margin:4px 18px 4px 0}" +
                ".printgrade{display:none}" +
                ".reset{margin-top:8px;padding:7px 12px;border:1px solid #64748b;" +
                "border-radius:5px;background:#fff;color:#182435;cursor:pointer}" +
                "summary{cursor:pointer;padding:8px 0}" +
                ".write{display:none;border-bottom:1px solid #64748b;" +
                "min-height:" + answerSpace.printHeightPx + "px;margin-top:12px}" +
                ".answers{display:none;break-before:page;page-break-before:always}" +
                ".answers:target{display:block}" +
                ".roundheading{border-bottom:2px solid #64748b;padding:9px 0}" +
                ".roundnav a{display:inline-block;margin:6px 12px 6px 0}" +
                ".answerlink{display:inline-block;margin:12px 0}" +
                "@media print{@page{margin:14mm}body{padding:0}" +
                "details,.work,textarea,.reset,.grade{display:none}" +
                ".write,.printgrade,.answers{display:block}" +
                ".answerlink,.roundnav{display:none}" +
                ".card{break-inside:avoid}.roundheading.newpage{" +
                    "break-before:page;page-break-before:always}}" +
                "</style></head><body><main><h1>Tweetalige aanvuloefening</h1>" +
                "<p>Moeilijkheid: " + escape(difficulty.title) + ". " +
                "Invulhint: " + escape(hintMode.title) + ". " +
                "Schrijfruimte: " + escape(answerSpace.title) + ". " +
                "Bestemming: " + escape(audience.title) + ". " +
                "Oefenrondes: " + escape(pass.title) + ". " +
                "Elke ronde heeft nieuwe gaten in dezelfde eigen zinnen; " +
                "bij erg korte of ongesegmenteerde zinnen kunnen gaten gelijk blijven. " +
                "Vul de ontbrekende woorden in. Dit zijn uitsluitend " +
                "zelfgekozen lokale zinnen, ongeverifieerde modelvertalingen " +
                "of eigen correcties. Geen automatische taalcontrole." +
                " Schrijf je antwoord direct in dit offline browserblad " +
                "(niet automatisch nagekeken of door Tuddy opgeslagen). " +
                (if (audience == PinnedClozeAudience.SELF_STUDY)
                    "De eigenlijke antwoorden kun je per kaart bewust openklappen " +
                        "of met de knop onderaan als compleet antwoordblad openen. "
                else "Dit leerlingenbestand bevat geen volledige antwoorden " +
                    "of verborgen antwoordblad. Vraag je docent om de oplossing. ") +
                "Beoordeel iedere kaart desgewenst zelf; er is geen score " +
                "of voortgangsopslag. Wissen leegt ook je kaartbeoordeling. " +
                "Sommige browsers kunnen zelf getypte velden herstellen. " +
                (if (audience == PinnedClozeAudience.SELF_STUDY)
                    "De antwoorden staan op een nieuwe afdrukpagina.</p>"
                else "Er is geen afdrukantwoordblad.</p>") +
                "<section id=\"questions\" aria-label=\"Oefenvragen\"><h2>Vragen</h2>"
        )
        if (pass.indices.size > 1) {
            out.append("<nav class=\"roundnav\" aria-label=\"Kies oefenronde\">")
            pass.indices.forEach { round ->
                out.append("<a href=\"#round-").append(round + 1)
                    .append("\">Ga naar ronde ").append(round + 1).append("</a>")
            }
            out.append("</nav>")
        }
        cards.forEachIndexed { index, card ->
            val (question, answer, hint, roundIndex) = card
            if (index == 0 || cards[index - 1].roundIndex != roundIndex) {
                out.append("<h2 class=\"roundheading")
                if (index != 0) out.append(" newpage")
                out.append("\" id=\"round-").append(roundIndex + 1)
                    .append("\">Oefenronde ").append(roundIndex + 1).append("</h2>")
            }
            out.append("<article class=\"card\"><b>Ronde ").append(roundIndex + 1)
                .append(" · kaart ").append(index % cardsPerRound + 1)
                .append("</b><p class=\"phrase\"><bdi dir=\"auto\">")
                .append(escape(question)).append("</bdi></p>")
                .append("<p class=\"hint\"><bdi dir=\"auto\">")
                .append(escape(hint)).append("</bdi></p>")
                .append("<form autocomplete=\"off\"><label class=\"work\" for=\"typed-").append(index + 1)
                .append("\">Mijn eigen antwoord (alleen tijdelijk in de browser)</label>")
                .append("<textarea id=\"typed-").append(index + 1)
                .append("\" rows=\"").append(answerSpace.browserRows)
                .append("\" dir=\"auto\" spellcheck=\"false\" " +
                    "autocomplete=\"off\" autocorrect=\"off\" " +
                    "aria-label=\"Mijn eigen antwoord voor kaart ").append(index + 1)
                .append("\"></textarea>")
                .append("<fieldset class=\"grade\"><legend>Zelf beoordelen " +
                    "(tijdelijk, zonder score)</legend>")
                .append("<label><input type=\"radio\" name=\"grade-").append(index + 1)
                .append("\" value=\"retry\"> Nog oefenen</label>")
                .append("<label><input type=\"radio\" name=\"grade-").append(index + 1)
                .append("\" value=\"known\"> Ik wist het</label></fieldset>")
                .append("<button class=\"reset\" type=\"reset\">" +
                    "Wis mijn antwoord en beoordeling</button></form>")
                .append("<p class=\"printgrade\">Zelf beoordelen: &#9633; " +
                    "Nog oefenen &nbsp; &#9633; Ik wist het</p>")
                .append("<div class=\"write\" aria-hidden=\"true\"></div>")
            if (audience == PinnedClozeAudience.SELF_STUDY) {
                out.append("<details><summary>Bekijk volledig antwoord</summary>")
                    .append("<p class=\"phrase\"><bdi dir=\"auto\">")
                    .append(escape(answer)).append("</bdi></p></details>")
            }
            out.append("</article>")
            if (out.length > MAX_BYTES) return null
        }
        out.append("</section>")
        if (audience == PinnedClozeAudience.SELF_STUDY) {
            out.append("<a class=\"answerlink\" href=\"#all-answers\">" +
                "Open volledige antwoordblad (bewuste keuze)</a>")
                .append("<section class=\"answers\" id=\"all-answers\">" +
                    "<h2>Antwoordblad</h2>" +
                    "<a class=\"answerlink\" href=\"#questions\">" +
                    "Terug naar vragen</a>")
            cards.forEachIndexed { index, card ->
                val answer = card.answer
                out.append("<p class=\"phrase\"><b>Ronde ").append(card.roundIndex + 1)
                    .append(" · kaart ").append(index % cardsPerRound + 1)
                    .append(".</b> <bdi dir=\"auto\">").append(escape(answer))
                    .append("</bdi></p>")
                if (out.length > MAX_BYTES) return null
            }
            out.append("</section>")
        }
        out.append("<p>Dit lokale HTML-bestand bevat geen " +
            "scripts, audio of netwerkbronnen. Je gekozen Android-bestandsprovider " +
            "kan het bestand wel extern opslaan of delen.</p></main></body></html>")
        return out.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
