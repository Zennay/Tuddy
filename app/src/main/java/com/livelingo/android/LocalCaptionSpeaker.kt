package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Explicit user-triggered pronunciation of a completed local caption.
 *
 * Only voices which the Android TTS engine itself marks as non-network and
 * already installed are admitted. Never asks for a voice/model download or
 * permits the Android engine to fall back to a network-required voice.
 *
 * The engine is destroyed on Stop/background, with stale init callbacks
 * fenced by a monotonic generation. No text or utterance is retained. An
 * explicitly selected voice preference is stored locally per language only.
 */
internal data class InstalledLocalVoice(
    val name: String,
    val languageTag: String,
    val quality: Int
)

internal enum class LocalSpeechPace(val rate: Float) {
    NORMAL(1.0f), SLOW(0.72f)
}

internal class LocalCaptionSpeaker(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    // Shared with normal/slow pronunciation and subtitle-sequence playback,
    // including the Android selected-text quick translator.
    private val playbackSession = LocalPlaybackDeviceSession(appContext)
    private var engine: TextToSpeech? = null
    private var generation = 0L

    fun stop() {
        generation++
        playbackSession.end() // Release audio focus and device/noisy listeners.
        val old = engine
        engine = null
        if (old != null) {
            runCatching { old.stop() }
            runCatching { old.shutdown() }
        }
    }

    /**
     * Enumerate Android-installed, non-network voices only after a user opens
     * the picker. The temporary engine is shut down before the callback; no
     * audio is spoken, no voice is downloaded and no transcript is supplied.
     * Generation fencing prevents a dismissed Activity from publishing a late
     * service callback into a new picker.
     */
    fun discoverInstalledVoices(
        languageTag: String,
        callback: (voices: List<InstalledLocalVoice>, notice: String) -> Unit
    ) {
        stop()
        if (!Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(languageTag)) {
            callback(emptyList(), "Deze taal heeft geen ondersteunde Android-stemcode.")
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") {
            callback(emptyList(), "Voor deze taal is geen bruikbare stemcode bekend.")
            return
        }
        val version = generation
        try {
            engine = TextToSpeech(appContext, TextToSpeech.OnInitListener { result ->
                main.post {
                    if (generation != version) return@post
                    val tts = engine
                    if (result != TextToSpeech.SUCCESS || tts == null) {
                        stop()
                        callback(emptyList(), "Android kon de lokale voorleesstemmen niet uitlezen.")
                        return@post
                    }
                    val found = runCatching {
                        tts.voices.orEmpty()
                            .asSequence()
                            .filter { voice ->
                                !voice.isNetworkConnectionRequired &&
                                    voice.locale.language == locale.language &&
                                    voice.features?.contains(
                                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
                                    ) != true &&
                                    voice.name.isNotBlank() && voice.name.length <= 256
                            }
                            .sortedWith(
                                compareByDescending<android.speech.tts.Voice> {
                                    it.locale.country.equals(locale.country, ignoreCase = true)
                                }.thenByDescending { it.quality }.thenBy { it.name }
                            )
                            .distinctBy { it.name }
                            .take(80)
                            .map { InstalledLocalVoice(
                                it.name, it.locale.toLanguageTag(), it.quality
                            ) }
                            .toList()
                    }.getOrNull()
                    stop()
                    callback(
                        found.orEmpty(),
                        when {
                            found == null ->
                                "Android kon de stemmen niet betrouwbaar uitlezen."
                            found.isEmpty() ->
                                "Geen geïnstalleerde, als offline gemelde stem voor deze taal."
                            else ->
                                "${found.size} door Android als lokaal gemelde stemmen. " +
                                "Dit is geen bewijs van werkelijke vliegtuigmodusisolatie."
                        }
                    )
                }
            })
        } catch (_: RuntimeException) {
            stop()
            callback(emptyList(), "De lokale Android-voorleesdienst kon niet worden geopend.")
        }
    }

    private fun chooseInstalledVoice(
        tts: TextToSpeech,
        locale: Locale,
        languageTag: String
    ): android.speech.tts.Voice? {
        val available = tts.voices.orEmpty()
            .filter { voice ->
                !voice.isNetworkConnectionRequired &&
                    voice.locale.language == locale.language &&
                    voice.features?.contains(
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
                    ) != true
            }
        val preferredName = LocalSpeechVoicePreferences.selectedVoiceName(
            appContext, languageTag
        )
        return available.firstOrNull { it.name == preferredName }
            ?: available.sortedWith(
                compareByDescending<android.speech.tts.Voice> {
                    it.locale.country.equals(locale.country, ignoreCase = true)
                }.thenByDescending { it.quality }
            ).firstOrNull()
    }

    /**
     * Reads a bounded subtitle sequence through an already-installed local voice.
     * Each cue starts only after Android reports the previous utterance done.
     * The caller owns progress UI and must call stop() on user stop/background.
     */
    fun speakSequence(
        lines: List<String>,
        languageTag: String,
        pace: LocalSpeechPace,
        onProgress: (completed: Int, total: Int) -> Unit,
        feedback: (String) -> Unit,
        onFinished: (completed: Boolean) -> Unit,
        onLineStarted: (index: Int) -> Unit = {},
        onWordRange: (index: Int, start: Int, endExclusive: Int) -> Unit = { _, _, _ -> }
    ) {
        stop()
        val safeLines = lines.map { it.replace('\n', ' ').replace('\r', ' ').trim() }
        if (safeLines.isEmpty() || safeLines.size > MAX_SEQUENCE_LINES ||
            safeLines.any { line ->
                line.isBlank() || line.length > MAX_LINE_CHARS ||
                    line.any { ch -> ch == '\u0000' || ch.isISOControl() }
            }
        ) {
            feedback("Voorlezen vereist 1–100 niet-lege regels van maximaal 400 tekens. Geen online fallback.")
            onFinished(false)
            return
        }
        if (!Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(languageTag)) {
            feedback("Voor deze taal is geen gecontroleerde offline stem bekend.")
            onFinished(false)
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") {
            feedback("Voor deze taal is geen gecontroleerde offline stem bekend.")
            onFinished(false)
            return
        }

        val version = generation
        var sequenceIndex = 0
        feedback("Geïnstalleerde lokale Android-stem controleren…")

        fun fail(message: String) {
            if (generation != version) return
            stop()
            feedback(message)
            onFinished(false)
        }

        if (!playbackSession.begin { reason -> fail(reason) }) {
            fail("Android kon de audiofocus of huidige uitvoer niet veilig bewaken. " +
                "Er is niets afgespeeld.")
            return
        }

        fun speakAt(tts: TextToSpeech, index: Int) {
            if (generation != version) return
            sequenceIndex = index
            onLineStarted(index)
            val result = runCatching {
                tts.speak(
                    safeLines[index],
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "tuddy-offline-sequence-$version-$index"
                )
            }.getOrDefault(TextToSpeech.ERROR)
            if (result != TextToSpeech.SUCCESS) {
                fail("Lokale voorleesregel ${index + 1} kon niet starten. Geen online fallback.")
            }
        }

        try {
            engine = TextToSpeech(appContext, TextToSpeech.OnInitListener { result ->
                main.post {
                    if (generation != version) return@post
                    val tts = engine
                    if (result != TextToSpeech.SUCCESS || tts == null) {
                        fail("Geen bruikbare lokale Android-spraakstem. Er is niets online afgespeeld.")
                        return@post
                    }
                    try {
                        val chosen = chooseInstalledVoice(tts, locale, languageTag)
                        if (chosen == null ||
                            tts.setVoice(chosen) != TextToSpeech.SUCCESS ||
                            tts.setAudioAttributes(LocalPlaybackDeviceSession.SPEECH_AUDIO_ATTRIBUTES) !=
                                TextToSpeech.SUCCESS ||
                            tts.setSpeechRate(pace.rate) != TextToSpeech.SUCCESS
                        ) {
                            fail("Geen geïnstalleerde lokale stem voor deze taal. Geen online fallback.")
                            return@post
                        }
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit

                            override fun onRangeStart(
                                utteranceId: String?,
                                start: Int,
                                end: Int,
                                frame: Int
                            ) {
                                val spokenIndex = utteranceId
                                    ?.substringAfterLast('-')
                                    ?.toIntOrNull() ?: return
                                main.post {
                                    if (generation != version || spokenIndex != sequenceIndex) {
                                        return@post
                                    }
                                    val line = safeLines.getOrNull(spokenIndex) ?: return@post
                                    if (start >= 0 && end > start && end <= line.length) {
                                        onWordRange(spokenIndex, start, end)
                                    }
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                val finishedIndex = utteranceId
                                    ?.substringAfterLast('-')
                                    ?.toIntOrNull() ?: return
                                main.post {
                                    if (generation != version || finishedIndex != sequenceIndex) {
                                        return@post
                                    }
                                    onProgress(finishedIndex + 1, safeLines.size)
                                    if (finishedIndex + 1 >= safeLines.size) {
                                        stop()
                                        feedback("Voorlezen voltooid: ${safeLines.size} regels lokaal afgespeeld.")
                                        onFinished(true)
                                    } else {
                                        speakAt(tts, finishedIndex + 1)
                                    }
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                main.post {
                                    if (generation == version) {
                                        fail("Lokale spraakfout; de resterende regels zijn gestopt. Geen online fallback.")
                                    }
                                }
                            }
                        })
                        speakAt(tts, 0)
                    } catch (_: RuntimeException) {
                        fail("Voorlezen niet mogelijk met de geïnstalleerde offline stemmen.")
                    }
                }
            })
        } catch (_: RuntimeException) {
            fail("De lokale Android-voorleesdienst kan niet worden gestart.")
        }
    }

    /**
     * User-triggered two-part listening: first the reviewed original, then
     * its completed translation. Both languages must have installed,
     * non-network Android voices BEFORE any audio begins. A single audio
     * focus/headset session covers the whole pair and is released after the
     * second completion, Stop/background, or any device/engine error.
     *
     * Never saves either sentence, spawns a model download or accepts an
     * online TTS voice. An old engine's late callbacks cannot advance a
     * newer pair or speak the next language.
     */
    fun speakBilingualPair(
        original: String,
        translated: String,
        sourceLanguage: String,
        targetLanguage: String,
        pace: LocalSpeechPace,
        feedback: (String) -> Unit
    ) {
        stop()
        val parts = listOf(original.trim(), translated.trim())
        if (parts.any { line ->
                line.isEmpty() || line.length > 400 ||
                    line.any { it == '\u0000' || it == '\r' || it == '\n' || it.isISOControl() }
            }
        ) {
            feedback("Tweetalig voorlezen vereist twee korte, voltooide regels. Geen online fallback.")
            return
        }
        val tags = listOf(sourceLanguage, targetLanguage)
        if (tags.any { !Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(it) }) {
            feedback("Voor één van deze talen is geen veilige Android-stemcode bekend.")
            return
        }
        val locales = tags.map { Locale.forLanguageTag(it) }
        if (locales.any { it.language.isBlank() || it.language == "und" }) {
            feedback("De twee talen moeten elk een bruikbare lokale voorleesstem hebben.")
            return
        }
        val version = generation
        if (!playbackSession.begin { reason ->
                if (generation == version) {
                    stop()
                    feedback(reason)
                }
            }) {
            stop()
            feedback("Geen veilige Android-audiofocus of uitvoerroute. Er is niets afgespeeld.")
            return
        }
        feedback("Twee geïnstalleerde offline Android-stemmen controleren…")
        var currentPart = 0

        fun fail(message: String) {
            if (generation != version) return
            stop()
            feedback(message)
        }

        fun sayPart(tts: TextToSpeech, voices: List<android.speech.tts.Voice>, index: Int) {
            if (generation != version || currentPart != index) return
            val result = runCatching {
                if (tts.setVoice(voices[index]) != TextToSpeech.SUCCESS) {
                    TextToSpeech.ERROR
                } else {
                    tts.speak(
                        parts[index], TextToSpeech.QUEUE_FLUSH, null,
                        "tuddy-offline-bilingual-$version-$index"
                    )
                }
            }.getOrDefault(TextToSpeech.ERROR)
            if (result != TextToSpeech.SUCCESS) {
                fail("Lokale stem voor ${tags[index].uppercase(Locale.ROOT)} kon niet starten. " +
                    "Resterende audio gestopt; geen online fallback.")
            }
        }

        try {
            engine = TextToSpeech(appContext, TextToSpeech.OnInitListener { result ->
                main.post {
                    if (generation != version) return@post
                    val tts = engine
                    if (result != TextToSpeech.SUCCESS || tts == null) {
                        fail("Android heeft geen bruikbare lokale voorleesdienst. Niets afgespeeld.")
                        return@post
                    }
                    try {
                        val voices = locales.mapIndexed { index, locale ->
                            chooseInstalledVoice(tts, locale, tags[index])
                        }
                        if (voices.any { it == null }) {
                            fail("Eén van de twee talen heeft geen geïnstalleerde offline stem. " +
                                "Geen gedeeltelijk of online voorlezen.")
                            return@post
                        }
                        val localVoices = voices.filterNotNull()
                        // Admit *both* voice selections before any private
                        // speech is sent to the TTS engine.
                        if (tts.setAudioAttributes(
                                LocalPlaybackDeviceSession.SPEECH_AUDIO_ATTRIBUTES
                            ) != TextToSpeech.SUCCESS ||
                            tts.setSpeechRate(pace.rate) != TextToSpeech.SUCCESS ||
                            tts.setVoice(localVoices[0]) != TextToSpeech.SUCCESS ||
                            tts.setVoice(localVoices[1]) != TextToSpeech.SUCCESS ||
                            tts.setVoice(localVoices[0]) != TextToSpeech.SUCCESS
                        ) {
                            fail("De twee geïnstalleerde Android-stemmen konden niet beide " +
                                "worden ingesteld. Er is niets afgespeeld.")
                            return@post
                        }
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) {
                                main.post {
                                    if (generation != version ||
                                        utteranceId != "tuddy-offline-bilingual-$version-$currentPart") {
                                        return@post
                                    }
                                    if (currentPart == 0) {
                                        currentPart = 1
                                        feedback("Origineel afgerond · vertaling lokaal voorlezen…")
                                        sayPart(tts, localVoices, 1)
                                    } else {
                                        stop()
                                        feedback("Beide zinnen lokaal voorgelezen; audiofocus vrijgegeven.")
                                    }
                                }
                            }
                            override fun onError(utteranceId: String?) {
                                main.post {
                                    if (generation == version &&
                                        utteranceId ==
                                            "tuddy-offline-bilingual-$version-$currentPart") {
                                        fail("Android stopte het tweetalige voorlezen. " +
                                            "Geen online fallback.")
                                    }
                                }
                            }
                        })
                        feedback("Origineel lokaal voorlezen · daarna de vertaling…")
                        sayPart(tts, localVoices, 0)
                    } catch (_: RuntimeException) {
                        fail("Android kon de tweetalige offline stemmen niet veilig starten.")
                    }
                }
            })
        } catch (_: RuntimeException) {
            fail("De lokale voorleesdienst kon niet worden geopend. Geen online fallback.")
        }
    }

    fun close() = stop()

    fun speak(translated: String, languageTag: String, feedback: (String) -> Unit) {
        speak(translated, languageTag, LocalSpeechPace.NORMAL, feedback)
    }

    fun speak(
        translated: String,
        languageTag: String,
        pace: LocalSpeechPace,
        feedback: (String) -> Unit
    ) {
        stop()
        // An on-device translator may produce huge or control-filled results.
        // Admit short printable completed lines only; do not retry over a
        // network speech engine or publish provider errors.
        val safe = translated.trim()
        if (safe.isEmpty() || safe.length > 400 ||
            safe.any { it == '\u0000' || it == '\r' || it == '\n' || it.isISOControl() } ||
            !Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(languageTag)) {
            feedback("Deze vertaling kan niet veilig worden voorgelezen.")
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") {
            feedback("Voor deze taal is geen gecontroleerde offline stem bekend.")
            return
        }
        val version = generation
        if (!playbackSession.begin { reason ->
                if (generation == version) {
                    stop()
                    feedback(reason)
                }
            }) {
            stop()
            feedback("Android kon de audiofocus of huidige uitvoer niet veilig bewaken. " +
                "Er is niets afgespeeld.")
            return
        }
        feedback("Geïnstalleerde lokale Android-stem controleren…")
        try {
            engine = TextToSpeech(appContext, TextToSpeech.OnInitListener { result ->
                main.post {
                    if (generation != version) return@post
                    val tts = engine
                    if (result != TextToSpeech.SUCCESS || tts == null) {
                        stop()
                        feedback("Geen bruikbare lokale Android-spraakstem. Er is niets online afgespeeld.")
                        return@post
                    }
                    try {
                        val chosen = chooseInstalledVoice(tts, locale, languageTag)
                        if (chosen == null ||
                            tts.setVoice(chosen) != TextToSpeech.SUCCESS ||
                            tts.setAudioAttributes(LocalPlaybackDeviceSession.SPEECH_AUDIO_ATTRIBUTES) !=
                                TextToSpeech.SUCCESS ||
                            tts.setSpeechRate(pace.rate) != TextToSpeech.SUCCESS) {
                            stop()
                            feedback("Geen geïnstalleerde lokale stem voor deze taal. Geen online fallback.")
                            return@post
                        }
                        // A finished one-line playback must release focus and
                        // device listeners without requiring a manual Stop tap.
                        // Late callbacks from an old TextToSpeech engine are
                        // fenced by the same generation used on background.
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) {
                                main.post {
                                    if (generation == version) {
                                        stop()
                                        feedback("Lokaal voorlezen voltooid; audiofocus vrijgegeven.")
                                    }
                                }
                            }
                            override fun onError(utteranceId: String?) {
                                main.post {
                                    if (generation == version) {
                                        stop()
                                        feedback("Android stopte het lokale voorlezen. Geen online fallback.")
                                    }
                                }
                            }
                        })
                        if (tts.speak(safe, TextToSpeech.QUEUE_FLUSH, null,
                                "tuddy-offline-caption-$version") != TextToSpeech.SUCCESS) {
                            stop()
                            feedback("Lokale stem kon niet starten. Geen online fallback.")
                        } else {
                            feedback(
                                if (pace == LocalSpeechPace.SLOW)
                                    "Rustig tempo met lokaal gemelde Android-stem. Uitspraak kan onnauwkeurig zijn."
                                else "Android gebruikt een als lokaal gemelde stem. Uitspraak kan onnauwkeurig zijn."
                            )
                        }
                    } catch (_: RuntimeException) {
                        stop()
                        feedback("Voorlezen niet mogelijk met de geïnstalleerde offline stemmen.")
                    }
                }
            })
        } catch (_: RuntimeException) {
            stop()
            feedback("De lokale Android-voorleesdienst kan niet worden gestart. Geen online fallback.")
        }
    }
    private companion object {
        const val MAX_SEQUENCE_LINES = 100
        const val MAX_LINE_CHARS = 400
    }

}
