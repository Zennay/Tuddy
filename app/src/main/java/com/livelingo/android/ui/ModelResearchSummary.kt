package com.livelingo.android.ui

/**
 * Builds a bounded model-research export without cutting through a candidate's
 * license or limitation record. The caller supplies a header and self-contained
 * candidate blocks; any omitted records are counted in an explicit notice.
 */
internal object ModelResearchSummary {
    private const val MAX_CHARS = 16_000
    private const val RESERVED_NOTICE_CHARS = 256

    fun render(header: String, candidateBlocks: List<String>): String {
        val completeLength = header.length + candidateBlocks.sumOf { it.length }
        if (completeLength <= MAX_CHARS) {
            return buildString(completeLength) {
                append(header)
                candidateBlocks.forEach(::append)
            }
        }

        val output = StringBuilder(header)
        var omitted = 0
        var reachedLimit = false
        candidateBlocks.forEach { block ->
            if (reachedLimit) {
                omitted += 1
            } else if (output.length + block.length + RESERVED_NOTICE_CHARS <= MAX_CHARS) {
                output.append(block)
            } else {
                reachedLimit = true
                omitted += 1
            }
        }

        if (omitted == 0) return output.toString()
        val notice =
            "\n[Export begrensd tot volledige kandidaatblokken: $omitted van ${candidateBlocks.size} " +
                "niet opgenomen. Verfijn de taal-, ASR/MT- of licentiefilter om deze afzonderlijk te delen.]"
        // The reserved space is deliberately larger than this notice; no record is ever sliced.
        check(output.length + notice.length <= MAX_CHARS)
        return output.append(notice).toString()
    }
}
