package com.livelingo.android

/**
 * A user may listen to only their explicitly reviewed OCR excerpt.
 * Never feed raw OCR blocks, a pending photo or another Activity's draft
 * directly to Android TTS. This is a bounded, read-only RAM transformation,
 * independent from translation and all outbound Android share actions.
 */
internal object PrivateImageTextReadbackPolicy {
    private const val MAX_READBACK_CHARS = 2_400
    private const val MAX_UTTERANCES = 12
    private const val MAX_UTTERANCE_CHARS = 360

    /** No truncation: the user has to deliberately select a shorter excerpt. */
    fun prepare(reviewed: String): List<String>? {
        val admitted = PrivateImageTextOutboundPolicy.reviewedText(reviewed) ?: return null
        if (admitted.length > MAX_READBACK_CHARS) return null
        val words = admitted.trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val utterances = mutableListOf<String>()
        val current = StringBuilder()
        for (word in words) {
            if (word.length > MAX_UTTERANCE_CHARS) return null
            val additional = word.length + if (current.isNotEmpty()) 1 else 0
            if (current.length + additional > MAX_UTTERANCE_CHARS) {
                if (current.isEmpty()) return null
                utterances.add(current.toString())
                if (utterances.size >= MAX_UTTERANCES) return null
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) utterances.add(current.toString())
        return utterances.takeIf { it.isNotEmpty() && it.size <= MAX_UTTERANCES }
    }
}
