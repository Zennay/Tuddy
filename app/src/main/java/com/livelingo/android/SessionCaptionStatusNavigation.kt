package com.livelingo.android

/**
 * Human-chosen, RAM-only navigation for a live conversation. Unlike a
 * filtered export, this class never mutates, hides or reorders captions:
 * it only identifies which existing row positions the user wants to visit.
 */
internal enum class SessionCaptionStatusFilter(val label: String) {
    ALL("Alle"),
    NEEDS_ATTENTION("Aandacht"),
    WAITING("Bezig"),
    FINISHED("Voltooid")
}

internal object SessionCaptionStatusNavigation {
    fun matches(status: String, filter: SessionCaptionStatusFilter): Boolean = when (filter) {
        SessionCaptionStatusFilter.ALL -> true
        SessionCaptionStatusFilter.NEEDS_ATTENTION ->
            status in setOf("Fout", "Model ontbreekt", "Gepauzeerd")
        SessionCaptionStatusFilter.WAITING -> status == "Bezig"
        SessionCaptionStatusFilter.FINISHED ->
            status in setOf("Lokaal vertaald", "Lokaal")
    }

    fun isActive(filter: SessionCaptionStatusFilter, normalizedQuery: String): Boolean =
        filter != SessionCaptionStatusFilter.ALL || normalizedQuery.isNotBlank()

    /** Indexes stay aligned with the original Compose LazyColumn items(lines). */
    fun matchingIndexes(
        statuses: List<String>,
        filter: SessionCaptionStatusFilter,
        normalizedQuery: String,
        matchesText: (Int) -> Boolean
    ): List<Int> {
        if (!isActive(filter, normalizedQuery)) return emptyList()
        return statuses.indices.filter { index ->
            matches(statuses[index], filter) &&
                (normalizedQuery.isBlank() || matchesText(index))
        }
    }
}
