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
    val detail: String,
    // Populated only by an Android 13+ read-only recall inspection. Never
    // inferred from a downloadable or online-only regional language.
    val installedSpeechTags: List<String> = emptyList(),
    val admittedSpeechTag: String? = null
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

    private fun report(
        tag: String,
        support: RecognitionSupport,
        allowLanguageVariant: Boolean = false
    ): SpeechPackReceipt {
        // Recall uses ML Kit TEXT language codes such as "nl" and "en",
        // whereas Android installed SPEECH packs usually carry region tags
        // such as "nl-NL" and "en-US". A confirmed installed variant is
        // useful for a voluntary one-shot local practice attempt, but it is
        // NOT evidence that the regionless requested locale itself is an
        // exact installed model. Strict live speech checks stay unchanged.
        fun reported(tags: Collection<String>): List<String> {
            val exact = tags.filter { it.equals(tag, ignoreCase = true) }
            if (!allowLanguageVariant || '-' in tag) return exact.distinct()
            val language = Locale.forLanguageTag(tag).language
            if (language.isBlank() || language == "und") return exact.distinct()
            val variants = tags.filter { observedTag ->
                val observed = Locale.forLanguageTag(observedTag)
                observed.language.equals(language, ignoreCase = true) &&
                    observed.country.isNotBlank() &&
                    observed.toLanguageTag().equals(observedTag, ignoreCase = true)
            }
            // Stable options; only Android-confirmed INSTALLED tags ever reach
            // the caller. Never synthesize a region or interpret downloadables
            // as installed. Bound the picker to prevent untrusted large lists.
            return (exact + variants).distinctBy { it.lowercase(Locale.ROOT) }
                .sortedWith(compareBy<String> { !it.equals(tag, ignoreCase = true) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it })
                .take(24)
        }
        val installed = reported(support.installedOnDeviceLanguages)
        if (installed.isNotEmpty()) {
            val selected = installed.firstOrNull { it.equals(tag, ignoreCase = true) }
                ?: installed.singleOrNull()
            return SpeechPackReceipt(tag, SpeechPackStage.READY,
                if (allowLanguageVariant && '-' !in tag)
                    "Android bevestigt ${installed.size} geïnstalleerde offline " +
                        "spraakvariant(en) voor $tag. De opname gebruikt de exacte " +
                        "Android-code van het gekozen pakket."
                else "Android meldt een geïnstalleerd offline spraakpakket voor $tag. " +
                    "Geen garantie op herkenning of nauwkeurigheid.",
                if (allowLanguageVariant) installed else emptyList(),
                if (allowLanguageVariant) selected else null)
        }
        if (reported(support.pendingOnDeviceLanguages).isNotEmpty()) {
            return SpeechPackReceipt(tag, SpeechPackStage.PENDING,
                "Android bereidt het spraakpakket voor $tag nog voor. Controleer straks opnieuw.")
        }
        if (reported(support.supportedOnDeviceLanguages).isNotEmpty()) {
            return SpeechPackReceipt(tag, SpeechPackStage.DOWNLOADABLE,
                "Offline spraak voor $tag wordt door dit toestel aangeboden; het pakket ontbreekt nog.")
        }
        if (reported(support.onlineLanguages).isNotEmpty()) {
            return SpeechPackReceipt(tag, SpeechPackStage.ONLINE_ONLY,
                "Voor $tag biedt de herkenningsdienst alleen online spraak. Tuddy gebruikt die niet.")
        }
        return SpeechPackReceipt(tag, SpeechPackStage.UNSUPPORTED,
            "Geen bevestigd offline spraakpakket voor $tag op dit toestel. Geen cloudfallback.")
    }

    // Preserve the existing public call shape so every live speech caller
    // retains its exact-region recognition contract and trailing lambda.
    fun check(tag: String, callback: (SpeechPackReceipt) -> Unit) {
        checkInternal(tag, callback, allowLanguageVariant = false)
    }

    /** Only voluntary one-shot recall accepts an installed regional variant. */
    fun checkRecallLanguage(tag: String, callback: (SpeechPackReceipt) -> Unit) {
        checkInternal(tag, callback, allowLanguageVariant = true)
    }

    private fun checkInternal(
        tag: String,
        callback: (SpeechPackReceipt) -> Unit,
        allowLanguageVariant: Boolean
    ) {
        if (Build.VERSION.SDK_INT < 33) {
            cancel()
            callback(SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
                "Android 12 kan spraakpakketten niet per taal opvragen. Controleer in Android-spraakinstellingen of offline spraak en deze taal zijn gedownload; Tuddy kan dit niet zelf bevestigen."))
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
                        val result = report(tag, support, allowLanguageVariant)
                        // Only an EXACT Android-installed tag, never a
                        // broad/recall regional match, releases a submitted
                        // physical speech-model download request.
                        if (!allowLanguageVariant && result.stage == SpeechPackStage.READY) {
                            LocalSpeechPackDownloadAdmission.verifiedInstalled(tag)
                        }
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
        val activeNetwork = network?.activeNetwork
        val capabilities = if (network == null || activeNetwork == null) {
            null
        } else {
            runCatching { network.getNetworkCapabilities(activeNetwork) }.getOrNull()
        }
        val networkBlocker = when {
            capabilities == null ->
                "Android kan de actieve netwerkverbinding niet bevestigen. Controleer wifi en probeer opnieuw."
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                "Maak eerst verbinding met wifi. Tuddy start geen download via mobiele data."
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ->
                "Dit wifi-netwerk meldt geen internettoegang. Herstel de verbinding voordat je het pakket aanvraagt."
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ->
                "Android heeft de internetverbinding nog niet gevalideerd. Voltooi eventueel de wifi-inlogpagina en probeer opnieuw."
            else -> null
        }
        if (networkBlocker != null) {
            callback(SpeechPackReceipt(tag, SpeechPackStage.ERROR, networkBlocker))
            return
        }
        LocalModelStorageAdmission.blocker()?.let { blocker ->
            callback(SpeechPackReceipt(tag, SpeechPackStage.ERROR,
                blocker + " De systeemdienst heeft geen nieuwe downloadopdracht gekregen."))
            return
        }
        LocalModelNetworkAdmission.blocker(context)?.let { blocker ->
            callback(SpeechPackReceipt(tag, SpeechPackStage.ERROR,
                blocker + " De Android-spraakdienst kreeg geen nieuwe downloadopdracht."))
            return
        }
        // A user can close or reopen this screen while Android's system
        // service continues a previously accepted download. Never start a
        // second overlapping request merely because the observer was destroyed.
        val physicalLease = LocalSpeechPackDownloadAdmission.acquire(tag)
        if (physicalLease == null) {
            val pending = LocalSpeechPackDownloadAdmission.pendingTag()
            callback(SpeechPackReceipt(tag, SpeechPackStage.PENDING,
                "Android heeft in dit app-proces al een onbevestigd offline " +
                    "spraakpakketverzoek voor ${pending ?: "een andere taal"}. " +
                    "Geen tweede verzoek gestart. Controleer later de " +
                    "daadwerkelijk geïnstalleerde modellen."))
            return
        }
        val (token, engine) = fresh(tag) ?: run {
            LocalSpeechPackDownloadAdmission.releaseBeforeSubmission(physicalLease)
            callback(SpeechPackReceipt(tag, SpeechPackStage.UNSUPPORTED,
                "Geen lokale spraakdienst om een pakket aan te vragen."))
            return
        }
        callback(SpeechPackReceipt(tag, SpeechPackStage.DOWNLOADING,
            "Android-spraakpakketverzoek voor $tag voorbereiden. Installatie niet bevestigd."))
        try {
            // The Android service may accept a model request even if its
            // invocation or callback registration later throws. Mark the
            // slot as submitted BEFORE invoking that external service;
            // uncertain completion remains blocked, not assumed canceled.
            LocalSpeechPackDownloadAdmission.markSubmitted(physicalLease)
            if (Build.VERSION.SDK_INT >= 34) {
                engine.triggerModelDownload(requestedLanguage(tag), context.mainExecutor,
                    object : ModelDownloadListener {
                        override fun onProgress(completedPercent: Int) {
                            if (token == epoch) callback(SpeechPackReceipt(
                                tag, SpeechPackStage.DOWNLOADING,
                                "Android meldt voortgang: ${completedPercent.coerceIn(0, 100)}%."))
                        }

                        override fun onSuccess() {
                            // Physical system completion is authoritative even
                            // when this UI's observer was canceled. Never
                            // present READY until a separate installed check.
                            LocalSpeechPackDownloadAdmission.systemCompleted(physicalLease)
                            if (token != epoch) return
                            check(tag, callback)
                        }

                        override fun onScheduled() {
                            // Scheduled != installed or physically completed:
                            // keep the process slot until exact installed
                            // verification, even after the screen closes.
                            if (token != epoch) return
                            cancel()
                            callback(SpeechPackReceipt(tag, SpeechPackStage.PENDING,
                                "Android heeft de modeldownload ingepland. " +
                                    "Geen nieuwe spraakpakketaanvraag zolang het " +
                                    "eerdere verzoek niet als voltooid is bevestigd."))
                        }

                        override fun onError(error: Int) {
                            LocalSpeechPackDownloadAdmission.systemCompleted(physicalLease)
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
                    "Android 13 heeft het verzoek ontvangen. Android meldt " +
                        "niet wanneer de download echt klaar is. " +
                        "Controleer het geïnstalleerde spraakpakket later " +
                        "bewust; geen tweede verzoek in dit app-proces."))
            }
        } catch (_: Exception) {
            if (token != epoch) return
            cancel()
            callback(SpeechPackReceipt(tag, SpeechPackStage.ERROR,
                "Android kon de spraakpakketaanvraag niet betrouwbaar afronden. " +
                    "Als de systeemdienst het verzoek al aannam, kan de download " +
                    "doorgaan. Geen tweede aanvraag in dit app-proces; " +
                    "controleer later de geïnstalleerde spraakpakketten."))
        }
    }
}
