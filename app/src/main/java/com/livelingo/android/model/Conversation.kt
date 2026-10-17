package com.livelingo.android.model

/** Immutable in-memory timeline; no speech/audio/transcript is ever persisted to disk. */
data class Caption(
    val id: Long,
    val source: String,
    val translation: String? = null,
    val timestamp: Long,
    val sourceLanguage: String? = null,
    val targetLanguage: String = "en",
)

data class Conversation(
    val lines: List<Caption> = emptyList(),
    val activeSource: String = "",
    val activeTranslation: String = "",
    val activeLanguage: String? = null,
    val revision: Long = 0,
    val nextId: Long = 1,
) {
    fun partial(text: String, language: String? = null): Conversation {
        val clean = text.trim()
        if (clean == activeSource && (language == null || activeLanguage == language)) return this
        return copy(activeSource = clean, activeTranslation = "", activeLanguage = language ?: activeLanguage, revision = revision + 1)
    }

    fun identifiedLanguage(revisionId: Long, language: String?): Conversation =
        if (revisionId == revision && activeSource.isNotEmpty() && language != null) copy(activeLanguage = language) else this

    fun interimTranslation(revisionId: Long, translated: String): Conversation =
        if (revisionId == revision && activeSource.isNotBlank()) copy(activeTranslation = translated) else this

    fun finish(text: String, now: Long, sourceLanguage: String? = null, targetLanguage: String = "en"): Conversation {
        val clean = text.trim().ifBlank { activeSource }
        if (clean.isBlank()) return copy(activeSource = "", activeTranslation = "", activeLanguage = null, revision = revision + 1)
        val caption = Caption(nextId, clean, timestamp = now, sourceLanguage = sourceLanguage ?: activeLanguage, targetLanguage = targetLanguage)
        return copy(
            lines = (lines + caption).takeLast(500),
            activeSource = "", activeTranslation = "", activeLanguage = null,
            nextId = nextId + 1, revision = revision + 1,
        )
    }

    fun finalLanguage(id: Long, language: String): Conversation = copy(
        lines = lines.map { if (it.id == id) it.copy(sourceLanguage = language) else it },
    )

    fun finalTranslation(id: Long, translated: String): Conversation = copy(
        lines = lines.map { if (it.id == id) it.copy(translation = translated) else it },
    )

    fun clear(): Conversation = Conversation(nextId = nextId)
}
