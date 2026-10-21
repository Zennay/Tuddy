package com.livelingo.android.model

/**
 * Select an intact written word from a user-visible local caption.
 *
 * Compose positions are UTF-16 offsets, but letters in Kurdish, Pashto and
 * other scripts may use surrogate pairs and combining marks. Walk code points
 * and keep marks and internal joiners with their letters. Never infer a word
 * from an adjacent punctuation mark, fetch a definition or alter source text.
 */
fun wordAt(text: String, characterOffset: Int): String? {
    if (characterOffset !in text.indices) return null

    // A TextLayoutResult may hit the second UTF-16 unit of a supplementary
    // code point. Normalize to its start rather than extracting half a glyph.
    val offset = if (characterOffset > 0 &&
        Character.isLowSurrogate(text[characterOffset]) &&
        Character.isHighSurrogate(text[characterOffset - 1]))
        characterOffset - 1 else characterOffset

    fun core(cp: Int): Boolean {
        val category = Character.getType(cp)
        return Character.isLetterOrDigit(cp) ||
            category == Character.NON_SPACING_MARK.toInt() ||
            category == Character.COMBINING_SPACING_MARK.toInt() ||
            category == Character.ENCLOSING_MARK.toInt()
    }
    // Apostrophes, hyphens and script joiners are meaningful *inside* words,
    // never as standalone tokens at either edge.
    fun joiner(cp: Int) = cp == '-'.code || cp == '\''.code ||
        cp == '’'.code || cp == 0x200C || cp == 0x200D
    fun part(cp: Int) = core(cp) || joiner(cp)

    if (!part(Character.codePointAt(text, offset))) return null
    var left = offset
    var right = offset + Character.charCount(Character.codePointAt(text, offset))
    while (left > 0) {
        val cp = Character.codePointBefore(text, left)
        if (!part(cp)) break
        left -= Character.charCount(cp)
    }
    while (right < text.length) {
        val cp = Character.codePointAt(text, right)
        if (!part(cp)) break
        right += Character.charCount(cp)
    }

    while (left < right && joiner(Character.codePointAt(text, left)))
        left += Character.charCount(Character.codePointAt(text, left))
    while (right > left && joiner(Character.codePointBefore(text, right)))
        right -= Character.charCount(Character.codePointBefore(text, right))
    if (left >= right) return null

    val candidate = text.substring(left, right)
    // Keep full input instead of truncating half a Unicode letter in the UI.
    // Existing local-word stores and pronunciation controls use 40 UTF-16
    // units; an oversized token is not silently saved as another word.
    if (candidate.length > 40 ||
        candidate.codePoints().noneMatch { Character.isLetter(it) }) return null

    // Do not reinterpret isolated marks or numbers as a dictionary word.
    return candidate
}
