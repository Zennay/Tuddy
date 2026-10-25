package com.livelingo.android.ui

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
import com.livelingo.android.PrivateClipboardExpiry
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import com.livelingo.android.PrivateCaptionCopy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Session-only reader. Copying is an explicit user-triggered system clipboard
 * action; the screen does not acquire the microphone or invoke translation,
 * network calls or speaker identification.
 */
internal data class FocusCaptionLine(
    val id: Long,
    val original: String,
    val translated: String,
    val status: String,
    val sourceCode: String,
    val targetCode: String,
    val speaker: String? = null, // Manual A/B marker; not an inferred identity.
    val personalMeaning: String? = null // Purely human note, never model-verified or exported.
)

@Composable
internal fun FocusTranscriptDialog(
    captions: List<FocusCaptionLine>,
    interimOriginal: String,
    interimTranslated: String,
    listening: Boolean,
    speechPhaseStatus: String = "",
    captureMode: String = "Doorlopend",
    translationPaused: Boolean = false,
    pendingTranslationCount: Int = 0,
    uncertainSpeechCount: Int = 0,
    onReviewUncertainSpeech: (() -> Unit)? = null,
    onToggleTranslationPaused: (() -> Unit)? = null,
    onToggleCaptureMode: (() -> Unit)? = null,
    fontScale: Float,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onCover: (() -> Unit)? = null,
    onStart: (() -> Unit)? = null,
    canStartOffline: Boolean = false,
    showMicrophonePermissionRecovery: Boolean = false,
    microphonePermissionGranted: Boolean = false,
    microphoneCanAskAgain: Boolean = false,
    microphonePermissionNotice: String = "",
    onRetryMicrophonePermission: (() -> Unit)? = null,
    onOpenMicrophoneSettings: (() -> Unit)? = null,
    archiveMode: Boolean = false,
    pinnedCaptionIds: Set<Long> = emptySet(),
    onTogglePin: ((Long) -> String)? = null,
    onCycleManualSpeaker: ((Long) -> Unit)? = null,
    onRequestTranslationCorrection: ((Long) -> Unit)? = null,
    rememberedCorrectionIds: Set<Long> = emptySet(),
    onRequestRememberedCorrection: ((Long) -> Unit)? = null,
    onRequestOriginalCorrection: ((Long) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var followLive by remember { mutableStateOf(true) }
    // RAM-only focused reading: never persist a text selection or auto-advance
    // to a newly arriving caption while the user is reading a prior line.
    var stepReading by remember { mutableStateOf(false) }
    var stepCaptionId by remember { mutableStateOf<Long?>(null) }
    var speakerFilter by remember { mutableStateOf<String?>(null) }
    var captionQuery by remember { mutableStateOf("") }
    var onlyPinned by remember { mutableStateOf(false) }
    var pinNotice by remember { mutableStateOf("") }
    var copyNotice by remember { mutableStateOf("") }
    var speakerMenuOpen by remember { mutableStateOf(false) }
    // Columns only when Android reports adequate width; never crush two
    // full-length languages into half a narrow portrait phone screen.
    val isWide = LocalConfiguration.current.screenWidthDp >= 600
    var preferColumns by remember { mutableStateOf(true) }
    val showColumns = isWide && preferColumns
    val matchedCaptions = SessionCaptionSearch.filter(
        captions, captionQuery, if (archiveMode) null else speakerFilter
    )
    val displayedCaptions = if (onlyPinned && !archiveMode)
        matchedCaptions.filter { it.id in pinnedCaptionIds } else matchedCaptions
    val focusedIndex = displayedCaptions.indexOfFirst { it.id == stepCaptionId }
        .takeIf { it >= 0 } ?: displayedCaptions.lastIndex
    val renderedCaptions = if (stepReading)
        displayedCaptions.getOrNull(focusedIndex)?.let { listOf(it) }.orEmpty()
    else displayedCaptions
    val showInterim = !stepReading && !archiveMode && speakerFilter == null &&
        captionQuery.isBlank() && !onlyPinned
    val manualScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    followLive = false
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(captionQuery, speakerFilter, onlyPinned, stepReading) {
        if (stepReading) {
            // A changed filter never silently reselects an unrelated hidden
            // caption; the final *visible* caption becomes the new starting point.
            stepCaptionId = displayedCaptions.lastOrNull()?.id
            listState.scrollToItem(0)
        } else if (captionQuery.isNotBlank() || onlyPinned) listState.scrollToItem(0)
    }
    LaunchedEffect(stepReading, stepCaptionId) {
        if (stepReading) listState.scrollToItem(0)
    }

    LaunchedEffect(speakerFilter, displayedCaptions.size,
        displayedCaptions.lastOrNull()?.translated,
        interimOriginal, interimTranslated, followLive, stepReading) {
        if (!stepReading && followLive && (displayedCaptions.isNotEmpty() ||
                (showInterim && interimOriginal.isNotBlank()))) {
            val index = if (showInterim && interimOriginal.isNotBlank()) displayedCaptions.size
                else (displayedCaptions.size - 1).coerceAtLeast(0)
            listState.scrollToItem(index)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (archiveMode) "Tuddy · Archief groot lezen" else "Tuddy · Leesmodus",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Sluiten") }
                }
                if (onCover != null && !archiveMode) {
                    OutlinedButton(
                        onClick = onCover,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🔒 Gesprek nu afdekken · microfoon stoppen") }
                }
                Text(
                    if (archiveMode) "■ Privé-JSON-archief · microfoon uit · alleen lezen"
                    else if (speechPhaseStatus.isNotBlank()) speechPhaseStatus
                    else if (listening) "● Microfoon actief · on-device herkenning"
                    else "■ Microfoon uit · alleen huidige gespreksregels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            captionQuery = ""
                            if (!archiveMode) {
                                speakerFilter = null
                                onlyPinned = false
                            }
                            followLive = true
                        },
                        enabled = !followLive && !stepReading
                    ) { Text(if (archiveMode) "↓ Laatste archiefregel" else
                        if (followLive) "Live ✓" else "Volg live") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            stepReading = !stepReading
                            if (stepReading) {
                                followLive = false
                                stepCaptionId = displayedCaptions.lastOrNull()?.id
                            } else stepCaptionId = null
                        }
                    ) {
                        Text(if (stepReading) "Toon alle gespreksregels"
                            else "Lees één zin tegelijk")
                    }
                }
                if (stepReading) {
                    Text(
                        "Rustige tekstweergave zonder interactieve woordkaarten of " +
                            "automatische sprongen. Je microfooninstelling verandert niet.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            enabled = focusedIndex > 0,
                            onClick = {
                                stepCaptionId = displayedCaptions.getOrNull(focusedIndex - 1)?.id
                                followLive = false
                            }
                        ) { Text("← Vorige zin") }
                        Text(
                            if (focusedIndex >= 0)
                                "${focusedIndex + 1} van ${displayedCaptions.size}"
                            else "0 van 0",
                            modifier = Modifier.weight(1f)
                                .semantics { heading() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            enabled = focusedIndex >= 0 &&
                                focusedIndex < displayedCaptions.lastIndex,
                            onClick = {
                                stepCaptionId = displayedCaptions.getOrNull(focusedIndex + 1)?.id
                                followLive = false
                            }
                        ) { Text("Volgende zin →") }
                    }
                }
                if (!archiveMode && uncertainSpeechCount > 0 &&
                    onReviewUncertainSpeech != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !listening,
                        onClick = onReviewUncertainSpeech
                    ) {
                        Text("⚠ ${uncertainSpeechCount} onzekere spraakzinnen controleren")
                    }
                    Text(
                        "Androids zekerheidsmelding is geen vastgestelde fout; " +
                            "alleen jij kunt de uitspraak controleren.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!archiveMode && onToggleTranslationPaused != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onToggleTranslationPaused
                    ) {
                        Text(if (translationPaused) "▶ Hervat live vertalen"
                            else "■ Pauzeer vertaling · blijf transcript lezen")
                    }
                    Text(
                        if (translationPaused)
                            "Alleen origineel. Al ingezette lokale modeltaken kunnen intern " +
                                "afronden, maar hun resultaten worden niet in het gesprek getoond. " +
                                "Eerder overgeslagen zinnen vertaal je later afzonderlijk."
                        else "Live vertaling aan." +
                            if (pendingTranslationCount > 0)
                                " $pendingTranslationCount zin(nen) wachten op lokale verwerking."
                            else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!archiveMode) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = listening || (canStartOffline && onStart != null),
                        onClick = {
                            if (listening) onStop() else onStart?.invoke()
                        }
                    ) {
                        Text(if (listening) "■ Stop microfoon" else
                            "🎙 Start offline luisteren")
                    }
                    if (showMicrophonePermissionRecovery &&
                        onRetryMicrophonePermission != null &&
                        onOpenMicrophoneSettings != null) {
                        MicrophonePermissionRecoveryPanel(
                            permissionGranted = microphonePermissionGranted,
                            canRequestAgain = microphoneCanAskAgain,
                            message = microphonePermissionNotice,
                            onRequestAgain = onRetryMicrophonePermission,
                            onOpenSystemSettings = onOpenMicrophoneSettings
                        )
                    }
                    if (!listening && !canStartOffline) {
                        Text("Offline spraakpakket of modelbeheer nog niet gereed; " +
                            "start eerst via de bestaande toestelvoorbereiding. " +
                            "Geen cloudspraakfallback.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (onToggleCaptureMode != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Luistermodus: $captureMode",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f))
                            TextButton(
                                enabled = !listening,
                                onClick = onToggleCaptureMode
                            ) { Text("Wissel") }
                        }
                        if (captureMode == "Eén zin") {
                            Text(
                                "Eén zin per tik. Na resultaat, stilte of timeout stopt " +
                                    "de microfoon; lokaal vertalen gaat daarna verder.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                if (!archiveMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box {
                        OutlinedButton(onClick = { speakerMenuOpen = true }) {
                            Text(when (speakerFilter) {
                                "A" -> "Spreker A"
                                "B" -> "Spreker B"
                                "?" -> "Onbekend"
                                else -> "Alle sprekers"
                            })
                        }
                        DropdownMenu(
                            expanded = speakerMenuOpen,
                            onDismissRequest = { speakerMenuOpen = false }
                        ) {
                            val choices = listOf(
                                null to "Alle sprekers (${captions.size})",
                                "A" to "Spreker A (${captions.count { it.speaker == "A" }})",
                                "B" to "Spreker B (${captions.count { it.speaker == "B" }})",
                                "?" to "Onbekend (${captions.count { it.speaker == null }})"
                            )
                            choices.forEach { (choice, title) ->
                                DropdownMenuItem(text = { Text(title) }, onClick = {
                                    speakerFilter = choice
                                    speakerMenuOpen = false
                                    followLive = captionQuery.isBlank()
                                })
                            }
                        }
                    }
                    Text(
                        "Handmatig gelabeld, geen sprekerherkenning.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                }
                OutlinedTextField(
                    value = captionQuery,
                    onValueChange = { input ->
                        captionQuery = input.take(120)
                        if (captionQuery.isNotBlank()) followLive = false
                    },
                    label = { Text(if (archiveMode) "Zoek in geopende archiefregels"
                        else "Zoek in deze live-sessie") },
                    placeholder = { Text("Origineel, lokale vertaling of eigen uitleg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (captionQuery.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${displayedCaptions.size} gevonden van ${captions.size} regels",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { captionQuery = "" }) {
                            Text("Zoekopdracht wissen")
                        }
                    }
                    Text("Zoeken gebruikt alleen afgeronde sessieregels. " +
                        "Arabische/Perzische toetsenbordvarianten en weggelaten klinkertekens " +
                        "kunnen dezelfde regel vinden; alleen letterlijke treffers worden " +
                        "gemarkeerd. Opgeslagen spelling verandert niet. Live volgen " +
                        "hervat alleen na een tik op Volg live.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!archiveMode && onTogglePin != null) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            onlyPinned = !onlyPinned
                            if (onlyPinned) followLive = false
                            pinNotice = ""
                        }) {
                            Text(if (onlyPinned) "★ Gemarkeerd (${pinnedCaptionIds.size}) ✓"
                                else "☆ Alleen gemarkeerd (${pinnedCaptionIds.size})")
                        }
                        Text("Tijdelijke sessiebladwijzers",
                            style = MaterialTheme.typography.labelSmall)
                    }
                    if (pinNotice.isNotBlank()) Text(pinNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isWide) {
                    OutlinedButton(
                        onClick = { preferColumns = !preferColumns },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showColumns) "▤ Naast elkaar · kies onder elkaar"
                            else "▥ Onder elkaar · kies naast elkaar")
                    }
                } else {
                    Text("Tip: draai je toestel horizontaal voor twee kolommen met " +
                        "origineel en vertaling naast elkaar.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!followLive) {
                    Text(if (archiveMode) "Archief teruglezen · gebruik Laatste archiefregel voor onderaan."
                        else "Teruglezen gepauzeerd. Tik op Volg live om verder te volgen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (copyNotice.isNotBlank()) {
                    Text(copyNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (stepReading)
                        "Eén tweetalige gespreksregel in leesvolgorde. " +
                            "Gebruik Vorige/Volgende; er wordt geen spraak automatisch afgespeeld."
                    else "Tik in een afgeronde zin of vertaling op een woord voor een lokale woordkaart. " +
                        "Zoekmarkering blijft zichtbaar. Geen online woordenboek of automatische betekenis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f).nestedScroll(manualScroll),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (renderedCaptions.isEmpty() && (!showInterim || interimOriginal.isBlank())) {
                        item(key = "empty") {
                            Text(
                                if (archiveMode) "Geen regels in deze archiefselectie. Sluit en wijzig de zoekterm."
                                 else if (onlyPinned)
                                    "Geen gemarkeerde regels voor deze selectie. " +
                                        "Wis het zoek- of sprekerfilter of voeg een markering toe."
                                 else if (captionQuery.isNotBlank())
                                    "Geen afgeronde regels met deze zoekterm. Wis de zoekopdracht."
                                 else if (speakerFilter == null)
                                    "Nog geen ondertitels. Sluit de leesmodus en tik op Luister."
                                else "Geen afgeronde regels met dit handmatige sprekerlabel. Kies Alle sprekers.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    items(renderedCaptions, key = { it.id }) { line ->
                        val highlightLatestFinal = !stepReading && listening && followLive && !archiveMode &&
                            captionQuery.isBlank() && speakerFilter == null && !onlyPinned &&
                            interimOriginal.isBlank() && line.original.isNotBlank() &&
                            line.id == displayedCaptions.lastOrNull()?.id
                        Card(shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Text(
                                    if (archiveMode) "${line.sourceCode.uppercase()} → ${line.targetCode.uppercase()} · privéarchief"
                                    else "${line.sourceCode.uppercase()} → ${line.targetCode.uppercase()} · " +
                                        (line.speaker?.let { "Spreker $it (handmatig)" }
                                            ?: "Spreker onbekend (handmatig)"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (highlightLatestFinal) {
                                    Text(
                                        "Laatste afgeronde regel · laatste tekstwoord benadrukt · geen audio-woordtiming",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!archiveMode && onCycleManualSpeaker != null) {
                                    TextButton(onClick = { onCycleManualSpeaker(line.id) }) {
                                        Text(when (line.speaker) {
                                            "A" -> "Spreker A (handmatig) → B"
                                            "B" -> "Spreker B (handmatig) → onbekend"
                                            else -> "Spreker onbekend → handmatig A"
                                        })
                                    }
                                }
                                if (!archiveMode && onTogglePin != null) {
                                    TextButton(onClick = {
                                        pinNotice = onTogglePin(line.id)
                                    }) {
                                        Text(if (line.id in pinnedCaptionIds)
                                            "★ Markering verwijderen" else
                                            "☆ Markeer voor dit gesprek")
                                    }
                                }
                                if (line.status in setOf("Lokaal", "Lokaal vertaald") &&
                                    line.original.isNotBlank() && line.translated.isNotBlank()
                                ) {
                                    TextButton(onClick = {
                                        val payload = PrivateCaptionCopy.snapshot(
                                            original = line.original,
                                            translated = line.translated,
                                            status = line.status,
                                            sourceCode = line.sourceCode,
                                            targetCode = line.targetCode
                                        )
                                        if (payload == null) {
                                            copyNotice = "Deze caption kan niet veilig worden gekopieerd."
                                        } else {
                                            copyNotice = if (PrivateClipboardExpiry.copy(
                                                    context, "focusgesprek tweetalig", payload
                                                )) {
                                                "Origineel + vertaling bewust gekopieerd. " +
                                                    "Andere apps kunnen het systeemklembord lezen; " +
                                                    "de ingestelde vervaltijd is slechts een poging."
                                            } else {
                                                "Kopiëren mislukt: er is niets naar het Android-klembord gestuurd."
                                            }
                                        }
                                    }) {
                                        Text("Kopieer origineel + vertaling")
                                    }
                                }
                                if (!archiveMode && !listening &&
                                    onRequestOriginalCorrection != null &&
                                    line.original.isNotBlank()
                                ) {
                                    TextButton(onClick = {
                                        onRequestOriginalCorrection(line.id)
                                    }) {
                                        Text("✎ Corrigeer oorspronkelijke zin…")
                                    }
                                }
                                if (!archiveMode && !listening &&
                                    onRequestTranslationCorrection != null &&
                                    line.status in setOf("Lokaal", "Lokaal vertaald") &&
                                    line.translated.isNotBlank()
                                ) {
                                    TextButton(onClick = {
                                        onRequestTranslationCorrection(line.id)
                                    }) {
                                        Text("✎ Corrigeer deze vertaling…")
                                    }
                                    if (line.id in rememberedCorrectionIds &&
                                        onRequestRememberedCorrection != null) {
                                        TextButton(onClick = {
                                            onRequestRememberedCorrection(line.id)
                                        }) {
                                            Text("↳ Bekijk mijn eerdere vertaalcorrectie…")
                                        }
                                    }
                                }
                                line.personalMeaning?.let { own ->
                                    Text("Mijn eigen uitleg (niet geverifieerd; geen export)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    SessionCaptionSearchText(own, captionQuery,
                                        MaterialTheme.typography.titleMedium.copy(
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize * fontScale,
                                            textDirection = TextDirection.ContentOrRtl
                                        ))
                                }
                                if (stepReading) {
                                    // Plain text gives TalkBack one stable original and one
                                    // stable translated span instead of dozens of tappable words.
                                    Text(
                                        "Origineel · ${line.sourceCode.uppercase()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        line.original,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale,
                                            textDirection = TextDirection.ContentOrRtl
                                        )
                                    )
                                    Text(
                                        "Vertaling · ${line.targetCode.uppercase()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        if (line.status in setOf("Lokaal", "Lokaal vertaald"))
                                            line.translated
                                        else "Nog geen afgeronde lokale vertaling: ${line.status}",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale,
                                            textDirection = TextDirection.ContentOrRtl
                                        )
                                    )
                                } else if (showColumns) {
                                    BilingualCaptionColumns(
                                        original = line.original,
                                        translation = line.translated,
                                        sourceCode = line.sourceCode,
                                        targetCode = line.targetCode,
                                        status = line.status,
                                        fontScale = fontScale,
                                        isInterim = false,
                                        highlightLatestFinal = highlightLatestFinal,
                                        searchQuery = captionQuery,
                                        enableOfflineWordCards = true,
                                        allowWordSpeech = !listening
                                    )
                                } else {
                                    SessionCaptionSearchText(line.original, captionQuery,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale,
                                        textDirection = TextDirection.ContentOrRtl
                                    ),
                                    enableOfflineWordCards = true,
                                    wordLanguage = line.sourceCode,
                                    allowWordSpeech = !listening,
                                    activeSpeechText = if (highlightLatestFinal) normalizedLiveFocusText(line.original) else null,
                                    activeSpeechRange = if (highlightLatestFinal) lastTextWordRange(line.original) else null)
                                    if (line.status == "Lokaal vertaald" || line.status == "Lokaal") {
                                    SessionCaptionSearchText(line.translated, captionQuery,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale,
                                            textDirection = TextDirection.ContentOrRtl
                                        ),
                                        enableOfflineWordCards = true,
                                        wordLanguage = line.targetCode,
                                        allowWordSpeech = !listening,
                                        activeSpeechText = if (highlightLatestFinal) normalizedLiveFocusText(line.translated) else null,
                                        activeSpeechRange = if (highlightLatestFinal) lastTextWordRange(line.translated) else null)
                                } else {
                                    Text("[Vertaling niet voltooid: ${line.status}]",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    if (showInterim && interimOriginal.isNotBlank()) {
                        item(key = "interim") {
                            Card(shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Live · nog niet afgerond",
                                        style = MaterialTheme.typography.labelMedium)
                                    if (showColumns) {
                                        BilingualCaptionColumns(
                                            original = interimOriginal,
                                            translation = interimTranslated,
                                            sourceCode = "",
                                            targetCode = "",
                                            status = "Voorlopig",
                                            fontScale = fontScale,
                                            isInterim = true
                                        )
                                    } else {
                                        LiveInterimWordFocus(
                                        text = interimOriginal,
                                        fontScale = fontScale
                                    )
                                    Text(
                                        "Laatste tekstwoord benadrukt · geen audio-tijdsynchronisatie.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (interimTranslated.isNotBlank()) {
                                        LiveInterimWordFocus(
                                            text = interimTranslated, fontScale = fontScale
                                        )
                                        Text("Laatste tekstwoord in lokale voorlopige " +
                                            "vertaling · geen audio-woordtiming.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("Vertaling volgt zodra een lokaal model resultaat geeft.",
                                            style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(if (archiveMode) "Alleen dit geopende privéarchief · geen import, opslag of delen."
                    else "Alleen de huidige sessie · niets automatisch bewaren of delen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
