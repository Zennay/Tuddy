package com.livelingo.android.ui

/**
 * Explicit export of the already rendered, user-authored practice overview.
 * The caller supplies only privatePracticeSummary(..., forExport = true):
 * neither audio bytes nor the Android audio URI are accepted.
 * No external fonts, scripts, links, fetches or persisted state.
 */
internal enum class PrivatePracticeReportFormat(
    val mime: String, val filename: String, val title: String
) {
    HTML("text/html", "Tuddy-mijn-luisteroefening.html", "HTML"),
    TXT("text/plain", "Tuddy-mijn-luisteroefening.txt", "TXT")
}

internal data class PendingPrivatePracticeReport(
    val summary: String,
    val format: PrivatePracticeReportFormat,
    val bytes: ByteArray
)

internal object PrivatePrintablePracticeSummary {
    private const val MAX_CHARS = 16_384
    private const val MAX_BYTES = 65_536

    private fun valid(summary: String): Boolean {
        if (summary.isBlank() || summary.length > MAX_CHARS ||
            !summary.startsWith("TUDDY · MIJN PRIVÉ LUISTEROEFENING\n")
        ) return false
        var index = 0
        while (index < summary.length) {
            val char = summary[index]
            if (Character.isHighSurrogate(char)) {
                if (index + 1 >= summary.length ||
                    !Character.isLowSurrogate(summary[index + 1])) return false
                index += 2
                continue
            }
            if (Character.isLowSurrogate(char) ||
                (char.isISOControl() && char != '\n' && char != '\t')
            ) return false
            index++
        }
        return true
    }

    fun snapshotText(summary: String): ByteArray? =
        if (valid(summary)) summary.toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES } else null

    fun snapshot(summary: String): ByteArray? {
        if (!valid(summary)) return null
        val escaped = buildString(summary.length) {
            summary.forEach { char ->
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(char)
                }
            }
        }
        val html = """
            <!doctype html>
            <html lang="nl"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <meta name="referrer" content="no-referrer">
            <title>Mijn privé Tuddy-luisteroefening</title>
            <style>
            body{font-family:system-ui,sans-serif;max-width:800px;margin:auto;padding:24px;
              line-height:1.6;color:#202332;background:#fff}
            h1{font-size:1.5rem}
            .report{white-space:pre-wrap;overflow-wrap:anywhere;unicode-bidi:plaintext;
              border:1px solid #555;padding:20px;border-radius:10px}
            .note{font-size:.9rem;color:#414854}
            @media print{body{padding:0}.report{border:0;padding:0}}
            </style></head><body>
            <main><h1>Mijn privé luisteroefening</h1>
            <p class="note">Eigen tijdcodes en beoordelingen, niet automatisch gecontroleerd.
            Audio en bestandslocaties zijn niet opgenomen. Dit bestand is bewust door mij
            bewaard en kan door mijn gekozen bestandsprovider gesynchroniseerd worden.</p>
            <div class="report">$escaped</div>
            </main></body></html>
        """.trimIndent()
        return html.toByteArray(Charsets.UTF_8).takeIf { it.size <= MAX_BYTES }
    }
}
