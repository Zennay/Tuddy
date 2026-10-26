package com.livelingo.android

import android.os.Handler
import android.os.Looper
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Explicit, read-only back-translation of ONE completed, ephemeral caption.
 *
 * A locally generated back-translation is a comparison aid, NEVER independent
 * validation. This controller does not change a conversation row, persist
 * speech/text, call downloadModelIfNeeded(), or use a network translator.
 *
 * Unlike a presentation timeout, a submitted ML Kit Task holds its actual
 * device work-budget lease until its own terminal callback: dismissing the
 * modal cannot invent a free physical translation slot.
 */
internal enum class LocalBackReviewStage { CHECKING, TRANSLATING, COMPLETED, UNAVAILABLE }

internal data class LocalBackReviewReceipt(
    val stage: LocalBackReviewStage,
    val message: String,
    val reverseText: String = ""
)

internal class PrivateOfflineBackTranslationReview {
    private val main = Handler(Looper.getMainLooper())
    private val inventory = OfflineTextModelInventory()
    private var revision = 0L
    private var deadline: Runnable? = null

    fun close() {
        revision++
        inventory.invalidate()
        deadline?.let(main::removeCallbacks)
        deadline = null
        // Submitted Tasks own their local translator and physical budget lease
        // until actual completion, even if their private UI has been dismissed.
    }

    fun request(
        original: String,
        translation: String,
        sourceCode: String,
        targetCode: String,
        callback: (LocalBackReviewReceipt) -> Unit
    ) {
        close()
        val version = revision
        val safeOriginal = LocalTranslationReviewPolicy.normalize(original)
        val safeTranslation = LocalTranslationReviewPolicy.normalize(translation)
        if (sourceCode == targetCode ||
            sourceCode !in TargetCatalog.codes || targetCode !in TargetCatalog.codes ||
            safeOriginal == null || safeTranslation == null ||
            safeOriginal.length > MAX_UNITS || safeTranslation.length > MAX_UNITS) {
            callback(LocalBackReviewReceipt(
                LocalBackReviewStage.UNAVAILABLE,
                "Alleen korte, afgeronde en veilig leesbare vertaalde zinnen kunnen lokaal worden vergeleken."
            ))
            return
        }
        val reverseFrom = targetCode
        val reverseTo = sourceCode
        callback(LocalBackReviewReceipt(
            LocalBackReviewStage.CHECKING,
            "Geïnstalleerde offline modellen voor $reverseFrom → $reverseTo controleren…"
        ))
        inventory.check(reverseFrom, reverseTo, TargetCatalog.codes) { receipt ->
            if (revision != version) return@check
            when (receipt.stage) {
                OfflineTextStage.CHECKING -> Unit
                OfflineTextStage.READY -> {
                    val budget = LocalMlKitWorkBudget.shared.acquire()
                    if (budget == null) {
                        callback(LocalBackReviewReceipt(
                            LocalBackReviewStage.UNAVAILABLE,
                            "Alle lokale vertaalplaatsen zijn bezet. Probeer later opnieuw; niets verzonden."
                        ))
                        return@check
                    }
                    val translator = try {
                        Translation.getClient(TranslatorOptions.Builder()
                            .setSourceLanguage(reverseFrom)
                            .setTargetLanguage(reverseTo)
                            .build())
                    } catch (_: Exception) {
                        budget.release()
                        callback(LocalBackReviewReceipt(
                            LocalBackReviewStage.UNAVAILABLE,
                            "Lokale terugvertaler kon niet worden geopend."
                        ))
                        return@check
                    }
                    callback(LocalBackReviewReceipt(
                        LocalBackReviewStage.TRANSLATING,
                        "Terugvertaling loopt op dit toestel. Je oorspronkelijke zin blijft ongewijzigd."
                    ))
                    val timer = Runnable {
                        if (revision != version) return@Runnable
                        deadline = null
                        // Never release the physical ML Kit budget here: the
                        // underlying Android Task may still be running.
                        revision++
                        callback(LocalBackReviewReceipt(
                            LocalBackReviewStage.UNAVAILABLE,
                            "Terugvertalen duurde te lang. Het oude modelwerk mag niets meer tonen; probeer later opnieuw."
                        ))
                    }
                    deadline = timer
                    main.postDelayed(timer, TIMEOUT_MS)
                    try {
                        translator.translate(safeTranslation).addOnCompleteListener { task ->
                            runCatching { translator.close() }
                            budget.release()
                            if (revision != version) return@addOnCompleteListener
                            deadline?.let(main::removeCallbacks)
                            deadline = null
                            val result = if (task.isSuccessful) {
                                runCatching { task.result }.getOrNull()
                                    ?.let(LocalTranslationReviewPolicy::normalize)
                                    ?.takeIf { it.length <= MAX_UNITS }
                            } else null
                            callback(if (result != null) {
                                LocalBackReviewReceipt(
                                    LocalBackReviewStage.COMPLETED,
                                    "Lokale terugvertaling klaar. Een vergelijkbare formulering bewijst geen correcte vertaling.",
                                    result
                                )
                            } else {
                                LocalBackReviewReceipt(
                                    LocalBackReviewStage.UNAVAILABLE,
                                    "Lokale terugvertaling mislukt of bevat onveilige tekst. Geen online fallback."
                                )
                            })
                        }
                    } catch (_: Exception) {
                        deadline?.let(main::removeCallbacks)
                        deadline = null
                        runCatching { translator.close() }
                        budget.release()
                        if (revision == version) callback(LocalBackReviewReceipt(
                            LocalBackReviewStage.UNAVAILABLE,
                            "De lokale vertaaltaak kon niet worden gestart."
                        ))
                    }
                }
                else -> callback(LocalBackReviewReceipt(
                    LocalBackReviewStage.UNAVAILABLE,
                    "Omgekeerde offline tekstmodellen zijn niet bevestigd. " +
                        "Bereid ze desgewenst apart en bewust via wifi voor in Offline startcheck."
                ))
            }
        }
    }

    companion object {
        const val MAX_UNITS = 1000
        private const val TIMEOUT_MS = 30_000L
    }
}
