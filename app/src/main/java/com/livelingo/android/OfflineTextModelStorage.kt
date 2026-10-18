package com.livelingo.android

import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel

/**
 * Explicit, local-only model storage management. Never downloads, estimates
 * device storage usage, or confuses text models with Android speech packs.
 * All callbacks are fenced by a revision so background UI cannot be updated.
 */
internal class OfflineTextModelStorage {
    private var revision = 0L

    fun invalidate() {
        revision++
    }

    fun list(callback: (List<String>?, String) -> Unit) {
        invalidate()
        val request = revision
        try {
            RemoteModelManager.getInstance()
                .getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { downloaded ->
                    if (request != revision) return@addOnSuccessListener
                    callback(downloaded.map { it.language }
                        .filter { it in TargetCatalog.codes && it != TranslateLanguage.ENGLISH }
                        .distinct().sorted(), "")
                }
                .addOnFailureListener {
                    if (request == revision) callback(null,
                        "De lokale ML Kit-modellen konden niet worden opgehaald.")
                }
        } catch (_: Exception) {
            if (request == revision) callback(null,
                "De lokale modelbeheerder is niet beschikbaar.")
        }
    }

    /**
     * A model must be explicitly selected, installed, and unused by the
     * currently selected pair. Re-read the inventory before deleting rather
     * than trusting the last displayed list. No network calls are made here.
     */
    fun removeUnused(
        code: String,
        selectedPair: () -> Set<String>,
        callback: (Boolean, String) -> Unit
    ) {
        if (code !in TargetCatalog.codes || code == TranslateLanguage.ENGLISH ||
            code in selectedPair()) {
            callback(false, "Dit tekstmodel is beschermd of wordt momenteel gebruikt.")
            return
        }
        invalidate()
        val request = revision
        try {
            val manager = RemoteModelManager.getInstance()
            manager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { downloaded ->
                    if (request != revision) return@addOnSuccessListener
                    if (code in selectedPair()) {
                        callback(false, "De taalkeuze veranderde; model is niet verwijderd.")
                        return@addOnSuccessListener
                    }
                    if (downloaded.none { it.language == code }) {
                        callback(false, "Model is inmiddels niet meer geïnstalleerd.")
                        return@addOnSuccessListener
                    }
                    try {
                        manager.deleteDownloadedModel(TranslateRemoteModel.Builder(code).build())
                            .addOnSuccessListener {
                                if (request == revision) callback(true,
                                    "Tekstmodel $code is verwijderd. Download het opnieuw via wifi als je het nodig hebt.")
                            }
                            .addOnFailureListener {
                                if (request == revision) callback(false,
                                    "ML Kit kon het gekozen tekstmodel niet verwijderen.")
                            }
                    } catch (_: Exception) {
                        if (request == revision) callback(false,
                            "Verwijderen is op dit toestel niet beschikbaar.")
                    }
                }
                .addOnFailureListener {
                    if (request == revision) callback(false,
                        "Model is niet verwijderd: controle van lokale downloads mislukte.")
                }
        } catch (_: Exception) {
            if (request == revision) callback(false,
                "Model is niet verwijderd: lokale modelbeheerder niet beschikbaar.")
        }
    }
}
