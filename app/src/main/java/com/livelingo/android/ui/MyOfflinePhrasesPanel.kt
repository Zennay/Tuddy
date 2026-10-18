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
    var assigningTopicTo by remember { mutableStateOf<PersonalPhrase?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var info by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var practiceOpen by remember { mutableStateOf(false) }
    var scheduledOpen by remember { mutableStateOf(false) }
    var startScheduleConsent by remember { mutableStateOf(false) }
    var eraseScheduleConsent by remember { mutableStateOf(false) }
    var displayedPhrase by remember { mutableStateOf<PersonalPhrase?>(null) }
    var editingPhrase by remember { mutableStateOf<PersonalPhrase?>(null) }
    var pendingImport by remember { mutableStateOf<List<PersonalPhrase>?>(null) }
    var importSource by remember { mutableStateOf("json") }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingTsvExport by remember { mutableStateOf<ByteArray?>(null) }
    var printableConsent by remember { mutableStateOf(false) }
    var printableAllLanguages by remember { mutableStateOf(false) }
    var pendingPrintable by remember { mutableStateOf<Pair<String?, ByteArray>?>(null) }
    val context = LocalContext.current
    val phraseTopics = remember(context) { PrivatePhraseTopics(context) }
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
            val current = deck.entries()?.let {
                PrivatePrintablePhrasebook.snapshot(it, pending.first)
            }
            if (current == null || !current.contentEquals(pending.second)) {
                info = "Eigen zinnen zijn gewijzigd. Kies opnieuw afdrukbaar zinnenboek."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(pending.second)
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
            printableAllLanguages || it.language == language
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
                        val snapshot = phrases?.let {
                            PrivatePrintablePhrasebook.snapshot(it, selection)
                        }
                        if (snapshot == null) {
                            info = "Kan geen veilig zinnenboek maken. " +
                                "Controleer de eigen zinnenlijst."
                        } else {
                            pendingPrintable = selection to snapshot
                            createPrintable.launch(
                                if (selection == null) "Tuddy-mijn-zinnenboek.html"
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
            title = { Text(if (importSource == "tsv")
                "Eigen zinnenlijst importeren?" else "Eigen zinnen vervangen?") },
            text = {
                Text("Geselecteerd: ${imported.size} eigen zinnen. " +
                    "${newCount} nieuw, ${duplicateCount} bestaan al in je privé-lijst. " +
                    "Samenvoegen bewaart altijd je bestaande tekst en betekenis bij " +
                    "een dubbele zin. Vervangen verwijdert de huidige lijst en " +
                    "zet alleen het gekozen bestand terug. " +
                    (if (mergeFits) "Samenvoegen past binnen 80 zinnen."
                    else "Samenvoegen kan niet: beschadigde lijst of meer dan 80 zinnen.") +
                    " Dit is geen gecontroleerde vertaling.")
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
