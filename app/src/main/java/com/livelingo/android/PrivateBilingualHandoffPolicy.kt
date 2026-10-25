package com.livelingo.android

/**
 * Explicit RAM-only comparison for Android's selected-text translation.
 *
 * Never silently includes source text in the existing translation-only
 * clipboard, sharesheet or PROCESS_TEXT replacement. Only the dedicated,
 * reviewed bilingual handoff path calls this formatter. Nothing is persisted,
 * uploaded or emitted automatically.
 */
internal object PrivateBilingualHandoffPolicy {
    const val MAX_HANDOFF_UNITS = 17_000

    fun format(
        sourceText: String,
        translatedText: String,
        sourceCode: String,
        targetCode: String,
        manuallyAdjusted: Boolean
    ): String? {
        if (sourceCode == targetCode || sourceCode !in TargetCatalog.codes ||
            targetCode !in TargetCatalog.codes ||
            LocalSelectedParagraphPlan.fromSelection(sourceText) == null) return null

        // The same strict Unicode and control-character admission applies to
        // text the user corrected, or ML Kit produced, before it leaves Tuddy.
        val translated = LocalTranslationReviewPolicy.normalize(translatedText)
            ?: return null
        val original = LocalTranslationReviewPolicy.normalize(sourceText)
            ?: return null
        val from = TargetCatalog.targets.firstOrNull { it.code == sourceCode }
            ?.title ?: sourceCode.uppercase()
        val to = TargetCatalog.targets.firstOrNull { it.code == targetCode }
            ?.title ?: targetCode.uppercase()
        val attribution = if (manuallyAdjusted)
            "Door gebruiker aangepast; niet onafhankelijk geverifieerd"
        else
            "Lokaal door ML Kit vertaald; niet onafhankelijk geverifieerd"

        val result = buildString {
            append("BRON · ").append(from).append(" (").append(sourceCode.uppercase()).append(")")
            append("\n").append(original)
            append("\n\nVERTALING · ").append(to).append(" (").append(targetCode.uppercase()).append(")")
            append("\n").append(attribution)
            append("\n").append(translated)
        }
        return result.takeIf { it.length <= MAX_HANDOFF_UNITS }
    }
}
