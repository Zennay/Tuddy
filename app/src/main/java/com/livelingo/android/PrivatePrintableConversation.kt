package com.livelingo.android

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Explicit, offline, print-friendly HTML of conversation captions.
 * The caller owns consent and Android SAF file creation. No external
 * resources, scripts, analytics, audio or invented translation text.
 */
internal object PrivatePrintableConversation {
    private const val MAX_ROWS = 500
    private const val MAX_UNITS = 4096
    private const val MAX_BYTES = 262_144
    private const val MAX_FUTURE_MS = 86_400_000L
    private val RTL = setOf("ar", "fa", "ur", "he", "ckb", "ps")

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
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

    private fun safe(value: String): Boolean =
        value.length <= MAX_UNITS && value.none {
            Character.isSurrogate(it) || it == '\u0000' ||
                (Character.isISOControl(it) && it != '\n' && it != '\r' && it != '\t') ||
                it == '\u2028' || it == '\u2029' ||
                (Character.getType(it) == Character.FORMAT.toInt() && it != '\u200C')
        }

    fun snapshot(rows: List<TranscriptExportRow>, onlyFinished: Boolean): ByteArray? {
        if (rows.isEmpty() || rows.size > MAX_ROWS) return null
        val current = System.currentTimeMillis()
        if (rows.any {
            it.recordedAtMillis <= 0L || it.recordedAtMillis > current + MAX_FUTURE_MS ||
                it.sourceCode !in TargetCatalog.codes ||
                it.targetCode !in TargetCatalog.codes ||
                it.original.isBlank() ||
                !safe(it.original) || !safe(it.translated)
        }) return null
        val selected = if (onlyFinished) rows.filter {
            it.status == "Lokaal" || it.status == "Lokaal vertaald"
        } else rows
        if (selected.isEmpty()) return null
        val dates = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
        val html = StringBuilder(
            "<!doctype html>\n<html lang=\"nl\"><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<meta name=\"referrer\" content=\"no-referrer\"><title>Mijn Tuddy-gesprek</title>" +
                "<style>body{font-family:system-ui,sans-serif;max-width:850px;margin:0 auto;" +
                "padding:24px;background:#fff;color:#151923;line-height:1.5}" +
                "h1{font-size:1.7rem}p.note{color:#475569}" +
                "article{border:1px solid #6b7280;border-radius:10px;padding:16px;" +
                "margin:14px 0;break-inside:avoid;page-break-inside:avoid}" +
                ".tag{font-size:.85rem;color:#475569;font-weight:600}" +
                ".original{font-size:1.25rem;font-weight:600;white-space:pre-wrap;" +
                "unicode-bidi:plaintext;overflow-wrap:anywhere;margin:6px 0}" +
                ".translation{font-size:1.14rem;white-space:pre-wrap;unicode-bidi:plaintext;" +
                "overflow-wrap:anywhere;margin:6px 0}" +
                "@media print{body{padding:0}article{border-color:#777}}" +
                "</style></head><body><main><h1>Mijn Tuddy-gesprek</h1>" +
                "<p class=\"note\">Alleen door mij expliciet opgeslagen tijdelijke " +
                "gespreksregels. Vertalingen zijn niet onafhankelijk gecontroleerd. " +
                "Audio en echte woordtiming zijn niet inbegrepen. De gekozen " +
                "bestandsprovider kan dit bestand delen of synchroniseren.</p>\n"
        )
        selected.forEachIndexed { index, row ->
            val originalDir = if (row.sourceCode in RTL) "rtl" else "ltr"
            val targetDir = if (row.targetCode in RTL) "rtl" else "ltr"
            val done = row.status == "Lokaal vertaald" && row.translated.isNotBlank()
            val same = row.status == "Lokaal" && row.sourceCode == row.targetCode
            html.append("<article><div class=\"tag\">")
                .append(index + 1).append(" · ")
                .append(escape(row.sourceCode.uppercase(Locale.ROOT)))
                .append(" → ").append(escape(row.targetCode.uppercase(Locale.ROOT)))
                .append(" · ").append(escape(dates.format(Date(row.recordedAtMillis))))
                .append("</div><div class=\"original\" lang=\"")
                .append(row.sourceCode).append("\" dir=\"").append(originalDir).append("\">")
                .append(escape(row.original)).append("</div>")
                .append("<div class=\"tag\">")
                .append(if (done) "Lokaal vertaalde tekst"
                    else if (same) "Gelijke teksttaal" else "Vertaling niet voltooid")
                .append("</div>")
            if (done || same) {
                html.append("<div class=\"translation\" lang=\"")
                    .append(row.targetCode).append("\" dir=\"").append(targetDir)
                    .append("\">").append(escape(if (same) row.original else row.translated))
                    .append("</div>")
            } else {
                html.append("<p>Geen voltooide vertaling beschikbaar.</p>")
            }
            html.append("</article>\n")
            if (html.length > MAX_BYTES) return null
        }
        html.append("</main></body></html>\n")
        return html.toString().toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BYTES }
    }
}
