package com.livelingo.android

/**
 * Offline, RAM-only search over an opened and validated private transcript.
 * Never creates an index on disk or uses cloud language/translation providers.
 */
internal object PrivateArchiveSearch {
    private const val MAX_QUERY_UNITS = 80

    fun normalizedQuery(input: String): String =
        input.trim().take(MAX_QUERY_UNITS).filterNot { it.isISOControl() }

    fun filter(
        rows: List<TranscriptExportRow>,
        userQuery: String
    ): List<TranscriptExportRow> {
        val query = PrivateOfflineSearch.fold(normalizedQuery(userQuery))
        if (query.isBlank()) return rows
        return rows.filter { row ->
            PrivateOfflineSearch.contains(row.original, query) ||
                (row.status == "Lokaal vertaald" &&
                    PrivateOfflineSearch.contains(row.translated, query)) ||
                PrivateOfflineSearch.contains(row.sourceCode, query) ||
                PrivateOfflineSearch.contains(row.targetCode, query)
        }
    }
}
