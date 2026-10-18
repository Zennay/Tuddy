package com.livelingo.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Purely visual, session-only screen. No persistence, microphone acquisition,
 * translator access, network call or speaker identification.
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
    fontScale: Float,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onStart: (() -> Unit)? = null,
    canStartOffline: Boolean = false,
    archiveMode: Boolean = false,
    pinnedCaptionIds: Set<Long> = emptySet(),
    onTogglePin: ((Long) -> String)? = null,
    onCycleManualSpeaker: ((Long) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var followLive by remember { mutableStateOf(true) }
    var speakerFilter by remember { mutableStateOf<String?>(null) }
    var captionQuery by remember { mutableStateOf("") }
    var onlyPinned by remember { mutableStateOf(false) }
    var pinNotice by remember { mutableStateOf("") }
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
    val showInterim = !archiveMode && speakerFilter == null &&
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

    LaunchedEffect(captionQuery, speakerFilter, onlyPinned) {
        if (captionQuery.isNotBlank() || onlyPinned) listState.scrollToItem(0)
    }

    LaunchedEffect(speakerFilter, displayedCaptions.size,
        displayedCaptions.lastOrNull()?.translated,
        interimOriginal, interimTranslated, followLive) {
        if (followLive && (displayedCaptions.isNotEmpty() ||
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
                Text(
                    if (archiveMode) "■ Privé-JSON-archief · microfoon uit · alleen lezen"
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
                        enabled = !followLive
                    ) { Text(if (archiveMode) "↓ Laatste archiefregel" else
                        if (followLive) "Live ✓" else "Volg live") }
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
                    if (!listening && !canStartOffline) {
                        Text("Offline spraakpakket of modelbeheer nog niet gereed; " +
                            "start eerst via de bestaande toestelvoorbereiding. " +
                            "Geen cloudspraakfallback.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Zoeken gebruikt alleen afgeronde sessieregels. De " +
                        "microfoon blijft onafhankelijk werken; live volgen " +
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
                HorizontalDivider()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f).nestedScroll(manualScroll),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (displayedCaptions.isEmpty() && (!showInterim || interimOriginal.isBlank())) {
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
                    items(displayedCaptions, key = { it.id }) { line ->
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
                                if (showColumns) {
                                    BilingualCaptionColumns(
                                        original = line.original,
                                        translation = line.translated,
                                        sourceCode = line.sourceCode,
                                        targetCode = line.targetCode,
                                        status = line.status,
                                        fontScale = fontScale,
                                        isInterim = false,
                                        searchQuery = captionQuery
                                    )
                                } else {
                                    SessionCaptionSearchText(line.original, captionQuery,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale,
                                        textDirection = TextDirection.ContentOrRtl
                                    ))
                                    if (line.status == "Lokaal vertaald" || line.status == "Lokaal") {
                                    SessionCaptionSearchText(line.translated, captionQuery,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontScale,
                                            textDirection = TextDirection.ContentOrRtl
                                        ))
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
