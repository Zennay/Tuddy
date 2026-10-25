package com.livelingo.android

import android.os.Handler
import android.os.Looper

/**
 * Session-scoped, read-only ML Kit model lookup broker.
 *
 * Multiple live captions in one direction share a single model inventory
 * request, with bounded waiting. Android may never deliver a model-inventory
 * callback. Such a failure must NOT permanently pin that language pair:
 * all waiting captions receive one UNKNOWN receipt, the old request is fenced,
 * and a later user-triggered retry can ask Android for a fresh inventory.
 *
 * No text or model readiness is persisted or cached, and no models are
 * downloaded here. Android's native query may still finish after a timeout;
 * the invalidated inventory discards that stale completion.
 */
internal class OfflinePairReadinessBroker {
    private data class Pending(
        val inventory: OfflineTextModelInventory,
        val listeners: MutableList<(OfflineTextReceipt) -> Unit>,
        var deadline: Runnable? = null
    )

    private val main = Handler(Looper.getMainLooper())
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
            // A callback captures its existing caption revision. Limit the
            // number held by one unresponsive Android provider call.
            if (inflight.listeners.size >= MAX_WAITING_CAPTIONS_PER_PAIR) {
                callback(OfflineTextReceipt(from, to, OfflineTextStage.UNKNOWN,
                    "Te veel lokale vertaalregels wachten op dezelfde modelcontrole. " +
                        "Probeer deze zin na afronding bewust opnieuw."))
                return
            }
            inflight.listeners.add(callback)
            callback(OfflineTextReceipt(from, to, OfflineTextStage.CHECKING,
                "De lokale modellen voor deze taalrichting worden al gecontroleerd…"))
            return
        }

        val request = Pending(OfflineTextModelInventory(), mutableListOf(callback))
        pending[key] = request
        val timeout = Runnable {
            if (pending[key] !== request || closed) return@Runnable
            finish(key, request, OfflineTextReceipt(from, to, OfflineTextStage.UNKNOWN,
                "Android gaf binnen 12 seconden geen antwoord over de geïnstalleerde " +
                    "lokale tekstmodellen. De modelcontrole is vrijgegeven; " +
                    "probeer later bewust opnieuw. Er is niets gedownload."))
        }
        request.deadline = timeout
        if (!main.postDelayed(timeout, MODEL_LOOKUP_TIMEOUT_MS)) {
            finish(key, request, OfflineTextReceipt(from, to, OfflineTextStage.UNKNOWN,
                "Android kon geen veilige deadline voor de modelcontrole starten. " +
                    "Probeer bewust opnieuw."))
            return
        }
        try {
            request.inventory.check(from, to, TargetCatalog.codes) { receipt ->
                if (closed || pending[key] !== request) return@check
                if (receipt.stage == OfflineTextStage.CHECKING) {
                    // A callback must not prevent the other waiting captions
                    // from receiving their own independent status.
                    request.listeners.toList().forEach { listener ->
                        runCatching { listener(receipt) }
                    }
                } else {
                    finish(key, request, receipt)
                }
            }
        } catch (_: Exception) {
            finish(key, request, OfflineTextReceipt(from, to, OfflineTextStage.UNKNOWN,
                "De lokale modelcontrole kon niet veilig worden voltooid. " +
                    "Probeer later bewust opnieuw."))
        }
    }

    private fun finish(
        pair: Pair<String, String>, request: Pending, receipt: OfflineTextReceipt
    ) {
        if (pending[pair] !== request) return
        pending.remove(pair)
        request.deadline?.let(main::removeCallbacks)
        request.deadline = null
        request.inventory.invalidate()
        val listeners = request.listeners.toList()
        request.listeners.clear()
        listeners.forEach { listener -> runCatching { listener(receipt) } }
    }

    /**
     * A storage change, conversation clear, or screen teardown invalidates
     * pending receipts. Never report READY based on an old model snapshot.
     */
    fun invalidate() {
        val old = pending.toMap()
        pending.clear()
        old.forEach { (pair, request) ->
            request.deadline?.let(main::removeCallbacks)
            request.deadline = null
            request.inventory.invalidate()
            val listeners = request.listeners.toList()
            request.listeners.clear()
            listeners.forEach { callback ->
                runCatching {
                    callback(OfflineTextReceipt(
                        pair.first, pair.second, OfflineTextStage.UNKNOWN,
                        "Tekstmodelstatus kan gewijzigd zijn. Controleer opnieuw."
                    ))
                }
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        val old = pending.values.toList()
        pending.clear()
        old.forEach { request ->
            request.deadline?.let(main::removeCallbacks)
            request.deadline = null
            request.inventory.invalidate()
            request.listeners.clear()
        }
    }

    companion object {
        const val MODEL_LOOKUP_TIMEOUT_MS = 12_000L
        const val MAX_WAITING_CAPTIONS_PER_PAIR = 64
    }
}
