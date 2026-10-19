package com.livelingo.android.ui

import android.content.Context
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.livelingo.android.PersonalPhrase
import java.io.ByteArrayOutputStream
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivatePhraseTopics
import com.livelingo.android.PrivatePrintablePhrasebook

private data class PendingTopicPhrasebook(
    val language: String?,
    val topic: String?,
    val bytes: ByteArray
)

/**
 * User-authored, editable offline study notes. No model calls, chat-log import
 * or claims that any phrase has been translated/checked by native speakers.
 */
@Composable
internal fun MyOfflinePhrasesPanel(
    deck: PrivatePhraseDeck,
    initialLanguage: String,
    onDismiss: () -> Unit
) {
    var language by remember { mutableStateOf(initialLanguage.takeIf {
        it in listOf("kmr", "ckb", "ps")
    } ?: "kmr") }
    var meaningLanguage by remember { mutableStateOf("nl") }
    var phrase by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var chosenTopicFilter by remember { mutableStateOf<String?>(null) }
    var manageTopicsOpen by remember { mutableStateOf(false) }
    var bulkTopicsOpen by remember { mutableStateOf(false) }
    var assigningTopicTo by remember { mutableStateOf<PersonalPhrase?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var info by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var pendingGroupRemoval by remember { mutableStateOf<List<PersonalPhrase>?>(null) }
    var practiceOpen by remember { mutableStateOf(false) }
    var scheduledOpen by remember { mutableStateOf(false) }
    var startScheduleConsent by remember { mutableStateOf(false) }
    var eraseScheduleConsent by remember { mutableStateOf(false) }
    var displayedPhrase by remember { mutableStateOf<PersonalPhrase?>(null) }
    var editingPhrase by remember { mutableStateOf<PersonalPhrase?>(null) }
    var pendingImport by remember { mutableStateOf<List<PersonalPhrase>?>(null) }
    var importSource by remember { mutableStateOf("json") }
    var pasteOpen by remember { mutableStateOf(false) }
    var pasteLines by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingTsvExport by remember { mutableStateOf<ByteArray?>(null) }
    var printableConsent by remember { mutableStateOf(false) }
    var printableAllLanguages by remember { mutableStateOf(false) }
    var printableOnlySelectedTopic by remember { mutableStateOf(false) }
    var pendingPrintable by remember { mutableStateOf<PendingTopicPhrasebook?>(null) }
    val context = LocalContext.current
    val phraseTopics = remember(context) { PrivatePhraseTopics(context) }
    fun matchesTopic(record: PersonalPhrase, topic: String?): Boolean = when (topic) {
        null -> true
        "__without__" -> phraseTopics.topicFor(record) == null
        else -> phraseTopics.topicFor(record) == topic
    }
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val snapshot = pendingExport
        pendingExport = null
        if (uri != null && snapshot != null) {
            if (deck.exportSnapshot()?.contentEquals(snapshot) != true) {
                info = "Eigen zinnen zijn gewijzigd. Maak een nieuwe back-up."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(snapshot)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                info = if (saved) "Je eigen zinnen zijn op jouw gekozen locatie opgeslagen."
                    else "Opslaan is mislukt. Je lokale zinnen blijven behouden."
            }
        }
    }
    val createTsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/tab-separated-values")
    ) { uri ->
        val snapshot = pendingTsvExport
        pendingTsvExport = null
        if (uri != null && snapshot != null) {
            if (deck.exportTsvSnapshot()?.contentEquals(snapshot) != true) {
                info = "Eigen zinnen zijn gewijzigd. Maak de TSV-lijst opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(snapshot)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                info = if (saved) "Zelfgeschreven zinnenlijst opgeslagen als UTF-8 TSV."
                    else "TSV niet opgeslagen. Je eigen zinnen blijven behouden."
            }
        }
    }
    // No bytes leave app-private memory until a consciously chosen SAF location.
    val createPrintable = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val pending = pendingPrintable
        pendingPrintable = null
        if (uri != null && pending != null) {
            val current = deck.entries()?.filter { note ->
                matchesTopic(note, pending.topic)
            }?.let { notes ->
                PrivatePrintablePhrasebook.snapshot(notes, pending.language)
            }
            if (current == null || !current.contentEquals(pending.bytes)) {
                info = "Eigen zinnen zijn gewijzigd. Kies opnieuw afdrukbaar zinnenboek."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(pending.bytes)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                info = if (saved)
                    "Afdrukbaar zinnenboek opgeslagen. Controleer inhoud en opslaglocatie."
                else "Het afdrukbare zinnenboek kon niet worden opgeslagen."
            }
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
                        if (count == 0 || output.size() + count > 50000)
                            throw IllegalArgumentException("oversized or invalid data")
                        output.write(chunk, 0, count)
                    }
                } ?: throw IllegalArgumentException("unreadable provider")
                deck.previewImport(output.toByteArray())
            }.getOrNull()
            if (candidate == null) {
                info = "Bestand geweigerd: onbekende versie, ongeldige zinnen of te groot. " +
                    "De huidige zinnen zijn behouden."
            } else {
                pendingImport = candidate
                info = "Bestand gecontroleerd. Bevestig zelf of je de huidige zinnen wilt vervangen."
            }
        }
    }
    val openTsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImport = null
        if (uri != null) {
            val candidate = runCatching {
                val output = ByteArrayOutputStream()
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("onleesbaar bestand")
                input.use { stream ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        if (count == 0 || output.size() + count > 50000)
                            throw IllegalArgumentException("ongeldige of te grote lijst")
                        output.write(buffer, 0, count)
                    }
                }
                deck.previewTsvImport(output.toByteArray())
            }.getOrNull()
            if (candidate == null) {
                info = "TSV-lijst geweigerd. Controleer de UTF-8-kopregel, " +
                    "vier tabkolommen, talen en de limiet van 80 zinnen. " +
                    "Er zijn geen eigen zinnen overschreven."
            } else {
                importSource = "tsv"
                pendingImport = candidate
                info = "Zelfgeschreven zinnenlijst ingelezen. " +
                    "Kies bewust samenvoegen of vervangen."
            }
        }
    }
    val phrases = remember(revision) { deck.entries() }
    val topics = remember(revision) { phraseTopics.topics() }
    val topicCounts = remember(revision) { phraseTopics.counts(phrases.orEmpty()) }
    val selectedTopic = chosenTopicFilter?.takeIf { it == "__without__" || it in topics }
    // Topic selection also scopes a user-requested study session; search text
    // affects only the visible list, never silently cuts down practice content.
    val topicPracticeNotes = remember(revision, selectedTopic) {
        phrases.orEmpty().filter { note ->
            when (selectedTopic) {
                null -> true
                "__without__" -> phraseTopics.topicFor(note) == null
                else -> phraseTopics.topicFor(note) == selectedTopic
            }
        }
    }
    val labels = mapOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe")

    if (pasteOpen) {
        AlertDialog(
            onDismissRequest = { pasteOpen = false; pasteLines = ""; pasteError = "" },
            title = { Text("Plak mijn eigen zinnen · offline") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Kopieer uit jouw eigen notities maximaal 40 regels. Gebruik per regel " +
                            "TWEE tabgescheiden kolommen: eigen zin ↹ eigen betekenis. " +
                            "De taalkeuzes uit dit scherm bepalen het taallabel, niet een " +
                            "spraak- of vertaalmodel. Er wordt niets automatisch opgeslagen " +
                            "of gedeeld.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Huidige invoertalen: ${language.uppercase()} → " +
                            "${meaningLanguage.uppercase()}. Wijzig deze eerst in het " +
                            "hoofdscherm als dat niet klopt.",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = pasteLines,
                        onValueChange = { incoming ->
                            if (incoming.length <= 12000) {
                                pasteLines = incoming
                                pasteError = ""
                            } else pasteError = "Maximaal 12.000 tekens; geen tekst is overgenomen."
                        },
                        label = { Text("Mijn zelfgeschreven zin [tab] eigen betekenis") },
                        supportingText = { Text("Geen kopregel nodig. Max. 40 regels, per regel precies één tab.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 10
                    )
                    if (pasteError.isNotBlank()) {
                        Text(pasteError, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Een volledige voorbeeldlijst verschijnt vóór toevoegen of vervangen. " +
                            "Bestaande betekenissen blijven bij samenvoegen behouden.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val lines = pasteLines.split('\n').map { it.removeSuffix("\r") }
                        .dropLastWhile { it.isEmpty() }
                    if (lines.size !in 1..40 || lines.any { it.isBlank() }) {
                        pasteError = "Gebruik 1 tot 40 niet-lege regels zonder lege tussenregels."
                    } else {
                        val rows = lines.map { it.split('\t') }
                        if (rows.any { it.size != 2 || it[0].isBlank() || it[1].isBlank() }) {
                            pasteError = "Iedere regel moet precies twee gevulde tabkolommen bevatten."
                        } else {
                            val tsv = buildString {
                                append("language\tphrase\tmeaningLanguage\townMeaning\n")
                                rows.forEach { cells ->
                                    append(language).append('\t').append(cells[0].trim())
                                    append('\t').append(meaningLanguage).append('\t')
                                        .append(cells[1].trim()).append('\n')
                                }
                            }
                            val parsed = deck.previewTsvImport(tsv.toByteArray(Charsets.UTF_8))
                            if (parsed == null) {
                                pasteError = "Een of meer regels zijn ongeldig of dubbel. " +
                                    "Maximaal 120 tekens per zin en 180 per betekenis; " +
                                    "alles blijft ongewijzigd."
                            } else {
                                pendingImport = parsed
                                importSource = "paste"
                                pasteOpen = false
                                pasteLines = ""
                                pasteError = ""
                                info = "Zelfgeschreven tekst gecontroleerd. Bekijk iedere " +
                                    "zin vóór je zelf samenvoegen of vervangen kiest."
                            }
                        }
                    }
                }) { Text("Controleer alle geplakte zinnen") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pasteOpen = false
                    pasteLines = ""
                    pasteError = ""
                }) { Text("Annuleren") }
            }
        )
        return
    }

    val removingGroup = pendingGroupRemoval
    if (removingGroup != null) {
        AlertDialog(
            onDismissRequest = { pendingGroupRemoval = null },
            title = { Text("Eigen zinnen uit deze themagroep wissen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Je staat op het punt ${removingGroup.size} zelfgeschreven " +
                            "zinnen uit de gekozen themagroep op dit toestel te verwijderen. " +
                            "Hieronder staat de HELE selectie. Dit wist ook bijbehorende " +
                            "favorieten en herhaaldatums voor deze zinnen, maar laat " +
                            "andere thema's, zinnen en afspraken intact.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Maak eventueel eerst zelf een privé-back-up via 'Bewaar back-up'. " +
                            "Deze knop verstuurt niets en de verwijdering kan niet " +
                            "automatisch ongedaan worden gemaakt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    removingGroup.forEachIndexed { index, item ->
                        HorizontalDivider()
                        Text("${index + 1}. ${item.language.uppercase()} · ${item.phrase}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = if (item.language == "ckb" ||
                                    item.language == "ps") TextDirection.Rtl
                                    else TextDirection.Ltr
                            ))
                        Text("Eigen betekenis: ${item.ownMeaning}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val removed = deck.removeMany(removingGroup)
                    if (removed) {
                        revision++
                        if (editingPhrase in removingGroup) {
                            editingPhrase = null
                            phrase = ""
                            meaning = ""
                        }
                    }
                    info = if (removed)
                        "${removingGroup.size} eigen zinnen uit één themagroep gewist. " +
                            "Andere zinnen en de themanamen zijn behouden."
                    else "Verwijderen geweigerd: de eigen lijst is gewijzigd, beschadigd " +
                        "of niet schrijfbaar. Je bestaande zinnen zijn behouden."
                    pendingGroupRemoval = null
                }) { Text("Verwijder uitsluitend deze ${removingGroup.size} zinnen") }
            },
            dismissButton = {
                TextButton(onClick = { pendingGroupRemoval = null }) {
                    Text("Behoud al mijn zinnen")
                }
            }
        )
        return
    }

    if (bulkTopicsOpen) {
        BulkAssignPrivatePhraseTopicsDialog(
            notes = topicPracticeNotes,
            topics = topics,
            storage = phraseTopics,
            fromGroup = selectedTopic ?: "__all__",
            onChanged = { revision++ },
            onDismiss = { bulkTopicsOpen = false }
        )
        return
    }
    if (manageTopicsOpen) {
        PrivatePhraseTopicsDialog(
            storage = phraseTopics,
            revision = revision,
            onChanged = { revision++ },
            onDismiss = { manageTopicsOpen = false }
        )
        return
    }
    val assigning = assigningTopicTo
    if (assigning != null) {
        AssignPrivatePhraseTopicDialog(
            note = assigning,
            storage = phraseTopics,
            topics = topics,
            onChanged = { revision++ },
            onDismiss = { assigningTopicTo = null }
        )
        return
    }

    val toDisplay = displayedPhrase
    if (toDisplay != null) {
        OfflinePhrasePresenter(
            notes = phrases.orEmpty(),
            initial = toDisplay,
            onDismiss = { displayedPhrase = null }
        )
        return
    }
    if (scheduledOpen) {
        ScheduledPhraseReviewPanel(
            deck = deck,
            onChanged = { revision++ },
            onDismiss = { scheduledOpen = false }
        )
        return
    }
    if (practiceOpen) {
        PersonalPhrasePracticePanel(
            notes = topicPracticeNotes,
            initialLanguage = language,
            onDismiss = { practiceOpen = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen offline zinnen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "Vul beide kanten ZELF in. Dit is geen vertaling door Tuddy, geen " +
                        "spraakherkenning en geen gecontroleerde woordenlijst. " +
                        "Bewaar geen gevoelige gesprekken of persoonsgegevens.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (editingPhrase != null) {
                    Text("✎ Je bewerkt één bestaande zin. Opslaan vervangt alleen deze " +
                        "zin en jouw betekenis, nooit het hele gesprek.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = {
                        editingPhrase = null
                        phrase = ""
                        meaning = ""
                        info = "Bewerken geannuleerd. De bewaarde zin is niet veranderd."
                    }) { Text("Annuleer bewerken") }
                }
                Text("Taal van je eigen zin")
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    labels.forEach { (code, title) ->
                        TextButton(onClick = { language = code }) {
                            Text(if (language == code) "$title ✓" else title)
                        }
                    }
                }
                OutlinedTextField(
                    value = phrase, onValueChange = { phrase = it.take(120) },
                    label = { Text("Eigen zin in ${labels[language]}") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (language == "ckb" || language == "ps")
                            TextDirection.Rtl else TextDirection.Ltr
                    )
                )
                Text("Betekenis die jij zelf invoert")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { meaningLanguage = "nl" }) {
                        Text(if (meaningLanguage == "nl") "Nederlands ✓" else "Nederlands")
                    }
                    TextButton(onClick = { meaningLanguage = "en" }) {
                        Text(if (meaningLanguage == "en") "English ✓" else "English")
                    }
                }
                OutlinedTextField(
                    value = meaning, onValueChange = { meaning = it.take(180) },
                    label = { Text("Je eigen betekenis (geen automatische vertaling)") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
                OutlinedButton(
                    enabled = phrase.isNotBlank() && meaning.isNotBlank(),
                    onClick = {
                        val old = editingPhrase
                        val saved = if (old == null) {
                            deck.save(language, phrase, meaningLanguage, meaning)
                        } else {
                            deck.edit(old, PersonalPhrase(language, phrase,
                                meaningLanguage, meaning))
                        }
                        if (saved) {
                            editingPhrase = null
                            phrase = ""
                            meaning = ""
                            revision++
                            info = if (old == null)
                                "Je eigen zin is lokaal opgeslagen. Niet automatisch vertaald."
                            else "Je bestaande eigen zin is bijgewerkt. Niet gecontroleerd door Tuddy."
                        } else {
                            info = "Opslaan mislukt. Controleer op dubbele zinnen, " +
                                "gewijzigde invoer of te veel tekens (120/180). " +
                                "De bestaande zinnen zijn behouden."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editingPhrase == null) "Bewaar mijn eigen zin"
                    else "Bewaar wijziging aan eigen zin") }
                Text("Alleen op jouw verzoek blijven deze zinnen in privé-appopslag staan. " +
                    "Ze worden niet gedeeld of automatisch overgenomen uit gesprekken.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    enabled = topicPracticeNotes.isNotEmpty(),
                    onClick = { practiceOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (selectedTopic == null) "Oefen mijn eigen zinnen"
                    else "Oefen dit thema · ${topicPracticeNotes.size} zinnen") }
                if (!deck.reviewsEnabled()) {
                    OutlinedButton(
                        enabled = phrases?.isNotEmpty() == true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { startScheduleConsent = true }
                    ) { Text("Plan mijn eigen zinnen voor herhaling…") }
                } else {
                    val scheduled = deck.scheduledReviews().orEmpty()
                    val due = scheduled.count {
                        it.dueDay <= java.time.LocalDate.now().toEpochDay()
                    }
                    OutlinedButton(
                        enabled = due > 0,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { scheduledOpen = true }
                    ) { Text("Herhaal eigen zinnen vandaag · $due") }
                    Text(
                        "Lokaal gepland: ${scheduled.size} eigen zinnen. Geen automatische " +
                            "vertaling, native-speakercontrole of meldingen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { eraseScheduleConsent = true }) {
                        Text("Wis herhaalplanning eigen zinnen")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = phrases?.isNotEmpty() == true,
                        onClick = {
                            val snapshot = deck.exportSnapshot()
                            if (snapshot == null) {
                                info = "Er zijn geen geldige eigen zinnen om te exporteren."
                            } else {
                                pendingExport = snapshot
                                createBackup.launch("tuddy-mijn-eigen-zinnen.json")
                            }
                        }
                    ) { Text("Bewaar back-up…") }
                    OutlinedButton(onClick = {
                        pendingImport = null
                        importSource = "json"
                        openBackup.launch(arrayOf("application/json", "text/plain",
                            "application/octet-stream"))
                    }) { Text("Herstel back-up…") }
                }
                OutlinedButton(
                    enabled = phrases?.isNotEmpty() == true,
                    onClick = {
                        printableAllLanguages = false
                        printableOnlySelectedTopic = selectedTopic != null
                        printableConsent = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("▤ Maak afdrukbaar tweetalig zinnenboek…") }
                Text("Maak een zelfstandig HTML-zinnenboek voor bekijken of afdrukken " +
                    "zonder internet. Alleen jouw eigen zinnen, niet geverifieerd.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        pendingImport = null
                        pasteLines = ""
                        pasteError = ""
                        pasteOpen = true
                    }
                ) { Text("Plak meerdere eigen zinnen uit notities…") }
                Text(
                    "Snel toevoegen zonder bestand: één zelfgeschreven zin en betekenis " +
                        "per tabgescheiden regel. Je kiest beide talen hierboven. " +
                        "Pas na volledige preview en je eigen bevestiging wordt iets bewaard.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        pendingImport = null
                        importSource = "tsv"
                        openTsv.launch(arrayOf(
                            "text/tab-separated-values", "text/plain",
                            "application/octet-stream"
                        ))
                    }
                ) { Text("Importeer eigen zinnenlijst (TSV)…") }
                OutlinedButton(
                    enabled = phrases?.isNotEmpty() == true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val snapshot = deck.exportTsvSnapshot()
                        if (snapshot == null) {
                            info = "Geen geldige eigen zinnen voor TSV-export."
                        } else {
                            pendingTsvExport = snapshot
                            createTsv.launch("tuddy-mijn-eigen-zinnen.tsv")
                        }
                    }
                ) { Text("Exporteer mijn eigen zinnen (TSV)…") }
                Text("Dit bestand kun je zelf bewerken en later via TSV-import " +
                    "weer toevoegen of vervangen. Het bevat alleen je eigen, " +
                    "niet onafhankelijk gecontroleerde zinnen.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Gebruik zelfgeschreven UTF-8-zinnen met vier tabkolommen: " +
                    "language, phrase, meaningLanguage, ownMeaning. " +
                    "Kies na het inlezen bewust samenvoegen of vervangen. " +
                    "Tuddy controleert de vertalingen niet.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Back-up is optioneel. De gekozen bestandsprovider kan online synchroniseren; " +
                    "Tuddy uploadt zelf niets. Import overschrijft pas na jouw bevestiging.",
                    style = MaterialTheme.typography.bodySmall)
                if (info.isNotBlank()) Text(info,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { manageTopicsOpen = true }
                ) { Text("Mijn zinnenthema’s beheren · ${topics.size}/12") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedTopic != null && topics.isNotEmpty() &&
                        topicPracticeNotes.isNotEmpty(),
                    onClick = { bulkTopicsOpen = true }
                ) {
                    Text("Verplaats ${topicPracticeNotes.size} eigen zinnen naar één thema…")
                }
                Text("Kies eerst een specifieke themagroep of 'Zonder thema'. " +
                    "Je kunt dan die hele groep bewust en atomair verplaatsen. " +
                    "Zo verandert 'Alle thema’s' nooit per ongeluk je hele zinnenboek.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedTopic != null && topicPracticeNotes.isNotEmpty() &&
                        phrases != null,
                    onClick = { pendingGroupRemoval = topicPracticeNotes.toList() }
                ) {
                    Text("Wis mijn ${topicPracticeNotes.size} zinnen uit dit thema…")
                }
                Text("Alleen de expliciet gekozen themagroep, nooit automatisch " +
                    "je volledige eigen zinnenboek. Na een volledige preview en " +
                    "jouw aparte bevestiging worden de zinnen atomair verwijderd.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Filter mijn eigen zinnen op zelfgekozen thema",
                    style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { chosenTopicFilter = null }) {
                    Text(if (selectedTopic == null) "Alle thema’s ✓" else "Alle thema’s")
                }
                TextButton(onClick = { chosenTopicFilter = "__without__" }) {
                    Text(if (selectedTopic == "__without__")
                        "Zonder thema ✓" else "Zonder thema")
                }
                topics.forEach { topic ->
                    TextButton(onClick = { chosenTopicFilter = topic }) {
                        Text((if (selectedTopic == topic) "✓ " else "") +
                            "$topic (${topicCounts[topic] ?: 0})")
                    }
                }
                OutlinedTextField(
                    value = search, onValueChange = { search = it.take(60) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Zoek in mijn eigen zinnen") }
                )
                if (phrases == null) {
                    Text("Lokale lijst is beschadigd. Opslaan is geblokkeerd. " +
                        "Je kunt de lokale lijst hieronder zelf wissen.",
                        color = MaterialTheme.colorScheme.error)
                } else {
                    val query = search.trim()
                    val shown = phrases.filter {
                        val searchMatches = query.isEmpty() ||
                            it.phrase.contains(query, ignoreCase = true) ||
                            it.ownMeaning.contains(query, ignoreCase = true) ||
                            labels[it.language].orEmpty().contains(query, ignoreCase = true)
                        val assigned = phraseTopics.topicFor(it)
                        val topicMatches = when (selectedTopic) {
                            null -> true
                            "__without__" -> assigned == null
                            else -> assigned == selectedTopic
                        }
                        searchMatches && topicMatches
                    }
                    Text("${shown.size} van ${phrases.size} zelf ingevoerde zinnen",
                        style = MaterialTheme.typography.bodySmall)
                    shown.forEach { item ->
                        HorizontalDivider()
                        Text("${labels[item.language]} · ${if (item.meaningLanguage == "nl") "NL" else "EN"}",
                            style = MaterialTheme.typography.labelSmall)
                        Text(item.phrase,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (item.language == "ckb" || item.language == "ps")
                                    TextDirection.Rtl else TextDirection.Ltr
                            ))
                        Text(item.ownMeaning, style = MaterialTheme.typography.bodyMedium)
                        Text("Thema: ${phraseTopics.topicFor(item) ?: "Niet ingedeeld"}",
                            style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { assigningTopicTo = item }) {
                            Text("▤ Kies thema voor deze zin")
                        }
                        OutlinedButton(onClick = { displayedPhrase = item }) {
                            Text("▣ Toon groot · gesprekskaart")
                        }
                        TextButton(onClick = {
                            editingPhrase = item
                            language = item.language
                            phrase = item.phrase
                            meaningLanguage = item.meaningLanguage
                            meaning = item.ownMeaning
                            info = "Pas deze zelf ingevoerde zin hierboven aan. " +
                                "Kies 'Bewaar wijziging' om precies dit item te vervangen."
                        }) { Text("✎ Bewerk deze zin") }
                        TextButton(onClick = {
                            if (deck.remove(item.language, item.phrase)) {
                                if (editingPhrase == item) {
                                    editingPhrase = null
                                    phrase = ""
                                    meaning = ""
                                }
                                revision++
                                info = "Eigen zin verwijderd."
                            } else info = "Verwijderen mislukt."
                        }) { Text("Verwijder eigen zin") }
                    }
                }
                TextButton(onClick = { confirmClear = true }) {
                    Text("Wis mijn volledige eigen zinnenlijst")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
    if (startScheduleConsent) {
        AlertDialog(
            onDismissRequest = { startScheduleConsent = false },
            title = { Text("Mijn eigen zinnen privé herhalen?") },
            text = { Text(
                "Alle zinnen en betekenissen zijn door jou zelf geschreven en niet " +
                    "door Tuddy geverifieerd. Pas na deze keuze bewaart de app " +
                    "lokaal een hash, niveau en volgende herhaaldatum per zin. " +
                    "Geen extra zinnenkopie, account, cloud, notificaties of spraakherkenning."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    val enabled = deck.enableReviews()
                    if (enabled) revision++
                    info = if (enabled) "Herhaalplanning gestart: nieuwe zinnen staan klaar."
                        else "Herhaalplanning kon niet worden gestart."
                    startScheduleConsent = false
                }) { Text("Start op dit toestel") }
            },
            dismissButton = {
                TextButton(onClick = { startScheduleConsent = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
    if (eraseScheduleConsent) {
        AlertDialog(
            onDismissRequest = { eraseScheduleConsent = false },
            title = { Text("Herhaaldatums eigen zinnen wissen?") },
            text = { Text(
                "Dit wist het optionele lokale schema en de niveaus; de eigen " +
                    "zinnen, betekenissen en favorieten blijven bewaard."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    val removed = deck.disableReviews()
                    if (removed) revision++
                    info = if (removed) "Alle herhaaldatums van eigen zinnen gewist."
                        else "Wissen van de herhaalplanning mislukt."
                    eraseScheduleConsent = false
                }) { Text("Wis planning") }
            },
            dismissButton = {
                TextButton(onClick = { eraseScheduleConsent = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
    if (printableConsent) {
        val included = phrases.orEmpty().filter {
            (printableAllLanguages || it.language == language) &&
                (!printableOnlySelectedTopic || matchesTopic(it, selectedTopic))
        }
        AlertDialog(
            onDismissRequest = { printableConsent = false },
            title = { Text("Eigen zinnenboek extern bewaren?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "De " + included.size + " zelfgeschreven zinnen en jouw eigen " +
                            "betekenissen komen in één zelfstandig HTML-bestand. " +
                            "Geen gecontroleerde vertalingen. Een Android-provider " +
                            "kan het bestand buiten Tuddy bewaren, synchroniseren " +
                            "of delen. Dit bestand valt niet onder 'Wis gesprek' " +
                            "of 'Wis oefengegevens'. Bewaar geen privégesprekken."
                    )
                    TextButton(onClick = {
                        printableAllLanguages = !printableAllLanguages
                    }) {
                        Text(if (printableAllLanguages)
                            "✓ Alle drie talen (" + (phrases?.size ?: 0) + " zinnen)"
                        else "✓ Alleen " + labels[language] + " (" + included.size + " zinnen)")
                    }
                    if (selectedTopic != null) {
                        TextButton(onClick = {
                            printableOnlySelectedTopic = !printableOnlySelectedTopic
                        }) {
                            Text(if (printableOnlySelectedTopic)
                                "✓ Alleen zichtbare themagroep (${included.size})"
                            else "○ Alle thema’s (ook buiten huidige filter)")
                        }
                    }
                    Text("Bewaar alléén de gekozen selectie op een externe locatie. " +
                        "De eigen themanamen staan niet in het HTML-bestand.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Bestand is zonder JavaScript of online lettertypen. " +
                        "De tekst wordt als afdrukbare kaarten opgemaakt.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = included.isNotEmpty(),
                    onClick = {
                        printableConsent = false
                        val selection = if (printableAllLanguages) null else language
                        val topic = if (printableOnlySelectedTopic) selectedTopic else null
                        val selectedNotes = phrases.orEmpty().filter { note ->
                            matchesTopic(note, topic)
                        }
                        val snapshot = PrivatePrintablePhrasebook.snapshot(
                            selectedNotes, selection
                        )
                        if (snapshot == null) {
                            info = "Kan geen veilig zinnenboek maken. " +
                                "Controleer de eigen zinnenlijst."
                        } else {
                            pendingPrintable = PendingTopicPhrasebook(selection, topic, snapshot)
                            createPrintable.launch(
                                if (topic != null) "Tuddy-zinnenboek-geselecteerd-thema.html"
                                else if (selection == null) "Tuddy-mijn-zinnenboek.html"
                                else "Tuddy-zinnenboek-" + selection + ".html"
                            )
                        }
                    }
                ) { Text("Ik begrijp dit, kies opslag…") }
            },
            dismissButton = {
                TextButton(onClick = { printableConsent = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
    if (pendingImport != null) {
        val imported = pendingImport!!
        val ownedKeys = phrases.orEmpty().map {
            it.language + "|" + it.phrase.lowercase(java.util.Locale.ROOT)
        }.toSet()
        val newCount = imported.count {
            it.language + "|" + it.phrase.lowercase(java.util.Locale.ROOT) !in ownedKeys
        }
        val duplicateCount = imported.size - newCount
        val mergeFits = phrases != null && phrases.size + newCount <= 80
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(if (importSource == "paste")
                "Geplakte eigen zinnen bekijken" else if (importSource == "tsv")
                "Eigen zinnenlijst importeren?" else "Eigen zinnen vervangen?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("Geselecteerd: ${imported.size} eigen zinnen. " +
                        "${newCount} nieuw, ${duplicateCount} bestaan al in je privé-lijst. " +
                        "Samenvoegen bewaart altijd je bestaande tekst en betekenis bij " +
                        "een dubbele zin. Vervangen verwijdert de huidige lijst en " +
                        "zet alleen de hieronder getoonde zinnen terug. " +
                        (if (mergeFits) "Samenvoegen past binnen 80 zinnen."
                        else "Samenvoegen kan niet: beschadigde lijst of meer dan 80 zinnen.") +
                        " Dit is geen gecontroleerde vertaling.")
                    Text("Volledige gecontroleerde import · geen opslag tot bevestiging",
                        style = MaterialTheme.typography.labelMedium)
                    imported.forEachIndexed { index, record ->
                        HorizontalDivider()
                        Text("${index + 1}. ${record.language.uppercase()} → " +
                            "${record.meaningLanguage.uppercase()}",
                            style = MaterialTheme.typography.labelSmall)
                        Text(record.phrase,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = if (record.language == "ckb" ||
                                    record.language == "ps") TextDirection.Rtl
                                    else TextDirection.Ltr
                            ))
                        Text("Eigen betekenis: ${record.ownMeaning}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Column {
                    TextButton(
                        enabled = mergeFits,
                        onClick = {
                            val merged = deck.mergeImported(imported)
                            if (merged) {
                                revision++
                                editingPhrase = null
                                phrase = ""
                                meaning = ""
                            }
                            info = if (merged)
                                "Nieuwe eigen zinnen toegevoegd; bestaande betekenissen behouden."
                            else "Samenvoegen geweigerd. Controleer de limiet en lijst; " +
                                "je bestaande zinnen zijn behouden."
                            pendingImport = null
                        }
                    ) { Text("Voeg alleen nieuwe zinnen toe") }
                    TextButton(onClick = {
                        val saved = deck.replaceImported(imported)
                        if (saved) {
                            revision++
                            editingPhrase = null
                            phrase = ""
                            meaning = ""
                        }
                        info = if (saved)
                            "Back-up heeft de lokale lijst vervangen na jouw keuze."
                        else "Herstellen mislukt. De oude lijst blijft behouden."
                        pendingImport = null
                    }) { Text("Vervang volledige lijst") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImport = null
                    info = "Import geannuleerd; eigen zinnen zijn behouden."
                }) { Text("Annuleren") }
            }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Alle eigen zinnen wissen?") },
            text = { Text("Deze actie verwijdert je lokaal bewaarde zinnen definitief. " +
                "De ingebouwde vijf basiszinnen blijven beschikbaar. " +
                "Je optionele herhaalplanning voor eigen zinnen wordt ook gewist.") },
            confirmButton = {
                TextButton(onClick = {
                    val cleared = deck.clear()
                    if (cleared) {
                        revision++
                        editingPhrase = null
                        phrase = ""
                        meaning = ""
                    }
                    info = if (cleared) "Je eigen zinnen zijn gewist."
                        else "Wissen mislukt; probeer opnieuw."
                    confirmClear = false
                }) { Text("Wis mijn zinnen") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Annuleren") }
            }
        )
    }
}

/**
 * Explicit group-scoped topic reassignment; does not rewrite phrase text.
 * Requires a named source group and a consciously selected destination.
 */
@Composable
private fun BulkAssignPrivatePhraseTopicsDialog(
    notes: List<PersonalPhrase>,
    topics: List<String>,
    storage: PrivatePhraseTopics,
    fromGroup: String,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    var destination by remember { mutableStateOf<String?>(null) }
    var accepted by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val sourceLabel = if (fromGroup == "__without__") "Zonder thema" else fromGroup
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn eigen zinnen als groep indelen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Geselecteerde brongroep: $sourceLabel · ${notes.size} eigen zinnen. " +
                        "Deze actie wijzigt uitsluitend hun lokale thema. " +
                        "Zelfgeschreven teksten, betekenissen, favorieten en " +
                        "herhaalplanning blijven ongewijzigd. Geen uploads.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Kies bewust het nieuwe thema", style = MaterialTheme.typography.labelMedium)
                topics.forEach { name ->
                    TextButton(onClick = {
                        destination = name
                        accepted = false
                        notice = ""
                    }) { Text(if (destination == name) "✓ $name" else name) }
                }
                TextButton(onClick = {
                    destination = "__none__"
                    accepted = false
                    notice = ""
                }) { Text(if (destination == "__none__")
                    "✓ Verwijder alleen de themakoppeling" else "Zonder thema plaatsen") }
                if (destination != null) {
                    Text(
                        "Voorbeeld: $sourceLabel → " +
                            (if (destination == "__none__") "Zonder thema" else destination) +
                            ". Bekijk alle eigen zinnen:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    notes.forEachIndexed { index, item ->
                        HorizontalDivider()
                        Text("${index + 1}. ${item.language.uppercase()} · ${item.phrase}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = if (item.language == "ckb" ||
                                    item.language == "ps") TextDirection.Rtl
                                    else TextDirection.Ltr
                            ))
                        Text("Huidig thema: ${storage.topicFor(item) ?: "Geen"}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { accepted = !accepted }) {
                        Text(if (accepted) "✓ Deze volledige groep wijzigen"
                            else "○ Ik heb alle ${notes.size} zinnen bekeken en bevestig")
                    }
                }
                if (notice.isNotBlank()) {
                    Text(notice, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = destination != null && accepted &&
                    notes.isNotEmpty() && notes.size <= 80 &&
                    fromGroup != "__all__",
                onClick = {
                    val target = destination!!.takeUnless { it == "__none__" }
                    val nowMatches = notes.all { phrase ->
                        val current = storage.topicFor(phrase)
                        if (fromGroup == "__without__") current == null
                        else current == fromGroup
                    }
                    val result = nowMatches && storage.assignMany(notes, target)
                    if (result) {
                        onChanged()
                        onDismiss()
                    } else {
                        notice = "Groep of bronzinnen zijn veranderd, of opslaan is mislukt. " +
                            "Niets gedeeltelijk gewijzigd. Kies opnieuw vanaf de lijst."
                        accepted = false
                    }
                }
            ) { Text("Bevestig groepsthema") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
