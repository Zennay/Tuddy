package com.livelingo.android.translation

import com.google.mlkit.nl.translate.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.livelingo.android.model.CommunityPhrasebook
import com.livelingo.android.model.LanguagePairPlan
import com.livelingo.android.model.Languages
import java.util.Locale

/**
 * All 59 ML Kit targets, without pre-downloading 58 source pairs per target.
 * Spoken source is automatically detected. We pre-cache at most 3 useful sources
 * and lazily install additional model pairs on Wi-Fi; translation is always local.
 * Model presence is cached only for this process; ML Kit persists installed models.
 */
class OfflineTranslator {
    private val translators = mutableMapOf<String, Translator>()
    private val installed = mutableSetOf<String>()
    private val downloads = mutableMapOf<String, MutableList<(Result<Unit>) -> Unit>>()
    private val wifiOnly = DownloadConditions.Builder().requireWifi().build()
    private var generation = 0L
    private var ready = false
    private var target = "en"

    fun prepare(targetCode: String, onProgress: (Int, Int) -> Unit, onDone: (Result<Unit>) -> Unit) {
        invalidate()
        if (Languages.isCommunityPhrasebook(targetCode)) {
            target = targetCode
            ready = true // Built-in exact-match vocabulary; NO speech or general translation claim.
            onDone(Result.success(Unit))
            return
        }
        if (Languages.find(targetCode) == null) {
            onDone(Result.failure(IllegalArgumentException("This language does not have an ML Kit offline model yet")))
            return
        }
        target = targetCode
        val request = generation
        val starter = LanguagePairPlan.starterSources(targetCode, Locale.getDefault().language)
        fun next(index: Int) {
            if (request != generation) return
            if (index == starter.size) {
                ready = true
                onDone(Result.success(Unit))
                return
            }
            onProgress(index, starter.size)
            ensureDownloaded(starter[index]) { result ->
                if (request != generation) return@ensureDownloaded
                if (result.isFailure) onDone(result) else next(index + 1)
            }
        }
        next(0)
    }

    private fun client(source: String): Translator {
        return translators.getOrPut(source) {
            val sourceTag = requireNotNull(TranslateLanguage.fromLanguageTag(source))
            val targetTag = requireNotNull(TranslateLanguage.fromLanguageTag(target))
            Translation.getClient(TranslatorOptions.Builder()
                .setSourceLanguage(sourceTag)
                .setTargetLanguage(targetTag)
                .build())
        }
    }

    private fun ensureDownloaded(source: String, done: (Result<Unit>) -> Unit) {
        if (source == target || source in installed) { done(Result.success(Unit)); return }
        if (Languages.find(source) == null) {
            done(Result.failure(IllegalArgumentException("No local translation model for $source")))
            return
        }
        val waiting = downloads[source]
        if (waiting != null) {
            if (waiting.size < 12) waiting.add(done)
            else done(Result.failure(IllegalStateException("Language pack is still downloading")))
            return
        }
        downloads[source] = mutableListOf(done)
        val request = generation
        try {
            // downloadModelIfNeeded is a no-op if this exact pair is already stored on device.
            client(source).downloadModelIfNeeded(wifiOnly)
                .addOnSuccessListener {
                    if (request == generation) {
                        installed.add(source)
                        downloads.remove(source)?.forEach { it(Result.success(Unit)) }
                    }
                }
                .addOnFailureListener { error ->
                    if (request == generation) {
                        downloads.remove(source)?.forEach {
                            it(Result.failure(IllegalStateException(
                                "Connect to Wi-Fi once to install the $source → $target offline pack", error)))
                        }
                    }
                }
        } catch (error: Exception) {
            downloads.remove(source)?.forEach { it(Result.failure(error)) }
        }
    }

    fun translate(source: String, text: String, onDone: (Result<String>) -> Unit) {
        if (!ready) { onDone(Result.failure(IllegalStateException("Prepare offline models first"))); return }
        if (source == target) { onDone(Result.success(text)); return }
        if (Languages.isCommunityPhrasebook(target)) {
            onDone(CommunityPhrasebook.translate(source, target, text))
            return
        }
        val request = generation
        ensureDownloaded(source) { downloaded ->
            if (request != generation) return@ensureDownloaded
            if (downloaded.isFailure) { onDone(Result.failure(downloaded.exceptionOrNull()!!)); return@ensureDownloaded }
            client(source).translate(text)
                .addOnSuccessListener { if (request == generation) onDone(Result.success(it)) }
                .addOnFailureListener { if (request == generation) onDone(Result.failure(it)) }
        }
    }

    fun invalidate() {
        generation++
        ready = false
        downloads.clear()
        installed.clear()
        translators.values.forEach { it.close() }
        translators.clear()
    }
    fun close() = invalidate()
}
