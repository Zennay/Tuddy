package com.livelingo.android

import android.os.Handler
import android.os.Looper
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Explicit, all-or-nothing translation for an already opened subtitle draft.
 *
 * It reads the actual local ML Kit model inventory before and immediately
 * before translating. It never downloads models, records text, or uses speech.
 * A generation token prevents callbacks from a cancelled/backgrounded editor
 * from publishing partial results.
 */
internal class PrivateSubtitleTranslationController {
    private val inventory = OfflineTextModelInventory()
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var activeTranslator: Translator? = null
    private var pendingDeadline: Runnable? = null

    private fun cancelDeadline() {
        pendingDeadline?.let(main::removeCallbacks)
        pendingDeadline = null
    }

    /**
     * Each model lookup and each cue gets a fresh 30-second UI deadline.
     * A timed-out ML Kit Task may still run: its physical budget lease must
     * remain occupied until the actual terminal callback arrives.
     */
    private fun armDeadline(
        token: Long,
        message: String,
        onComplete: (texts: List<String>?, error: String?) -> Unit,
        onStalled: () -> Unit = {}
    ): Boolean {
        cancelDeadline()
        val task = Runnable {
            if (token == generation) {
                pendingDeadline = null
                onStalled()
                complete(token, null, message, onComplete)
            }
        }
        pendingDeadline = task
        if (main.postDelayed(task, DEADLINE_MS)) return true
        pendingDeadline = null
        if (token == generation) {
            complete(token, null,
                "Android kon de lokale vertaaldeadline niet veilig starten. " +
                    "Het concept is ongewijzigd.", onComplete)
        }
        return false
    }

    fun cancel() {
        generation++
        cancelDeadline()
        inventory.invalidate()
        closeActiveTranslator()
    }

    fun check(
        source: String,
        target: String,
        onReceipt: (OfflineTextReceipt) -> Unit
    ) {
        cancel()
        val token = generation
        // The *user's first* "Controleer lokale modellen" action must not
        // remain on CHECKING forever either. This is independent of the
        // second mandatory preflight immediately before batch translation.
        if (!armDeadline(
                token,
                "Android antwoordde niet binnen 30 seconden op de controle " +
                    "van reeds geïnstalleerde modellen. Tik bewust opnieuw.",
                { _, error ->
                    onReceipt(OfflineTextReceipt(
                        source, target, OfflineTextStage.UNKNOWN,
                        error ?: "Lokale modelcontrole is afgebroken."
                    ))
                }
            )) return
        inventory.check(source, target, TargetCatalog.codes) { receipt ->
            if (token != generation) return@check
            if (receipt.stage != OfflineTextStage.CHECKING) cancelDeadline()
            onReceipt(receipt)
        }
    }

    fun translate(
        cues: List<PrivateSubtitleCue>,
        source: String,
        target: String,
        onProgress: (completed: Int, total: Int) -> Unit,
        onComplete: (texts: List<String>?, error: String?) -> Unit,
        sourceRowNumbers: List<Int> = cues.indices.map { it + 1 },
        onSourceRowProblem: (sourceRow: Int) -> Unit = {}
    ) {
        cancel()
        val token = generation
        if (cues.isEmpty() || cues.size > MAX_CUES) {
            complete(token, null, "Dit ondertitelconcept bevat geen geldige vertaalbatch.", onComplete)
            return
        }
        if (sourceRowNumbers.size != cues.size ||
            sourceRowNumbers.any { it !in 1..MAX_CUES } ||
            sourceRowNumbers.zipWithNext().any { (left, right) -> left >= right }
        ) {
            complete(
                token, null,
                "De oorspronkelijke regelnummers komen niet overeen met de vertaalbatch.",
                onComplete
            )
            return
        }
        if (source !in TargetCatalog.codes || target !in TargetCatalog.codes) {
            complete(token, null, "Kies een ondersteunde bron- en doeltaal.", onComplete)
            return
        }
        if (source == target) {
            complete(token, null, "Kies twee verschillende talen om te vertalen.", onComplete)
            return
        }

        if (!armDeadline(
                token,
                "Android bevestigde de lokaal geïnstalleerde tekstmodellen niet " +
                    "binnen 30 seconden. Probeer de modelcontrole bewust opnieuw. " +
                    "Het concept is ongewijzigd.",
                onComplete
            )) return

        inventory.check(source, target, TargetCatalog.codes) { receipt ->
            if (token != generation) return@check
            if (receipt.stage == OfflineTextStage.CHECKING) return@check
            if (receipt.stage != OfflineTextStage.READY) {
                complete(token, null, receipt.detail, onComplete)
                return@check
            }
            cancelDeadline()

            val client = runCatching {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(source)
                        .setTargetLanguage(target)
                        .build()
                )
            }.getOrNull()
            if (client == null) {
                complete(
                    token, null,
                    "De lokale tekstvertaler kon niet worden geopend. Het concept is ongewijzigd.",
                    onComplete
                )
                return@check
            }
            activeTranslator = client
            val translated = arrayOfNulls<String>(cues.size)

            fun translateAt(index: Int) {
                if (token != generation) return
                if (index >= cues.size) {
                    val result = translated.map { it.orEmpty() }
                    if (result.any { it.isBlank() }) {
                        complete(
                            token, null,
                            "Niet iedere regel is vertaald. Het concept is ongewijzigd.",
                            onComplete
                        )
                    } else {
                        complete(token, result, null, onComplete)
                    }
                    return
                }

                val input = cues[index].text.replace('\n', ' ').trim()
                val sourceRow = sourceRowNumbers[index]
                if (input.isBlank()) {
                    onSourceRowProblem(sourceRow)
                    complete(
                        token, null,
                        "Regel $sourceRow is leeg. Het concept is ongewijzigd.",
                        onComplete
                    )
                    return
                }

                // One physical ML Kit task at a time for this subtitle batch,
                // shared with all other actual running ML Kit tasks in Tuddy.
                // Cancel/retry does not free this lease while Android's prior
                // task is still in flight; only completion does.
                val lease = LocalMlKitWorkBudget.shared.acquire()
                if (lease == null) {
                    onSourceRowProblem(sourceRow)
                    complete(
                        token, null,
                        "De lokale vertaler verwerkt nog andere Android-verzoeken. " +
                            "Regel $sourceRow is niet gestart. Probeer later opnieuw. " +
                            "Het concept is ongewijzigd.",
                        onComplete
                    )
                    return
                }
                if (!armDeadline(
                        token,
                        "Lokale vertaling van regel $sourceRow reageerde niet binnen " +
                            "30 seconden. Controleer deze regel en probeer bewust opnieuw. " +
                            "Er zijn geen gedeeltelijke wijzigingen toegepast.",
                        onComplete,
                        onStalled = { onSourceRowProblem(sourceRow) }
                    )) {
                    lease.release() // no task submitted
                    return
                }
                val task = try {
                    client.translate(input)
                } catch (_: Exception) {
                    lease.release() // only release if ML Kit never accepted the task
                    onSourceRowProblem(sourceRow)
                    complete(
                        token, null,
                        "Lokale vertaling van regel $sourceRow kon niet starten. " +
                            "Het concept is ongewijzigd.", onComplete
                    )
                    return
                }
                task.addOnCompleteListener { outcome ->
                    // This executes even if the user cancelled, timed out,
                    // deleted the dialog or Android closed the translator.
                    // Physical budget tracks actual device work, not a UI row.
                    lease.release()
                    if (token != generation) return@addOnCompleteListener
                    val clean = if (outcome.isSuccessful) {
                        outcome.result?.let(::normalize)
                    } else null
                    if (clean == null) {
                        onSourceRowProblem(sourceRow)
                        complete(
                            token, null,
                            if (outcome.isSuccessful)
                                "Vertaalde regel $sourceRow overschrijdt de veilige " +
                                    "tekstgrenzen. Het concept is ongewijzigd."
                            else
                                "Lokale vertaling van regel $sourceRow mislukte. " +
                                    "Er zijn geen gedeeltelijke wijzigingen toegepast.",
                            onComplete
                        )
                    } else {
                        translated[index] = clean
                        cancelDeadline()
                        onProgress(index + 1, cues.size)
                        translateAt(index + 1)
                    }
                }
            }

            translateAt(0)
        }
    }

    private fun normalize(value: String): String? {
        val text = value.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (text.isBlank() || text.length > MAX_CUE_CHARS || text.lines().size > MAX_CUE_LINES ||
            text.lines().any { it.length > MAX_LINE_CHARS } ||
            text.any { ch ->
                ch == '\u0000' || (ch.isISOControl() && ch != '\n' && ch != '\t')
            }
        ) return null
        return text
    }

    private fun complete(
        token: Long,
        texts: List<String>?,
        error: String?,
        onComplete: (List<String>?, String?) -> Unit
    ) {
        if (token != generation) return
        cancelDeadline()
        closeActiveTranslator()
        generation++
        onComplete(texts, error)
    }

    private fun closeActiveTranslator() {
        val current = activeTranslator
        activeTranslator = null
        runCatching { current?.close() }
    }

    private companion object {
        const val DEADLINE_MS = 30_000L
        const val MAX_CUES = 500
        const val MAX_CUE_CHARS = 2400
        const val MAX_CUE_LINES = 8
        const val MAX_LINE_CHARS = 800
    }
}
