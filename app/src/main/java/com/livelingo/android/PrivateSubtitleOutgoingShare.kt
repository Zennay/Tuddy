package com.livelingo.android

/**
 * User-authorized, text-only Android outgoing share. Restrict sharing to a
 * small consciously selected SRT/WebVTT excerpt, never the whole live
 * transcript, source file URI, audio, metadata or arbitrary private archive.
 */
internal object PrivateSubtitleOutgoingShare {
    private const val MAX_SHARED_CUES = 24
    private const val MAX_SHARED_BYTES = 16 * 1024

    /** A user-selected small TTML XML excerpt, never an entire conversation. */
    fun ttmlText(cues: List<PrivateSubtitleCue>): String? {
        if (cues.size !in 1..MAX_SHARED_CUES) return null
        val bytes = PrivateTtmlSubtitleExport.snapshot(cues) ?: return null
        if (bytes.size > MAX_SHARED_BYTES) return null
        return bytes.toString(Charsets.UTF_8).takeIf { text ->
            text.isNotBlank() &&
                text.toByteArray(Charsets.UTF_8).contentEquals(bytes)
        }
    }

    /** A deliberately selected unstyled ASS text excerpt, never a file/URI. */
    fun assText(cues: List<PrivateSubtitleCue>): String? {
        if (cues.size !in 1..MAX_SHARED_CUES) return null
        val bytes = PrivateAssSubtitleExport.snapshot(cues) ?: return null
        if (bytes.size > MAX_SHARED_BYTES) return null
        val text = bytes.toString(Charsets.UTF_8)
        return text.takeIf { it.isNotBlank() &&
            it.toByteArray(Charsets.UTF_8).contentEquals(bytes) }
    }

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
