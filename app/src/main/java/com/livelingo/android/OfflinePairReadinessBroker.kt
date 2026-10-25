package com.livelingo.android

/**
 * Session-scoped, read-only ML Kit model lookup broker.
 *
 * A burst of final live captions in the same direction needs one model
 * inventory request, NOT one per private sentence. The broker only remembers
 * language codes and a transient callback list until Android answers.
 * No transcript, cached readiness across model changes, network call, or
 * model download. Different language pairs remain independent.
 */
internal class OfflinePairReadinessBroker {
    private data class Pending(
        val inventory: OfflineTextModelInventory,
        val listeners: MutableList<(OfflineTextReceipt) -> Unit>
    )

    private val pending = mutableMapOf<Pair<String, String>, Pending>()
    private var closed = false

    fun check(from: String, to: String, callback: (OfflineTextReceipt) -> Unit) {
        if (closed) {
            callback(OfflineTextReceipt(from, to, OfflineTextStage.UNKNOWN,
                "Lokale modelcontrole is gestopt. Probeer bewust opnieuw."))
            return
        }
        val key = from to to
        val inflight = pending[key]
        if (inflight != null) {
            inflight.listeners.add(callback)
            callback(OfflineTextReceipt(from, to, OfflineTextStage.CHECKING,
                "De lokale modellen voor deze taalrichting worden al gecontroleerd…"))
            return
        }
        val request = Pending(OfflineTextModelInventory(), mutableListOf(callback))
        pending[key] = request
        request.inventory.check(from, to, TargetCatalog.codes) { receipt ->
            if (closed || pending[key] !== request) return@check
            if (receipt.stage == OfflineTextStage.CHECKING) {
                request.listeners.toList().forEach { it(receipt) }
            } else {
                pending.remove(key)
                request.listeners.toList().forEach { it(receipt) }
                request.listeners.clear()
            }
        }
    }

    /**
     * Model storage changed or this screen was wiped. The previous result is
     * now unknown, never READY by assumption; surface a retryable local error
     * rather than waiting for a callback that must be discarded.
     */
    fun invalidate() {
        val old = pending.toMap()
        pending.clear()
        old.forEach { (pair, request) ->
            request.inventory.invalidate()
            request.listeners.toList().forEach { callback ->
                callback(OfflineTextReceipt(
                    pair.first, pair.second, OfflineTextStage.UNKNOWN,
                    "Tekstmodelstatus kan gewijzigd zijn. Controleer opnieuw."
                ))
            }
            request.listeners.clear()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        val old = pending.values.toList()
        pending.clear()
        old.forEach { it.inventory.invalidate(); it.listeners.clear() }
    }
}
