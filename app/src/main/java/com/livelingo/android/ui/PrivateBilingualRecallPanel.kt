package com.livelingo.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import com.livelingo.android.PrivateRecallSpeechAttempt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livelingo.android.BilingualReplayPair
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

private enum class RecallPromptSide { ORIGINAL, TRANSLATION }

/**
 * An opt-in, entirely in-memory recall round over an existing bounded,
 * translated conversation snapshot. A learner sees the original phrase,
 * deliberately reveals the already-completed local translation, and marks
 * whether to try it again. Wrong answers are never sent to ML Kit or stored.
 *
 * The caller has already validated the pairs and both language codes. This
 * panel never mutates conversation rows or starts Android audio automatically.
 */
@Composable
internal fun PrivateBilingualRecallPanel(
    practice: List<BilingualReplayPair>,
    sourceCode: String,
    targetCode: String,
    speaker: LocalCaptionSpeaker,
    recallSpeech: PrivateRecallSpeechAttempt,
    enabled: Boolean,
    strictOfflineRadioOnly: Boolean
) {
    // When a caption is edited/deleted or the selected language, start or
    // count changes, discard every old answer/review index and stop old audio.
    var started by remember(practice, sourceCode, targetCode) { mutableStateOf(false) }
    var revealed by remember(practice, sourceCode, targetCode) { mutableStateOf(false) }
    var position by remember(practice, sourceCode, targetCode) { mutableStateOf(0) }
    var missed by remember(practice, sourceCode, targetCode) {
        mutableStateOf(emptySet<Int>())
    }
    var retryOnly by remember(practice, sourceCode, targetCode) {
        mutableStateOf(false)
    }
    var retryIndices by remember(practice, sourceCode, targetCode) {
        mutableStateOf(emptyList<Int>())
    }
    var round by remember(practice, sourceCode, targetCode) { mutableStateOf(1) }
    var audioNotice by remember(practice, sourceCode, targetCode) {
        mutableStateOf("")
    }
    var typedAnswer by remember(practice, sourceCode, targetCode) {
        mutableStateOf("")
    }
    // Only final local ASR text enters this temporary Compose state, never the
    // Android keyboard's EditText, ML Kit translation, scoring or disk storage.
    var spokenAnswer by remember(practice, sourceCode, targetCode) {
        mutableStateOf("")
    }
    var recordingAnswer by remember(practice, sourceCode, targetCode) {
        mutableStateOf(false)
    }
    var speechNotice by remember(practice, sourceCode, targetCode) {
        mutableStateOf("")
    }
    var permissionNotice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Never start automatically from a delayed Android permission dialog.
        permissionNotice = if (granted)
            "Microfoon toegestaan. Tik opnieuw op 'Spreek mijn antwoord in' om lokaal te beginnen."
        else "Microfoon niet toegestaan. Je kunt gewoon typen of nadenken."
    }
    var promptSide by remember(practice, sourceCode, targetCode) {
        mutableStateOf(RecallPromptSide.ORIGINAL)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(speaker, practice, sourceCode, targetCode, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE ||
                event == Lifecycle.Event.ON_STOP) {
                // Clear the learner's unsubmitted answer before Android may
                // snapshot the screen or another app becomes foreground.
                speaker.stop()
                recallSpeech.cancel()
                recordingAnswer = false
                typedAnswer = ""
                spokenAnswer = ""
                revealed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.stop()
            recallSpeech.cancel()
            typedAnswer = ""
            spokenAnswer = ""
            recordingAnswer = false
        }
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            recallSpeech.cancel()
            recordingAnswer = false
            spokenAnswer = ""
        }
    }

    val currentIndices = if (retryOnly) retryIndices else practice.indices.toList()
    val current = currentIndices.getOrNull(position)?.let(practice::get)
    val done = started && current == null && currentIndices.isNotEmpty()

    fun begin() {
        speaker.stop()
        recallSpeech.cancel()
        recordingAnswer = false
        spokenAnswer = ""
        speechNotice = ""
        started = true
        position = 0
        revealed = false
        missed = emptySet()
        retryOnly = false
        retryIndices = emptyList()
        round = 1
        audioNotice = ""
        typedAnswer = ""
    }

    fun choosePrompt(side: RecallPromptSide) {
        if (promptSide == side) return
        speaker.stop()
        recallSpeech.cancel()
        recordingAnswer = false
        spokenAnswer = ""
        speechNotice = ""
        promptSide = side
        started = false
        position = 0
        revealed = false
        missed = emptySet()
        retryOnly = false
        retryIndices = emptyList()
        round = 1
        audioNotice = ""
        typedAnswer = ""
    }

    fun mark(tryAgain: Boolean) {
        val index = currentIndices.getOrNull(position) ?: return
        speaker.stop()
        recallSpeech.cancel()
        recordingAnswer = false
        spokenAnswer = ""
        speechNotice = ""
        if (tryAgain) missed = missed + index
        position++
        revealed = false
        audioNotice = ""
        typedAnswer = ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HorizontalDivider()
        Text(
            "Zelf overhoren · eerst herinneren, daarna antwoord onthullen",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Dezelfde ${practice.size} afgeronde lokale vertaalparen, alleen in " +
                "het huidige RAM-gesprek. Jij bepaalt wanneer je kijkt of luistert. " +
                "Een fout antwoord verandert geen ondertitel, model of woordenlijst.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Oefenrichting: kies welke kant je zelf wilt terugvertalen. " +
                "Richting wisselen wist je lopende antwoorden en begint een nieuwe ronde.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = promptSide == RecallPromptSide.ORIGINAL,
                enabled = enabled && practice.isNotEmpty(),
                onClick = { choosePrompt(RecallPromptSide.ORIGINAL) },
                label = { Text("${sourceCode.uppercase()} → ${targetCode.uppercase()}") }
            )
            FilterChip(
                selected = promptSide == RecallPromptSide.TRANSLATION,
                enabled = enabled && practice.isNotEmpty(),
                onClick = { choosePrompt(RecallPromptSide.TRANSLATION) },
                label = { Text("${targetCode.uppercase()} → ${sourceCode.uppercase()}") }
            )
        }
        if (!started) {
            OutlinedButton(
                onClick = ::begin,
                enabled = enabled && practice.isNotEmpty()
            ) { Text("Begin stille overhoorronde") }
        } else if (done) {
            Text(
                "Ronde $round voltooid · ${currentIndices.size - missed.size} goed " +
                    "· ${missed.size} nog oefenen.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (missed.isNotEmpty()) {
                OutlinedButton(
                    enabled = enabled,
                    onClick = {
                        speaker.stop()
                        retryIndices = missed.sorted()
                        retryOnly = true
                        missed = emptySet()
                        position = 0
                        revealed = false
                        audioNotice = ""
                        typedAnswer = ""
                        round++
                    }
                ) { Text("Oefen alleen gemiste zinnen opnieuw") }
            } else {
                Text(
                    "Alle gekozen zinnen zelf als goed beoordeeld. " +
                        "Dit is oefenvoortgang, geen onafhankelijke taaltoets.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = ::begin, enabled = enabled) {
                Text("Begin volledige ronde opnieuw")
            }
        } else if (current != null) {
            val promptIsOriginal = promptSide == RecallPromptSide.ORIGINAL
            val promptLanguage = if (promptIsOriginal) sourceCode else targetCode
            val answerLanguage = if (promptIsOriginal) targetCode else sourceCode
            val question = if (promptIsOriginal) current.original else current.translated
            val reference = if (promptIsOriginal) current.translated else current.original
            Text(
                "Ronde $round · zin ${position + 1}/${currentIndices.size}" +
                    " · ${promptLanguage.uppercase()} → ${answerLanguage.uppercase()}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(question, style = MaterialTheme.typography.bodyLarge)
            if (!revealed) {
                Text("Je kunt typen, zelf nadenken of bewust één lokaal antwoord inspreken. " +
                    "Niet automatisch beoordeeld; geen cloudspraak of modeldownload." +
                        if (strictOfflineRadioOnly)
                            " Strikte vliegtuigmodus geldt ook voor je antwoordmicrofoon."
                        else " Androids oproep-, mute- en headsetmicrofoonbeleid gelden ook hier.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        enabled = enabled && !recordingAnswer,
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                                PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                speaker.stop()
                                spokenAnswer = ""
                                speechNotice = ""
                                recordingAnswer = recallSpeech.start(
                                    answerLanguage,
                                    strictOfflineRadioOnly = strictOfflineRadioOnly,
                                    onNotice = { notice ->
                                        speechNotice = notice
                                        if (!recallSpeech.isRecording()) recordingAnswer = false
                                    },
                                    onFinalAnswer = { answer ->
                                        spokenAnswer = answer
                                        recordingAnswer = false
                                    }
                                )
                            }
                        }
                    ) { Text("🎙 Spreek mijn antwoord in") }
                    if (recordingAnswer) {
                        TextButton(onClick = {
                            recallSpeech.cancel()
                            recordingAnswer = false
                            spokenAnswer = ""
                            speechNotice = "Opname gestopt. Geen gesproken antwoord bewaard."
                        }) { Text("■ Stop opname") }
                    }
                }
                if (permissionNotice.isNotBlank()) {
                    Text(permissionNotice, style = MaterialTheme.typography.bodySmall)
                }
                if (speechNotice.isNotBlank()) {
                    Text(speechNotice, style = MaterialTheme.typography.bodySmall)
                }
                if (spokenAnswer.isNotBlank()) {
                    Text("Mijn gesproken poging · alleen tijdelijk",
                        style = MaterialTheme.typography.labelMedium)
                    Text(spokenAnswer, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = {
                        spokenAnswer = ""
                        recallSpeech.cancel()
                        recordingAnswer = false
                    }) { Text("Wis mijn gesproken poging") }
                }
                Text("Mijn eigen antwoord · alleen tijdelijk",
                    style = MaterialTheme.typography.labelMedium)
                PrivateRecallAnswerField(
                    value = typedAnswer,
                    onValueChange = { candidate ->
                        // The native Android EditText constrains the buffer
                        // before calling us; revalidate at this RAM boundary.
                        typedAnswer = candidate.replace('\n', ' ').replace('\r', ' ')
                            .filterNot { it.isISOControl() }.take(400)
                    },
                    enabled = enabled
                )
                Text(
                    "Maximaal 400 tekens. Autocorrectie en persoonlijk " +
                        "toetsenbordleren zijn voor dit veld uitgevraagd bij Android; " +
                        "toetsenbordapps kunnen hun eigen privacybeleid hebben.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (typedAnswer.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            typedAnswer = ""
                            speaker.stop()
                        },
                        enabled = enabled
                    ) { Text("Wis mijn getypte antwoord") }
                }
                Text(
                    "Je eigen antwoord wordt niet nagekeken of opgeslagen. " +
                        "De referentie blijft verborgen tot jij op Onthul tikt.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = {
                        speaker.stop()
                        recallSpeech.cancel()
                        recordingAnswer = false
                        revealed = true
                        audioNotice = ""
                    },
                    enabled = enabled,
                    modifier = Modifier.semantics {
                        contentDescription =
                            "Onthul bestaande referentie voor zin ${position + 1}"
                    }
                ) { Text("Onthul referentie") }
            } else {
                if (spokenAnswer.isNotBlank()) {
                    Text("Mijn eerder gesproken poging · niet automatisch beoordeeld",
                        style = MaterialTheme.typography.labelMedium)
                    Text(spokenAnswer, style = MaterialTheme.typography.bodyLarge)
                }
                if (typedAnswer.isNotBlank()) {
                    Text("Mijn getypte poging · niet automatisch beoordeeld",
                        style = MaterialTheme.typography.labelMedium)
                    Text(typedAnswer, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Geen getypt antwoord · alleen in gedachten geoefend.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    if (promptIsOriginal)
                        "Bestaande lokale vertaling · kan fouten bevatten"
                    else "Oorspronkelijke zin · referentie, geen beoordeling",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(reference, style = MaterialTheme.typography.bodyLarge)
                Text("Vergelijk zelf; de app geeft geen automatische score of antwoordanalyse.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { mark(false) }, enabled = enabled) {
                        Text("Zelf goed")
                    }
                    TextButton(onClick = { mark(true) }, enabled = enabled) {
                        Text("Nog oefenen")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = enabled,
                    onClick = {
                        recallSpeech.cancel()
                        recordingAnswer = false
                        speaker.speak(
                            question, promptLanguage, LocalSpeechPace.NORMAL
                        ) { audioNotice = it }
                    }
                ) { Text("▶ Luister vraag") }
                if (revealed) {
                    TextButton(
                        enabled = enabled,
                        onClick = {
                            recallSpeech.cancel()
                            recordingAnswer = false
                            speaker.speak(
                                reference, answerLanguage, LocalSpeechPace.NORMAL
                            ) { audioNotice = it }
                        }
                    ) { Text("▶ Luister referentie") }
                }
            }
            if (audioNotice.isNotBlank()) {
                Text(audioNotice, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = ::begin, enabled = enabled) {
                Text("Stop en begin opnieuw")
            }
        }
    }
}
