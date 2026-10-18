package com.livelingo.android

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * User-initiated private eight-week calendar snapshot: dates and counts ONLY.
 * The caller alone handles Android's document picker and explicit save.
 */
internal object PrivateStudyCalendar {
    private val dateFormat = DateTimeFormatter.BASIC_ISO_DATE
    private const val MAX_BYTES = 16_384

    fun snapshot(
        words: PrivateVocabularyDeck,
        ownWords: PrivateOwnWordDeck,
        phrases: PrivatePhraseDeck
    ): ByteArray? {
        val curated = if (words.reviewsEnabled()) words.scheduledReviews().map { it.dueDay }
            else emptyList()
        val own = if (ownWords.reviewsEnabled())
            ownWords.scheduledReviews()?.map { it.dueDay } else emptyList()
        val personal = if (phrases.reviewsEnabled())
            phrases.scheduledReviews()?.map { it.dueDay } else emptyList()
        // Do not pretend that unreadable local data contains zero cards.
        if (own == null || personal == null) return null

        val today = LocalDate.now()
        val start = today.toEpochDay()
        val stamp = today.format(dateFormat) + "T000000Z"
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Tuddy//Local review calendar//NL",
            "CALSCALE:GREGORIAN",
            "METHOD:PUBLISH",
            "X-WR-CALNAME:Tuddy offline studieplanning"
        )
        var events = 0
        for (offset in 0L..55L) {
            val date = today.plusDays(offset)
            val due = start + offset
            fun count(rows: List<Long>) =
                rows.count { if (offset == 0L) it <= due else it == due }
            val base = count(curated)
            val mine = count(own)
            val sentences = count(personal)
            val total = base + mine + sentences
            if (total == 0) continue
            events++
            lines.addAll(
                listOf(
                    "BEGIN:VEVENT",
                    "UID:tuddy-review-" + date.format(dateFormat) + "@offline.local",
                    "DTSTAMP:$stamp",
                    "DTSTART;VALUE=DATE:" + date.format(dateFormat),
                    "DTEND;VALUE=DATE:" + date.plusDays(1).format(dateFormat),
                    "SUMMARY:Tuddy - $total studiekaarten",
                    "DESCRIPTION:Basis $base / Eigen woorden $mine / Eigen zinnen $sentences. " +
                        "Planning is voorlopig en verandert na jouw zelfbeoordeling.",
                    "TRANSP:TRANSPARENT",
                    "END:VEVENT"
                )
            )
        }
        if (events == 0) return null
        lines += "END:VCALENDAR"
        // ASCII-only fields keep RFC 5545 line folding trivial and avoid leaking study text.
        val bytes = (lines.joinToString("\r\n") + "\r\n").toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= MAX_BYTES }
    }
}
