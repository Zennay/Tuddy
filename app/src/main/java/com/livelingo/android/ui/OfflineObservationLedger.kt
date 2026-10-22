package com.livelingo.android.ui

/**
 * Operator-entered observations of a physical, disconnected Android device.
 * Neither audio nor private text nor identifiers are accepted here. These
 * records are not an automated inference of device/model correctness.
 */
internal enum class OfflineObservationDirection(val label: String) {
    NL_EN("NL → EN"),
    EN_NL("EN → NL")
}

internal data class OfflineObservedAttempt(
    val direction: OfflineObservationDirection,
    val recognizedSpeech: Boolean,
    val translatedLocally: Boolean,
    val endToEndLatencyMs: Long?
)

internal data class OfflineObservationTotals(
    val attempts: Int,
    val recognized: Int,
    val translated: Int,
    val failedSpeech: Int,
    val failedTranslationAfterSpeech: Int,
    val timedTranslations: Int
)

internal object OfflineObservationLedger {
    const val REQUIRED_ATTEMPTS_PER_DIRECTION = 10

    fun totals(
        attempts: List<OfflineObservedAttempt>,
        direction: OfflineObservationDirection
    ): OfflineObservationTotals {
        val selected = attempts.filter { it.direction == direction }
        val recognized = selected.count { it.recognizedSpeech }
        val translated = selected.count { it.recognizedSpeech && it.translatedLocally }
        return OfflineObservationTotals(
            attempts = selected.size,
            recognized = recognized,
            translated = translated,
            failedSpeech = selected.size - recognized,
            failedTranslationAfterSpeech = recognized - translated,
            timedTranslations = selected.count {
                it.recognizedSpeech && it.translatedLocally &&
                    it.endToEndLatencyMs != null
            }
        )
    }

    fun canRecord(
        attempts: List<OfflineObservedAttempt>,
        proposed: OfflineObservedAttempt
    ): Boolean {
        if (totals(attempts, proposed.direction).attempts >= REQUIRED_ATTEMPTS_PER_DIRECTION) return false
        if (proposed.translatedLocally && !proposed.recognizedSpeech) return false
        if (proposed.endToEndLatencyMs != null &&
            (!proposed.translatedLocally || proposed.endToEndLatencyMs !in 1L..600000L)) return false
        return true
    }

    fun complete(attempts: List<OfflineObservedAttempt>): Boolean =
        OfflineObservationDirection.entries.all {
            totals(attempts, it).attempts == REQUIRED_ATTEMPTS_PER_DIRECTION
        }

    fun timedSuccessesComplete(attempts: List<OfflineObservedAttempt>): Boolean =
        attempts.filter { it.recognizedSpeech && it.translatedLocally }
            .all { it.endToEndLatencyMs != null }

    fun worstEndToEndLatencyMs(attempts: List<OfflineObservedAttempt>): Long? =
        attempts.mapNotNull { it.endToEndLatencyMs }.maxOrNull()
}
