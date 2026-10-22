package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import org.json.JSONObject

/**
 * Opt-in offline reader for user-supplied YouTube timed-text JSON3 files.
 * Text segments are combined as a cue, never as recognized words. Layout
 * events and styling have no executable meaning and are discarded.
 */
internal object PrivateYoutubeJson3SubtitleReader {
    private const val MAX_EVENTS = 3_000
    private const val MAX_CUES = 500
    private const val MAX_TIME_MS = 359_999_999L
    private const val MAX_SEGMENTS_PER_CUE = 200
    private val integer = Regex("""^[0-9]{1,9}$""")

    private fun millis(value: Any?): Long? {
        val raw = value?.toString() ?: return null
        if (!integer.matches(raw)) return null
        return raw.toLongOrNull()?.takeIf { it in 0L..MAX_TIME_MS }
    }

    fun read(raw: String): List<PrivateSubtitleCue>? {
        if (raw.isBlank() || raw.length > PrivateSubtitleFileReader.MAX_BYTES) return null
        val encoded = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(raw))
        }.getOrNull() ?: return null
        if (encoded.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        if (raw.any { ch ->
                ch == '\u0000' || (ch.isISOControl() &&
                    ch != '\n' && ch != '\r' && ch != '\t')
            }) return null
        val json = raw.removePrefix("\uFEFF")
        if (!StrictJsonKeyGuard.accepts(json)) return null
        val root = runCatching { JSONObject(json) }
            .getOrNull() ?: return null
        // Distinguish this from a Tuddy private transcript JSON document.
        val events = root.optJSONArray("events") ?: return null
        if (events.length() !in 1..MAX_EVENTS) return null

        val cues = ArrayList<PrivateSubtitleCue>()
        var previousStart = -1L
        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: return null
            // YouTube JSON3 includes layout-only events with no segments.
            // Never invent a subtitle from those metadata records.
            val segments = event.optJSONArray("segs") ?: continue
            if (segments.length() !in 1..MAX_SEGMENTS_PER_CUE) return null
            val start = millis(event.opt("tStartMs")) ?: return null
            val duration = millis(event.opt("dDurationMs")) ?: return null
            if (duration == 0L || start > MAX_TIME_MS - duration ||
                start < previousStart) return null
            val end = start + duration
            val text = buildString {
                for (j in 0 until segments.length()) {
                    val segment = segments.optJSONObject(j) ?: return null
                    val fragment = segment.opt("utf8") as? String ?: return null
                    append(fragment)
                    if (length > 2_400) return null
                }
            }.trim()
            // Empty segment collections sometimes represent caption clearing.
            if (text.isEmpty()) continue
            if (text.lines().size > 8 || text.lines().any {
                    it.isEmpty() || it.length > 800
                } || text.any { ch ->
                    ch == '\u0000' || ch == '\r' ||
                        (ch.isISOControl() && ch != '\n' && ch != '\t')
                }) return null
            val startLabel = PrivateSubtitleTimelineNavigation
                .formatTimestamp(start) ?: return null
            val endLabel = PrivateSubtitleTimelineNavigation
                .formatTimestamp(end) ?: return null
            cues.add(PrivateSubtitleCue(start, end, startLabel, endLabel, text))
            previousStart = start
            if (cues.size > MAX_CUES) return null
        }
        if (cues.isEmpty()) return null
        // Every accepted document must be representable as user-controlled,
        // unverified SRT and VTT without losing segment text or timestamps.
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        return cues
    }
}
