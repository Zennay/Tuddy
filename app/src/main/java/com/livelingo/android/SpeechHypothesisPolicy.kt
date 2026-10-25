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

    fun select(candidates: List<String?>?): String {
        if (candidates == null) return ""
        for (candidate in candidates.take(MAX_ALTERNATIVES)) {
            // Untrusted recognizer text must be bounded, printable and
            // valid Unicode before any interim caption, TTS or export path.
            val text = candidate?.let(LocalTextBoundaryPolicy::speechHypothesis)
            if (text != null) return text
        }
        // A provider returning only blank/oversized/corrupt alternatives
        // must not produce an invented subtitle or unbounded RAM history.
        return ""
    }
}
