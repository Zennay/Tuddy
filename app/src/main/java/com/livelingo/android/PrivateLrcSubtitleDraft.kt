package com.livelingo.android

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * User-supplied LRC line start times only. Cue ends are derived from the
 * following start, and the final end is explicitly estimated (+3 seconds).
 * No audio alignment, speech recognition, or translation is performed.
 */
internal object PrivateLrcSubtitleDraft {
    private const val MAX_ROWS = 550
    private const val MAX_CUES = 500
    private const val MAX_TIME_MS = 359_999_999L
    // Consecutive [mm:ss.xx] tags refer to the *same* lyric in repeated choruses.
    private val timestampTag = Regex("""\[(\d{1,3}):([0-5]\d)\.(\d{2,3})\]""")
    private const val MAX_TIMESTAMPS_PER_LINE = 16
    // User-provided whole-track calibration, not discovered from audio.
    private const val MAX_OFFSET_MS = 30_000L
    private val offsetHeader = Regex("""^\[offset:([+-]?[0-9]{1,5})\]$""")
    private val metadata = Regex("""^\[(?:ar|ti|al|by|length):[^\r\n\[\]]{0,200}\]$""")

    /**
     * Only a header before the first lyric can move cues.
     * The Android preview calls this only for a successfully parsed document.
     */
    fun declaredOffsetMillis(raw: String): Long? {
        val lines = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n').lineSequence()
        for (source in lines) {
            val line = source.trim()
            if (line.isEmpty() || metadata.matches(line)) continue
            return offsetHeader.matchEntire(line)?.groupValues?.get(1)?.toLongOrNull()
        }
        return null
    }

    /**
     * Explicit editor-only change: replace or insert the single LRC header.
     * A valid existing document must stay valid after rewriting; neither the
     * Android-picked original file nor any audio is touched.
     */
    fun withDeclaredOffset(raw: String, newOffsetMs: Long): String? {
        if (newOffsetMs !in -MAX_OFFSET_MS..MAX_OFFSET_MS || preview(raw) == null)
            return null
        val lines = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n').split('\n').toMutableList()
        var existingOffset = -1
        for ((index, source) in lines.withIndex()) {
            val line = source.trim()
            if (line.isEmpty() || metadata.matches(line)) continue
            if (offsetHeader.matches(line)) existingOffset = index
            break
        }
        // Removing a header keeps all actual text unchanged. Inserting a new
        // one happens at the start, before authored lyric rows and metadata.
        if (existingOffset >= 0) lines.removeAt(existingOffset)
        if (newOffsetMs != 0L) {
            val header = "[offset:" +
                (if (newOffsetMs > 0L) "+" else "") + newOffsetMs + "]"
            lines.add(if (existingOffset >= 0) existingOffset else 0, header)
        }
        val candidate = lines.joinToString("\n")
        return candidate.takeIf { preview(it) != null }
    }

    /**
     * Shift only explicitly chosen chronological LRC cues, in milliseconds.
     * Multiple tags on one physical refrain remain separate authored starts.
     * Fail closed rather than silently moving a different lyric or rewriting
     * any music metadata, free text or the optional global [offset:] header.
     */
    fun withShiftedRows(
        raw: String,
        sourceIndices: List<Int>,
        deltaMillis: Long
    ): String? {
        if (deltaMillis !in -30_000L..30_000L || deltaMillis == 0L) return null
        val before = preview(raw) ?: return null
        if (sourceIndices.isEmpty() || sourceIndices.size > before.size ||
            sourceIndices.distinct().size != sourceIndices.size ||
            sourceIndices.any { it !in before.indices }) return null
        val desired = sourceIndices.associateWith { index ->
            before[index].startMillis + deltaMillis
        }
        return withExactStarts(raw, before, desired)
    }

    /**
     * Explicitly replace ONE authored LRC cue start with an exact user time.
     * Unlike a ±30-second group shift, this is useful for correcting a
     * mistakenly transcribed minute or second. The complete document must
     * still preserve its original cue identities and chronological order.
     */
    fun withExactCueStart(
        raw: String,
        cueIndex: Int,
        newStartMillis: Long
    ): String? {
        val before = preview(raw) ?: return null
        if (cueIndex !in before.indices ||
            newStartMillis !in 0L until MAX_TIME_MS ||
            newStartMillis == before[cueIndex].startMillis) return null
        return withExactStarts(raw, before, mapOf(cueIndex to newStartMillis))
    }

    /**
     * Strict bulk edit of at most 32 explicit original cue start times.
     * Example: "1=00:00:02.000;4=00:01:07.250". Original cue order
     * and every unmentioned timestamp must remain unchanged.
     */
    fun withExactCueBatch(raw: String, writtenAssignments: String): String? {
        if (writtenAssignments.isBlank() || writtenAssignments.length > 512)
            return null
        val before = preview(raw) ?: return null
        val entries = writtenAssignments.trim()
            .split(';', '\n')
            .map { it.trim() }
        if (entries.isEmpty() || entries.size > 32 ||
            entries.any { it.isBlank() }) return null
        val assignments = LinkedHashMap<Int, Long>(entries.size)
        val pattern = Regex("""^([1-9][0-9]{0,2})\s*=\s*([0-9]{1,2}:[0-5][0-9]:[0-5][0-9][.,][0-9]{3})$""")
        for (entry in entries) {
            val groups = pattern.matchEntire(entry)?.groupValues ?: return null
            val index = (groups[1].toIntOrNull() ?: return null) - 1
            val target = PrivateSubtitleTimelineNavigation.timestamp(groups[2])
                ?: return null
            if (index !in before.indices ||
                assignments.putIfAbsent(index, target) != null) return null
        }
        if (assignments.all { (index, desired) ->
                desired == before[index].startMillis
            }) return null
        return withExactStarts(raw, before, assignments)
    }

    /**
     * Exact two-point correction of a CONTIGUOUS authored cue interval.
     * The user picks the first and last original cue plus two intended
     * timestamps; intermediate cue starts are interpolated only from those
     * explicit anchors, not from sound or speech recognition.
     *
     * The scale is intentionally limited to 0.5..2x, and cues outside the
     * interval are never modified. An inversion, duplicate or impossible
     * resulting SRT/VTT document rejects the entire proposal.
     */
    fun withRangeAnchors(
        raw: String,
        firstIndex: Int,
        lastIndex: Int,
        targetFirstMillis: Long,
        targetLastMillis: Long
    ): String? {
        val before = preview(raw) ?: return null
        if (firstIndex !in before.indices ||
            lastIndex !in before.indices || firstIndex >= lastIndex ||
            targetFirstMillis !in 0L until MAX_TIME_MS ||
            targetLastMillis !in 0L until MAX_TIME_MS) return null
        val oldFirst = before[firstIndex].startMillis
        val oldLast = before[lastIndex].startMillis
        val oldSpan = oldLast - oldFirst
        val newSpan = targetLastMillis - targetFirstMillis
        if (oldSpan <= 0L || newSpan <= 0L ||
            newSpan * 2L < oldSpan || newSpan > oldSpan * 2L) return null
        val desired = (firstIndex..lastIndex).associateWith { index ->
            val distance = before[index].startMillis - oldFirst
            targetFirstMillis + (distance * newSpan + oldSpan / 2L) / oldSpan
        }
        // Preserve the originally chosen anchor timestamps exactly.
        if (desired[firstIndex] != targetFirstMillis ||
            desired[lastIndex] != targetLastMillis) return null
        if ((firstIndex..lastIndex).all { index ->
                desired[index] == before[index].startMillis
            }) return null
        return withExactStarts(raw, before, desired)
    }

    /** Shared lossless rewrite of timestamp TAGS, never entire lyric rows. */
    private fun withExactStarts(
        raw: String,
        before: List<PrivateSubtitleCue>,
        desired: Map<Int, Long>
    ): String? {
        if (desired.isEmpty() || desired.size > before.size ||
            desired.keys.any { it !in before.indices } ||
            desired.values.any { it !in 0L until MAX_TIME_MS }) return null
        val normalized = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n').toMutableList()
        val globalOffset = declaredOffsetMillis(raw) ?: 0L
        data class TagLocation(
            val absoluteStart: Long,
            val lineIndex: Int,
            val from: Int,
            val until: Int
        )
        val tags = ArrayList<TagLocation>(before.size)
        lines.forEachIndexed { lineIndex, source ->
            val line = source.trim()
            if (line.isEmpty() || metadata.matches(line) ||
                offsetHeader.matches(line)) return@forEachIndexed
            val leading = source.indexOf(line)
            var cursor = 0
            while (cursor < line.length && line[cursor] == '[') {
                val match = timestampTag.find(line, cursor)
                    ?.takeIf { it.range.first == cursor } ?: break
                val authored = match.groupValues[1].toLong() * 60_000L +
                    match.groupValues[2].toLong() * 1_000L +
                    match.groupValues[3].padEnd(3, '0').toLong()
                tags += TagLocation(
                    authored + globalOffset, lineIndex,
                    leading + match.range.first, leading + match.range.last + 1
                )
                cursor = match.range.last + 1
            }
        }
        if (tags.size != before.size) return null
        val chronology = tags.sortedBy { it.absoluteStart }
        if (chronology.indices.any { index ->
                chronology[index].absoluteStart != before[index].startMillis
            }) return null

        val replacements = HashMap<Int, MutableList<Pair<TagLocation, String>>>()
        for ((index, newAbsolute) in desired) {
            val original = chronology[index]
            val authored = newAbsolute - globalOffset
            if (authored < 0L || authored / 60_000L > 999L) return null
            val newTag = String.format(
                Locale.ROOT, "[%02d:%02d.%03d]",
                authored / 60_000L, (authored / 1_000L) % 60L, authored % 1_000L
            )
            replacements.getOrPut(original.lineIndex) { mutableListOf() }
                .add(original to newTag)
        }
        for ((lineIndex, edits) in replacements) {
            var current = lines[lineIndex]
            for ((location, tag) in edits.sortedByDescending { it.first.from }) {
                current = current.replaceRange(location.from, location.until, tag)
            }
            lines[lineIndex] = current
        }
        val proposal = lines.joinToString("\n")
        val after = preview(proposal) ?: return null
        // Reject any hidden cue rearrangement; every unchanged original cue
        // must still be at its own index with exactly the original words.
        if (after.size != before.size || before.indices.any { index ->
                after[index].text != before[index].text ||
                    after[index].startMillis !=
                        (desired[index] ?: before[index].startMillis)
            }) return null
        return proposal
    }

    /**
     * Save the user's exact UTF-8 LRC SOURCE (not reconstructed cue text)
     * after explicit Android CreateDocument consent. This retains optional
     * offset and permitted metadata that cue-only LRC export omits.
     * No automatic provider write, overwrite or sound access happens here.
     */
    fun authoredSourceBytes(raw: String): ByteArray? {
        if (preview(raw) == null) return null
        val encoded = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(raw))
        }.getOrNull() ?: return null
        if (encoded.remaining() == 0 ||
            encoded.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        return ByteArray(encoded.remaining()).also { encoded.get(it) }
    }

    fun preview(raw: String): List<PrivateSubtitleCue>? {
        val bytes = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(raw))
        }.getOrNull() ?: return null
        if (bytes.remaining() == 0 ||
            bytes.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        val normalized = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.any {
                it == '\u0000' || (it.isISOControl() && it != '\n' && it != '\t')
            }) return null
        val rows = normalized.split('\n')
        if (rows.size > MAX_ROWS) return null
        val times = mutableListOf<Pair<Long, String>>()
        var header = true
        var offsetMs = 0L
        var offsetWasDeclared = false
        for (rawLine in rows) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (header && metadata.matches(line)) continue
            if (header && line.startsWith("[offset:")) {
                if (offsetWasDeclared) return null
                val millis = offsetHeader.matchEntire(line)?.groupValues?.get(1)
                    ?.toLongOrNull() ?: return null
                if (millis !in -MAX_OFFSET_MS..MAX_OFFSET_MS) return null
                offsetWasDeclared = true
                offsetMs = millis
                continue
            }
            header = false

            // LRC allows repeated occurrences of one lyric in a single row:
            // [00:01.25][01:05.80]same chorus text. Each explicit tag becomes
            // an independent cue; no text repetition or timing is invented.
            var position = 0
            val starts = ArrayList<Long>(2)
            while (position < line.length && line[position] == '[') {
                val match = timestampTag.find(line, position)
                    ?.takeIf { it.range.first == position } ?: break
                val minute = match.groupValues[1].toLong()
                val second = match.groupValues[2].toLong()
                val fraction = match.groupValues[3].padEnd(3, '0').toLong()
                val authoredStart = minute * 60_000L + second * 1_000L + fraction
                val start = authoredStart + offsetMs
                if (start !in 0L until MAX_TIME_MS) return null
                starts.add(start)
                if (starts.size > MAX_TIMESTAMPS_PER_LINE ||
                    times.size + starts.size > MAX_CUES) return null
                position = match.range.last + 1
            }
            if (starts.isEmpty()) return null
            val words = line.substring(position).trim()
            if (words.isEmpty() || words.length > 800 ||
                "-->" in words || words.any { it.isISOControl() }) return null
            for (start in starts) times.add(start to words)
        }
        if (times.isEmpty()) return null

        // A repeated lyric may list its second occurrence long before the
        // next physical LRC row. Restore chronological cue order explicitly.
        // Identical timestamps are ambiguous and must never silently overwrite.
        val ordered = times.sortedBy { it.first }
        if (ordered.zipWithNext().any { (a, b) -> a.first >= b.first })
            return null
        val cues = ordered.mapIndexed { index, pair ->
            val start = pair.first
            val end = if (index + 1 < ordered.size) ordered[index + 1].first
                else minOf(MAX_TIME_MS, start + 3_000L)
            if (end <= start) return null
            PrivateSubtitleCue(start, end, label(start), label(end), pair.second)
        }
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        return cues
    }

    private fun label(ms: Long) =
        String.format(Locale.ROOT, "%02d:%02d:%02d.%03d",
            ms / 3_600_000L, (ms / 60_000L) % 60L,
            (ms / 1_000L) % 60L, ms % 1_000L)
}
