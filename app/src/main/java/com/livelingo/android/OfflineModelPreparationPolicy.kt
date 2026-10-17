package com.livelingo.android

/**
 * ML Kit text-model readiness is a different capability from Android speech.
 * This policy may authorize a *user initiated* Wi-Fi model download only for
 * one explicitly chosen, supported language pair; it never downloads models
 * while captions arrive, and never adds Kurdish/Pashto as ML Kit languages.
 */
internal enum class OfflinePairDecision {
    ALREADY_SAME_LANGUAGE, MODEL_REQUIRED, UNSUPPORTED
}

internal object OfflineModelPreparationPolicy {
    fun decide(
        sourceCode: String,
        targetCode: String,
        supportedTextCodes: Collection<String>
    ): OfflinePairDecision {
        if (sourceCode !in supportedTextCodes || targetCode !in supportedTextCodes) {
            return OfflinePairDecision.UNSUPPORTED
        }
        return if (sourceCode == targetCode) OfflinePairDecision.ALREADY_SAME_LANGUAGE
        else OfflinePairDecision.MODEL_REQUIRED
    }
}

/**
 * A downloader can finish after Stop, app disposal or language selection.
 * Never report that a *different* pair is available or launch additional
 * work based on the stale completion. This is RAM-only state.
 */
internal class OfflinePreparationEpoch {
    private var version = 0L

    fun begin(): Long {
        invalidate()
        return version
    }

    fun invalidate() {
        check(version != Long.MAX_VALUE) { "Offline preparation generation exhausted" }
        version += 1L
    }

    fun isCurrent(receipt: Long): Boolean = receipt > 0L && receipt == version
}
