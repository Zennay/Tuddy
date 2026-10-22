package com.livelingo.android

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Explicit document transfer of HUMAN-AUTHORED in-memory corrections.
 * No implicit persistence, network, clipboard, logging or model admission.
 * The import replaces current RAM entries only after an external UI confirmation.
 */
internal object SessionTranslationCorrectionTransfer {
    const val MAX_BYTES = 48_000
    private const val FORMAT = "tuddy-human-corrections"
    private const val VERSION = 1
    private val rootKeys = setOf("format", "version", "entries")
    private val rowKeys = setOf("original", "translation", "sourceCode", "targetCode")

    private fun exactKeys(json: JSONObject, expected: Set<String>): Boolean {
        if (json.length() != expected.size) return false
        return json.keys().asSequence().toSet() == expected
    }

    fun encode(entries: List<SessionTranslationCorrection>): ByteArray? {
        if (!SessionTranslationCorrections.validEntries(entries)) return null
        val rows = JSONArray()
        entries.forEach { item ->
            rows.put(JSONObject().apply {
                put("original", item.original)
                put("translation", item.translation)
                put("sourceCode", item.sourceCode)
                put("targetCode", item.targetCode)
            })
        }
        val payload = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("entries", rows)
        }.toString().toByteArray(Charsets.UTF_8)
        return payload.takeIf { it.size <= MAX_BYTES }
    }

    fun decode(bytes: ByteArray): List<SessionTranslationCorrection>? {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        val source = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            return null
        }
        val root = runCatching { JSONObject(source) }.getOrNull() ?: return null
        if (!exactKeys(root, rootKeys) ||
            root.opt("format") != FORMAT ||
            root.opt("version") !is Int || root.optInt("version") != VERSION) return null
        val array = root.optJSONArray("entries") ?: return null
        if (array.length() > SessionTranslationCorrections.MAX_ENTRIES) return null
        val rows = mutableListOf<SessionTranslationCorrection>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: return null
            if (!exactKeys(item, rowKeys) ||
                rowKeys.any { item.opt(it) !is String }) return null
            rows.add(
                SessionTranslationCorrection(
                    item.getString("original"), item.getString("translation"),
                    item.getString("sourceCode"), item.getString("targetCode")
                )
            )
        }
        return rows.takeIf { SessionTranslationCorrections.validEntries(it) }
    }
}
