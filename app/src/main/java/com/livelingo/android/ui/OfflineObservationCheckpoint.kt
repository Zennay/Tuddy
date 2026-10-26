package com.livelingo.android.ui

/**
 * A deliberately limited, explicitly exported operator checkpoint.
 *
 * Holds only manual pass/fail classifications and optional durations for at
 * most 20 offline observation attempts, plus the expected/installed APK hashes.
 * No transcript, phrase, microphone audio, device ID, free text, radio history,
 * approval checkboxes, or acceptance verdict is ever serialized.
 *
 * Import deliberately does NOT restore safety/consent confirmations: they
 * must be observed and reconfirmed on the current physical session.
 */
internal data class OfflineObservationCheckpoint(
    val installedApkSha256: String,
    val expectedApkSha256: String?,
    val attempts: List<OfflineObservedAttempt>
)

internal object OfflineObservationCheckpointCodec {
    private const val HEADER = "TUDDY-MANUAL-OFFLINE-CHECKPOINT-V1"
    const val MAX_BYTES = 4096
    private val SHA256 = Regex("[a-f0-9]{64}")

    fun encode(draft: OfflineObservationCheckpoint): ByteArray? {
        if (!valid(draft)) return null
        val text = buildString {
            appendLine(HEADER)
            appendLine(draft.installedApkSha256)
            appendLine(draft.expectedApkSha256 ?: "-")
            appendLine(draft.attempts.size)
            draft.attempts.forEach { attempt ->
                append(attempt.direction.name)
                append('|')
                append(if (attempt.recognizedSpeech) '1' else '0')
                append('|')
                append(if (attempt.translatedLocally) '1' else '0')
                append('|')
                appendLine(attempt.endToEndLatencyMs?.toString() ?: "-")
            }
        }
        return text.toByteArray(Charsets.UTF_8).takeIf { it.size in 1..MAX_BYTES }
    }

    fun decode(bytes: ByteArray): OfflineObservationCheckpoint? {
        if (bytes.size !in 1..MAX_BYTES) return null
        val raw = bytes.toString(Charsets.UTF_8)
        // Only ASCII from this narrow grammar; invalid Unicode/extra lines
        // must never be treated as a partial or acceptable checkpoint.
        if (raw.any { it.code !in 10..126 || it == '\r' }) return null
        val lines = raw.split('\n')
        if (lines.lastOrNull() != "") return null
        val values = lines.dropLast(1)
        if (values.size !in 4..24 || values[0] != HEADER) return null
        val count = values[3].toIntOrNull() ?: return null
        if (count !in 0..20 || values.size != 4 + count ||
            count.toString() != values[3]) return null
        val attempts = values.drop(4).map { row ->
            val parts = row.split('|')
            if (parts.size != 4) return null
            val direction = OfflineObservationDirection.entries
                .firstOrNull { it.name == parts[0] } ?: return null
            val recognized = when (parts[1]) {
                "1" -> true
                "0" -> false
                else -> return null
            }
            val translated = when (parts[2]) {
                "1" -> true
                "0" -> false
                else -> return null
            }
            val duration = if (parts[3] == "-") null else
                parts[3].toLongOrNull()?.takeIf {
                    it in 1L..600000L && it.toString() == parts[3]
                } ?: return null
            OfflineObservedAttempt(direction, recognized, translated, duration)
        }
        val checkpoint = OfflineObservationCheckpoint(
            values[1], values[2].takeUnless { it == "-" }, attempts
        )
        // Round-trip matching rejects unknown fields, normalization and
        // unsupported formats without accepting untrusted extra metadata.
        return checkpoint.takeIf {
            valid(it) && encode(it)?.contentEquals(bytes) == true
        }
    }

    private fun valid(draft: OfflineObservationCheckpoint): Boolean {
        if (!SHA256.matches(draft.installedApkSha256) ||
            (draft.expectedApkSha256 != null &&
                !SHA256.matches(draft.expectedApkSha256))) return false
        val accepted = mutableListOf<OfflineObservedAttempt>()
        for (attempt in draft.attempts) {
            if (!OfflineObservationLedger.canRecord(accepted, attempt)) return false
            accepted.add(attempt)
        }
        return draft.attempts.size <= 20
    }
}
