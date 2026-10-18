package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalCaptionSpeaker
import kotlin.random.Random

/**
 * A one-time self-rated practice round over the CURRENT in-memory captions.
 * Never stores a response, promotes model text to a vetted phrase, or invents
 * a target sentence while translation is still pending.
 */
private data class SessionPracticeRound(
    val pending: List<FocusCaptionLine>,
    val total: Int,
    val retryIds: Set<Long> = emptySet(),
    val revealed: Boolean = false,
    val attempts: Int = 0,
    val remembered: Int = 0,
    val stillHard: Int = 0
) {
    val current: FocusCaptionLine? get() = pending.firstOrNull()

    fun reveal(): SessionPracticeRound =
        if (current == null) this else copy(revealed = true)

    fun grade(knew: Boolean): SessionPracticeRound {
        val caption = current ?: return this
        if (!revealed) return this
        val following = pending.drop(1)
        return if (knew) {
            copy(pending = following, revealed = false, attempts = attempts + 1,
                remembered = remembered + 1)
        } else if (caption.id !in retryIds) {
            // Just one local revisit; never create an endless practice loop.
            copy(pending = following + caption, revealed = false,
                retryIds = retryIds + caption.id, attempts = attempts + 1)
        } else {
            copy(pending = following, revealed = false, attempts = attempts + 1,
                stillHard = stillHard + 1)
        }
    }

    companion object {
        const val BATCH_SIZE = 12
        fun start(captions: List<FocusCaptionLine>, offset: Int): SessionPracticeRound {
            if (captions.isEmpty()) return SessionPracticeRound(emptyList(), 0)
            val first = offset.coerceAtLeast(0) % captions.size
            val group = (0 until minOf(BATCH_SIZE, captions.size))
                .map { captions[(first + it) % captions.size] }
            return SessionPracticeRound(group, group.size)
        }
    }
}

/**
 * Explicit learning from genuinely finished on-device bilingual text.
 * The transcript is never copied into a private deck, exported or persisted.
 * Callers must stop live microphone capture before opening this dialog.
 */
@Composable
internal fun SessionCaptionStudyDialog(
    captions: List<FocusCaptionLine>,
    onDismiss: () -> Unit,
    initialCaptionId: Long? = null
) {
    val allFinished = remember(captions) {
        captions.asSequence()
            .filter { it.status == "Lokaal vertaald" &&
                it.original.isNotBlank() && it.translated.isNotBlank() &&
                it.original != it.translated &&
                it.original.length <= 400 && it.translated.length <= 400
            }
            .distinctBy { it.id }
            .toList().takeLast(80)
    }
    // Speaker identities are NEVER inferred. These are solely labels that the
    // person explicitly set in the active conversation.
    var speakerChoice by remember { mutableStateOf<String?>(null) }
    val available = remember(allFinished, speakerChoice) {
        if (speakerChoice == null) allFinished
        else allFinished.filter { line ->
            if (speakerChoice == "?") line.speaker == null
            else line.speaker == speakerChoice
        }
    }
    var shuffleVersion by remember { mutableIntStateOf(0) }
    var batchOffset by remember(initialCaptionId, available) {
        mutableIntStateOf(available.indexOfFirst { it.id == initialCaptionId }.coerceAtLeast(0))
    }
    val deck = remember(available, shuffleVersion) {
        if (shuffleVersion == 0) available
        else available.shuffled(Random(shuffleVersion))
    }
    var round by remember(deck, batchOffset) {
        mutableStateOf(SessionPracticeRound.start(deck, batchOffset))
    }
    var reverse by remember { mutableStateOf(false) }
    var speechNotice by remember { mutableStateOf("") }
    val current = round.current
    val prompt = if (reverse) current?.translated.orEmpty() else current?.original.orEmpty()
    val answer = if (reverse) current?.original.orEmpty() else current?.translated.orEmpty()
    val promptCode = if (reverse) current?.targetCode.orEmpty() else current?.sourceCode.orEmpty()
    val answerCode = if (reverse) current?.sourceCode.orEmpty() else current?.targetCode.orEmpty()
    fun dir(tag: String) = if (tag.substringBefore('-') in
        setOf("ar", "fa", "ur", "he", "ps", "ckb")) TextDirection.Rtl
    else TextDirection.Ltr

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    DisposableEffect(lifecycleOwner, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    LaunchedEffect(current?.id, current?.original, current?.translated,
        reverse, round.revealed, round.attempts, shuffleVersion, batchOffset) {
        speaker.stop()
        speechNotice = ""
    }

    AlertDialog(
        onDismissRequest = { speaker.stop(); onDismiss() },
        title = { Text("Oefen dit gesprek · tijdelijk") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Oefen uitsluitend daadwerkelijk lokaal afgeronde vertalingen " +
                        "uit dit gesprek. Geen correctheidscontrole, automatische " +
                        "opslag of nieuwe download. Wis gesprek verwijdert de bron.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("${available.size} afgeronde tweetalige zinnen · maximaal 80 " +
                    "meest recente · groepjes van ${SessionPracticeRound.BATCH_SIZE}",
                    style = MaterialTheme.typography.labelMedium)
                if (allFinished.any { it.speaker == "A" || it.speaker == "B" }) {
                    Text("Optioneel: oefen alleen een handmatig gelabelde spreker.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(null to "Iedereen", "A" to "A", "B" to "B",
                            "?" to "Onbekend").forEach { (choice, label) ->
                            TextButton(
                                enabled = speakerChoice != choice,
                                onClick = { speakerChoice = choice }
                            ) { Text(if (speakerChoice == choice) "$label ✓" else label) }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { reverse = !reverse },
                        modifier = Modifier.weight(1f)) {
                        Text(if (reverse) "Vertaling → origineel ↔"
                            else "Origineel → vertaling ↔")
                    }
                    TextButton(onClick = {
                        shuffleVersion++
                        batchOffset = 0
                    }, enabled = deck.size > 1) { Text("↻ Schud") }
                }
                if (available.isEmpty()) {
                    Text(if (allFinished.isEmpty())
                        "Geen afzonderlijk lokaal voltooide vertaling beschikbaar. " +
                            "Herkenning alleen of een onvoltooide vertaling is geen leerantwoord."
                        else "Voor dit handmatige sprekerlabel zijn geen afgeronde " +
                            "tweetalige leerkaarten. Kies Iedereen.")
                } else if (current != null) {
                    Text("Beoordeeld ${round.attempts} keer · " +
                        "nog ${round.pending.size} kaart(en) inclusief eventuele herkansing",
                        style = MaterialTheme.typography.labelMedium)
                    HorizontalDivider()
                    Text("Vraag · ${promptCode.uppercase()}",
                        style = MaterialTheme.typography.labelLarge)
                    Text(prompt, modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = dir(promptCode)
                        ))
                    if (round.revealed) {
                        HorizontalDivider()
                        Text("Modelvertaling · ${answerCode.uppercase()} · niet geverifieerd",
                            style = MaterialTheme.typography.labelLarge)
                        Text(answer, modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = dir(answerCode),
                                color = MaterialTheme.colorScheme.primary
                            ))
                    } else {
                        Text("Bedenk eerst je antwoord; de vertaling en het " +
                            "voorlezen ervan blijven verborgen tot je tikt.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    OfflinePhraseSpeechActions(
                        original = prompt,
                        originalLanguage = promptCode,
                        meaning = answer,
                        meaningLanguage = answerCode,
                        originalLabel = "Vraag",
                        meaningLabel = "Antwoord",
                        speaker = speaker,
                        onNotice = { speechNotice = it },
                        meaningReady = round.revealed
                    )
                    if (speechNotice.isNotBlank()) {
                        Text(speechNotice, style = MaterialTheme.typography.bodySmall)
                    }
                    if (round.revealed) {
                        Text("Jouw eigen oordeel, geen gevalideerde vertaalscore.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { round = round.grade(false) },
                                modifier = Modifier.weight(1f)) { Text("Nog oefenen") }
                            Button(onClick = { round = round.grade(true) },
                                modifier = Modifier.weight(1f)) { Text("Ik wist het") }
                        }
                    } else {
                        Button(onClick = { round = round.reveal() },
                            modifier = Modifier.fillMaxWidth()) { Text("Toon vertaling") }
                    }
                } else {
                    Text("Deze oefenronde is klaar.",
                        style = MaterialTheme.typography.titleLarge)
                    Text("${round.remembered} keer zelf herkend · " +
                        "${round.stillHard} na herkansing nog lastig. " +
                        "Resultaten verdwijnen bij sluiten.")
                    OutlinedButton(onClick = {
                        round = SessionPracticeRound.start(deck, batchOffset)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Oefen deze groep opnieuw")
                    }
                    if (deck.size > SessionPracticeRound.BATCH_SIZE) {
                        Button(onClick = {
                            batchOffset = (batchOffset + SessionPracticeRound.BATCH_SIZE) % deck.size
                            round = SessionPracticeRound.start(deck, batchOffset)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Volgende gesprekszinnen oefenen")
                        }
                    }
                }
                Text("Alleen reeds geïnstalleerde, door Android als offline " +
                    "gemelde stemmen. Geen opname, geen vertaalcontrole, " +
                    "geen externe opslag en geen audio-woordtiming.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { speaker.stop(); onDismiss() }) {
                Text("Sluiten zonder opslaan")
            }
        }
    )
}
