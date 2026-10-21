package com.livelingo.android

import java.util.Locale

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

/** Space is applied to both the browser answer field and real paper writing lines. */
internal enum class PinnedClozeAnswerSpace(
    val title: String, val printLines: Int, val browserRows: Int
) {
    STANDARD("Normaal · twee schrijflijnen", 2, 3),
    SPACIOUS("Ruim · vier schrijflijnen", 4, 5)
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

/** Choose whether repeated rounds retain source order or change it predictably. */
internal enum class PinnedClozeCardOrder(val title: String) {
    SOURCE("Originele zinsvolgorde"),
    VARIED("Afwisselende kaartvolgorde per ronde")
}

internal object PrivatePinnedCaptionClozeHtml {
    private data class PracticeCard(
        val question: String,
        val answer: String,
        val hint: String,
        val questionLanguageCode: String,
        val answerLanguageCode: String,
        val hintLanguageCode: String,
        val roundIndex: Int
    )
    private const val MAX_BYTES = 262_144
    private val LANGUAGE_TAG_PREFIX =
        Regex("""^\[([A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*)\]\s""")
    private val LANGUAGE_TAG_CODE =
        Regex("""^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$""")
    private val RTL_BASE_LANGUAGES =
        setOf("ar", "ckb", "dv", "fa", "he", "ps", "sd", "ug", "ur", "yi")
    private val RTL_SCRIPTS = setOf("arab", "hebr", "syrc", "thaa")

    /**
     * A visible [ar]/[ps]/[ckb] label is Latin text, so dir=auto on the whole
     * string incorrectly makes Arabic-script content LTR. Honor a BCP-47
     * language/script tag when present; otherwise let the actual text decide.
     */
    private fun htmlDirection(value: String, languageCode: String? = null): String {
        val tag = languageCode?.takeIf { LANGUAGE_TAG_CODE.matches(it) }
            ?: LANGUAGE_TAG_PREFIX.find(value)?.groupValues?.get(1) ?: return "auto"
        val subtags = tag.split('-')
        val script = subtags.firstOrNull {
            it.length == 4 && it.all { ch -> ch.isLetter() }
        }?.lowercase(Locale.ROOT)
        if (script != null) return if (script in RTL_SCRIPTS) "rtl" else "ltr"
        return if (subtags.first().lowercase(Locale.ROOT) in RTL_BASE_LANGUAGES)
            "rtl" else "ltr"
    }

    /** Keep the Android consent preview in the export's exact script direction. */
    fun isRightToLeft(languageCode: String): Boolean =
        htmlDirection("", languageCode) == "rtl"

    private fun htmlLanguageAttribute(languageCode: String?): String {
        if (languageCode == null || !LANGUAGE_TAG_CODE.matches(languageCode)) return ""
        return " lang=\"" + escape(languageCode) + "\""
    }

    /** Keep visible language labels LTR and let assistive technology read the actual phrase in its language. */
    private fun htmlBidiText(
        value: String, languageCode: String, showLanguageTags: Boolean
    ): String {
        val prefix = if (showLanguageTags) LANGUAGE_TAG_PREFIX.find(value) else null
        val text = if (prefix == null) value else value.substring(prefix.value.length)
        val label = if (prefix == null) "" else
            "<span class=\"language-tag\" dir=\"ltr\" lang=\"en\">[" +
                escape(prefix.groupValues[1]) + "]</span> "
        return label + "<bdi dir=\"" + htmlDirection(value, languageCode) + "\"" +
            htmlLanguageAttribute(languageCode) + ">" + escape(text) + "</bdi>"
    }

    /** Shared exact visual order for the HTML generator and native preview. */
    fun orderedCardIndices(
        count: Int, roundIndex: Int,
        order: PinnedClozeCardOrder
    ): List<Int> {
        if (count <= 0 || count > SessionCaptionPins.MAX_PINS * 2) return emptyList()
        val indices = (0 until count).toList()
        return when (order) {
            PinnedClozeCardOrder.SOURCE -> indices
            PinnedClozeCardOrder.VARIED -> when (roundIndex) {
                0 -> indices
                1 -> indices.asReversed()
                else -> {
                    val pivot = (count + 1) / 2
                    indices.drop(pivot) + indices.take(pivot)
                }
            }
        }
    }

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
        if (begin >= end) return word // Authored punctuation is not a lexical answer.
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
        // Java's default regex \\s misses NBSP and many international
        // whitespace characters. Honor only boundaries *already authored*
        // in the original sentence (including explicit U+200B). Preserve
        // the exact separators in every round instead of collapsing them.
        fun isBoundary(point: Int): Boolean =
            Character.isWhitespace(point) || Character.isSpaceChar(point) ||
                point == 0x0085 || point == 0x200B
        val text = value.trim()
        val segments = ArrayList<Pair<String, Boolean>>()
        var cursor = 0
        while (cursor < text.length) {
            val start = cursor
            val boundary = isBoundary(text.codePointAt(cursor))
            do {
                cursor += Character.charCount(text.codePointAt(cursor))
            } while (cursor < text.length &&
                isBoundary(text.codePointAt(cursor)) == boundary)
            segments.add(text.substring(start, cursor) to boundary)
        }
        // Standalone punctuation (e.g. "..." or "—") must remain
        // visible and must not change the number of answer-bearing tokens.
        val lexical = segments.map { (piece, boundary) ->
            !boundary && piece.codePoints().anyMatch { cp -> partOfTerm(cp) }
        }
        val wordCount = lexical.count { it }
        if (wordCount == 0) return "________"
        // Never infer word segmentation when there is only one authored
        // lexical token. Keep authored separators and punctuation intact.
        if (wordCount == 1) return segments.mapIndexed { idx, (piece, _) ->
            if (lexical[idx]) hiddenWord(piece, PinnedClozeHintMode.BLANK) else piece
        }.joinToString("")
        val pass = roundIndex.coerceIn(0, 2)
        var wordIndex = 0
        return segments.mapIndexed { segmentIndex, (textPart, _) ->
            if (!lexical[segmentIndex]) textPart else {
                val index = wordIndex++
                val hide = if (wordCount < 3) index == (1 - pass % 2)
                    else when (difficulty) {
                        PinnedClozeDifficulty.EASY -> (index + pass) % 3 == 2
                        PinnedClozeDifficulty.NORMAL -> when (pass) {
                            0 -> index % 2 == 1
                            1 -> index % 2 == 0
                            else -> index % 6 in 1..3
                        }
                        PinnedClozeDifficulty.CHALLENGE -> (index + pass) % 3 != 0
                    }
                if (hide) hiddenWord(textPart, hintMode) else textPart
            }
        }
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
        pass: PinnedClozePass = PinnedClozePass.FIRST,
        cardOrder: PinnedClozeCardOrder = PinnedClozeCardOrder.SOURCE
    ): ByteArray? {
        // Same exact source-data and language-label validation as TSV.
        // Printed text must not gain spreadsheet formula-safety apostrophes.
        if (PrivatePinnedCaptionStudyTsv.snapshot(rows, direction, showLanguageTags) == null)
            return null
        // Preserve each answer exactly while deliberately varying gap placements.
        // No randomness, remote model or inferred word timings.
        val cards = ArrayList<PracticeCard>()
        pass.indices.forEach { round ->
            val roundCards = ArrayList<PracticeCard>()
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
                    roundCards.add(PracticeCard(front, back, previewHint(target,
                        if (showLanguageTags) row.targetCode else null,
                        difficulty, hintMode, round), row.sourceCode, row.targetCode,
                        row.targetCode, round))
                if (direction != PinnedStudyDirection.SOURCE_TO_TARGET)
                    roundCards.add(PracticeCard(back, front, previewHint(source,
                        if (showLanguageTags) row.sourceCode else null,
                        difficulty, hintMode, round), row.targetCode, row.sourceCode,
                        row.sourceCode, round))
            }
            // The same exact card index order powers the Android consent preview.
            cards.addAll(orderedCardIndices(roundCards.size, round, cardOrder).map {
                roundCards[it]
            })
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
                ".language-tag{font-size:.85em;color:#526173;white-space:nowrap}" +
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
                ".write{display:none;margin-top:12px}" +
                ".writeline{height:32px;border-bottom:1px solid #64748b}" +
                ".answers{display:none;break-before:page;page-break-before:always}" +
                ".answers:target{display:block}" +
                ".roundheading{border-bottom:2px solid #64748b;padding:9px 0}" +
                ".roundnav a{display:inline-block;margin:6px 12px 6px 0}" +
                ".cardnav{display:flex;gap:18px;flex-wrap:wrap;margin-top:10px}" +
                ".card:target{outline:2px solid #2563a6;scroll-margin-top:12px}" +
                ".answer-row{border-bottom:1px solid #b6c0ca;padding:10px 0;" +
                    "break-inside:avoid;page-break-inside:avoid}" +
                ".answerlink{display:inline-block;margin:12px 0}" +
                ".printcontrol{display:inline-block;margin:14px 0 0 8px;" +
                    "font-weight:600;cursor:pointer}" +
                ".printkey{margin-left:0;vertical-align:middle}" +
                "@media print{@page{margin:14mm}body{padding:0}" +
                "details,.work,textarea,.reset,.grade{display:none}" +
                ".write,.printgrade{display:block}" +
                ".answers,.answers:target{display:none}" +
                ".printkey:checked ~ .answers," +
                    ".printkey:checked ~ .answers:target{display:block}" +
                ".answerlink,.roundnav,.cardnav,.printkey,.printcontrol{display:none}" +
                ".card{break-inside:avoid}.roundheading.newpage{" +
                    "break-before:page;page-break-before:always}}" +
                "</style></head><body><main><h1>Tweetalige aanvuloefening</h1>" +
                "<p>Moeilijkheid: " + escape(difficulty.title) + ". " +
                "Invulhint: " + escape(hintMode.title) + ". " +
                "Schrijfruimte: " + escape(answerSpace.title) + ". " +
                "Bestemming: " + escape(audience.title) + ". " +
                "Oefenrondes: " + escape(pass.title) + ". " +
                "Kaartvolgorde: " + escape(cardOrder.title) + ". " +
                "Elke ronde heeft andere invulgaten. Met afwisselende kaartvolgorde " +
                "komt ronde 2 in omgekeerde volgorde en ronde 3 met een verschoven start; " +
                "bij een enkele kaart blijft de volgorde vanzelf gelijk. " +
                "Bij korte of ongesegmenteerde zinnen kunnen gaten gelijk blijven. " +
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
                    "Het volledige antwoordblad wordt alleen meegeprint als je " +
                    "het afdrukvakje eerst bewust aanvinkt.</p>"
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
            val question = card.question
            val answer = card.answer
            val hint = card.hint
            val roundIndex = card.roundIndex
            if (index == 0 || cards[index - 1].roundIndex != roundIndex) {
                out.append("<h2 class=\"roundheading")
                if (index != 0) out.append(" newpage")
                out.append("\" id=\"round-").append(roundIndex + 1)
                    .append("\">Oefenronde ").append(roundIndex + 1).append("</h2>")
            }
            out.append("<article class=\"card\" id=\"card-").append(index + 1)
                .append("\"><b>Ronde ").append(roundIndex + 1)
                .append(" · kaart ").append(index % cardsPerRound + 1)
                .append("</b><p class=\"phrase\">")
                .append(htmlBidiText(question, card.questionLanguageCode, showLanguageTags))
                .append("</p>")
                .append("<p class=\"hint\">")
                .append(htmlBidiText(hint, card.hintLanguageCode, showLanguageTags))
                .append("</p>")
                .append("<form autocomplete=\"off\"><label class=\"work\" for=\"typed-").append(index + 1)
                .append("\">Mijn eigen antwoord (alleen tijdelijk in de browser)</label>")
                .append("<textarea id=\"typed-").append(index + 1)
                .append("\" rows=\"").append(answerSpace.browserRows)
                .append("\" dir=\"").append(htmlDirection(answer, card.answerLanguageCode))
                .append("\"").append(htmlLanguageAttribute(card.answerLanguageCode))
                .append(" spellcheck=\"false\" autocomplete=\"off\" " +
                    "autocorrect=\"off\" aria-label=\"Mijn eigen antwoord voor kaart ").append(index + 1)
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
            out.append("<div class=\"write\" aria-hidden=\"true\" dir=\"")
                .append(htmlDirection(answer, card.answerLanguageCode))
                .append("\"").append(htmlLanguageAttribute(card.answerLanguageCode))
                .append(">")
            repeat(answerSpace.printLines) {
                out.append("<div class=\"writeline\"></div>")
            }
            out.append("</div>")
            if (audience == PinnedClozeAudience.SELF_STUDY) {
                out.append("<details><summary>Bekijk volledig antwoord</summary>")
                    .append("<p class=\"phrase\">")
                    .append(htmlBidiText(answer, card.answerLanguageCode, showLanguageTags))
                    .append("</p></details>")
            }
            out.append("<nav class=\"cardnav\" aria-label=\"Kaartnavigatie\">")
            if (index > 0) {
                out.append("<a href=\"#card-").append(index)
                    .append("\">← Vorige kaart</a>")
            }
            if (index + 1 < cards.size) {
                out.append("<a href=\"#card-").append(index + 2)
                    .append("\">Volgende kaart →</a>")
            }
            out.append("</nav></article>")
            if (out.length > MAX_BYTES) return null
        }
        out.append("</section>")
        if (audience == PinnedClozeAudience.SELF_STUDY) {
            out.append("<input id=\"print-answers\" class=\"printkey\" " +
                    "type=\"checkbox\" aria-label=\"Print ook het antwoordblad\">" +
                    "<label class=\"printcontrol\" for=\"print-answers\">" +
                    "Print ook het antwoordblad (bewuste keuze)</label>")
            out.append("<a class=\"answerlink\" href=\"#all-answers\">" +
                "Open volledige antwoordblad (bewuste keuze)</a>")
                .append("<section class=\"answers\" id=\"all-answers\">" +
                    "<h2>Antwoordblad</h2>" +
                    "<a class=\"answerlink\" href=\"#questions\">" +
                    "Terug naar vragen</a>")
            cards.forEachIndexed { index, card ->
                val answer = card.answer
                out.append("<div class=\"answer-row\"><b>Ronde ")
                    .append(card.roundIndex + 1)
                    .append(" · kaart ").append(index % cardsPerRound + 1)
                    .append("</b><p class=\"phrase\">Vraag: ")
                    .append(htmlBidiText(card.question, card.questionLanguageCode, showLanguageTags))
                    .append("</p><p class=\"phrase\">Juiste antwoord: ")
                    .append(htmlBidiText(answer, card.answerLanguageCode, showLanguageTags))
                    .append("</p><a class=\"answerlink\" href=\"#card-")
                    .append(index + 1).append("\">Terug naar deze kaart</a></div>")
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
