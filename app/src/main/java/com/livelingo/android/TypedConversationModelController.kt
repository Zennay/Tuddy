package com.livelingo.android

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * A dedicated text-only model gate for a user-selected conversation direction.
 * Unlike the speech settings this accepts all supported ML Kit TEXT pairs.
 * No network operation except an explicit user-requested Wi-Fi model download.
 */
internal class TypedConversationModelController {
    private val inventory = OfflineTextModelInventory()
    private val epoch = OfflinePreparationEpoch()

    fun invalidate() {
        inventory.invalidate()
        epoch.invalidate()
    }

    fun check(from: String, to: String, result: (OfflineTextReceipt) -> Unit) {
        inventory.check(from, to, TargetCatalog.codes, result)
    }

    fun prepareViaWifi(
        from: String, to: String,
        result: (String, OfflineTextReceipt?) -> Unit
    ) {
        when (OfflineModelPreparationPolicy.decide(from, to, TargetCatalog.codes)) {
            OfflinePairDecision.UNSUPPORTED -> {
                result("Ongeldig of niet-ondersteund teksttaalpaar.", null)
                return
            }
            OfflinePairDecision.ALREADY_SAME_LANGUAGE -> {
                check(from, to) { result("Geen extra vertaalmodel nodig.", it) }
                return
            }
            OfflinePairDecision.MODEL_REQUIRED -> Unit
        }
        val requested = epoch.begin()
        val translator = try {
            Translation.getClient(TranslatorOptions.Builder()
                .setSourceLanguage(from).setTargetLanguage(to).build())
        } catch (_: Exception) {
            result("Lokale ML Kit-tekstvertaler kon niet worden geopend.", null)
            return
        }
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
                .addOnSuccessListener {
                    translator.close()
                    if (epoch.isCurrent(requested)) {
                        check(from, to) { verified ->
                            if (epoch.isCurrent(requested)) {
                                result(
                                    if (verified.stage == OfflineTextStage.READY)
                                        "Dit teksttaalpaar is lokaal beschikbaar."
                                    else "Download klaar; lokale beschikbaarheid nog niet bevestigd.",
                                    verified
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    translator.close()
                    if (epoch.isCurrent(requested))
                        result("Download niet beschikbaar. Controleer wifi en opslag.", null)
                }
        } catch (_: Exception) {
            translator.close()
            if (epoch.isCurrent(requested))
                result("Tekstmodellen konden niet worden voorbereid.", null)
        }
    }
}
