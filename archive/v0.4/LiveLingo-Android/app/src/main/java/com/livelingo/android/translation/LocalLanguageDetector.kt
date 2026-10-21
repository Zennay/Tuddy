package com.livelingo.android.translation

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.livelingo.android.model.Languages

/** Bundled on-device text language classifier: never triggers a language-model download. */
class LocalLanguageDetector {
    private val identifier: LanguageIdentifier = LanguageIdentification.getClient()

    fun identify(text: String, onResult: (String?) -> Unit) {
        if (text.length < 8) { onResult(null); return }
        identifier.identifyLanguage(text)
            .addOnSuccessListener { onResult(Languages.normalize(it)) }
            .addOnFailureListener { onResult(null) }
    }
    fun close() = identifier.close()
}
