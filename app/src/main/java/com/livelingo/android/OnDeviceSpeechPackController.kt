package com.livelingo.android

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.speech.ModelDownloadListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Explicit, user-driven on-device SPEECH model readiness. ML Kit translation
 * models and the five-phrase Kurmanji/Sorani/Pashto preview are separate.
 *
 * Never creates a network speech recognizer. Download is permitted only after
 * an explicit tap while the currently active network is Wi-Fi. Android's own
 * recognition service manages any bytes: no vendor endpoint or cloud fallback.
 * Android 12 cannot enumerate installed recognition languages; say so.
 */
internal enum class SpeechPackStage {
    UNCHECKED, CHECKING, READY, DOWNLOADABLE, PENDING,
    ONLINE_ONLY, UNSUPPORTED, UNKNOWN, DOWNLOADING, REQUESTED, ERROR
}

internal data class SpeechPackReceipt(
    val tag: String,
    val stage: SpeechPackStage,
    val detail: String
) {
    val canDownload: Boolean get() = stage == SpeechPackStage.DOWNLOADABLE || stage == SpeechPackStage.ERROR
}

internal class OnDeviceSpeechPackController(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private var epoch: Long = 0L

    fun cancel() {
        epoch++
        recognizer?.destroy()
        recognizer = null
    }

    private fun fresh(tag: String): Pair<Long, SpeechRecognizer>? {
        cancel()
        if (Build.VERSION.SDK_INT < 33 ||
            !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) return null
        return try {
            val engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = engine
            epoch to engine
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun requestedLanguage(intentTag: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, intentTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

    private fun includesRequestedTag(tags: Collection<String>, selected: String): Boolean {
        // Only an explicitly reported, exact language/region pack is treated
        // as installed. A generic "en" cannot prove that "en-US" is installed.
        val canonical = selected.lowercase(Locale.ROOT)
        return tags.any { it.lowercase(Locale.ROOT) == canonical }
    }

    private fun report(tag: String, support: RecognitionSupport): SpeechPackReceipt {
        if (includesRequestedTag(support.installedOnDeviceLanguages, tag)) {
            return SpeechPackReceipt(tag, SpeechPackStage.READY,
                "Android meldt een geïnstalleerd offline spraakpakket voor $tag. Geen garantie op nauwkeurigheid.")
        }
        if (includesRequestedTag(support.pendingOnDeviceLanguages, tag)) {
            return SpeechPackReceipt(tag, SpeechPackStage.PENDING,
                "Android bereidt het spraakpakket voor $tag nog voor. Controleer straks opnieuw.")
        }
        if (includesRequestedTag(support.supportedOnDeviceLanguages, tag)) {
            return SpeechPackReceipt(tag, SpeechPackStage.DOWNLOADABLE,
                "Offline spraak voor $tag wordt door dit toestel aangeboden; het pakket ontbreekt nog.")
        }
        if (includesRequestedTag(support.onlineLanguages, tag)) {
            return SpeechPackReceipt(tag, SpeechPackStage.ONLINE_ONLY,
                "Voor $tag biedt de herkenningsdienst alleen online spraak. Tuddy gebruikt die niet.")
        }
        return SpeechPackReceipt(tag, SpeechPackStage.UNSUPPORTED,
            "Geen bevestigd offline spraakpakket voor $tag op dit toestel. Geen cloudfallback.")
    }

    fun check(tag: String, callback: (SpeechPackReceipt) -> Unit) {
        if (Build.VERSION.SDK_INT < 33) {
            cancel()
            callback(SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
                "Android 12 kan spraakpakketten niet per taal opvragen. Start offline spraak alleen als Android het ondersteunt."))
            return
        }
        val (token, engine) = fresh(tag) ?: run {
            callback(SpeechPackReceipt(tag, SpeechPackStage.UNSUPPORTED,
                "Geen lokale Android-spraakdienst beschikbaar. Er wordt niets naar cloudspraak doorgestuurd."))
            return
        }
        callback(SpeechPackReceipt(tag, SpeechPackStage.CHECKING,
            "Android controleert het lokale spraakpakket voor $tag…"))
        try {
            engine.checkRecognitionSupport(requestedLanguage(tag), context.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(support: RecognitionSupport) {
                        if (token != epoch) return
                        val result = report(tag, support)
                        cancel()
                        callback(result)
                    }

                    override fun onError(error: Int) {
                        if (token != epoch) return
                        cancel()
                        callback(SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
                            "Android kon pakketondersteuning niet bevestigen (code $error)."))
                    }
                })
        } catch (_: Exception) {
            if (token != epoch) return
            cancel()
            callback(SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
                "De lokale spraakdienst ondersteunt deze pakketcontrole niet."))
        }
    }

    fun download(tag: String, callback: (SpeechPackReceipt) -> Unit) {
        // Never start network or disk work for unsupported Android versions or
        // without user-established Wi-Fi. No eager/bulk model downloads.
        if (Build.VERSION.SDK_INT < 33) {
            callback(SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
                "Op deze Androidversie is pakketdownload vanuit Tuddy niet beschikbaar."))
            return
        }
        val network = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = network?.getNetworkCapabilities(network.activeNetwork)
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
            callback(SpeechPackReceipt(tag, SpeechPackStage.ERROR,
                "Maak eerst verbinding met wifi. Tuddy start geen download via mobiele data."))
            return
        }
        val (token, engine) = fresh(tag) ?: run {
            callback(SpeechPackReceipt(tag, SpeechPackStage.UNSUPPORTED,
                "Geen lokale spraakdienst om een pakket aan te vragen."))
            return
        }
        callback(SpeechPackReceipt(tag, SpeechPackStage.DOWNLOADING,
            "Offline spraakpakket $tag aangevraagd via Android; installatie nog niet bevestigd."))
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                engine.triggerModelDownload(requestedLanguage(tag), context.mainExecutor,
                    object : ModelDownloadListener {
                        override fun onProgress(completedPercent: Int) {
                            if (token == epoch) callback(SpeechPackReceipt(
                                tag, SpeechPackStage.DOWNLOADING,
                                "Android meldt voortgang: ${completedPercent.coerceIn(0, 100)}%."))
                        }

                        override fun onSuccess() {
                            if (token != epoch) return
                            // Never declare a downloaded model READY from a download
                            // receipt alone; ask Android for real installed coverage.
                            check(tag, callback)
                        }

                        override fun onScheduled() {
                            if (token != epoch) return
                            cancel()
                            callback(SpeechPackReceipt(tag, SpeechPackStage.PENDING,
                                "Android heeft de modeldownload ingepland. Controleer later opnieuw."))
                        }

                        override fun onError(error: Int) {
                            if (token != epoch) return
                            cancel()
                            callback(SpeechPackReceipt(tag, SpeechPackStage.ERROR,
                                "Pakketdownload via Android mislukt (code $error). Herprobeer op wifi."))
                        }
                    })
            } else {
                engine.triggerModelDownload(requestedLanguage(tag))
                cancel()
                callback(SpeechPackReceipt(tag, SpeechPackStage.REQUESTED,
                    "Android 13 heeft het verzoek ontvangen. Controleer later of het pakket is geïnstalleerd."))
            }
        } catch (_: Exception) {
            if (token != epoch) return
            cancel()
            callback(SpeechPackReceipt(tag, SpeechPackStage.ERROR,
                "De lokale Android-spraakdienst kon het pakket niet aanvragen."))
        }
    }
}
