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

internal object PrivateBilingualConversationReplay {
    const val MAX_PAIRS = 8

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
