package com.livelingo.android

import com.livelingo.android.model.LocalGlossary

/**
 * Explicit user-chosen plain-text printout of already saved, curated words.
 *
 * This is not a transcript export, a personal phrase translation, an Android
 * auto-backup, or an automatically shared document. The caller must launch
 * the system document picker and write this RAM snapshot on explicit consent.
 */
internal object PrivateVocabularyStudySheet {
    private const val MAX_WORDS = 120
    private const val MAX_BYTES = 32_768
    private val SUPPORTED = setOf("nl", "en", "de", "fr", "es")

    fun snapshot(words: List<SavedWord>): ByteArray? {
        if (words.isEmpty() || words.size > MAX_WORDS) return null
        val keys = mutableSetOf<String>()
        val text = StringBuilder(
            "Tuddy | Eigen woordenblad (zelf bewaard)\n" +
            "Alleen termen uit het kleine ingebouwde NL/EN/DE/FR/ES-woordenboek.\n" +
            "Geen gesprek, opname, automatisch vertaalresultaat of persoonlijke zinnen.\n" +
            "Let op: je gekozen bestandsprovider kan dit blad synchroniseren.\n\n"
        )
        val sorted = words.sortedWith(compareBy(SavedWord::language, SavedWord::word))
        for (word in sorted) {
            if (word.language !in SUPPORTED || word.word.isBlank() ||
                word.word.length > 40 || word.word.contains('|') ||
                word.word.any { it.isISOControl() }) return null
            val key = word.language + "|" + word.word
            if (!keys.add(key)) return null
            // Always re-resolve from the built-in curated glossary. Never use
            // an imported or stale arbitrary definition attached to a caller.
            val entry = LocalGlossary.lookup(word.language, word.word) ?: return null
            if (entry.definition.isBlank() || entry.definition.length > 250 ||
                entry.definition.any { it.isISOControl() } ||
                entry.illustration.length > 16 ||
                entry.illustration.any { it.isISOControl() }) return null
            text.append(word.language.uppercase()).append(" · ")
                .append(word.word).append(" ").append(entry.illustration)
                .append("\n  Betekenis: ").append(entry.definition).append("\n\n")
            if (text.toString().toByteArray(Charsets.UTF_8).size > MAX_BYTES) return null
        }
        return text.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
