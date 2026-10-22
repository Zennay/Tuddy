package com.livelingo.android

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** One explicitly authored turn; never ASR output or a machine translation. */
internal data class PrivateAuthoredConversationTurn(
    val note: PersonalPhrase,
    val fromMeaning: Boolean
)

/**
 * Strict, offline, round-trippable encoding of the existing manual dialogue
 * text export. No automatic persistence and no execution of input content.
 */
internal object PrivateAuthoredConversationScript {
    const val MAX_BYTES = 8192
    private const val HEADER = "Tuddy - mijn eigen offline gesprekszinnen\n" +
        "Zelf geschreven betekenissen; niet geverifieerd of automatisch vertaald.\n" +
        "Geen herkende spraak, audiobestand, kloktijden of offline modelbewijs.\n\n"
    private val sourceLanguages = setOf("kmr", "ckb", "ps")
    private val meaningLanguages = setOf("nl", "en")
    private val marker = Regex("""^(\d{1,2})\. (kmr|ckb|ps|nl|en) -> (kmr|ckb|ps|nl|en)$""")

    private fun valid(note: PersonalPhrase): Boolean =
        note.language in sourceLanguages && note.meaningLanguage in meaningLanguages &&
            PrivatePhraseTextPolicy.valid(note.phrase, 120) &&
            PrivatePhraseTextPolicy.valid(note.ownMeaning, 180) &&
            !note.phrase.contains('\n') && !note.ownMeaning.contains('\n')

    fun render(turns: List<PrivateAuthoredConversationTurn>): ByteArray? {
        if (turns.size !in 1..12 || turns.any { !valid(it.note) }) return null
        val script = buildString {
            append(HEADER)
            turns.forEachIndexed { index, turn ->
                val note = turn.note
                val spoken = if (turn.fromMeaning) note.ownMeaning else note.phrase
                val answer = if (turn.fromMeaning) note.phrase else note.ownMeaning
                val from = if (turn.fromMeaning) note.meaningLanguage else note.language
                val to = if (turn.fromMeaning) note.language else note.meaningLanguage
                appendLine((index + 1).toString() + ". " + from + " -> " + to)
                appendLine("Eigen bronzin: " + spoken)
                appendLine("Mijn gekozen paar: " + answer)
                appendLine()
            }
        }
        return script.toByteArray(Charsets.UTF_8).takeIf { it.size in 1..MAX_BYTES }
    }

    /**
     * Accept only the exact documented text schema (and common CRLF).
     * A malformed, truncated or invented translation never enters a session.
     */
    fun read(bytes: ByteArray): List<PrivateAuthoredConversationTurn>? {
        if (bytes.size !in 1..MAX_BYTES) return null
        val decoded = runCatching {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        }.getOrNull() ?: return null
        val normalized = decoded.removePrefix("\uFEFF").replace("\r\n", "\n")
        if (!normalized.startsWith(HEADER) || normalized.contains('\r')) return null
        val body = normalized.removePrefix(HEADER).trimEnd('\n')
        if (body.isBlank()) return null
        val blocks = body.split("\n\n")
        if (blocks.size !in 1..12) return null
        val result = ArrayList<PrivateAuthoredConversationTurn>(blocks.size)
        blocks.forEachIndexed { index, block ->
            val lines = block.split('\n')
            if (lines.size != 3) return null
            val found = marker.matchEntire(lines[0]) ?: return null
            if (found.groupValues[1].toIntOrNull() != index + 1) return null
            if (!lines[1].startsWith("Eigen bronzin: ") ||
                !lines[2].startsWith("Mijn gekozen paar: ")) return null
            val from = found.groupValues[2]
            val to = found.groupValues[3]
            val spoken = lines[1].removePrefix("Eigen bronzin: ")
            val answer = lines[2].removePrefix("Mijn gekozen paar: ")
            val fromMeaning = from in meaningLanguages && to in sourceLanguages
            if (!fromMeaning && !(from in sourceLanguages && to in meaningLanguages))
                return null
            val note = if (fromMeaning)
                PersonalPhrase(to, answer, from, spoken)
            else PersonalPhrase(from, spoken, to, answer)
            if (!valid(note)) return null
            result.add(PrivateAuthoredConversationTurn(note, fromMeaning))
        }
        return result.takeIf { it.isNotEmpty() }
    }
}
