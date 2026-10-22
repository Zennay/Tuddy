package com.livelingo.android.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.OwnWord
import com.livelingo.android.SavedWord
import com.livelingo.android.PrivateVocabularyDeck
import com.livelingo.android.PrivateVocabularyStudySheet
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import java.io.ByteArrayOutputStream

/**
 * Follow only the active saved word and only when Android's installed local
 * TTS voice reports a text range. No range is inferred from timing.
 */
@Composable
private fun SavedWordFollowText(
    word: SavedWord,
    activeSpeechKey: String?,
    activeSpeechText: String?,
    activeSpeechRange: Pair<Int, Int>?
): AnnotatedString {
    val key = "${word.language}|${word.word}"
    val normalized = word.word.replace('\n', ' ').replace('\r', ' ').trim()
    val followsSpeech = activeSpeechKey == key &&
        activeSpeechText != null && normalized == activeSpeechText
    val displayedWord = if (followsSpeech) activeSpeechText.orEmpty() else word.word
    val range = activeSpeechRange.takeIf { followsSpeech }
    val prefix = "${word.entry.illustration} "
    val suffix = " · ${word.language}"
    val background = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onPrimary
    return remember(prefix, displayedWord, suffix, range, background, foreground) {
        buildAnnotatedString {
            append(prefix)
            val offset = length
            append(displayedWord)
            append(suffix)
            if (range != null && range.first >= 0 &&
                range.second > range.first && range.second <= displayedWord.length
            ) {
                addStyle(
                    SpanStyle(
                        background = background,
                        color = foreground,
                        fontWeight = FontWeight.Bold
                    ),
                    offset + range.first,
                    offset + range.second
                )
            }
        }
    }
}

/**
 * Offline review/delete screen. Optional date and interval metadata persist only
 * after separate consent; curated word keys are the only study content stored.
 * A full conversation is never loaded from or saved to device storage.
 */
@Composable
internal fun SavedWordsPanel(
    deck: PrivateVocabularyDeck,
    revision: Int,
    onChanged: () -> Unit,
    onDismiss: () -> Unit,
    allowSpeech: Boolean = true,
    incomingBackupJsonText: String? = null,
    onIncomingBackupJsonConsumed: (String) -> Unit = {}
) {
    val allWords = remember(revision) { deck.entries() }
    var search by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(emptySet<String>()) }
    val searchTerm = search.trim()
    val words = if (searchTerm.isBlank()) allWords else allWords.filter {
        it.word.contains(searchTerm, ignoreCase = true) ||
            it.language.contains(searchTerm, ignoreCase = true)
    }
    val reviewDates = if (deck.reviewsEnabled()) deck.scheduledReviews().associateBy {
        "${it.saved.language}|${it.saved.word}"
    } else emptyMap()
    val localToday = java.time.LocalDate.now().toEpochDay()
    var confirmDelete by remember { mutableStateOf(false) }
    var practiceOpen by remember { mutableStateOf(false) }
    var quizOpen by remember { mutableStateOf(false) }
    var quizScope by remember { mutableStateOf<List<OwnWord>?>(null) }
    var scheduledReviewOpen by remember { mutableStateOf(false) }
    var confirmStartReview by remember { mutableStateOf(false) }
    var confirmEraseReview by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingShareSnapshot by remember { mutableStateOf<Pair<Int, ByteArray>?>(null) }
    var pendingStudySheet by remember { mutableStateOf<Pair<Int, ByteArray>?>(null) }
    var pendingImport by remember { mutableStateOf<List<String>?>(null) }
    var transferStatus by remember { mutableStateOf("") }
    val context = LocalContext.current
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val snapshot = pendingExport
        pendingExport = null
        if (uri != null && snapshot != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(snapshot)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            transferStatus = if (saved) "Woordenback-up opgeslagen op jouw gekozen locatie."
                else "Opslaan mislukt; alle lokaal bewaarde woorden blijven behouden."
        }
    }
    val saveStudySheet = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = pendingStudySheet
        pendingStudySheet = null
        if (uri != null && pending != null && pending.first == revision) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(pending.second)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            transferStatus = if (saved)
                "Eigen woordenblad opgeslagen op jouw gekozen locatie."
            else "Woordenblad opslaan is mislukt. Je oorspronkelijke woorden blijven behouden."
        } else if (uri != null) {
            transferStatus = "De woordselectie is gewijzigd; kies opnieuw Bewaar woordenblad."
        }
    }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImport = null
        if (uri != null) {
            val candidate = runCatching {
                val output = ByteArrayOutputStream()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val chunk = ByteArray(4096)
                    while (true) {
                        val count = input.read(chunk)
                        if (count < 0) break
                        if (count == 0 || output.size() + count > 16_384) {
                            throw IllegalArgumentException("Invalid backup size")
                        }
                        output.write(chunk, 0, count)
                    }
                } ?: throw IllegalArgumentException("Unreadable provider")
                deck.previewImport(output.toByteArray())
            }.getOrNull()
            if (candidate == null) {
                transferStatus = "Bestand geweigerd: onbekend formaat, te groot of niet-erkende woorden. " +
                    "Je bestaande woorden zijn behouden."
            } else {
                pendingImport = candidate
                transferStatus = "Bestand gecontroleerd. Vervangen gebeurt pas na jouw bevestiging."
            }
        }
    }

    LaunchedEffect(incomingBackupJsonText) {
        val shared = incomingBackupJsonText ?: return@LaunchedEffect
        val bytes = shared.toByteArray(Charsets.UTF_8)
        if (pendingImport != null || pendingExport != null || pendingShareSnapshot != null ||
            pendingStudySheet != null || practiceOpen || quizOpen || scheduledReviewOpen) {
            transferStatus = "Er staat al een woordenlijstimport of export open. Sluit die eerst " +
                "en deel de back-up opnieuw; je huidige woorden blijven behouden."
        } else {
            val candidate = if (bytes.isNotEmpty() && bytes.size <= 16_384 &&
                bytes.toString(Charsets.UTF_8) == shared) deck.previewImport(bytes) else null
            if (candidate == null) {
                transferStatus = "Gedeelde Tuddy-woordenlijstback-up geweigerd: ongeldige JSON/UTF-8, " +
                    "dubbele sleutels, onbekende versie, niet-herkende woorden of groter dan 16 KB. " +
                    "Je huidige woorden zijn behouden."
            } else {
                pendingImport = candidate
                transferStatus = "Gedeelde Tuddy-woordenlijstback-up gecontroleerd. Bekijk de volledige " +
                    "lijst en bevestig apart als je de huidige woorden wilt vervangen."
            }
        }
        onIncomingBackupJsonConsumed(shared)
    }


    if (practiceOpen) {
        WordPracticePanel(
            savedWords = allWords,
            onDismiss = { practiceOpen = false },
            allowSpeech = allowSpeech
        )
        return
    }

    if (quizOpen) {
        OwnWordQuizDialog(
            notes = quizScope.orEmpty(),
            curated = true,
            onDismiss = { quizOpen = false; quizScope = null }
        )
        return
    }

    if (scheduledReviewOpen) {
        ScheduledWordReviewPanel(
            deck = deck,
            onChanged = onChanged,
            onDismiss = { scheduledReviewOpen = false }
        )
        return
    }

    // One speaker for the visible word list, not a TTS engine per saved word.
    val owner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    var audioNotice by remember { mutableStateOf("") }
    var activeSpeechKey by remember { mutableStateOf<String?>(null) }
    var activeSpeechText by remember { mutableStateOf<String?>(null) }
    var activeSpeechRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    DisposableEffect(owner, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speaker.stop()
                activeSpeechKey = null
                activeSpeechText = null
                activeSpeechRange = null
                audioNotice = "Voorlezen gestopt bij verlaten van het scherm."
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    LaunchedEffect(search, revision, allowSpeech) {
        speaker.stop()
        activeSpeechKey = null
        activeSpeechText = null
        activeSpeechRange = null
        audioNotice = if (allowSpeech) "" else "Voorlezen uit terwijl de microfoon luistert."
    }

    fun pronounceSavedWord(word: OwnWord, pace: LocalSpeechPace) {
        val safe = word.word.replace('\n', ' ').replace('\r', ' ').trim()
        if (!allowSpeech || safe.isBlank() || safe.length > 400 ||
            safe.any { it.isISOControl() } || word.language.isBlank()
        ) {
            speaker.stop()
            activeSpeechKey = null
            activeSpeechText = null
            activeSpeechRange = null
            audioNotice = if (!allowSpeech)
                "Voorlezen uit terwijl de microfoon luistert."
            else "Dit woord kan niet veilig met een lokale stem worden voorgelezen."
            return
        }
        activeSpeechKey = "${word.language}|${word.word}"
        activeSpeechText = safe
        activeSpeechRange = null
        audioNotice = "Lokale voorlezing voorbereiden…"
        speaker.speakSequence(
            lines = listOf(safe),
            languageTag = word.language,
            pace = pace,
            onProgress = { completed, total ->
                audioNotice = "Lokaal voorlezen: $completed/$total regels."
            },
            feedback = { audioNotice = it },
            onFinished = { completed ->
                activeSpeechKey = null
                activeSpeechText = null
                activeSpeechRange = null
                if (completed) audioNotice = "Voorlezen voltooid."
            },
            onLineStarted = { activeSpeechRange = null },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }

    AlertDialog(
        onDismissRequest = { speaker.stop(); onDismiss() },
        title = { Text("Mijn opgeslagen woorden") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Alleen zelf bewaarde woorden uit de kleine gecontroleerde woordenlijst. " +
                        "Je gesprekken worden niet opgeslagen en niets gaat naar een server.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    enabled = allWords.isNotEmpty(),
                    onClick = {
                        speaker.stop()
                        activeSpeechKey = null
                        activeSpeechText = null
                        activeSpeechRange = null
                        practiceOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Oefen opgeslagen woorden") }
                OutlinedButton(
                    enabled = allWords.size >= 2,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        speaker.stop()
                        quizScope = allWords.map { OwnWord(it.language, it.word, it.entry.definition) }
                        quizOpen = true
                    }
                ) { Text("🎯 Quiz · bewaarde basiswoorden") }
                if (!deck.reviewsEnabled()) {
                    OutlinedButton(
                        enabled = allWords.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { speaker.stop(); confirmStartReview = true }
                    ) { Text("Start privé herhaalplanning…") }
                    Text(
                        "Alleen na jouw toestemming bewaart Tuddy een niveau en " +
                            "volgende herhaaldatum per bewaard woord, volledig lokaal.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val reviewWords = deck.scheduledReviews()
                    val dueCount = reviewWords.count {
                        it.dueDay <= java.time.LocalDate.now().toEpochDay()
                    }
                    OutlinedButton(
                        enabled = dueCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { speaker.stop(); scheduledReviewOpen = true }
                    ) { Text("Herhaal vandaag · $dueCount van ${reviewWords.size}") }
                    Text(
                        if (dueCount == 0) "Geen woorden vandaag. Later opnieuw openen."
                        else "Bekijk de betekenis en beoordeel zelf. Bekende woorden " +
                            "komen later terug, moeilijke eerder.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { confirmEraseReview = true }) {
                        Text("Wis mijn herhaalplanning")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = allWords.isNotEmpty(),
                        onClick = {
                            val snapshot = deck.exportSnapshot()
                            if (snapshot == null) {
                                transferStatus = "Geen geldige opgeslagen woorden om te bewaren."
                            } else {
                                pendingExport = snapshot
                                createBackup.launch("tuddy-mijn-woorden.json")
                            }
                        }
                    ) { Text("Bewaar back-up…") }
                    OutlinedButton(onClick = {
                        pendingImport = null
                        openBackup.launch(arrayOf("application/json", "text/plain"))
                    }) { Text("Herstel back-up…") }
                }
                OutlinedButton(
                    enabled = allWords.isNotEmpty() && pendingImport == null &&
                        pendingExport == null && pendingShareSnapshot == null &&
                        pendingStudySheet == null,
                    onClick = {
                        val snapshot = deck.exportSnapshot()
                        val keys = snapshot?.let(deck::previewImport)
                        if (snapshot == null || keys == null) {
                            transferStatus = "Geen geldige woordenback-up beschikbaar om te delen."
                        } else {
                            pendingShareSnapshot = revision to snapshot
                            transferStatus = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Deel JSON-back-up…") }
                Text(
                    "JSON-back-up: alleen gecontroleerde woordidentiteiten, nooit gesprekken. " +
                        "Een gekozen cloudbestandsprovider kan bestanden extern synchroniseren.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    enabled = allWords.isNotEmpty(),
                    onClick = {
                        val sheet = PrivateVocabularyStudySheet.snapshot(allWords)
                        if (sheet == null) {
                            transferStatus = "Geen veilig woordenblad beschikbaar voor deze selectie."
                        } else {
                            pendingStudySheet = revision to sheet
                            transferStatus = ""
                            saveStudySheet.launch("Tuddy-mijn-woordenblad.txt")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Bewaar leesbaar woordenblad…") }
                Text(
                    "Afzonderlijk leesbaar TXT met je zelf bewaarde woorden én " +
                        "de kleine, ingebouwde woordenboekbetekenissen. Alleen na jouw " +
                        "bestandskeuze; nooit automatische export van gesprek of oefenscores.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (transferStatus.isNotBlank()) Text(
                    transferStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(40) },
                    label = { Text("Zoek opgeslagen woord of taal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedButton(
                    enabled = words.size >= 2,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        speaker.stop()
                        quizScope = words.map { OwnWord(it.language, it.word, it.entry.definition) }
                        quizOpen = true
                    }
                ) { Text("Quiz met deze ${words.size} gevonden basiswoorden") }
                Text("${words.size} van ${allWords.size} woorden · betekenissen verborgen om te oefenen",
                    style = MaterialTheme.typography.bodySmall)
                if (audioNotice.isNotBlank()) {
                    Text(audioNotice, style = MaterialTheme.typography.bodySmall)
                }
                if (words.isEmpty()) {
                    Text(if (allWords.isEmpty())
                        "Nog geen woorden. Tik op een woord in een ondertitel en kies Bewaar woord."
                        else "Geen opgeslagen woorden met deze zoekterm.")
                }
                words.forEach { word ->
                    HorizontalDivider()
                    val key = "${word.language}|${word.word}"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (activeSpeechKey == key)
                            MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(
                            SavedWordFollowText(
                                word = word,
                                activeSpeechKey = activeSpeechKey,
                                activeSpeechText = activeSpeechText,
                                activeSpeechRange = activeSpeechRange
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (activeSpeechKey == key) {
                            Text(
                                if (activeSpeechRange != null)
                                    "Nu voorgelezen · indicatieve tekstmarkering van de stem."
                                else "Nu voorgelezen · deze stem geeft geen woordbereik terug.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        reviewDates[key]?.let { scheduled ->
                            val dueLabel = if (scheduled.dueDay <= localToday)
                                "Vandaag herhalen"
                            else "Opnieuw op " + java.time.LocalDate
                                .ofEpochDay(scheduled.dueDay).toString()
                            Text(dueLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        if (key in revealed) {
                            Text(word.entry.definition, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = {
                                revealed = if (key in revealed) revealed - key else revealed + key
                            }) { Text(if (key in revealed) "Verberg betekenis" else "Toon betekenis") }
                            TextButton(onClick = {
                                speaker.stop()
                                if (activeSpeechKey == key) {
                                    activeSpeechKey = null
                                    activeSpeechText = null
                                    activeSpeechRange = null
                                }
                                if (deck.remove(word.language, word.word)) {
                                    revealed = revealed - key
                                    onChanged()
                                }
                            }) { Text("Verwijder") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(enabled = allowSpeech && activeSpeechText == null, onClick = {
                                pronounceSavedWord(word, LocalSpeechPace.NORMAL)
                            }) { Text("🔊 Hoor woord") }
                            TextButton(enabled = allowSpeech && activeSpeechText == null, onClick = {
                                pronounceSavedWord(word, LocalSpeechPace.SLOW)
                            }) { Text("Rustig") }
                            TextButton(onClick = {
                                speaker.stop()
                                activeSpeechKey = null
                                activeSpeechText = null
                                activeSpeechRange = null
                                audioNotice = "Lokale voorlezing gestopt."
                            }) { Text("■ Stop") }
                        }
                    }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                speaker.stop()
                activeSpeechKey = null
                activeSpeechText = null
                activeSpeechRange = null
                onDismiss()
            }) { Text("Sluiten") }
        },
        dismissButton = {
            if (allWords.isNotEmpty()) {
                TextButton(onClick = { confirmDelete = true }) { Text("Wis alle woorden") }
            }
        }
    )

    if (confirmStartReview) {
        AlertDialog(
            onDismissRequest = { confirmStartReview = false },
            title = { Text("Lokale herhaalplanning starten?") },
            text = { Text(
                "Tuddy bewaart vanaf nu alleen herhaalniveau en volgende datum " +
                    "voor zelf opgeslagen basiswoorden op dit toestel. " +
                    "Geen gesprekken, account, cloudsync of meldingen. " +
                    "Je kunt dit later afzonderlijk wissen; woorden blijven dan bewaard."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    transferStatus = if (deck.enableReviews()) {
                        onChanged()
                        "Herhaalplanning gestart; alle opgeslagen woorden zijn nu beschikbaar."
                    } else "Herhaalplanning kon niet worden gestart."
                    confirmStartReview = false
                }) { Text("Start alleen lokaal") }
            },
            dismissButton = {
                TextButton(onClick = { confirmStartReview = false }) {
                    Text("Annuleren")
                }
            }
        )
    }

    if (confirmEraseReview) {
        AlertDialog(
            onDismissRequest = { confirmEraseReview = false },
            title = { Text("Herhaalplanning wissen?") },
            text = { Text(
                "Alle lokale herhaaldatums en niveaus worden gewist. " +
                    "Je opgeslagen woorden en bestaande oefenmodus blijven bestaan."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    val cleared = deck.disableReviews()
                    if (cleared) onChanged()
                    transferStatus = if (cleared) "Herhaalplanning definitief gewist."
                        else "Wissen mislukt; probeer opnieuw."
                    confirmEraseReview = false
                }) { Text("Wis planning") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEraseReview = false }) { Text("Annuleren") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Alle opgeslagen woorden wissen?") },
            text = { Text("Dit wist de lokale woordenlijst en de optionele herhaalplanning definitief, niet je tijdelijke gesprek.") },
            confirmButton = {
                TextButton(onClick = {
                    if (deck.clear()) onChanged()
                    confirmDelete = false
                }) { Text("Wis woorden") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuleren") }
            }
        )
    }

    val shareSnapshot = pendingShareSnapshot
    val shareCandidate = shareSnapshot?.second?.let(deck::previewImport)
    if (shareSnapshot != null) {
        AlertDialog(
            onDismissRequest = { pendingShareSnapshot = null },
            title = { Text("Deel opgeslagen woorden?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Je deelt een momentopname van ${shareCandidate?.size ?: 0} gecontroleerde " +
                            "woordidentiteiten. Alleen taalcode en woord worden gedeeld; geen " +
                            "definities, gesprekken, audio of oefengegevens. Android toont daarna " +
                            "de apps waar je de tekst zelf naartoe kunt sturen."
                    )
                    Text("Volledige lijst die wordt gedeeld:", style = MaterialTheme.typography.labelMedium)
                    if (shareCandidate == null) {
                        Text(
                            "De momentopname kan niet meer worden gelezen. Er wordt niets gedeeld.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        shareCandidate.forEachIndexed { index, key ->
                            HorizontalDivider()
                            Text("${index + 1}. ${key.substringBefore('|').uppercase()} · ${key.substringAfter('|')}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = shareCandidate != null,
                    onClick = {
                        val snapshot = pendingShareSnapshot
                        val keys = snapshot?.second?.let(deck::previewImport)
                        if (snapshot == null || keys == null) {
                            transferStatus = "De back-up kon niet veilig worden gedeeld."
                        } else if (snapshot.first != revision) {
                            transferStatus = "De woordenlijst is gewijzigd. Er is niets gedeeld; open de deelpreview opnieuw."
                        } else {
                            val launched = runCatching {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Tuddy-woordenlijstback-up")
                                    putExtra(Intent.EXTRA_TEXT, snapshot.second.toString(Charsets.UTF_8))
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Deel Tuddy-woordenlijstback-up")
                                )
                            }.isSuccess
                            transferStatus = if (launched)
                                "Android Delen geopend. Kies zelf een ontvanger; Tuddy uploadt niets."
                            else "Delen kon niet worden geopend. Je lokale woorden zijn ongewijzigd."
                        }
                        pendingShareSnapshot = null
                    }
                ) { Text("Deel via Android…") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingShareSnapshot = null
                    transferStatus = "Delen geannuleerd; er is niets verstuurd."
                }) { Text("Annuleren") }
            }
        )
    }

    val importCandidate = pendingImport
    if (importCandidate != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Opgeslagen woorden vervangen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Deze back-up bevat ${importCandidate.size} herkende woorden uit Tuddy's " +
                        "kleine lokale woordenlijst. Hiermee vervang je je huidige woorden. " +
                        "Gesprekken of eigen zinnen worden niet geïmporteerd. " +
                        "Een bestaande herhaalplanning wordt voor deze woorden opnieuw gestart.")
                    Text("Volledige lijst ter controle:", style = MaterialTheme.typography.labelMedium)
                    importCandidate.forEachIndexed { index, key ->
                        HorizontalDivider()
                        Text("${index + 1}. ${key.substringBefore('|').uppercase()} · ${key.substringAfter('|')}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val restored = deck.replaceImported(importCandidate)
                    if (restored) onChanged()
                    transferStatus = if (restored) "Woordenlijst lokaal hersteld."
                        else "Herstel mislukt; de oorspronkelijke woorden blijven behouden."
                    pendingImport = null
                }) { Text("Vervang woorden") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImport = null
                    transferStatus = "Herstel geannuleerd; niets vervangen."
                }) { Text("Annuleren") }
            }
        )
    }
}
