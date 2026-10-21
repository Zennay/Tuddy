package com.livelingo.android

/**
 * One deliberately confirmed correction set for a user-opened subtitle draft.
 * The rules are applied top to bottom to a temporary list, not written to the
 * provider. Each intermediate result must remain safely exportable as SRT/VTT.
 * This is literal replacement, never user-supplied regular expressions.
 */
internal object PrivateSubtitleCorrectionScript {
    private const val MAX_RULES = 12
    private const val MAX_INPUT = 4096

    data class Rule(
        val search: String,
        val replacement: String,
        val fieldOverride: SubtitleReplaceField? = null
    )

    fun parse(input: String): List<Rule>? {
        if (input.isBlank() || input.length > MAX_INPUT) return null
        // Accept ordinary Windows CRLF pastes and one final newline, not
        // embedded control characters or empty correction rows.
        val normalized = input.replace("\r\n", "\n")
        if (normalized.any { it == '\r' || it == '\u0000' ||
                (it.isISOControl() && it != '\n') }) return null
        val lines = normalized.removeSuffix("\n").lines()
        if (lines.size !in 1..MAX_RULES || lines.any { it.isBlank() }) return null
        val rules = ArrayList<Rule>(lines.size)
        for (line in lines) {
            val delimiter = line.indexOf("=>")
            if (delimiter < 1) return null
            val left = line.substring(0, delimiter).trim()
            // A recipe can explicitly target source and meaning independently.
            // Unprefixed rules retain the global user-selected text scope.
            val scopedField = when {
                left.startsWith("@bron:") -> SubtitleReplaceField.FIRST_LINE
                left.startsWith("@betekenis:") -> SubtitleReplaceField.SECOND_LINE
                left.startsWith("@alles:") -> SubtitleReplaceField.ALL_TEXT
                left.startsWith("@") -> return null
                else -> null
            }
            val search = if (scopedField == null) left else
                left.substringAfter(':').trim()
            val replacement = line.substring(delimiter + 2).trim()
            if (search.isEmpty() || search.length > 100 || replacement.length > 200 ||
                search.contains("=>") || search.any { it.isISOControl() } ||
                replacement.any { it.isISOControl() }) return null
            rules += Rule(search, replacement, scopedField)
        }
        return rules
    }

    fun preview(
        cues: List<PrivateSubtitleCue>,
        script: String,
        matchCase: Boolean,
        wholeWords: Boolean,
        fromRow: Int,
        throughRow: Int,
        field: SubtitleReplaceField,
        explicitRows: String,
        skipUnmatched: Boolean = false
    ): SubtitleBatchPreview? {
        val rules = parse(script) ?: return null
        var current = cues
        var replacements = 0
        var skipped = 0
        val ruleChanges = ArrayList<Int>(rules.size)
        for (rule in rules) {
            // A missing phrase may be skipped only when explicitly requested;
            // malformed ranges, malformed cues and unsafe resulting text are
            // always rejected by the same complete draft validation.
            val result = PrivateSubtitleBatchReplace.preview(
                current, rule.search, rule.replacement, matchCase,
                fromRow, throughRow, rule.fieldOverride ?: field,
                explicitRows, wholeWords, allowNoChange = skipUnmatched
            ) ?: return null
            if (result.changedCues == 0) skipped++
            ruleChanges.add(result.replacements)
            current = result.updated
            replacements += result.replacements
        }
        val changed = current.indices.count { current[it].text != cues[it].text }
        if (changed == 0) return null
        return SubtitleBatchPreview(
            current, changed, replacements,
            appliedRules = rules.size - skipped,
            skippedRules = skipped,
            ruleChanges = ruleChanges
        )
    }
}
