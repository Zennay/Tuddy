package com.livelingo.android

import java.util.Locale

/**
 * Combine two manually selected caption tracks into a temporary parallel reading copy.
 *
 * Pairing is based ONLY on overlapping timestamps (not an audio clock, translation
 * engine or language identification). Each cue is paired at most once. Secondary
 * cues without a sufficiently overlapping primary cue remain visible as standalone
 * rows, rather than silently disappearing. Original source files are untouched.
 */
/** Exact original 1-based row numbers, intentionally paired by the user. */
internal data class SubtitleManualPair(val primaryRow: Int, val secondaryRow: Int)

internal data class SubtitleCuePairingLink(
    val primarySourceRow: Int,
    val secondarySourceRow: Int,
    val primaryStart: String,
    val secondaryStart: String,
    val overlapMillis: Long,
    val primaryText: String,
    val secondaryText: String,
    val manuallyChosen: Boolean = false
)

internal data class SubtitleTrackPairingPreview(
    val cues: List<PrivateSubtitleCue>,
    val pairedCount: Int,
    val unpairedSecondTrack: Int,
    val unpairedFirstTrack: Int,
    val pairedLinks: List<SubtitleCuePairingLink>,
    val unpairedPrimaryRows: List<Int> = emptyList(),
    val unpairedSecondaryRows: List<Int> = emptyList()
)

internal object PrivateSubtitleTrackPairing {
    // Offsets are explicitly authored by the user (up to ten minutes each way).
    // Timing, pair counts, and BOTH export snapshots still gate the preview.
    private fun stamp(millis: Long): String = String.format(Locale.ROOT,
        "%02d:%02d:%02d,%03d", millis / 3_600_000L,
        millis / 60_000L % 60, millis / 1000L % 60, millis % 1000L)

    fun preview(
        primary: List<PrivateSubtitleCue>,
        secondary: List<PrivateSubtitleCue>,
        secondTrackOffsetMs: Long = 0L,
        excludedSecondaryRows: Set<Int> = emptySet(),
        manualPairs: List<SubtitleManualPair> = emptyList()
    ): SubtitleTrackPairingPreview? {
        if (!PrivateSubtitleOffset.isSupported(secondTrackOffsetMs) ||
            primary.isEmpty() || secondary.isEmpty() ||
            primary.size > 500 || secondary.size > 500 ||
            excludedSecondaryRows.size > secondary.size ||
            excludedSecondaryRows.any { it !in 1..secondary.size } ||
            PrivateSubtitleDraftEditor.snapshot(primary, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(secondary, SubtitleDraftFormat.SRT) == null
        ) return null

        // A pinned pairing is never inferred from audio or text. Require
        // unique, chronological source-row pairs, not exceeding a bounded 32.
        // A pinned secondary cue cannot also be manually excluded.
        if (manualPairs.size > 32) return null
        val ordered = manualPairs.sortedBy { it.primaryRow }
        var previousPrimary = 0
        var previousSecondary = 0
        for (pair in ordered) {
            if (pair.primaryRow !in 1..primary.size ||
                pair.secondaryRow !in 1..secondary.size ||
                pair.primaryRow <= previousPrimary ||
                pair.secondaryRow <= previousSecondary ||
                pair.secondaryRow in excludedSecondaryRows) return null
            previousPrimary = pair.primaryRow
            previousSecondary = pair.secondaryRow
        }

        // The offset is user-selected and applies only in this temporary draft.
        // Reject shifts beyond the same limits as the underlying SRT/VTT editor.
        val adjusted = mutableListOf<PrivateSubtitleCue>()
        for (cue in secondary) {
            val start = cue.startMillis + secondTrackOffsetMs
            val end = cue.endMillis + secondTrackOffsetMs
            if (start < 0L || end > 359_999_999L) return null
            adjusted += if (secondTrackOffsetMs == 0L) cue else cue.copy(
                startMillis = start, endMillis = end,
                startLabel = stamp(start), endLabel = stamp(end)
            )
        }
        // Optimize source-order-respecting one-to-one caption pairing over the
        // complete authored timelines. The old greedy algorithm could assign
        // an early secondary cue to a later primary cue, blocking a better
        // chronological pairing for the following caption.
        //
        // First maximize the number of valid pairs, then their total real
        // overlap. Preserve the original order of both sources and keep EVERY
        // unmatched secondary cue as a visible standalone row.
        //
        // Each axis is capped at 500 cues. The O(n*m) DP uses bounded local
        // arrays only; it does not retain, upload or persist source text.
        val n = primary.size
        val m = adjusted.size
        val width = m + 1
        val cells = (n + 1) * width
        val bestPairs = IntArray(cells) { -1 }
        val bestOverlaps = LongArray(cells)
        val pinnedPrimary = IntArray(n) { -1 }
        val pinnedSecondary = IntArray(m) { -1 }
        ordered.forEach {
            pinnedPrimary[it.primaryRow - 1] = it.secondaryRow - 1
            pinnedSecondary[it.secondaryRow - 1] = it.primaryRow - 1
        }
        // 1=skip primary, 2=skip secondary, 3=pair. Ties prefer a
        // chronological pair over either skip; otherwise prefer earlier
        // primary cues on equal-quality paths.
        val moves = ByteArray(cells)

        fun pairOverlap(source: PrivateSubtitleCue, candidate: PrivateSubtitleCue): Long {
            val overlap = minOf(source.endMillis, candidate.endMillis) -
                maxOf(source.startMillis, candidate.startMillis)
            if (overlap <= 0L) return 0L
            val shorter = minOf(source.endMillis - source.startMillis,
                candidate.endMillis - candidate.startMillis)
            return if (shorter > 0L && overlap >= (shorter + 1L) / 2L) overlap
                else 0L
        }

        // A valid path must include every explicitly pinned pair. Skipping
        // either of its source rows is forbidden. Unpinned gaps retain the
        // maximum-pair / maximum-overlap objective and source ordering.
        bestPairs[n * width + m] = 0
        for (i in n - 1 downTo 0) {
            val cell = i * width + m
            if (pinnedPrimary[i] < 0 && bestPairs[cell + width] >= 0) {
                bestPairs[cell] = 0
                moves[cell] = 1
            }
        }
        for (j in m - 1 downTo 0) {
            val cell = n * width + j
            if (pinnedSecondary[j] < 0 && bestPairs[cell + 1] >= 0) {
                bestPairs[cell] = 0
                moves[cell] = 2
            }
        }

        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                val cell = i * width + j
                val below = (i + 1) * width + j
                val right = cell + 1
                val diagonal = below + 1
                var count = -1
                var totalOverlap = 0L
                var move: Byte = 0
                if (pinnedPrimary[i] < 0 && bestPairs[below] >= 0) {
                    count = bestPairs[below]
                    totalOverlap = bestOverlaps[below]
                    move = 1
                }
                if (pinnedSecondary[j] < 0 && bestPairs[right] >= 0 &&
                    (bestPairs[right] > count ||
                        (bestPairs[right] == count &&
                            bestOverlaps[right] > totalOverlap))) {
                    count = bestPairs[right]
                    totalOverlap = bestOverlaps[right]
                    move = 2
                }
                val explicitlyPaired = pinnedPrimary[i] == j
                val ordinaryPair = pinnedPrimary[i] < 0 &&
                    pinnedSecondary[j] < 0
                // An explicit manual pairing may have no timestamp overlap;
                // the user already supplied both exact original source rows.
                val overlap = if ((j + 1) in excludedSecondaryRows) 0L
                    else pairOverlap(primary[i], adjusted[j])
                if (bestPairs[diagonal] >= 0 &&
                    (explicitlyPaired || (ordinaryPair && overlap > 0L))) {
                    val realOverlap = if (explicitlyPaired)
                        maxOf(0L, minOf(primary[i].endMillis,
                            adjusted[j].endMillis) -
                            maxOf(primary[i].startMillis, adjusted[j].startMillis))
                        else overlap
                    val pairedCount = bestPairs[diagonal] + 1
                    val pairedOverlap = bestOverlaps[diagonal] + realOverlap
                    if (pairedCount > count ||
                        (pairedCount == count && pairedOverlap >= totalOverlap)) {
                        count = pairedCount
                        totalOverlap = pairedOverlap
                        move = 3
                    }
                }
                bestPairs[cell] = count
                bestOverlaps[cell] = totalOverlap
                moves[cell] = move
            }
        }
        if (bestPairs[0] < 0) return null

        val matched = arrayOfNulls<String>(n)
        val links = ArrayList<SubtitleCuePairingLink>()
        val leftovers = ArrayList<PrivateSubtitleCue>()
        var paired = 0
        var primaryIndex = 0
        var secondaryIndex = 0
        while (primaryIndex < n && secondaryIndex < m) {
            when (moves[primaryIndex * width + secondaryIndex].toInt()) {
                3 -> {
                    val source = primary[primaryIndex]
                    val candidate = adjusted[secondaryIndex]
                    val manuallyChosen =
                        pinnedPrimary[primaryIndex] == secondaryIndex
                    val overlap = if (manuallyChosen)
                        maxOf(0L, minOf(source.endMillis, candidate.endMillis) -
                            maxOf(source.startMillis, candidate.startMillis))
                    else pairOverlap(source, candidate)
                    links += SubtitleCuePairingLink(
                        primarySourceRow = primaryIndex + 1,
                        secondarySourceRow = secondaryIndex + 1,
                        primaryStart = source.startLabel,
                        secondaryStart = candidate.startLabel,
                        overlapMillis = overlap,
                        primaryText = source.text,
                        secondaryText = candidate.text,
                        manuallyChosen = manuallyChosen
                    )
                    matched[primaryIndex] = candidate.text
                    paired++
                    primaryIndex++
                    secondaryIndex++
                }
                1 -> primaryIndex++
                else -> {
                    leftovers += adjusted[secondaryIndex]
                    secondaryIndex++
                }
            }
        }
        while (secondaryIndex < m) {
            leftovers += adjusted[secondaryIndex]
            secondaryIndex++
        }

        val rows = primary.mapIndexed { index, cue ->
            val other = matched[index]
            // A user-authored secondary cue is never silently erased, even
            // when both texts happen to be identical: the separate track
            // remains visible as its own second line.
            if (other == null) cue
            else cue.copy(text = cue.text + "\n" + other)
        }.plus(leftovers).sortedBy { it.startMillis }

        // Enforce BOTH export formats before allowing the RAM-only change.
        if (PrivateSubtitleDraftEditor.snapshot(rows, SubtitleDraftFormat.SRT) == null ||
            PrivateSubtitleDraftEditor.snapshot(rows, SubtitleDraftFormat.WEBVTT) == null
        ) return null
        val linkedSecondaryRows = links.mapTo(HashSet<Int>()) {
            it.secondarySourceRow
        }
        return SubtitleTrackPairingPreview(
            cues = rows,
            pairedCount = paired,
            unpairedSecondTrack = leftovers.size,
            unpairedFirstTrack = n - paired,
            pairedLinks = links.toList(),
            unpairedPrimaryRows = matched.indices.filter { matched[it] == null }
                .map { it + 1 },
            unpairedSecondaryRows = (1..m).filterNot { it in linkedSecondaryRows }
        )
    }
}
