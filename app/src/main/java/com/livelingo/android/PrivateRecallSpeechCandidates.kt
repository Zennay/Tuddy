package com.livelingo.android

/**
 * Treat Android's on-device recognizer alternatives as *suggestions*, not
 * graded answers or evidence that a language is supported.
 *
 * Only the first few returned candidates are inspected. Never truncate a
 * word, alter script or leak the provider's confidence values; rejected,
 * repeated and malformed candidates are omitted. The learner decides which
 * hypothesis, if any, was actually said before revealing the answer.
 */
internal object PrivateRecallSpeechCandidates {
    private const val MAX_INSPECTED = 8
    private const val MAX_OFFERED = 4
    private const val MAX_CHARS = 400

    fun admitted(raw: List<String?>?): List<String> {
        if (raw == null) return emptyList()
        val unique = LinkedHashSet<String>()
        for (candidate in raw.take(MAX_INSPECTED)) {
            val normalized = candidate?.let(LocalTextBoundaryPolicy::speechHypothesis)
                ?.takeIf { it.length <= MAX_CHARS && it.none(Char::isISOControl) }
                ?: continue
            unique.add(normalized)
            if (unique.size == MAX_OFFERED) break
        }
        return unique.toList()
    }
}
