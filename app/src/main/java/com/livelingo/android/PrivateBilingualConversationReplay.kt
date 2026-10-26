package com.livelingo.android

/**
 * A deliberately small, ephemeral practice snapshot. Only translated caption
 * rows that already exist in the user's current RAM conversation are eligible.
 * The plan cannot trigger translation or model download and never persists.
 */
internal data class BilingualReplayCaption(
    val original: String,
    val translated: String,
    val sourceCode: String,
    val targetCode: String,
    val status: String
)

internal data class BilingualReplayPair(val original: String, val translated: String)

/** One user-selectable direction that already has a playable translation. */
internal data class BilingualReplayDirection(
    val sourceCode: String,
    val targetCode: String,
    val playableCount: Int
)


/**
 * Source-first lets someone hear what was actually said; translation-first
 * permits private recall practice. Both use the identical existing, strictly
 * local voice admission for each of the two languages.
 */
internal enum class BilingualReplayOrder {
    SOURCE_FIRST, TRANSLATION_FIRST
}

internal object PrivateBilingualConversationReplay {
    const val MAX_PAIRS = 8
    const val MAX_DIRECTIONS = 16

    /**
     * Only offer directions with at least one safely playable, already
     * completed translation in this RAM conversation. Recency determines
     * menu order; no call can start ML Kit, TTS, or a model download.
     *
     * Reuse available() for the same Unicode/text boundaries and bounded
     * capacity as the eventual playback action. An unsupported/unfinished
     * pair must not appear as a misleading "ready" direction.
     */
    fun directions(rows: List<BilingualReplayCaption>): List<BilingualReplayDirection> =
        rows.asReversed().asSequence()
            .filter { it.status == "Lokaal vertaald" &&
                it.sourceCode in TargetCatalog.codes &&
                it.targetCode in TargetCatalog.codes &&
                it.sourceCode != it.targetCode
            }
            .map { it.sourceCode to it.targetCode }
            .distinct()
            .take(MAX_DIRECTIONS)
            .map { (source, target) ->
                BilingualReplayDirection(
                    source, target,
                    available(rows, source, target).size
                )
            }
            .filter { it.playableCount > 0 }
            .toList()


    /**
     * An explicit starting caption takes precedence over "latest N"; when
     * no caption was selected, preserve the old default: last N in order.
     * Never mutate the current conversation or include an unfinished caption.
     */
    fun window(
        eligible: List<BilingualReplayPair>,
        requestedCount: Int,
        startIndex: Int?
    ): List<BilingualReplayPair> {
        if (eligible.isEmpty()) return emptyList()
        val safe = eligible.takeLast(MAX_PAIRS)
        val count = requestedCount.coerceIn(1, MAX_PAIRS)
        val from = startIndex?.takeIf { it in safe.indices }
            ?: (safe.size - count).coerceAtLeast(0)
        return safe.drop(from).take(count)
    }

    fun available(
        rows: List<BilingualReplayCaption>,
        sourceCode: String,
        targetCode: String
    ): List<BilingualReplayPair> {
        if (sourceCode !in TargetCatalog.codes ||
            targetCode !in TargetCatalog.codes || sourceCode == targetCode) return emptyList()
        return rows.asReversed().asSequence()
            .filter {
                it.sourceCode == sourceCode && it.targetCode == targetCode &&
                    it.status == "Lokaal vertaald"
            }
            .mapNotNull { row ->
                // This same boundary is enforced again by the Android TTS
                // adapter; no silent truncation or replacement of private text.
                val source = LocalTextBoundaryPolicy.speechHypothesis(row.original)
                val target = LocalTextBoundaryPolicy.speechHypothesis(row.translated)
                if (source == null || target == null ||
                    source.length > MAX_SPOKEN_UNITS || target.length > MAX_SPOKEN_UNITS)
                    null
                else BilingualReplayPair(source, target)
            }
            .take(MAX_PAIRS)
            .toList()
            .asReversed()
    }

    private const val MAX_SPOKEN_UNITS = 400
}
