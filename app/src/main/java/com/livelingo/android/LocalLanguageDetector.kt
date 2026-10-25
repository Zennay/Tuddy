package com.livelingo.android

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier

/**
 * Reuses the bundled on-device language classifier for live captions.
 * No model download, network translation or transcript persistence occurs here.
 */
internal class LocalLanguageDetector {
    private var identifier: LanguageIdentifier? = null
    private var generation = 0L

    fun identify(text: String, onResult: (String?) -> Unit) {
        // The policy checks Unicode text length and the available confidence
        // margin. Never propose a language from a short or ambiguous guess.
        if (text.isBlank()) {
            onResult(null)
            return
        }
        val client = runCatching {
            identifier ?: LanguageIdentification.getClient().also { identifier = it }
        }.getOrNull()
        if (client == null) {
            onResult(null)
            return
        }
        val request = generation
        try {
            client.identifyPossibleLanguages(text)
                .addOnSuccessListener { languages ->
                    if (request == generation) {
                        onResult(ConfidentTextLanguagePolicy.select(
                            text,
                            languages.map { it.languageTag to it.confidence }
                        ))
                    }
                }
                .addOnFailureListener {
                    if (request == generation) onResult(null)
                }
        } catch (_: Exception) {
            if (request == generation) onResult(null)
        }
    }

    fun close() {
        generation++
        val previous = identifier
        identifier = null
        runCatching { previous?.close() }
    }
}
