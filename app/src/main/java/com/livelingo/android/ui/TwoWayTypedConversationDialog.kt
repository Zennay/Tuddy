package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import com.livelingo.android.PrivatePhraseDeck
import com.livelingo.android.PrivateTypedLanguagePairs
import com.livelingo.android.TypedLanguagePair
import com.livelingo.android.OfflineTextReceipt
import com.livelingo.android.OfflineTextStage
import com.livelingo.android.OfflineTypedCaptionPolicy
import com.livelingo.android.TargetCatalog
import com.livelingo.android.TypedConversationModelController
import com.livelingo.android.UnavailableTextLanguageCatalog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** A temporary UI-only view of the last conversation lines, not an archive. */
internal data class TypedConversationLine(
    val original: String,
    val translated: String,
    val status: String,
    val source: String,
    val target: String
)

/**
 * Two people can type successive turns without changing any speech-packet
 * setting. Only supported ML Kit TEXT pairs are available, and the true
 * source is picked by the user instead of guessed from a short sentence.
 */
@Composable
internal fun TwoWayTypedConversationDialog(
    initialSource: String,
    initialTarget: String,
    initialSharedDraft: String? = null,
    recentLines: List<TypedConversationLine>,
    retryableCount: (String, String) -> Int,
    onRetryMissing: (String, String) -> Int,
    onSend: (String, String, String) -> Boolean,
    onSaveCuratedWord: (String, String) -> Boolean,
    readOwnMeaning: (String, String) -> String?,
    onSaveOwnMeaning: (String, String, String) -> Boolean,
    onOpenLimitedPhraseboard: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var from by remember { mutableStateOf(initialSource.takeIf { it in TargetCatalog.codes } ?: "nl") }
    var to by remember { mutableStateOf(initialTarget.takeIf { it in TargetCatalog.codes } ?: "en") }
    var draft by remember { mutableStateOf(
        initialSharedDraft?.take(OfflineTypedCaptionPolicy.MAX_INPUT_UNITS).orEmpty()
    ) }
    var externalShareReview by remember { mutableStateOf(initialSharedDraft != null) }
    var alternating by remember { mutableStateOf(true) }
    var chooser by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var modelStatus by remember { mutableStateOf<OfflineTextReceipt?>(null) }
    var preparing by remember { mutableStateOf(false) }
    var favoritesOpen by remember { mutableStateOf(false) }
    var confirmEraseFavorites by remember { mutableStateOf(false) }
    var favoriteModelsOpen by remember { mutableStateOf(false) }
    var preparedMeaningPickerOpen by remember { mutableStateOf(false) }
    var pendingLimitedPreviewCode by remember { mutableStateOf<String?>(null) }
    var favoriteNotice by remember { mutableStateOf("") }
    var favoritesRevision by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val favorites = remember(context) { PrivateTypedLanguagePairs(context) }
    val selfAuthoredPhrases = remember(context) { PrivatePhraseDeck(context) }
    val pinnedPairs = remember(favoritesRevision) { favorites.entries() }
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    var speechNotice by remember { mutableStateOf("") }
    var activeSpeechLineIndex by remember { mutableStateOf<Int?>(null) }
    var activeSpeechSide by remember { mutableStateOf<String?>(null) }
    var activeSpeechText by remember { mutableStateOf<String?>(null) }
    var activeSpeechRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var readerOpen by remember { mutableStateOf(false) }
    var studyOpen by remember { mutableStateOf(false) }
    val models = remember { TypedConversationModelController() }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun clearSpeech() {
        speaker.stop()
        activeSpeechLineIndex = null
        activeSpeechSide = null
        activeSpeechText = null
        activeSpeechRange = null
    }

    fun pronounceLine(
        lineIndex: Int,
        text: String,
        language: String,
        side: String,
        pace: LocalSpeechPace
    ) {
        val safe = text.replace('\n', ' ').replace('\r', ' ').trim()
        if (safe.isBlank() || safe.length > 400 ||
            safe.any { it.isISOControl() } || language.isBlank()
        ) {
            clearSpeech()
            speechNotice = "Deze gespreksregel kan niet veilig met een lokale stem worden voorgelezen."
            return
        }
        activeSpeechLineIndex = lineIndex
        activeSpeechSide = side
        activeSpeechText = safe
        activeSpeechRange = null
        speechNotice = "Lokale voorlezing voorbereiden…"
        speaker.speakSequence(
            lines = listOf(safe),
            languageTag = language,
            pace = pace,
            onProgress = { completed, total ->
                speechNotice = "Lokaal voorlezen: $completed/$total regels."
            },
            feedback = { speechNotice = it },
            onFinished = { completed ->
                activeSpeechLineIndex = null
                activeSpeechSide = null
                activeSpeechText = null
                activeSpeechRange = null
                if (completed) speechNotice = "Voorlezen voltooid."
            },
            onLineStarted = {
                activeSpeechRange = null
                speechNotice = if (side == "original")
                    "Nu voorgelezen: origineel." else "Nu voorgelezen: vertaling."
            },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }

    DisposableEffect(models, speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                clearSpeech()
                speechNotice = "Voorlezen gestopt bij verlaten van het scherm."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            models.invalidate()
            speaker.close()
        }
    }
    LaunchedEffect(from, to, recentLines.size) {
        clearSpeech()
        models.invalidate()
        modelStatus = OfflineTextReceipt(from, to, OfflineTextStage.UNCHECKED,
            "Controleer de lokale tekstmodellen voor deze richting.")
        models.check(from, to) { receipt ->
            if (receipt.source == from && receipt.target == to) modelStatus = receipt
        }
    }

    fun switchTurn() {
        if (preparing) return
        val previous = from
        from = to
        to = previous
        chooser = null
        search = ""
        info = ""
        clearSpeech()
        speechNotice = ""
    }
    fun usePinnedPair(pair: TypedLanguagePair) {
        if (preparing || pair.from !in TargetCatalog.codes ||
            pair.to !in TargetCatalog.codes || pair.from == pair.to) return
        from = pair.from
        to = pair.to
        // An externally shared sentence has no trusted known source yet:
        // preserve it across intentional language selection for manual review.
        if (!externalShareReview) draft = ""
        chooser = null
        search = ""
        favoritesOpen = false
        clearSpeech()
        speechNotice = ""
        info = if (externalShareReview)
            "Gedeelde tekst blijft als concept staan. Controleer de echte brontaal " +
                "en pas de zin aan vóór vertalen."
        else "Favoriet gekozen; controleer of beide lokale tekstmodellen aanwezig zijn."
    }
    val rtlLanguages = setOf("ar", "fa", "ur", "he")
    fun title(code: String): String = TargetCatalog.targets
        .firstOrNull { it.code == code }?.title ?: code

    // Keep the same unsent typed draft and selected language direction while
    // switching to a larger read-only presentation of this RAM conversation.
    if (studyOpen) {
        SessionCaptionStudyDialog(
            captions = recentLines.takeLast(80).mapIndexed { index, line ->
                FocusCaptionLine(
                    index.toLong(), line.original, line.translated,
                    line.status, line.source, line.target
                )
            },
            onDismiss = { studyOpen = false }
        )
        return
    }
    if (preparedMeaningPickerOpen) {
        PrivatePreparedMeaningPicker(
            deck = selfAuthoredPhrases,
            hasUnsentDraft = draft.isNotBlank(),
            onChosen = { ownMeaning, writtenLanguage ->
                // Only NL/EN user-authored meanings are admitted here; the
                // corresponding Kurdish/Pashto source is NEVER translated by
                // a model that does not support those languages.
                if (writtenLanguage in setOf("nl", "en") &&
                    OfflineTypedCaptionPolicy.normalize(ownMeaning) != null) {
                    from = writtenLanguage
                    draft = ownMeaning
                    externalShareReview = false
                    info = if (from == to)
                        "Je eigen betekenis staat als ONVERSTUURD concept klaar. " +
                            "Kies nog een andere ondersteunde doeltaal vóór lokaal vertalen."
                    else "Eigen, niet geverifieerde betekenis overgenomen als " +
                        "ONVERSTUURD concept. Controleer en tik zelf op toevoegen."
                } else {
                    info = "Eigen betekenis is ongeldig. Je concept is niet veranderd."
                }
                preparedMeaningPickerOpen = false
            },
            onDismiss = { preparedMeaningPickerOpen = false }
        )
        return
    }
    if (readerOpen) {
        TwoWayConversationReadingBoard(
            lines = recentLines,
            onSaveCuratedWord = onSaveCuratedWord,
            readOwnMeaning = readOwnMeaning,
            onSaveOwnMeaning = onSaveOwnMeaning,
            onDismiss = { readerOpen = false },
            onSubmitTurn = onSend, initialSource = from, initialTarget = to
        )
        return
    }

    if (favoriteModelsOpen) {
        TypedFavoriteOfflineModelsDialog(
            savedPairs = pinnedPairs,
            onDismiss = { favoriteModelsOpen = false }
        )
        return
    }

    val previewCode = pendingLimitedPreviewCode
    if (previewCode != null) {
        AlertDialog(
            onDismissRequest = { pendingLimitedPreviewCode = null },
            title = { Text("Onverstuurd tekstconcept verlaten?") },
            text = {
                Text("De bestaande tekst voor je tweeweg-gesprek wordt niet " +
                    "opgeslagen. Wil je deze editor verlaten om uitsluitend " +
                    "de vijf vaste, niet geverifieerde basiswoorden te bekijken?")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingLimitedPreviewCode = null
                    onOpenLimitedPhraseboard(previewCode)
                }) { Text("Verlaat concept · open basiswoorden") }
            },
            dismissButton = {
                TextButton(onClick = { pendingLimitedPreviewCode = null }) {
                    Text("Bewaar concept hier")
                }
            }
        )
        return
    }

    if (confirmEraseFavorites) {
        AlertDialog(
            onDismissRequest = { confirmEraseFavorites = false },
            title = { Text("Alle opgeslagen teksttaalparen wissen?") },
            text = {
                Text("Dit verwijdert je ${pinnedPairs.size} privé opgeslagen " +
                    "favoriete teksttaalrichtingen. Huidige tekst, gesprekken, " +
                    "spraakfavorieten en gedownloade modellen blijven ongemoeid. " +
                    "Na wissen kun je deze taalparen alleen opnieuw zelf opslaan.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEraseFavorites = false
                    val cleared = favorites.clear()
                    if (cleared) favoritesRevision++
                    favoriteNotice = if (cleared)
                        "Alle favoriete tekstrichtingen gewist."
                    else "Tekstfavorieten konden niet worden gewist."
                }) { Text("Wis alle tekstfavorieten") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEraseFavorites = false }) {
                    Text("Annuleren")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tweeweg-tekstgesprek · offline") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Twee mensen typen om de beurt in hun eigen taal. " +
                    "Dit gebruikt uitsluitend lokale ML Kit-tekstvertaling, " +
                    "geen microfoon, cloudspraak of automatisch bewaarde gesprekken.",
                    style = MaterialTheme.typography.bodySmall)
                if (externalShareReview) {
                    Text("Gedeelde tekst uit een andere app: niets wordt automatisch " +
                        "verstuurd of vertaald. Kies eerst de echte brontaal en " +
                        "de gewenste doeltaal uit de tekstcatalogus. " +
                        "Een favoriet verandert de taal, maar wist dit gedeelde " +
                        "concept niet. Controleer de inhoud zelf.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text("Actieve richting", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            enabled = !preparing, onClick = { chooser = "source"; search = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Van: " + title(from)) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            enabled = !preparing, onClick = { chooser = "target"; search = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Naar: " + title(to)) }
                    }
                }
                if (chooser != null) {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it.take(35) },
                        label = { Text("Zoek taal of taalcode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(
                        modifier = Modifier.heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        val available = TargetCatalog.search(search)
                        available.take(15).forEach { choice ->
                            TextButton(onClick = {
                                if (chooser == "source") from = choice.code else to = choice.code
                                chooser = null
                                search = ""
                                info = ""
                            }) { Text(choice.title + " (" + choice.code + ")") }
                        }
                        // Legacy v0.4 listed these as desired languages. Keep
                        // them VISIBLE but never select them as an ML Kit code.
                        val unavailable = UnavailableTextLanguageCatalog.search(search)
                        if (search.isNotBlank() && unavailable.isNotEmpty()) {
                            Text("Gevraagd maar niet beschikbaar als offline model",
                                style = MaterialTheme.typography.labelMedium)
                            unavailable.forEach { item ->
                                Text(item.title + " (" + item.code + ")",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error)
                                Text(item.reason,
                                    style = MaterialTheme.typography.bodySmall)
                                if (item.hasFivePhrasePreview) {
                                    TextButton(onClick = {
                                        if (draft.isBlank()) {
                                            onOpenLimitedPhraseboard(item.code)
                                        } else {
                                            pendingLimitedPreviewCode = item.code
                                        }
                                    }) {
                                        Text("Bekijk 5 basiswoorden · geen vrije vertaling")
                                    }
                                }
                            }
                        } else if (search.isNotBlank() && available.isEmpty()) {
                            Text("Geen ondersteunde teksttaal gevonden. " +
                                "Geen automatisch model beschikbaar.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(onClick = { chooser = null }) { Text("Sluit taallijst") }
                }
                OutlinedButton(
                    enabled = !preparing && from != to,
                    onClick = { switchTurn() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⇄ Wissel wie typt · " + to.uppercase() + " → " + from.uppercase()) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        enabled = !preparing && from != to &&
                            TypedLanguagePair(from, to) !in pinnedPairs &&
                            pinnedPairs.size < PrivateTypedLanguagePairs.MAX_PAIRS,
                        onClick = {
                            val saved = favorites.pin(TypedLanguagePair(from, to))
                            if (saved) favoritesRevision++
                            favoriteNotice = if (saved)
                                "Tekstrichting privé bewaard. Geen model of gespreksinhoud opgeslagen."
                                else "Favoriet kon niet worden bewaard."
                        }
                    ) { Text("☆ Bewaar tekstrichting") }
                    TextButton(onClick = { favoritesOpen = !favoritesOpen }) {
                        Text(if (favoritesOpen) "Sluit favorieten"
                            else "★ Favorieten (${pinnedPairs.size})")
                    }
                }
                if (favoritesOpen) {
                    Text("Alleen taalkeuzes zijn bewaard. Paketten worden nooit als " +
                        "geïnstalleerd verondersteld.", style = MaterialTheme.typography.bodySmall)
                    pinnedPairs.forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                enabled = !preparing,
                                onClick = { usePinnedPair(pair) },
                                modifier = Modifier.weight(1f)
                            ) { Text("${pair.from.uppercase()} → ${pair.to.uppercase()}") }
                            TextButton(onClick = {
                                if (favorites.remove(pair)) {
                                    favoritesRevision++
                                    favoriteNotice = "Tekstfavoriet verwijderd."
                                } else favoriteNotice = "Favoriet kon niet worden verwijderd."
                            }) { Text("Verwijder") }
                        }
                    }
                    if (pinnedPairs.isEmpty()) {
                        Text("Nog geen tekstrichtingen opgeslagen.",
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        OutlinedButton(
                            enabled = !preparing,
                            onClick = {
                                speaker.stop()
                                speechNotice = ""
                                favoriteModelsOpen = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("✓ Check alle favorieten · offline modellen…") }
                        OutlinedButton(
                            onClick = { confirmEraseFavorites = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Wis alle tekstfavorieten…") }
                    }
                }
                if (favoriteNotice.isNotBlank()) {
                    Text(favoriteNotice, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Na versturen automatisch wisselen",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                    Switch(checked = alternating, onCheckedChange = { alternating = it })
                }
                val receipt = modelStatus
                Text(
                    "Offline tekstmodellen: " + (receipt?.detail
                        ?: "Nog niet op dit toestel gecontroleerd."),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (receipt?.stage == OfflineTextStage.MISSING)
                        MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextButton(enabled = !preparing, onClick = {
                        models.check(from, to) { modelStatus = it }
                    }) { Text("Controleer modellen") }
                    if (from != to && receipt?.stage != OfflineTextStage.READY) {
                        TextButton(enabled = !preparing, onClick = {
                            preparing = true
                            info = "Alleen na deze tik probeert Tuddy via wifi de modellen te downloaden."
                            models.prepareViaWifi(from, to) { message, verified ->
                                preparing = false
                                info = message
                                if (verified != null) modelStatus = verified
                                else models.check(from, to) { modelStatus = it }
                            }
                        }) { Text(if (preparing) "Download loopt…" else "Modellen via wifi…") }
                    }
                }
                val retryable = retryableCount(from, to)
                if (retryable > 0) {
                    Text("Eerder niet vertaald voor deze richting: $retryable zinnen.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        enabled = !preparing && modelStatus?.stage == OfflineTextStage.READY,
                        onClick = {
                            val retried = onRetryMissing(from, to)
                            info = if (retried > 0)
                                "$retried eerdere zinnen worden opnieuw lokaal vertaald."
                            else "Er zijn geen passende onvoltooide zinnen meer."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("↻ Herprobeer ontbrekende vertalingen (max. 12)") }
                    Text("Alleen na jouw tik. Dit hergebruikt reeds aanwezige " +
                        "tekstmodellen en downloadt niets automatisch.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (from == to) {
                    Text("Kies twee verschillende teksttalen voor een vertaling.",
                        color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !preparing,
                    onClick = {
                        speaker.stop()
                        speechNotice = ""
                        preparedMeaningPickerOpen = true
                    }
                ) { Text("▤ Gebruik mijn voorbereide eigen tekst…") }
                Text(
                    "Neemt alleen jouw opgeslagen NL/EN-betekenis als concept over. " +
                        "Nooit automatisch verzenden, geen Kurmanji/Pasjtoe-model.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= OfflineTypedCaptionPolicy.MAX_INPUT_UNITS) draft = it },
                    label = { Text("Typ de zin in " + title(from)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = if (from in rtlLanguages) TextDirection.Rtl else TextDirection.Ltr
                    ),
                    minLines = 2, maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = !preparing && from != to &&
                        OfflineTypedCaptionPolicy.normalize(draft) != null,
                    onClick = {
                        val normalized = OfflineTypedCaptionPolicy.normalize(draft)
                        if (normalized != null && onSend(normalized, from, to)) {
                            draft = ""
                            externalShareReview = false
                            info = "Zin aan het tijdelijke gesprek toegevoegd; " +
                                "alleen werkelijk lokaal voltooide vertalingen worden getoond."
                            if (alternating) switchTurn()
                        } else {
                            info = "Zin kon niet aan dit gesprek worden toegevoegd."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Voeg deze beurt toe en vertaal lokaal") }
                if (info.isNotBlank()) Text(info,
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Laatste gespreksregels · alleen tijdelijk geheugen",
                    style = MaterialTheme.typography.titleSmall)
                Text("Tik op een woord in de bron of afgeronde vertaling " +
                    "voor de offline woordkaart. Alleen een bewuste bewaarknop " +
                    "legt je eigen betekenis vast.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    speaker.stop()
                    speechNotice = ""
                    readerOpen = true
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("▣ Toon tijdelijk gesprek groot · zoek en lees")
                }
                Text("Grotere leesweergave toont maximaal 80 RAM-regels en " +
                    "komt terug naar deze onopgeslagen invoer.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    enabled = recentLines.any { it.status == "Lokaal vertaald" &&
                        it.original.isNotBlank() && it.translated.isNotBlank() &&
                        it.original != it.translated &&
                        it.original.length <= 400 && it.translated.length <= 400 },
                    onClick = {
                        speaker.stop()
                        speechNotice = ""
                        studyOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧠 Oefen dit tijdelijke gesprek · zonder opslaan") }
                val firstVisibleLineIndex = (recentLines.size - 6).coerceAtLeast(0)
                recentLines.takeLast(6).forEachIndexed { visibleIndex, row ->
                    val lineIndex = firstVisibleLineIndex + visibleIndex
                    val originalActive = activeSpeechLineIndex == lineIndex &&
                        activeSpeechSide == "original" && activeSpeechText != null
                    val translatedActive = activeSpeechLineIndex == lineIndex &&
                        activeSpeechSide == "translated" && activeSpeechText != null
                    Text(row.source.uppercase() + " → " + row.target.uppercase() +
                        " · " + row.status, style = MaterialTheme.typography.labelSmall)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (originalActive) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (originalActive) Text(
                                if (activeSpeechRange != null)
                                    "Nu voorgelezen · indicatieve tekstmarkering van de stem."
                                else "Nu voorgelezen · deze stem geeft geen woordbereik terug.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            OfflineWordText(
                                text = row.original,
                                language = row.source,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (row.source in rtlLanguages)
                                        TextDirection.Rtl else TextDirection.Ltr
                                ),
                                onSaveWord = onSaveCuratedWord,
                                readOwnMeaning = readOwnMeaning,
                                saveOwnMeaning = onSaveOwnMeaning,
                                activeSpeechText = activeSpeechText.takeIf { originalActive },
                                activeSpeechRange = activeSpeechRange.takeIf { originalActive }
                            )
                        }
                    }
                    TextButton(
                        enabled = activeSpeechText == null && row.original.isNotBlank() &&
                            row.original.length <= 400,
                        onClick = {
                            pronounceLine(
                                lineIndex, row.original, row.source, "original",
                                LocalSpeechPace.NORMAL
                            )
                        }
                    ) { Text("🔊 Origineel lokaal voorlezen") }
                    if (row.status == "Lokaal vertaald" || row.status == "Lokaal") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (translatedActive)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (translatedActive) Text(
                                    if (activeSpeechRange != null)
                                        "Nu voorgelezen · indicatieve tekstmarkering van de stem."
                                    else "Nu voorgelezen · deze stem geeft geen woordbereik terug.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                OfflineWordText(
                                    text = row.translated,
                                    language = row.target,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDirection = if (row.target in rtlLanguages)
                                            TextDirection.Rtl else TextDirection.Ltr
                                    ),
                                    onSaveWord = onSaveCuratedWord,
                                    readOwnMeaning = readOwnMeaning,
                                    saveOwnMeaning = onSaveOwnMeaning,
                                    activeSpeechText = activeSpeechText.takeIf { translatedActive },
                                    activeSpeechRange = activeSpeechRange.takeIf { translatedActive }
                                )
                            }
                        }
                        if (row.status == "Lokaal vertaald" &&
                            row.translated.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    enabled = activeSpeechText == null && row.translated.length <= 400,
                                    onClick = {
                                        pronounceLine(
                                            lineIndex, row.translated, row.target, "translated",
                                            LocalSpeechPace.NORMAL
                                        )
                                    }
                                ) { Text("🔊 Vertaling") }
                                TextButton(
                                    enabled = activeSpeechText == null && row.translated.length <= 400,
                                    onClick = {
                                        pronounceLine(
                                            lineIndex, row.translated, row.target, "translated",
                                            LocalSpeechPace.SLOW
                                        )
                                    }
                                ) { Text("Rustiger") }
                            }
                        }
                    } else {
                        Text("Vertaling nog niet voltooid of model ontbreekt.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
                                if (recentLines.isNotEmpty()) {
                    TextButton(onClick = {
                        speaker.stop()
                        speechNotice = "Voorlezen gestopt."
                    }, modifier = Modifier.fillMaxWidth()) { Text("■ Stop lokale stem") }
                }
                if (speechNotice.isNotBlank()) {
                    Text(speechNotice, style = MaterialTheme.typography.bodySmall)
                }
                Text("Voorlezen start alleen nadat jij tikt en uitsluitend met een " +
                    "door Android als lokaal geïnstalleerd gemelde stem; " +
                    "zonder stem geen internetfallback.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Kurmanji, Sorani en Pasjtoe zijn geen ML Kit-teksttalen. " +
                    "Uitvoer kan fouten bevatten; laat gevoelige gesprekken " +
                    "door een mens controleren. Modeldownloads vereisen wifi " +
                    "en jouw afzonderlijke keuze.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Terug naar gesprek")
            }
        }
    )
}
