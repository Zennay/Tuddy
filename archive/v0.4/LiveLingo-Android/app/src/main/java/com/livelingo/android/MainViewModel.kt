package com.livelingo.android

import android.app.Application
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.livelingo.android.model.Conversation
import com.livelingo.android.model.Languages
import com.livelingo.android.model.LocalGlossary
import com.livelingo.android.speech.OnDeviceSpeech
import com.livelingo.android.translation.LocalLanguageDetector
import com.livelingo.android.translation.OfflineTranslator
import java.util.Locale

data class WordInsight(
    val word: String,
    val language: String?,
    val translation: String? = null,
    val definition: String? = null,
    val illustration: String? = null,
)

data class LiveState(
    val targetLanguage: String = "en",
    val prepared: Boolean = false,
    val preparing: Boolean = false,
    val listening: Boolean = false,
    val detectedSource: String? = null,
    val message: String = "Choose the language you want to read, then prepare offline models.",
    val conversation: Conversation = Conversation(),
    val insight: WordInsight? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("livelingo-settings", 0)
    var state by mutableStateOf(LiveState(targetLanguage =
        Languages.normalizeTarget(preferences.getString("target-language", null))
            ?: Languages.normalize(Locale.getDefault().language) ?: "nl"))
        private set

    private val translator = OfflineTranslator()
    private val detector = LocalLanguageDetector()
    private var setupId = 0L
    private var sessionId = 0L
    private var peekId = 0L
    private var lastInterimRequestAt = 0L
    private var spokenLanguage: String? = null
    private val speech = OnDeviceSpeech(
        context = application.applicationContext,
        onPartial = ::partial,
        onFinal = ::finish,
        onDetected = { code ->
            spokenLanguage = code
            if (code != null) state = state.copy(detectedSource = code)
        },
        onState = { active -> state = state.copy(listening = active) },
        onFailure = { message -> state = state.copy(message = message) },
    )

    val speechAvailable get() = speech.available
    val canAutoSwitchVoice get() = Build.VERSION.SDK_INT >= 34

    fun selectTarget(code: String) {
        if (Languages.findTarget(code) == null || code == state.targetLanguage || state.preparing) return
        preferences.edit().putString("target-language", code).apply()
        speech.stop()
        ++setupId
        ++sessionId
        dismissWord()
        translator.invalidate()
        // Historical lines retain the language of THEIR translation. Do not discard user history.
        state = state.copy(targetLanguage = code, prepared = false, preparing = false, listening = false,
            detectedSource = null, conversation = state.conversation.partial(""),
            message = if (Languages.isCommunityPhrasebook(code))
                "${Languages.findTarget(code)?.name}: only 5 beginner expressions from Dutch/English offline. No free speech translation yet."
            else "Reading language: ${Languages.find(code)?.name}. Prepare its local starter packs.")
    }

    fun prepare() {
        if (state.preparing || state.listening) return
        val request = ++setupId
        state = state.copy(preparing = true, prepared = false,
            message = if (Languages.isCommunityPhrasebook(state.targetLanguage))
                "Opening built-in 5-expression offline phrasebook…"
            else "Preparing up to 3 common language pairs on Wi-Fi…")
        translator.prepare(state.targetLanguage,
            onProgress = { count, total ->
                if (request == setupId) state = state.copy(message = "Offline model ${count + 1} of $total…")
            },
            onDone = prepareCallback@{ result ->
                if (request != setupId) return@prepareCallback
                state = state.copy(
                    preparing = false, prepared = result.isSuccess,
                    message = if (result.isSuccess && Languages.isCommunityPhrasebook(state.targetLanguage))
                        "Basic words only: hello, thanks, yes, no, water (Dutch/English → selected language). Free-form translation unavailable."
                    else if (result.isSuccess)
                        "Starter packs installed. Other detected languages download locally on Wi-Fi when needed."
                    else "Offline pack unavailable. Connect to Wi-Fi and retry: ${result.exceptionOrNull()?.localizedMessage}",
                )
            },
        )
    }

    fun start() {
        if (state.listening || state.preparing || !state.prepared) return
        if (!speech.available) {
            state = state.copy(message = "No offline speech recognizer available. Install speech packs in Android settings.")
            return
        }
        ++sessionId
        spokenLanguage = null
        state = state.copy(message = if (canAutoSwitchVoice) "Listening with automatic language detection…" else
            "Listening with phone's speech language; auto-switch needs Android 14+.")
        val seedLanguage = Languages.find(Languages.normalize(Locale.getDefault().language)) ?: Languages.choices[0]
        speech.start(seedLanguage.speechTag)
    }

    fun stop() {
        speech.stop()
        dismissWord()
        state = state.copy(conversation = state.conversation.partial(""),
            message = "Paused. Transcript is stored in RAM only.")
    }

    fun clear() {
        speech.stop()
        ++sessionId
        dismissWord()
        state = state.copy(conversation = state.conversation.clear(),
            detectedSource = null, message = "Conversation erased from memory.")
    }

    private fun partial(text: String) {
        val updated = state.conversation.partial(text, spokenLanguage)
        if (updated === state.conversation) return
        state = state.copy(conversation = updated)
        val now = System.currentTimeMillis()
        if (state.prepared && now - lastInterimRequestAt >= 650) {
            lastInterimRequestAt = now
            val revision = updated.revision
            val epoch = sessionId
            detect(text, spokenLanguage) { language ->
                if (epoch != sessionId || revision != state.conversation.revision) return@detect
                state = state.copy(conversation = state.conversation.identifiedLanguage(revision, language),
                    detectedSource = language ?: state.detectedSource)
                if (language != null) translator.translate(language, text) { result ->
                    if (epoch == sessionId && result.isSuccess) {
                        state = state.copy(conversation = state.conversation.interimTranslation(revision, result.getOrThrow()))
                    }
                }
            }
        }
    }

    private fun finish(text: String) {
        val updated = state.conversation.finish(text, System.currentTimeMillis(), spokenLanguage, state.targetLanguage)
        state = state.copy(conversation = updated)
        val caption = updated.lines.lastOrNull() ?: return
        val epoch = sessionId
        val target = state.targetLanguage
        detect(caption.source, caption.sourceLanguage) { source ->
            if (epoch != sessionId || state.targetLanguage != target) return@detect
            if (state.conversation.lines.none { it.id == caption.id }) return@detect
            if (source == null) {
                state = state.copy(message = "Couldn't identify that phrase; try a longer utterance.")
                return@detect
            }
            state = state.copy(detectedSource = source,
                conversation = state.conversation.finalLanguage(caption.id, source))
            translator.translate(source, caption.source) { result ->
                if (epoch != sessionId || state.targetLanguage != target) return@translate
                if (result.isSuccess) state = state.copy(
                    conversation = state.conversation.finalTranslation(caption.id, result.getOrThrow()))
                else state = state.copy(message = "Translation unavailable for ${Languages.find(source)?.name}: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    private fun detect(text: String, speechLanguage: String?, onResult: (String?) -> Unit) {
        if (speechLanguage != null) onResult(speechLanguage)
        else detector.identify(text, onResult)
    }

    /** Only use a preinstalled translator and bundled dictionary; no Wikipedia/network requests. */
    fun peekWord(word: String, language: String?) {
        val clean = word.trim()
        if (clean.isBlank()) return
        val token = ++peekId
        val code = Languages.normalize(language)
        val entry = LocalGlossary.lookup(code, clean)
        state = state.copy(insight = WordInsight(clean, code, definition = entry?.definition,
            illustration = entry?.illustration))
        if (code != null && state.prepared && !Languages.isCommunityPhrasebook(code)) {
            translator.translate(code, clean) { result ->
                if (token == peekId && result.isSuccess) {
                    state = state.copy(insight = state.insight?.takeIf { it.word == clean }
                        ?.copy(translation = result.getOrThrow()))
                }
            }
        }
    }

    fun dismissWord() {
        ++peekId
        state = state.copy(insight = null)
    }

    override fun onCleared() {
        speech.close()
        translator.close()
        detector.close()
        super.onCleared()
    }
}
