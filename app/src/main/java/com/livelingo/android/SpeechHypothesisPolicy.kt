package com.livelingo.android

/**
 * Android SpeechRecognizer may return a blank highest-ranked hypothesis while
 * a usable alternative follows. Use the first bounded, nonblank candidate
 * rather than silently throwing away a real user's utterance.
 *
 * The same pure, RAM-only policy is used for interim and final results.
 */
internal object SpeechHypothesisPolicy {
    private const val MAX_ALTERNATIVES = 8
    private const val MAX_CAPTION_CHARS = 1500

    fun select(candidates: List<String?>?): String {
        if (candidates == null) return ""
        for (candidate in candidates.take(MAX_ALTERNATIVES)) {
            val text = candidate?.trim() ?: continue
            if (text.isNotBlank() && text.length <= MAX_CAPTION_CHARS) {
                return text
            }
        }
        // A provider returning only blank/oversized/corrupt alternatives
        // must not produce an invented subtitle or unbounded RAM history.
        return ""
    }
}
