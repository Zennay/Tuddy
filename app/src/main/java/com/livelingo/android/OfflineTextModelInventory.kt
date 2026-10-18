package com.livelingo.android

import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel

/**
 * Read-only inventory of actually downloaded ML Kit TEXT language models.
 * Completely independent from Android on-device SpeechRecognizer support.
 *
 * ML Kit's English model is built-in. Other source AND target language
 * models must be present locally for an offline translation pair.
 * This scanner NEVER requests a download and stores no user data.
 */
internal enum class OfflineTextStage {
    UNCHECKED, CHECKING, READY, MISSING, UNSUPPORTED, UNKNOWN
}

internal data class OfflineTextReceipt(
    val source: String,
    val target: String,
    val stage: OfflineTextStage,
    val detail: String
)

internal class OfflineTextModelInventory {
    private var revision = 0L

    fun invalidate() {
        revision++
    }

    fun check(
        source: String,
        target: String,
        allowed: Collection<String>,
        callback: (OfflineTextReceipt) -> Unit
    ) {
        invalidate()
        val checkRevision = revision
        if (source !in allowed || target !in allowed) {
            callback(OfflineTextReceipt(source, target, OfflineTextStage.UNSUPPORTED,
                "Dit teksttaalpaar wordt niet ondersteund door de lokale ML Kit-vertaler."))
            return
        }
        if (source == target) {
            callback(OfflineTextReceipt(source, target, OfflineTextStage.READY,
                "Gelijke bron- en doeltaal: geen tekstvertaalmodel nodig."))
            return
        }
        callback(OfflineTextReceipt(source, target, OfflineTextStage.CHECKING,
            "Gedownloade lokale tekstmodellen controleren…"))
        try {
            RemoteModelManager.getInstance()
                .getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { downloaded ->
                    if (revision != checkRevision) return@addOnSuccessListener
                    val installedCodes = downloaded.map { it.language }.toSet()
                    // English is built into ML Kit and has no downloadable
                    // TranslateRemoteModel. Never ask for an EN model.
                    val required = setOf(source, target) - TranslateLanguage.ENGLISH
                    val missing = required - installedCodes
                    val result = if (missing.isEmpty()) {
                        OfflineTextReceipt(source, target, OfflineTextStage.READY,
                            "Beide talen zijn lokaal beschikbaar voor tekstvertaling. Spraakstatus apart controleren.")
                    } else {
                        OfflineTextReceipt(source, target, OfflineTextStage.MISSING,
                            "Tekstmodel ontbreekt voor: ${missing.sorted().joinToString(", ")}. " +
                                "Tik op Tekstmodel offline voorbereiden wanneer wifi beschikbaar is.")
                    }
                    callback(result)
                }
                .addOnFailureListener {
                    if (revision != checkRevision) return@addOnFailureListener
                    callback(OfflineTextReceipt(source, target, OfflineTextStage.UNKNOWN,
                        "ML Kit kon niet controleren of de tekstmodellen lokaal aanwezig zijn."))
                }
        } catch (_: Exception) {
            if (revision != checkRevision) return
            callback(OfflineTextReceipt(source, target, OfflineTextStage.UNKNOWN,
                "Lokale modelinventaris kon niet worden geopend."))
        }
    }
}
