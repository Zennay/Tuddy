package com.livelingo.android

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
    private var generation = 0L
    private var activeTranslator: Translator? = null

    fun cancel() {
        generation++
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
        inventory.check(source, target, TargetCatalog.codes) { receipt ->
            if (token == generation) onReceipt(receipt)
        }
    }

    fun translate(
        cues: List<PrivateSubtitleCue>,
        source: String,
        target: String,
        onProgress: (completed: Int, total: Int) -> Unit,
        onComplete: (texts: List<String>?, error: String?) -> Unit
    ) {
        cancel()
        val token = generation
        if (cues.isEmpty() || cues.size > MAX_CUES) {
            complete(token, null, "Dit ondertitelconcept bevat geen geldige vertaalbatch.", onComplete)
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

        inventory.check(source, target, TargetCatalog.codes) { receipt ->
            if (token != generation) return@check
            if (receipt.stage == OfflineTextStage.CHECKING) return@check
            if (receipt.stage != OfflineTextStage.READY) {
                complete(token, null, receipt.detail, onComplete)
                return@check
            }

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
                if (input.isBlank()) {
                    complete(
                        token, null,
                        "Regel ${index + 1} is leeg. Het concept is ongewijzigd.",
                        onComplete
                    )
                    return
                }

                try {
                    client.translate(input)
                        .addOnSuccessListener { raw ->
                            if (token != generation) return@addOnSuccessListener
                            val clean = normalize(raw)
                            if (clean == null) {
                                complete(
                                    token, null,
                                    "Vertaalde regel ${index + 1} overschrijdt de veilige tekstgrenzen. " +
                                        "Het concept is ongewijzigd.",
                                    onComplete
                                )
                            } else {
                                translated[index] = clean
                                onProgress(index + 1, cues.size)
                                translateAt(index + 1)
                            }
                        }
                        .addOnFailureListener {
                            if (token == generation) {
                                complete(
                                    token, null,
                                    "Lokale vertaling van regel ${index + 1} mislukte. " +
                                        "Er zijn geen gedeeltelijke wijzigingen toegepast.",
                                    onComplete
                                )
                            }
                        }
                } catch (_: Exception) {
                    complete(
                        token, null,
                        "Lokale vertaling van regel ${index + 1} kon niet starten. " +
                            "Het concept is ongewijzigd.",
                        onComplete
                    )
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
        const val MAX_CUES = 500
        const val MAX_CUE_CHARS = 2400
        const val MAX_CUE_LINES = 8
        const val MAX_LINE_CHARS = 800
    }
}
