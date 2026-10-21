package com.livelingo.android

import java.io.StringReader
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** A bounded text-only TTML / DFXP reader; no network, DTD, styles or inherited timelines. */
internal object PrivateTtmlSubtitleReader {
    private const val MAX_CUES = 500
    private const val MAX_TIME_MS = 359_999_999L
    private val clock = Regex("""^(\d{2,3}):([0-5]\d):([0-5]\d)(?:\.(\d{1,3}))?$""")
    private val seconds = Regex("""^(\d{1,6})(?:\.(\d{1,3}))?s$""")
    private val millis = Regex("""^(\d{1,9})ms$""")

    fun read(raw: String): List<PrivateSubtitleCue>? {
        if (raw.isBlank() || raw.length > PrivateSubtitleFileReader.MAX_BYTES) return null
        val encoded = runCatching {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(raw))
        }.getOrNull() ?: return null
        if (encoded.remaining() > PrivateSubtitleFileReader.MAX_BYTES) return null
        val trimmed = raw.removePrefix("\uFEFF").trimStart()
        val xml = if (trimmed.startsWith("<?xml ")) {
            val end = trimmed.indexOf("?>")
            if (end < 0 || end > 160) return null
            trimmed.substring(end + 2)
        } else trimmed
        // No document type, custom entity declarations, comments or CDATA.
        if (xml.contains("<!") || xml.contains("<?")) return null
        if (raw.any { it == '\u0000' ||
                (it.isISOControl() && it != '\r' && it != '\n' && it != '\t') }) return null
        return runCatching { parse(raw.removePrefix("\uFEFF")) }.getOrNull()
    }

    private fun parse(raw: String): List<PrivateSubtitleCue>? {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()
        parser.setInput(StringReader(raw))
        val stack = ArrayList<String>()
        val cues = ArrayList<PrivateSubtitleCue>()
        val paragraph = StringBuilder()
        var skippedHead = 0
        var hasBody = false
        var activeStart: Long? = null
        var activeEnd: Long? = null
        var lastStart = -1L

        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    val parent = stack.lastOrNull()
                    stack.add(name)
                    if (skippedHead > 0) {
                        skippedHead++
                        continue
                    }
                    when {
                        stack.size == 1 && name == "tt" -> {
                            if (hasTiming(parser)) return null
                        }
                        stack.size == 2 && name == "head" && parent == "tt" &&
                            !hasBody -> skippedHead = 1
                        name == "body" && parent == "tt" && !hasBody -> {
                            if (hasTiming(parser)) return null
                            hasBody = true
                        }
                        name == "div" && parent in setOf("body", "div") -> {
                            if (hasTiming(parser)) return null
                        }
                        name == "p" && parent in setOf("body", "div") &&
                            activeStart == null -> {
                            if (hasTiming(parser, paragraph = true)) return null
                            val start = time(parser.getAttributeValue(null, "begin"))
                                ?: return null
                            val end = time(parser.getAttributeValue(null, "end"))
                                ?: return null
                            if (start < lastStart || end <= start) return null
                            activeStart = start
                            activeEnd = end
                            paragraph.setLength(0)
                        }
                        name == "span" && parent in setOf("p", "span") &&
                            activeStart != null -> {
                            if (hasTiming(parser)) return null
                        }
                        name == "br" && parent in setOf("p", "span") &&
                            activeStart != null -> {
                            if (hasTiming(parser)) return null
                            paragraph.append('\n')
                        }
                        else -> return null
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (stack.isEmpty() || stack.last() != parser.name) return null
                    val name = stack.removeAt(stack.lastIndex)
                    if (skippedHead > 0) {
                        skippedHead--
                        continue
                    }
                    if (name == "p") {
                        val start = activeStart ?: return null
                        val end = activeEnd ?: return null
                        val cleaned = paragraph.toString().replace(Regex("[ \\t]+"), " ").trim()
                        val rows = cleaned.split('\n').map { it.trim() }
                        if (cleaned.isBlank() || cleaned.length > 2_400 ||
                            rows.size > 8 ||
                            rows.any { it.isBlank() || it.length > 800 }) return null
                        cues.add(PrivateSubtitleCue(start, end, stamp(start),
                            stamp(end), rows.joinToString("\n")))
                        if (cues.size > MAX_CUES) return null
                        lastStart = start
                        activeStart = null
                        activeEnd = null
                    }
                }
                XmlPullParser.TEXT -> {
                    if (skippedHead == 0) {
                        val part = parser.text.orEmpty()
                        if (activeStart == null) {
                            if (part.isNotBlank()) return null
                        } else {
                            paragraph.append(part)
                            if (paragraph.length > 4_800) return null
                        }
                    }
                }
                XmlPullParser.END_DOCUMENT -> break
                else -> return null
            }
        }
        if (stack.isNotEmpty() || !hasBody || activeStart != null || cues.isEmpty())
            return null
        if (PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT) == null)
            return null
        return cues
    }

    private fun hasTiming(
        parser: XmlPullParser,
        paragraph: Boolean = false
    ): Boolean {
        val forbidden = setOf(
            "dur", "timeContainer", "repeatCount", "repeatDur",
            "timeBase", "frameRate", "frameRateMultiplier", "tickRate", "subFrameRate"
        )
        for (i in 0 until parser.attributeCount) {
            val name = parser.getAttributeName(i)
            if (name in forbidden || (!paragraph && name in setOf("begin", "end")))
                return true
            if (name == "space" &&
                parser.getAttributeNamespace(i) == "http://www.w3.org/XML/1998/namespace" &&
                parser.getAttributeValue(i) == "preserve") return true
        }
        return false
    }

    private fun time(raw: String?): Long? {
        if (raw == null) return null
        clock.matchEntire(raw)?.let {
            val hours = it.groupValues[1].toLongOrNull() ?: return null
            val mins = it.groupValues[2].toLong()
            val secs = it.groupValues[3].toLong()
            val fraction = it.groupValues[4].padEnd(3, '0').toLongOrNull() ?: 0L
            return (hours * 3_600_000L + mins * 60_000L + secs * 1_000L +
                fraction).takeIf { value -> value <= MAX_TIME_MS }
        }
        seconds.matchEntire(raw)?.let {
            val value = it.groupValues[1].toLongOrNull() ?: return null
            val fraction = it.groupValues[2].padEnd(3, '0').toLongOrNull() ?: 0L
            return (value * 1_000L + fraction)
                .takeIf { answer -> answer <= MAX_TIME_MS }
        }
        millis.matchEntire(raw)?.let {
            return it.groupValues[1].toLongOrNull()?.takeIf { value ->
                value <= MAX_TIME_MS
            }
        }
        return null
    }

    private fun stamp(ms: Long): String = String.format(
        Locale.ROOT, "%02d:%02d:%02d,%03d",
        ms / 3_600_000L, ms / 60_000L % 60,
        ms / 1_000L % 60, ms % 1_000L
    )
}
