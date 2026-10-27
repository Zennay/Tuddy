package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Temporary large-screen presentation of user-selected transcript lines.
 * No file, clipboard, external share or hidden persistence operations.
 * Any speech is explicitly triggered via already installed non-network voices.
 * Translations are shown ONLY for completed on-device captions.
 */
@Composable
internal fun TwoWayConversationReadingBoard(
    lines: List<TypedConversationLine>,
    onSaveCuratedWord: (String, String) -> Boolean,
    readOwnMeaning: (String, String) -> String?,
    onSaveOwnMeaning: (String, String, String) -> Boolean,
    onDismiss: () -> Unit,
    onSubmitTurn: ((String, String, String) -> Boolean)? = null,
    initialSource: String = "nl",
    initialTarget: String = "en"
) {
    var query by remember { mutableStateOf("") }
    var finishedOnly by remember { mutableStateOf(false) }
    var large by remember { mutableStateOf(true) }
    var faceToFace by remember { mutableStateOf(false) }
    var followNewest by remember { mutableStateOf(true) }
    var audioNotice by remember { mutableStateOf("") }
    var activeSpeechLineIndex by remember { mutableStateOf<Int?>(null) }
    var activeSpeechSide by remember { mutableStateOf<String?>(null) }
    var activeSpeechText by remember { mutableStateOf<String?>(null) }
    var activeSpeechRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val context = LocalContext.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun clearSpeech() {
        speaker.stop()
        activeSpeechLineIndex = null
        activeSpeechSide = null
        activeSpeechText = null
        activeSpeechRange = null
        audioNotice = ""
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
            audioNotice = "Deze gespreksregel kan niet veilig met een lokale stem worden voorgelezen."
            return
        }
        activeSpeechLineIndex = lineIndex
        activeSpeechSide = side
        activeSpeechText = safe
        activeSpeechRange = null
        audioNotice = "Lokale voorlezing voorbereiden…"
        speaker.speakSequence(
            lines = listOf(safe),
            languageTag = language,
            pace = pace,
            onProgress = { completed, total ->
                audioNotice = "Lokaal voorlezen: $completed/$total regels."
            },
            feedback = { audioNotice = it },
            onFinished = { completed ->
                activeSpeechLineIndex = null
                activeSpeechSide = null
                activeSpeechText = null
                activeSpeechRange = null
                if (completed) audioNotice = "Voorlezen voltooid."
            },
            onLineStarted = {
                activeSpeechRange = null
                audioNotice = if (side == "original")
                    "Nu voorgelezen: origineel." else "Nu voorgelezen: vertaling."
            },
            onWordRange = { _, start, endExclusive ->
                activeSpeechRange = start to endExclusive
            }
        )
    }

    DisposableEffect(speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                clearSpeech()
                audioNotice = "Voorlezen gestopt bij verlaten van het scherm."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    val scroll = rememberLazyListState()
    // Match the main live-caption timeline: a person's real scroll gesture
    // pauses automatic following so new captions cannot steal their place.
    // Programmatic scrollToItem does not emit UserInput and remains allowed.
    val pauseFollowOnUserScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    followNewest = false
                }
                return Offset.Zero
            }
        }
    }
    val admitted = lines.takeLast(80)
    fun completed(row: TypedConversationLine): Boolean =
        row.status == "Lokaal vertaald" || row.status == "Lokaal"
    val selected = admitted.filter { row ->
        (!finishedOnly || completed(row)) && (
            query.isBlank() ||
            row.original.contains(query.trim(), ignoreCase = true) ||
            (completed(row) && row.translated.contains(query.trim(), ignoreCase = true))
        )
    }
    val lastRow = selected.lastOrNull()
    LaunchedEffect(query, finishedOnly) {
        // An intentional filter change invalidates the current spoken selection.
        clearSpeech()
    }
    LaunchedEffect(selected) {
        // A new unrelated live caption must not cut off the older line a
        // person explicitly chose to hear. Stop only if that selected
        // original/translation disappears or changes (including eviction).
        val index = activeSpeechLineIndex
        val spoken = activeSpeechText
        if (index != null && spoken != null) {
            val current = selected.getOrNull(index)
            val currentText = when (activeSpeechSide) {
                "original" -> current?.original
                "translated" -> current
                    ?.takeIf { completed(it) }?.translated
                else -> null
            }
            if (currentText?.replace('\n', ' ')?.replace('\r', ' ')?.trim() != spoken) {
                clearSpeech()
            }
        }
    }
    LaunchedEffect(followNewest, selected.size, lastRow?.translated, query) {
        if (followNewest && query.isBlank() && selected.isNotEmpty()) {
            scroll.scrollToItem(selected.lastIndex)
        }
    }
    val rtl = setOf("ar", "fa", "ur", "he", "ps", "ckb")
    fun dir(code: String): TextDirection =
        if (code.substringBefore('-') in rtl) TextDirection.Rtl else TextDirection.Ltr

    if (faceToFace) {
        FaceToFaceConversationDialog(
            lines = admitted, onBack = { faceToFace = false },
            onSubmitTurn = onSubmitTurn, initialSource = initialSource,
            initialTarget = initialTarget
        )
        return
    }

    Dialog(
        onDismissRequest = { clearSpeech(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tuddy · Gesprek groot lezen",
                        style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { clearSpeech(); onDismiss() }) { Text("Sluiten") }
                }
                Text("Tik op een woord voor de kleine ingebouwde offline woordkaart. " +
                    "Een eigen betekenis wordt pas na jouw invoer en bewaarknop " +
                    "in je privéwoordenlijst opgeslagen.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Alleen dit tijdelijke gesprek · geen automatische opslag, " +
                    "deling of cloudspraak. Lokale vertalingen kunnen fouten bevatten.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = {
                        clearSpeech()
                        faceToFace = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("↕ Tegenover elkaar · tweetalige tafelmodus") }
                Text("Leg het toestel plat tussen twee mensen: bovenste helft draait 180° " +
                    "naar de ander. Alleen afgeronde lokale vertalingen; wissel zelf de kanten.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = query, onValueChange = {
                        query = it.take(80)
                        if (it.isNotBlank()) followNewest = false
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Zoek in afgeronde originele of vertaalde tekst") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { large = !large }) {
                        Text(if (large) "Kleinere tekst" else "Grotere tekst")
                    }
                    TextButton(onClick = {
                        query = ""
                        followNewest = true
                    }) { Text(if (followNewest) "↓ Nieuwste volgen" else "↓ Hervat live volgen") }
                    Text("${selected.size} regels", style = MaterialTheme.typography.labelSmall)
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Alleen afgeronde regels",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                    Switch(checked = finishedOnly, onCheckedChange = { finishedOnly = it })
                }
                if (!followNewest && query.isBlank()) {
                    Text(
                        "Historie lezen: automatisch meescrollen staat uit. " +
                            "Tik op 'Hervat live volgen' om terug te gaan naar de nieuwste regel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider()
                if (selected.isEmpty()) {
                    Text(if (admitted.isEmpty()) "Er zijn nog geen tijdelijke gespreksregels."
                    else "Geen regels gevonden voor deze selectie.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .nestedScroll(pauseFollowOnUserScroll),
                    state = scroll,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    itemsIndexed(selected) { index, row ->
                        val originalActive = activeSpeechLineIndex == index &&
                            activeSpeechSide == "original" && activeSpeechText != null
                        val translatedActive = activeSpeechLineIndex == index &&
                            activeSpeechSide == "translated" && activeSpeechText != null
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "${index + 1}. ${row.source.uppercase()} → " +
                                    "${row.target.uppercase()} · ${row.status}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (originalActive)
                                    MaterialTheme.colorScheme.secondaryContainer
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
                                            fontSize = if (large) 30.sp else 21.sp,
                                            lineHeight = if (large) 39.sp else 29.sp,
                                            fontWeight = FontWeight.Medium,
                                            textDirection = dir(row.source)
                                        ),
                                        onSaveWord = onSaveCuratedWord,
                                        readOwnMeaning = readOwnMeaning,
                                        saveOwnMeaning = onSaveOwnMeaning,
                                        activeSpeechText = activeSpeechText.takeIf { originalActive },
                                        activeSpeechRange = activeSpeechRange.takeIf { originalActive }
                                    )
                                }
                            }
                            if (completed(row)) {
                                if (row.status == "Lokaal vertaald" &&
                                    row.translated.isNotBlank()) {
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
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = if (large) 30.sp else 21.sp,
                                                    lineHeight = if (large) 39.sp else 29.sp,
                                                    textDirection = dir(row.target)
                                                ),
                                                onSaveWord = onSaveCuratedWord,
                                                readOwnMeaning = readOwnMeaning,
                                                saveOwnMeaning = onSaveOwnMeaning,
                                                activeSpeechText = activeSpeechText.takeIf { translatedActive },
                                                activeSpeechRange = activeSpeechRange.takeIf { translatedActive }
                                            )
                                        }
                                    }
                                } else {
                                    Text("Geen afzonderlijke vertaling",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text("Nog geen voltooide lokale vertaling. " +
                                    "Er wordt geen resultaat verzonnen.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            OfflineReadingTurnAudioActions(
                                turn = row,
                                activeSpeechText = activeSpeechText,
                                onSpeak = { text, language, side, pace ->
                                    pronounceLine(index, text, language, side, pace)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                TextButton(
                    onClick = {
                        clearSpeech()
                        audioNotice = "Lokale stem gestopt."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("■ Stop alle lokale voorlezing") }
                if (audioNotice.isNotBlank()) {
                    Text(audioNotice, style = MaterialTheme.typography.bodySmall)
                }
                Text("Alleen met een door Android als lokaal geïnstalleerd gemelde " +
                    "stem na jouw tik. Ontbreekt de stem, dan wordt niets " +
                    "naar een netwerkspraakdienst gestuurd.",
                    style = MaterialTheme.typography.bodySmall)
                Text("De meest recente maximaal 80 regels worden alleen in " +
                    "appgeheugen getoond. Wis gesprek verwijdert ze ook hier.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
