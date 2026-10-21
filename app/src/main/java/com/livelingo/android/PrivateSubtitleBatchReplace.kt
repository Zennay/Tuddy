package com.livelingo.android

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Local, explicitly confirmed search-and-replace for a user-opened subtitle
 * document. Literal phrases only: no regex supplied by the document or user.
 * Never mutates the original cues and never touches subtitle timestamps.
 */
internal data class SubtitleBatchPreview(
    val updated: List<PrivateSubtitleCue>,
    val changedCues: Int,
    val replacements: Int
)

internal enum class SubtitleReplaceField { ALL_TEXT, FIRST_LINE, SECOND_LINE }

internal object PrivateSubtitleBatchReplace {
    fun preview(
        cues: List<PrivateSubtitleCue>,
        search: String,
        replacement: String,
        matchCase: Boolean,
        fromRow: Int = 1,
        throughRow: Int = cues.size,
        field: SubtitleReplaceField = SubtitleReplaceField.ALL_TEXT,
        explicitRows: String = "",
        wholeWords: Boolean = false
    ): SubtitleBatchPreview? {
        if (cues.isEmpty() || cues.size > 500 ||
            fromRow !in 1..cues.size || throughRow !in fromRow..cues.size ||
            search.isBlank() || search.length > 100 || search != search.trim() ||
            replacement.length > 200 ||
            (search + replacement).any { it == '\u0000' || it == '\r' ||
                it == '\n' || it.isISOControl() }) return null
        // A deliberate disjoint selection takes precedence over Van/Tot;
        // never substitute displayed search-result positions for source rows.
        val selectedRows = if (explicitRows.isNotBlank())
            PrivateSubtitleRowSelection.parse(explicitRows, cues.size)?.toSet()
                ?: return null
        else null
        // Match user-authored text literally. Java's CASE_INSENSITIVE flag
        // alone is ASCII-oriented; pair it with UNICODE_CASE so case-insensitive
        // replacement also works for international subtitle scripts.
        val flags = if (matchCase) 0 else
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        // Opt-in Unicode-aware whole word correction: never replace inside
        // a longer word, including letters with combining marks.
        val wordChar = """[\p{L}\p{M}\p{N}_]"""
        if (wholeWords) {
            val first = Character.codePointAt(search, 0)
            val last = Character.codePointBefore(search, search.length)
            fun wordEdge(code: Int): Boolean =
                Character.isLetterOrDigit(code) ||
                Character.getType(code) == Character.NON_SPACING_MARK.toInt() ||
                Character.getType(code) == Character.COMBINING_SPACING_MARK.toInt()
            if (!wordEdge(first) || !wordEdge(last)) return null
        }
        val literal = Pattern.quote(search)
        val expression = if (wholeWords)
            "(?<!$wordChar)$literal(?!$wordChar)"
        else literal
        val pattern = Pattern.compile(expression, flags)
        var affected = 0
        var occurrences = 0
        val draft = cues.mapIndexed { index, cue ->
            // Never touch a subtitle outside the explicitly selected 1-based range.
            if (selectedRows != null) {
                if (index !in selectedRows) return@mapIndexed cue
            } else if (index + 1 !in fromRow..throughRow) return@mapIndexed cue
            // For manually paired bilingual cues, restrict changes to the
            // first or second line. Other lines remain byte-for-byte text
            // identical. Never infer which language a line represents.
            val lines = cue.text.lines()
            val chosen = when (field) {
                SubtitleReplaceField.ALL_TEXT -> cue.text
                SubtitleReplaceField.FIRST_LINE -> lines.first()
                SubtitleReplaceField.SECOND_LINE ->
                    lines.getOrNull(1) ?: return@mapIndexed cue
            }
            val matcher = pattern.matcher(chosen)
            var matches = 0
            while (matcher.find()) matches++
            if (matches == 0) return@mapIndexed cue
            // Quote replacement text so authored dollar signs and backslashes
            // remain literal characters, never regex substitution tokens.
            val substituted = pattern.matcher(chosen)
                .replaceAll(Matcher.quoteReplacement(replacement))
            val edited = when (field) {
                SubtitleReplaceField.ALL_TEXT -> substituted
                SubtitleReplaceField.FIRST_LINE ->
                    (listOf(substituted) + lines.drop(1)).joinToString("\n")
                SubtitleReplaceField.SECOND_LINE ->
                    (listOf(lines.first(), substituted) + lines.drop(2))
                        .joinToString("\n")
            }
            if (edited == cue.text) return@mapIndexed cue
            if (edited != edited.trim() ||
                edited.isBlank() ||
                edited.length > 2400 ||
                edited.lines().size > 8 ||
                edited.lines().any { it.length > 800 }) return null
            affected++
            occurrences += matches
            cue.copy(text = edited)
        }
        if (affected == 0 ||
            PrivateSubtitleDraftEditor.snapshot(draft, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(draft, SubtitleDraftFormat.WEBVTT) == null)
            return null
        return SubtitleBatchPreview(draft, affected, occurrences)
    }
}
