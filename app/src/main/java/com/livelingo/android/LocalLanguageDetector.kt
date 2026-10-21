package com.livelingo.android

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import java.util.Locale

/**
 * Reuses the bundled on-device language classifier for live captions.
 * No model download, network translation or transcript persistence occurs here.
 */
internal class LocalLanguageDetector {
    private var identifier: LanguageIdentifier? = null
    private var generation = 0L

    fun identify(text: String, onResult: (String?) -> Unit) {
        if (text.length < 8) {
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
            client.identifyLanguage(text)
                .addOnSuccessListener { language ->
                    if (request == generation) {
                        onResult(language.takeIf { it.isNotBlank() && it != "und" }
                            ?.lowercase(Locale.ROOT))
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
        identifier?.close()
        identifier = null
    }
}
