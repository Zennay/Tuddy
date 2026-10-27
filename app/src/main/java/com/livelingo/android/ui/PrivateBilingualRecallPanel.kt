package com.livelingo.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import com.livelingo.android.PrivateRecallSpeechAttempt
import com.livelingo.android.PrivateRecallRoundReview
import com.livelingo.android.RecallSelfAssessment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    // RAM-only self-review; no learner text or speech is recorded here.
    var review by remember(practice, sourceCode, targetCode) {
        mutableStateOf(PrivateRecallRoundReview())
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
    // Only bounded final Android ASR alternatives from this exact recall
    // attempt. The learner chooses before revealing the reference.
    var speechChoices by remember(practice, sourceCode, targetCode) {
        mutableStateOf(emptyList<String>())
    }
    // Explicit post-reveal compare of exactly one currently visible answer.
    // Not persisted and never feeds the learner's manual self-mark.
    var comparisonSource by remember(practice, sourceCode, targetCode) {
        mutableStateOf<String?>(null)
    }
    var recordingAnswer by remember(practice, sourceCode, targetCode) {
        mutableStateOf(false)
    }
    var speechNotice by remember(practice, sourceCode, targetCode) {
        mutableStateOf("")
    }
    var permissionNotice by remember { mutableStateOf("") }
    // The previous recall marks/position may survive concealment, but neither
    // the current answer nor its revealed reference is shown until the learner
    // consciously resumes the private round from this foreground screen.
    var privateRoundConcealed by remember(practice, sourceCode, targetCode) {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // A permission result can arrive after the learner concealed this
        // exercise or after another panel took over. Never resurface the
        // previous draft or expose the screen from this asynchronous callback.
        if (privateRoundConcealed || !enabled) {
            permissionNotice = ""
        } else permissionNotice = if (granted)
            "Microfoon toegestaan. Tik opnieuw op 'Spreek mijn antwoord in' om lokaal te beginnen."
        else "Microfoon niet toegestaan. Je kunt gewoon typen of nadenken."
    }
    var promptSide by remember(practice, sourceCode, targetCode) {
        mutableStateOf(RecallPromptSide.ORIGINAL)
    }
    // Android 13+ may list multiple independently installed regional ASR packs
    // for a broad ML Kit text language (e.g., en -> en-GB / en-US).
    // Never persist these choices; require an explicit second tap to record.
    var installedAnswerSpeechTags by remember(
        practice, sourceCode, targetCode, promptSide
    ) { mutableStateOf(emptyList<String>()) }
    var chosenAnswerSpeechTag by remember(
        practice, sourceCode, targetCode, promptSide
    ) { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    fun concealPrivateRecall() {
        speaker.stop()
        recallSpeech.cancel()
        recordingAnswer = false
        typedAnswer = ""
        spokenAnswer = ""
        speechChoices = emptyList()
        comparisonSource = null
        audioNotice = ""
        speechNotice = ""
        permissionNotice = ""
        revealed = false
        privateRoundConcealed = true
    }
    DisposableEffect(speaker, practice, sourceCode, targetCode, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE ||
                event == Lifecycle.Event.ON_STOP) {
                // The entire prompt, reference and native IME must be hidden
                // before another app / lock screen may become foreground.
                // Preserve only the user's self-marked round progress.
                concealPrivateRecall()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.stop()
            recallSpeech.cancel()
            typedAnswer = ""
            spokenAnswer = ""
            speechChoices = emptyList()
            comparisonSource = null
            recordingAnswer = false
        }
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            // Even if the panel remains composed beneath another screen,
            // remove the visible reference, current prompt and native IME.
            concealPrivateRecall()
        }
    }

    if (privateRoundConcealed || !enabled) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider()
            Text(
                "Privé overhoorronde afgeschermd",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "De oefenzin, onthulde referentie en tijdelijke getypte of " +
                    "gesproken antwoorden zijn verborgen. Audio en microfoon " +
                    "zijn gestopt. Eerder zelf beoordeelde zinnen blijven " +
                    "alleen in de huidige RAM-ronde beschikbaar.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                enabled = enabled &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
                onClick = {
                    // A deliberate foreground tap resumes on the same
                    // position. No previous answer or reference is restored.
                    revealed = false
                    privateRoundConcealed = false
                }
            ) { Text("Hervat privé oefening bewust") }
            TextButton(
                enabled = enabled,
                onClick = {
                    // Delete even self-reported grades and sequence position,
                    // with NO briefly revealed prompt or attempted answer.
                    concealPrivateRecall()
                    started = false
                    position = 0
                    review = PrivateRecallRoundReview()
                    retryOnly = false
                    retryIndices = emptyList()
                    round = 1
                }
            ) { Text("Wis deze tijdelijke oefenronde") }
        }
        return
    }

    val currentIndices = if (retryOnly) retryIndices else practice.indices.toList()
    val missed = review.missed(currentIndices)
    val current = currentIndices.getOrNull(position)?.let(practice::get)
    val done = started && current == null && currentIndices.isNotEmpty()

    fun begin() {
        speaker.stop()
        recallSpeech.cancel()
        recordingAnswer = false
        spokenAnswer = ""
        speechChoices = emptyList()
        comparisonSource = null
        speechNotice = ""
        started = true
        position = 0
        revealed = false
        review = PrivateRecallRoundReview()
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
        speechChoices = emptyList()
        comparisonSource = null
        speechNotice = ""
        promptSide = side
        started = false
        position = 0
        revealed = false
        review = PrivateRecallRoundReview()
        retryOnly = false
        retryIndices = emptyList()
        round = 1
        audioNotice = ""
        typedAnswer = ""
    }

    fun mark(assessment: RecallSelfAssessment) {
        val index = currentIndices.getOrNull(position) ?: return
        if (!started || !enabled) return
        speaker.stop()
        recallSpeech.cancel()
        recordingAnswer = false
        spokenAnswer = ""
        speechChoices = emptyList()
        comparisonSource = null
        speechNotice = ""
        review = review.mark(index, currentIndices, assessment)
        position++
        revealed = false
        audioNotice = ""
        typedAnswer = ""
    }

    /**
     * Undo only the preceding SELF mark, not a private typed/spoken answer.
     * Deliberately reveal no reference on return; a learner chooses again.
     */
    fun revisePrevious() {
        if (!started || !enabled || position <= 0) return
        val preceding = currentIndices.getOrNull(position - 1) ?: return
        speaker.stop()
        recallSpeech.cancel()
        recordingAnswer = false
        spokenAnswer = ""
        speechChoices = emptyList()
        typedAnswer = ""
        comparisonSource = null
        speechNotice = ""
        audioNotice = ""
        review = review.undo(preceding)
        position--
        revealed = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Zelf overhoren · eerst herinneren, daarna antwoord onthullen",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { concealPrivateRecall() }) {
                Text("Verberg privé oefening")
            }
        }
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
        Text(
            "Sla desgewenst een zin over of herzie via Vorige zin je laatste " +
                "beoordeling. Eerdere getypte en gesproken antwoorden komen " +
                "niet terug; de referentie wordt opnieuw verborgen.",
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
                "Ronde $round voltooid · ${review.goodCount(currentIndices)} zelf goed " +
                    "· ${review.needsPracticeCount(currentIndices)} nog oefenen " +
                    "· ${review.skippedCount(currentIndices)} overgeslagen.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (missed.isNotEmpty()) {
                OutlinedButton(
                    enabled = enabled,
                    onClick = {
                        speaker.stop()
                        retryIndices = missed.sorted()
                        retryOnly = true
                        comparisonSource = null
                        spokenAnswer = ""
                        speechChoices = emptyList()
                        review = PrivateRecallRoundReview()
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
            TextButton(onClick = ::revisePrevious, enabled = enabled && position > 0) {
                Text("← Herzie laatste beoordeling vóór afronden")
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
            Text(
                "Zelf beoordeeld: ${review.goodCount(currentIndices)} goed · " +
                    "${review.needsPracticeCount(currentIndices)} opnieuw · " +
                    "${review.skippedCount(currentIndices)} overgeslagen.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(question, style = MaterialTheme.typography.bodyLarge)
            if (!revealed) {
                Text("Je kunt typen, zelf nadenken of bewust één lokaal antwoord inspreken. " +
                    "Android 13+ controleert eerst het lokale spraakpakket voor deze " +
                    "antwoordtaal, zonder download. Android 12 kan de pakketstatus " +
                    "niet bewijzen. Niet automatisch beoordeeld; geen cloudspraak." +
                        if (strictOfflineRadioOnly)
                            " Strikte vliegtuigmodus geldt ook voor je antwoordmicrofoon."
                        else " Androids oproep-, mute- en headsetmicrofoonbeleid gelden ook hier.",
                    style = MaterialTheme.typography.bodySmall)
                if (installedAnswerSpeechTags.isNotEmpty()) {
                    Text(
                        "Kies één door Android bevestigd geïnstalleerd spraakpakket " +
                            "voor ${answerLanguage.uppercase()}. De gekozen regio geldt " +
                            "alleen voor deze oefenrichting en is geen nauwkeurigheidsgarantie.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                        items(installedAnswerSpeechTags, key = { it }) { installedTag ->
                            FilterChip(
                                selected = chosenAnswerSpeechTag == installedTag,
                                enabled = enabled && !recordingAnswer,
                                onClick = {
                                    chosenAnswerSpeechTag = installedTag
                                    speechNotice = "Pakket $installedTag gekozen. " +
                                        "Tik opnieuw bewust op Spreek mijn antwoord in."
                                },
                                label = { Text("Geïnstalleerde regio $installedTag") }
                            )
                        }
                    }
                }
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
                                speechChoices = emptyList()
                                comparisonSource = null
                                speechNotice = ""
                                recordingAnswer = recallSpeech.start(
                                    chosenAnswerSpeechTag ?: answerLanguage,
                                    strictOfflineRadioOnly = strictOfflineRadioOnly,
                                    onNotice = { notice ->
                                        speechNotice = notice
                                        // Admission is asynchronous on Android 13+;
                                        // reflect both preflight and actual mic state.
                                        recordingAnswer = recallSpeech.isRecording()
                                    },
                                    onFinalAnswer = { options ->
                                        // These are suggestions from Android,
                                        // never verified correctness or a grade.
                                        speechChoices = options
                                        spokenAnswer = options.firstOrNull().orEmpty()
                                        comparisonSource = null
                                        recordingAnswer = false
                                    },
                                    onVariantSelectionRequired = { installedTags ->
                                        // Only tags from Android's INSTALLED speech
                                        // list, never downloadables or text models.
                                        // No microphone begins until user selects
                                        // a region and explicitly taps Play again.
                                        installedAnswerSpeechTags = installedTags
                                        chosenAnswerSpeechTag = null
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
                            speechChoices = emptyList()
                            comparisonSource = null
                            speechNotice = "Spraakpakketcontrole of opname gestopt. " +
                                "Geen gesproken antwoord bewaard."
                        }) { Text("■ Stop controle/opname") }
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
                    if (speechChoices.size > 1) {
                        Text(
                            "Android gaf meerdere lokale transcripties. Kies wat je " +
                                "daadwerkelijk uitsprak; geen automatische beoordeling.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        speechChoices.forEachIndexed { index, choice ->
                            FilterChip(
                                selected = spokenAnswer == choice,
                                enabled = enabled && !recordingAnswer && !revealed,
                                onClick = {
                                    speaker.stop()
                                    spokenAnswer = choice
                                    comparisonSource = null
                                    audioNotice = ""
                                    speechNotice = "Herkenningsoptie ${index + 1} gekozen. " +
                                        "Jij bepaalt of dit klopt; onthul daarna de referentie."
                                },
                                label = {
                                    Text("Optie ${index + 1} · $choice", maxLines = 3)
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .semantics {
                                        contentDescription =
                                            "Android-herkenningsoptie ${index + 1} van " +
                                                "${speechChoices.size}: $choice"
                                    }
                            )
                        }
                    }
                    TextButton(onClick = {
                        spokenAnswer = ""
                        speechChoices = emptyList()
                        comparisonSource = null
                        recallSpeech.cancel()
                        recordingAnswer = false
                    }) { Text("Wis mijn gesproken poging") }
                }
                Text("Mijn eigen antwoord · alleen tijdelijk",
                    style = MaterialTheme.typography.labelMedium)
                PrivateRecallAnswerField(
                    value = typedAnswer,
                    onValueChange = { candidate ->
                        // A spoken version of an older draft must not continue
                        // when the learner changes their private answer.
                        speaker.stop()
                        audioNotice = ""
                        // The native Android EditText constrains the buffer
                        // before calling us; revalidate at this RAM boundary.
                        typedAnswer = candidate.replace('\n', ' ').replace('\r', ' ')
                            .filterNot { it.isISOControl() }.take(400)
                        comparisonSource = null
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
                            comparisonSource = null
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
                TextButton(
                    enabled = enabled,
                    onClick = { mark(RecallSelfAssessment.SKIPPED) }
                ) { Text("Sla over · later oefenen") }
                OutlinedButton(
                    onClick = {
                        speaker.stop()
                        recallSpeech.cancel()
                        recordingAnswer = false
                        revealed = true
                        comparisonSource = null
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
                Text(
                    "Jij beoordeelt het antwoord. Je kunt daarnaast vrijwillig de " +
                        "letterlijke woorden vergelijken; dat is geen betekenis- of " +
                        "uitspraakcontrole.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (typedAnswer.isNotBlank() || spokenAnswer.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (typedAnswer.isNotBlank()) {
                            TextButton(onClick = {
                                comparisonSource = if (comparisonSource == "typed") null else "typed"
                            }, enabled = enabled) {
                                Text(if (comparisonSource == "typed")
                                    "Verberg getypte woordvergelijking" else "Vergelijk getypte woorden")
                            }
                        }
                        if (spokenAnswer.isNotBlank()) {
                            TextButton(onClick = {
                                comparisonSource = if (comparisonSource == "spoken") null else "spoken"
                            }, enabled = enabled) {
                                Text(if (comparisonSource == "spoken")
                                    "Verberg gesproken woordvergelijking" else "Vergelijk gesproken woorden")
                            }
                        }
                    }
                    if (comparisonSource == "typed" && typedAnswer.isNotBlank()) {
                        PrivateRecallWordComparisonCard(
                            learnerAttempt = typedAnswer,
                            revealedReference = reference,
                            attemptLabel = "getypt"
                        )
                    } else if (comparisonSource == "spoken" && spokenAnswer.isNotBlank()) {
                        PrivateRecallWordComparisonCard(
                            learnerAttempt = spokenAnswer,
                            revealedReference = reference,
                            attemptLabel = "lokaal herkende spraak"
                        )
                    }
                    // Only rendered AFTER explicit Onthul. Never queue an
                    // undisclosed reference or an unsubmitted learner draft.
                    PrivateRecallAuditoryComparisonPanel(
                        typedAttempt = typedAnswer,
                        spokenAttempt = spokenAnswer,
                        revealedReference = reference,
                        answerLanguageTag = answerLanguage,
                        speaker = speaker,
                        enabled = enabled && !recordingAnswer,
                        onNotice = { audioNotice = it }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { mark(RecallSelfAssessment.SELF_GOOD) }, enabled = enabled) {
                        Text("Zelf goed")
                    }
                    TextButton(onClick = { mark(RecallSelfAssessment.TRY_AGAIN) }, enabled = enabled) {
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
            if (position > 0) {
                TextButton(onClick = ::revisePrevious, enabled = enabled) {
                    Text("← Vorige zin · herzie mijn beoordeling")
                }
            }
            TextButton(onClick = ::begin, enabled = enabled) {
                Text("Stop en begin opnieuw")
            }
        }
    }
}
