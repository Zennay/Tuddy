package com.livelingo.android

/**
 * Exact-match human-authored suggestions for the current app session only.
 * Never persists data, uses speech/translation models or applies text.
 */
internal data class SessionTranslationCorrection(
    val original: String,
    val translation: String,
    val sourceCode: String,
    val targetCode: String
)

internal class SessionTranslationCorrections {
    companion object {
        const val MAX_ENTRIES = 40
        const val MAX_TEXT_UNITS = 400
    }

    private val items = mutableListOf<SessionTranslationCorrection>()
    fun entries(): List<SessionTranslationCorrection> = items.toList()

    fun suggest(original: String, sourceCode: String, targetCode: String): String? =
        items.lastOrNull { item ->
            item.original == original && item.sourceCode == sourceCode &&
                item.targetCode == targetCode
        }?.translation

    private fun valid(
        original: String, translation: String, sourceCode: String, targetCode: String
    ): Boolean = original.isNotBlank() && translation.isNotBlank() &&
        original == original.trim() && translation == translation.trim() &&
        original.length <= MAX_TEXT_UNITS && translation.length <= MAX_TEXT_UNITS &&
        sourceCode in TargetCatalog.codes && targetCode in TargetCatalog.codes &&
        sourceCode != targetCode &&
        listOf(original, translation).none { value ->
            value.any { it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\t') }
        }

    fun remember(
        original: String, translation: String, sourceCode: String, targetCode: String
    ): Boolean {
        if (!valid(original, translation, sourceCode, targetCode)) return false
        items.removeAll {
            it.original == original && it.sourceCode == sourceCode && it.targetCode == targetCode
        }
        if (items.size >= MAX_ENTRIES) items.removeAt(0)
        items.add(SessionTranslationCorrection(original, translation, sourceCode, targetCode))
        return true
    }

    /**
     * Change exactly the chosen human suggestion without altering the language pair,
     * rewriting old captions or silently overwriting a second suggestion.
     * A stale/deleted entry or an exact-key collision fails without mutation.
     */
    fun revise(
        previous: SessionTranslationCorrection, original: String, translation: String
    ): Boolean {
        val index = items.indexOf(previous)
        if (index < 0 ||
            !valid(original, translation, previous.sourceCode, previous.targetCode)) return false
        if (items.any { item ->
                item != previous && item.original == original &&
                    item.sourceCode == previous.sourceCode &&
                    item.targetCode == previous.targetCode
            }) return false
        items[index] = previous.copy(original = original, translation = translation)
        return true
    }

    fun forget(entry: SessionTranslationCorrection): Boolean = items.remove(entry)
    fun clear() = items.clear()
}
