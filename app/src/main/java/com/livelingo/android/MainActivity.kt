package com.livelingo.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch

private data class LanguageChoice(val label: String, val speechCode: String, val translateCode: String)
private val spoken = listOf(
    LanguageChoice("Nederlands", "nl-NL", TranslateLanguage.DUTCH),
    LanguageChoice("English", "en-US", TranslateLanguage.ENGLISH),
    LanguageChoice("Deutsch", "de-DE", TranslateLanguage.GERMAN),
    LanguageChoice("Français", "fr-FR", TranslateLanguage.FRENCH),
    LanguageChoice("Español", "es-ES", TranslateLanguage.SPANISH),
    LanguageChoice("العربية", "ar-SA", TranslateLanguage.ARABIC)
)
private data class Line(val id: Long, val original: String, val translated: String, val status: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TuddyScreen() }
    }
}

@Composable
private fun TuddyScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFAB9BFF),
        background = androidx.compose.ui.graphics.Color(0xFF10111C),
        surface = androidx.compose.ui.graphics.Color(0xFF202238)
    )
    val lines = remember { mutableStateListOf<Line>() }
    var partial by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(spoken[0]) }
    var target by remember { mutableStateOf(spoken[1]) }
    var sourceOpen by remember { mutableStateOf(false) }
    var targetOpen by remember { mutableStateOf(false) }
    var targetQuery by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var follow by remember { mutableStateOf(true) }
    var generation by remember { mutableIntStateOf(0) }
    val voiceSessions = remember { VoiceSessionGeneration() }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val offlineAvailable = remember { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun updateLine(id: Long, translation: String, status: String, token: Int) {
        if (token != generation) return
        val index = lines.indexOfFirst { it.id == id }
        if (index >= 0) lines[index] = lines[index].copy(translated = translation, status = status)
    }

    fun translationStillRelevant(id: Long, token: Int): Boolean =
        TranslationWorkPolicy.mayContinue(token, generation, lines.any { it.id == id })

    fun translate(text: String, id: Long, targetCode: String, sourceCode: String, token: Int) {
        if (!translationStillRelevant(id, token)) return
        // Text language detection and translation remain ON DEVICE. Models are downloaded only
        // when the user is on Wi-Fi; without a downloaded model, fail transparently.
        val detector = LanguageIdentification.getClient()
        detector.identifyLanguage(text)
            .addOnSuccessListener { guess ->
                detector.close()
                // Clear/remove may occur while language detection is pending.
                // Do not begin a *new* Wi-Fi model download for abandoned text.
                if (!translationStillRelevant(id, token)) return@addOnSuccessListener
                val identified = guess.takeIf { it != "und" && TranslateLanguage.fromLanguageTag(it) != null }
                val from = identified?.let { TranslateLanguage.fromLanguageTag(it) } ?: sourceCode
                if (from == targetCode) {
                    updateLine(id, text, "Lokaal", token)
                    return@addOnSuccessListener
                }
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(from)
                    .setTargetLanguage(targetCode)
                    .build()
                val translator = Translation.getClient(options)
                val conditions = DownloadConditions.Builder().requireWifi().build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        // A model download already underway may finish after
                        // the conversation was cleared. Close without launching
                        // an unnecessary translation for now-absent text.
                        if (!translationStillRelevant(id, token)) {
                            translator.close()
                            return@addOnSuccessListener
                        }
                        translator.translate(text)
                            .addOnSuccessListener { output ->
                                updateLine(id, output, "Lokaal vertaald", token)
                                translator.close()
                            }
                            .addOnFailureListener {
                                updateLine(id, "Vertalen mislukt: ${it.localizedMessage ?: "onbekende fout"}", "Fout", token)
                                translator.close()
                            }
                    }
                    .addOnFailureListener {
                        updateLine(id, "Taalmodel ontbreekt — verbind met wifi en probeer opnieuw.", "Model ontbreekt", token)
                        translator.close()
                    }
            }
            .addOnFailureListener {
                detector.close()
                updateLine(id, "Taaldetectie mislukt.", "Fout", token)
            }
    }

    fun stop() {
        voiceSessions.invalidate() // Stop/background immediately invalidates prior callbacks.
        try { recognizer?.cancel() } catch (_: Exception) { }
        listening = false
        partial = ""
    }

    fun start() {
        error = ""
        // The runtime permission dialog may return after the Activity has
        // stopped. The microphone must never start from that stale callback.
        if (!MicrophoneStartPolicy.mayStart(lifecycleOwner.lifecycle.currentState)) {
            error = "Microfoon gestopt: open Tuddy en tik opnieuw op Luister."
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            error = "Microfoontoegang ontbreekt. Geef toestemming via Android."
            return
        }
        if (!offlineAvailable) {
            error = "Deze telefoon heeft geen on-device spraakherkenning. Tuddy schakelt niet over naar cloud-spraak."
            return
        }
        if (listening) return
        try {
            // Every start, including a rapid restart after Android's own end-of-speech,
            // must invalidate the previous recognizer's delayed callbacks.
            val voiceToken = voiceSessions.begin()
            recognizer?.destroy()
            val speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = speech
            val selectedTarget = target.translateCode
            val selectedSource = source.translateCode
            val token = generation
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if (voiceSessions.isCurrent(voiceToken)) listening = true }
                override fun onBeginningOfSpeech() { if (voiceSessions.isCurrent(voiceToken)) listening = true }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (voiceSessions.isCurrent(voiceToken)) listening = false }
                override fun onError(code: Int) {
                    if (!voiceSessions.isCurrent(voiceToken)) return
                    listening = false
                    partial = ""
                    error = "Offline spraakherkenning niet beschikbaar of onderbroken (code $code). Controleer het gedownloade spraakpakket."
                }
                override fun onResults(results: Bundle?) {
                    if (!voiceSessions.isCurrent(voiceToken)) return
                    listening = false
                    val sentence = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    partial = ""
                    if (sentence.isBlank() || token != generation) return
                    val id = System.nanoTime()
                    lines.add(Line(id, sentence, "Vertalen…", "Bezig"))
                    translate(sentence, id, selectedTarget, selectedSource, token)
                }
                override fun onPartialResults(results: Bundle?) {
                    if (token == generation && voiceSessions.isCurrent(voiceToken)) {
                        partial = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull().orEmpty()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, source.speechCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speech.startListening(intent)
            listening = true
        } catch (exception: Exception) {
            listening = false
            error = "Offline spraakdienst kon niet starten: ${exception.localizedMessage}"
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when {
            !granted -> error = "Microfoontoegang is nodig. Geef toestemming via de Android-instellingen."
            MicrophoneStartPolicy.mayStart(lifecycleOwner.lifecycle.currentState) -> start()
            else -> error = "Tuddy was op de achtergrond. Open de app en tik opnieuw op Luister."
        }
    }

    // Activity remaining composed in the background must NOT keep the mic open.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            generation++
            stop()
            recognizer?.destroy()
            recognizer = null
        }
    }

    LaunchedEffect(lines.size, follow) {
        if (follow && lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Tuddy", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Gesprekken begrijpen, ook zonder internet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { sourceOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Van: ${source.label}", maxLines = 1)
                        }
                        DropdownMenu(expanded = sourceOpen, onDismissRequest = { sourceOpen = false }) {
                            spoken.forEach {
                                DropdownMenuItem(text = { Text(it.label) }, onClick = {
                                    stop()
                                    source = it
                                    sourceOpen = false
                                })
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { targetOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Naar: ${target.label}", maxLines = 1)
                        }
                        DropdownMenu(expanded = targetOpen, onDismissRequest = { targetOpen = false }) {
                            Text("59 talen · offline tekstmodellen", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                            OutlinedTextField(
                                value = targetQuery,
                                onValueChange = { targetQuery = it },
                                label = { Text("Zoek taal") },
                                modifier = Modifier.width(310.dp).padding(horizontal = 10.dp),
                                singleLine = true
                            )
                            TargetCatalog.search(targetQuery).take(35).forEach { choice ->
                                DropdownMenuItem(text = { Text(choice.title) }, onClick = {
                                    stop()
                                    target = LanguageChoice(choice.title, choice.code, choice.code)
                                    targetQuery = ""
                                    targetOpen = false
                                })
                            }
                        }
                    }
                }
                Text(
                    "Koerdisch (Kurmanji/Sorani) en Pasjtoe: alleen mini-woordenlijst in v0.4-bronpakket; vrije vertaling nog niet beschikbaar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val canStart = offlineAvailable && !listening
                    Button(
                        onClick = {
                            if (listening) stop()
                            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED) start()
                            else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        enabled = offlineAvailable,
                    ) { Text(if (listening) "■ Stop" else "🎙 Luister") }
                    OutlinedButton(onClick = {
                        generation++
                        stop()
                        lines.clear()
                        error = ""
                    }) { Text("Wis gesprek") }
                    TextButton(onClick = { follow = !follow }) {
                        Text(if (follow) "Live ✓" else "Volg live")
                    }
                }
                if (!offlineAvailable) {
                    Text("Geen lokale spraakherkenning op dit apparaat. Geen cloudfallback.", color = MaterialTheme.colorScheme.error)
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                HorizontalDivider()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(lines, key = { it.id }) { line ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(line.original, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(line.translated, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(line.status, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (partial.isNotBlank()) {
                        item {
                            Card {
                                Column(Modifier.padding(14.dp)) {
                                    Text("● Spreekt nu…", color = MaterialTheme.colorScheme.primary)
                                    Text(partial, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Text(
                    "Privacy: alleen lokaal spraakherkennen en vertalen. Taalmodellen eerst via wifi downloaden. Gesprek staat alleen in tijdelijk geheugen en wordt niet opgeslagen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
