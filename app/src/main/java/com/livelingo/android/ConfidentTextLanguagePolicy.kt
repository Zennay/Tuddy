package com.livelingo.android

import java.util.Locale

/**
 * Pure admission policy for a *suggestion*, never evidence of the spoken
 * language or a substitute for Android's selected on-device ASR package.
 *
 * A guess that is close to a different language is not safe enough to silently
 * change the translation direction. No text, scores or language histories are
 * persisted. The selected source remains the default when uncertain.
 */
internal object ConfidentTextLanguagePolicy {
    private const val MIN_CODEPOINTS = 20
    private const val MIN_LETTERS = 12
    private const val MIN_CONFIDENCE = 0.80f
    private const val MIN_MARGIN = 0.20f

    fun select(text: String, candidates: List<Pair<String, Float>>): String? {
        if (text.codePointCount(0, text.length) < MIN_CODEPOINTS ||
            text.count(Char::isLetter) < MIN_LETTERS) return null
        val ranked = candidates.mapNotNull { (raw, confidence) ->
            val tag = raw.trim().replace('_', '-').lowercase(Locale.ROOT)
            if (!Regex("^[a-z]{2,3}(?:-[a-z0-9]{2,8}){0,3}$").matches(tag) ||
                tag == "und" || !confidence.isFinite() ||
                confidence !in 0f..1f) null
            else tag to confidence
        }.sortedByDescending { it.second }
        val best = ranked.firstOrNull() ?: return null
        val second = ranked.drop(1).firstOrNull { it.first != best.first }
        if (best.second < MIN_CONFIDENCE ||
            (second != null && best.second - second.second < MIN_MARGIN)) return null
        return best.first
    }
}
