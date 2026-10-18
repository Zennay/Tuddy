package com.livelingo.android

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

internal object PrivateSubtitleBatchReplace {
    fun preview(
        cues: List<PrivateSubtitleCue>,
        search: String,
        replacement: String,
        matchCase: Boolean
    ): SubtitleBatchPreview? {
        if (cues.isEmpty() || cues.size > 500 ||
            search.isBlank() || search.length > 100 || search != search.trim() ||
            replacement.length > 200 ||
            (search + replacement).any { it == '\u0000' || it == '\r' ||
                it == '\n' || it.isISOControl() }) return null
        val pattern = Regex(
            Regex.escape(search),
            if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
        )
        var affected = 0
        var occurrences = 0
        val draft = cues.map { cue ->
            val matches = pattern.findAll(cue.text).count()
            if (matches == 0) return@map cue
            val edited = pattern.replace(cue.text) { replacement }
            if (edited == cue.text) return@map cue
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
