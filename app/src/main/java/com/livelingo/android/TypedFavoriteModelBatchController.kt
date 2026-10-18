package com.livelingo.android

/**
 * One explicitly requested, bounded sequence of local ML Kit TEXT model checks
 * or Wi-Fi preparations for saved text-only conversation pairs.
 *
 * A fresh inventory read precedes EACH download. Already installed pairs are
 * skipped, unknown states fail closed, and no new download is started after
 * cancellation, background disposal or another request supersedes this run.
 * A download already accepted by ML Kit may still finish in the background.
 *
 * Never touches SpeechRecognizer, user sentences, Android preferences or
 * any networking outside the user's explicit prepareViaWifi request.
 */
internal class TypedFavoriteModelBatchController {
    private val model = TypedConversationModelController()
    private var epoch = 0L

    fun cancel() {
        epoch++
        model.invalidate()
    }

    private fun admit(pairs: List<TypedLanguagePair>): Boolean =
        pairs.isNotEmpty() &&
            pairs.size <= PrivateTypedLanguagePairs.MAX_PAIRS &&
            pairs.distinct().size == pairs.size &&
            pairs.all {
                it.from != it.to &&
                    it.from in TargetCatalog.codes &&
                    it.to in TargetCatalog.codes
            }

    /** A read-only inventory, with each pair examined in order. */
    fun inspect(
        pairs: List<TypedLanguagePair>,
        onReceipt: (TypedLanguagePair, OfflineTextReceipt) -> Unit,
        onFinished: (String) -> Unit
    ) {
        cancel()
        val request = epoch
        if (!admit(pairs)) {
            onFinished("Geen geldige favoriete teksttaalparen om te controleren.")
            return
        }
        var ready = 0
        var missing = 0
        var other = 0
        fun next(index: Int) {
            if (epoch != request) return
            if (index == pairs.size) {
                onFinished("$ready van ${pairs.size} teksttaalparen lokaal beschikbaar; " +
                    "$missing ontbreken; $other niet bevestigd.")
                return
            }
            val pair = pairs[index]
            model.check(pair.from, pair.to) { receipt ->
                if (epoch != request) return@check
                onReceipt(pair, receipt)
                when (receipt.stage) {
                    OfflineTextStage.CHECKING -> Unit
                    else -> {
                        when (receipt.stage) {
                            OfflineTextStage.READY -> ready++
                            OfflineTextStage.MISSING -> missing++
                            else -> other++
                        }
                        next(index + 1)
                    }
                }
            }
        }
        next(0)
    }

    /**
     * Only called after separate on-screen user confirmation. Scan before
     * download, don't re-download READY pairs and stop on first unconfirmed
     * failure instead of silently assuming remaining models were installed.
     */
    fun prepareMissingViaWifi(
        pairs: List<TypedLanguagePair>,
        onReceipt: (TypedLanguagePair, OfflineTextReceipt) -> Unit,
        onFinished: (String) -> Unit
    ) {
        cancel()
        val request = epoch
        if (!admit(pairs)) {
            onFinished("Geen geldige favoriete teksttaalparen om voor te bereiden.")
            return
        }
        var ready = 0
        var prepared = 0
        fun next(index: Int) {
            if (epoch != request) return
            if (index == pairs.size) {
                onFinished("Klaar: $ready eerder lokaal beschikbare en $prepared " +
                    "zojuist voorbereide teksttaalparen. " +
                    "Dit bevestigt geen offline spraakherkenning.")
                return
            }
            val pair = pairs[index]
            model.check(pair.from, pair.to) { receipt ->
                if (epoch != request) return@check
                onReceipt(pair, receipt)
                when (receipt.stage) {
                    OfflineTextStage.CHECKING -> Unit
                    OfflineTextStage.READY -> {
                        ready++
                        next(index + 1)
                    }
                    OfflineTextStage.MISSING -> {
                        model.prepareViaWifi(pair.from, pair.to) { message, verified ->
                            if (epoch != request) return@prepareViaWifi
                            if (verified != null) onReceipt(pair, verified)
                            if (verified?.stage == OfflineTextStage.READY) {
                                prepared++
                                next(index + 1)
                            } else {
                                onFinished("Gestopt bij ${pair.from.uppercase()} → " +
                                    "${pair.to.uppercase()}: $message. Reeds aanwezige " +
                                    "modellen blijven bruikbaar; overige niet gedownload.")
                            }
                        }
                    }
                    else -> onFinished("Gestopt: de lokale modelstatus voor " +
                        "${pair.from.uppercase()} → ${pair.to.uppercase()} " +
                        "kan niet veilig worden bevestigd. Geen download gestart.")
                }
            }
        }
        next(0)
    }
}
