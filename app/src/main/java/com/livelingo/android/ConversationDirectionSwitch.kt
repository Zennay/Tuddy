package com.livelingo.android

/**
 * A manual two-way conversation only reverses if the current translation
 * target is also an explicitly supported on-device speech picker language.
 *
 * ML Kit text translation supports more targets than Android speech. Never
 * infer availability of an installed offline speech pack from this mapping.
 * The caller must stop the active microphone and invalidate readiness first.
 */
internal data class ReversedConversationDirection(
    val sourceSpeechTag: String,
    val targetTextCode: String
)

internal object ConversationDirectionSwitch {
    fun plan(
        currentSpeechTag: String,
        currentSourceTextCode: String,
        currentTargetTextCode: String,
        spokenPairs: List<Pair<String, String>>
    ): ReversedConversationDirection? {
        if (currentSourceTextCode == currentTargetTextCode) return null
        if (spokenPairs.none {
                it.first == currentSpeechTag && it.second == currentSourceTextCode
            }) return null
        val reversedSpeech = spokenPairs.filter { it.second == currentTargetTextCode }
        if (reversedSpeech.size != 1) return null
        return ReversedConversationDirection(
            sourceSpeechTag = reversedSpeech.single().first,
            targetTextCode = currentSourceTextCode
        )
    }
}
