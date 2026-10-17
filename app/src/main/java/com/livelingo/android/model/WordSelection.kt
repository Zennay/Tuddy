package com.livelingo.android.model

/**
 * Exact local text selection algorithm from archived v0.4 Languages.kt.
 * Returns only a contiguous word at a tapped character index, never calls
 * a dictionary service or infers a definition from arbitrary speech.
 */
fun wordAt(text: String, characterOffset: Int): String? {
    if (characterOffset !in text.indices) return null
    fun valid(c: Char) = c.isLetter() || c == '\'' || c == '’' || c == '-'
    if (!valid(text[characterOffset])) return null
    var left = characterOffset
    var right = characterOffset + 1
    while (left > 0 && valid(text[left - 1])) left--
    while (right < text.length && valid(text[right])) right++
    return text.substring(left, right).trim('\'', '’', '-').takeIf { it.isNotEmpty() }
}
