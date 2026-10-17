package com.livelingo.android.model

/**
 * Additional admission for the user-facing five-phrase preview.
 * Keeps CommunityPhrasebook.kt byte-identical to the archived v0.4 source.
 * This is NOT general machine translation, language detection or speech recognition.
 */
object SafeCommunityPhrasebook {
    private const val MAX_PREVIEW_CHARS = 256

    private fun plainSingleLine(value: String): Boolean =
        value.length in 1..MAX_PREVIEW_CHARS && value.none { ch ->
            Character.isISOControl(ch) ||
                Character.getType(ch) == Character.FORMAT.toInt() ||
                Character.isSurrogate(ch) ||
                ch == '\u2028' || ch == '\u2029'
        }

    fun translate(source: String?, target: String, phrase: String): Result<String> {
        if (!plainSingleLine(phrase)) return Result.failure(UnsupportedOperationException(
            "Only short single-line visible beginner phrases are supported. No translation invented."))
        return CommunityPhrasebook.translate(source, target, phrase)
    }
}
