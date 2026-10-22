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
        private const val MAX_TEXT_UNITS = 400
    }
    private val items = mutableListOf<SessionTranslationCorrection>()
    fun entries(): List<SessionTranslationCorrection> = items.toList()

    fun suggest(original: String, sourceCode: String, targetCode: String): String? =
        items.lastOrNull { item ->
            item.original == original && item.sourceCode == sourceCode &&
                item.targetCode == targetCode
        }?.translation

    fun remember(original: String, translation: String, sourceCode: String, targetCode: String): Boolean {
        if (original.isBlank() || translation.isBlank() ||
            original != original.trim() || translation != translation.trim() ||
            original.length > MAX_TEXT_UNITS || translation.length > MAX_TEXT_UNITS ||
            sourceCode !in TargetCatalog.codes || targetCode !in TargetCatalog.codes ||
            sourceCode == targetCode ||
            listOf(original, translation).any { value ->
                value.any { it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\t') }
            }) return false
        items.removeAll {
            it.original == original && it.sourceCode == sourceCode && it.targetCode == targetCode
        }
        if (items.size >= MAX_ENTRIES) items.removeAt(0)
        items.add(SessionTranslationCorrection(original, translation, sourceCode, targetCode))
        return true
    }

    fun forget(entry: SessionTranslationCorrection): Boolean = items.remove(entry)
    fun clear() = items.clear()
}
