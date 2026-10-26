package com.livelingo.android

import android.os.Handler
import android.os.Looper
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * User-selected, text-only model readiness and preparation.
 *
 * Local inventory callbacks are read-only and capped at 12 seconds. A separate
 * explicit Wi-Fi download may legitimately take longer: after 120 seconds the
 * UI reports an UNKNOWN state and offers a fresh read-only check, but the
 * underlying accepted ML Kit Task and its process-wide download admission
 * stay alive until Android actually reports a terminal result. No automatic
 * retry, cancel assumption, network speech, or inferred model availability.
 */
internal class TypedConversationModelController {
    private val inventory = OfflineTextModelInventory()
    private val epoch = OfflinePreparationEpoch()
    private val main = Handler(Looper.getMainLooper())
    private var pendingUiDeadline: Runnable? = null

    private fun clearUiDeadline() {
        pendingUiDeadline?.let(main::removeCallbacks)
        pendingUiDeadline = null
    }

    fun invalidate() {
        clearUiDeadline()
        inventory.invalidate()
        epoch.invalidate()
        // Do NOT close or release an already accepted ML Kit download here.
        // Only its actual terminal Task callback owns that physical lease.
    }

    private fun armUiDeadline(
        request: Long,
        milliseconds: Long,
        expired: () -> Unit
    ): Boolean {
        clearUiDeadline()
        val timeout = Runnable {
            if (!epoch.isCurrent(request)) return@Runnable
            pendingUiDeadline = null
            inventory.invalidate()
            epoch.invalidate()
            expired()
        }
        pendingUiDeadline = timeout
        if (main.postDelayed(timeout, milliseconds)) return true
        pendingUiDeadline = null
        inventory.invalidate()
        epoch.invalidate()
        expired()
        return false
    }

    fun check(from: String, to: String, result: (OfflineTextReceipt) -> Unit) {
        invalidate()
        val request = epoch.begin()
        if (!armUiDeadline(request, INVENTORY_DEADLINE_MS) {
                result(OfflineTextReceipt(from, to, OfflineTextStage.UNKNOWN,
                    "Android bevestigde de lokale tekstmodellen niet binnen 12 seconden. " +
                        "De status blijft onbekend; controleer later bewust opnieuw."))
            }) return
        try {
            inventory.check(from, to, TargetCatalog.codes) { receipt ->
                if (!epoch.isCurrent(request)) return@check
                if (receipt.stage != OfflineTextStage.CHECKING) clearUiDeadline()
                result(receipt)
            }
        } catch (_: Exception) {
            if (epoch.isCurrent(request)) {
                invalidate()
                result(OfflineTextReceipt(from, to, OfflineTextStage.UNKNOWN,
                    "De lokale modelinventaris kon niet veilig worden gelezen."))
            }
        }
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
                check(from, to) { receipt ->
                    if (receipt.stage != OfflineTextStage.CHECKING) {
                        result("Geen extra vertaalmodel nodig.", receipt)
                    }
                }
                return
            }
            OfflinePairDecision.MODEL_REQUIRED -> Unit
        }
        invalidate()
        val request = epoch.begin()
        val lease = LocalTextModelDownloadAdmission.shared.acquire()
        if (lease == null) {
            result("ML Kit is nog bezig met een eerder bewust aangevraagde " +
                "tekstmodeldownload. Start geen tweede download; " +
                "controleer later de lokale modellen opnieuw.", null)
            return
        }
        val translator = try {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(from).setTargetLanguage(to).build()
            )
        } catch (_: Exception) {
            lease.release()
            result("Lokale ML Kit-tekstvertaler kon niet worden geopend.", null)
            return
        }
        if (!armUiDeadline(request, DOWNLOAD_UI_DEADLINE_MS) {
                result("Android heeft de modeldownload na twee minuten nog niet " +
                    "bevestigd. Een eenmaal gestart ML Kit-verzoek kan " +
                    "doorgaan; er is geen tweede download gestart. " +
                    "Controleer later de modellen opnieuw.", null)
            }) {
            translator.close()
            lease.release() // No download submitted.
            return
        }

        var submitted = false
        try {
            val task = translator.downloadModelIfNeeded(
                DownloadConditions.Builder().requireWifi().build()
            )
            submitted = true
            task.addOnCompleteListener { outcome ->
                // Always release on the *physical* completion, even after
                // screen disposal or the bounded UI deadline.
                runCatching { translator.close() }
                lease.release()
                if (!epoch.isCurrent(request)) return@addOnCompleteListener
                clearUiDeadline()
                if (!outcome.isSuccessful) {
                    result("Download niet bevestigd. Controleer wifi en opslag; " +
                        "het model wordt niet als lokaal beschikbaar gemeld.", null)
                } else {
                    check(from, to) { verified ->
                        if (verified.stage != OfflineTextStage.CHECKING) {
                            result(
                                if (verified.stage == OfflineTextStage.READY)
                                    "Dit teksttaalpaar is opnieuw lokaal geverifieerd."
                                else "ML Kit meldde dat de download is afgerond, " +
                                    "maar lokale beschikbaarheid is niet bevestigd.",
                                verified
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            if (!submitted) {
                runCatching { translator.close() }
                lease.release()
            }
            // If submission succeeded but registering its listener threw,
            // physical completion is unknown. Do not incorrectly release the
            // process-wide admission or assume the download has stopped.
            if (epoch.isCurrent(request)) {
                invalidate()
                result(
                    if (submitted)
                        "Download mogelijk gestart, maar Android kon geen veilige " +
                            "afronding terugmelden. Geen tweede download starten " +
                            "in dit app-proces; controleer later de modellen."
                    else "Tekstmodellen konden niet via wifi worden voorbereid.",
                    null
                )
            }
        }
    }

    companion object {
        const val INVENTORY_DEADLINE_MS = 12_000L
        const val DOWNLOAD_UI_DEADLINE_MS = 120_000L
    }
}
