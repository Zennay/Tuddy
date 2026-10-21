package com.livelingo.android

/**
 * User-authorized, text-only Android outgoing share. Restrict sharing to a
 * small consciously selected SRT/WebVTT excerpt, never the whole live
 * transcript, source file URI, audio, metadata or arbitrary private archive.
 */
internal object PrivateSubtitleOutgoingShare {
    private const val MAX_SHARED_CUES = 24
    private const val MAX_SHARED_BYTES = 16 * 1024

    fun text(cues: List<PrivateSubtitleCue>, format: SubtitleDraftFormat): String? {
        if (cues.size !in 1..MAX_SHARED_CUES) return null
        val bytes = PrivateSubtitleDraftEditor.snapshot(cues, format) ?: return null
        if (bytes.size > MAX_SHARED_BYTES) return null
        // The existing strict snapshot rejects control characters and invalid
        // timestamps. Android receives only literal UTF-8 caption text.
        return bytes.toString(Charsets.UTF_8).takeIf {
            it.isNotBlank() && it.toByteArray(Charsets.UTF_8).size == bytes.size
        }
    }
}
