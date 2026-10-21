package com.livelingo.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.livelingo.android.LiveState
import com.livelingo.android.WordInsight
import com.livelingo.android.model.Languages
import com.livelingo.android.model.wordAt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFF0B1018)
private val Raised = Color(0xFF192330)
private val Bright = Color(0xFF69F5B3)
private val Muted = Color(0xFFA7B5C6)

@Composable
fun LiveLingoScreen(
    state: LiveState,
    speechAvailable: Boolean,
    canAutoSwitchVoice: Boolean,
    onPrepare: () -> Unit,
    onSelectTarget: (String) -> Unit,
    onPeekWord: (String, String?) -> Unit,
    onDismissPeek: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Bright, background = Background, surface = Raised)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(top = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("LiveLingo", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("THE WORLD, SUBTITLED", color = Muted, fontSize = 10.sp, letterSpacing = 1.8.sp)
                    }
                    Surface(color = Color(0xFF173B2C), shape = RoundedCornerShape(20.dp)) {
                        Text("●  LOCAL AI", color = Bright, fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp))
                    }
                }
                Spacer(Modifier.height(19.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SPOKEN LANGUAGE", fontSize = 10.sp, color = Muted)
                        Text(state.detectedSource?.let { Languages.find(it)?.name } ?: "Auto-detect",
                            fontSize = 18.sp, color = Color.White)
                        Text(if (canAutoSwitchVoice) "Speech packs and device support vary" else "Voice auto-switch requires Android 14+",
                            fontSize = 10.sp, color = Muted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TRANSLATE TO", fontSize = 10.sp, color = Muted)
                        TargetSelector(target = state.targetLanguage, enabled = !state.preparing,
                            onSelect = onSelectTarget)
                    }
                }
                Spacer(Modifier.height(15.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("CONVERSATION", fontSize = 11.sp, color = Muted, letterSpacing = 1.1.sp)
                    TextButton(onClick = onClear,
                        enabled = state.conversation.lines.isNotEmpty() || state.conversation.activeSource.isNotBlank()) {
                        Text("Clear transcript", color = Muted, fontSize = 11.sp)
                    }
                }
                Transcript(modifier = Modifier.weight(1f), state = state,
                    onPeekWord = onPeekWord, onDismissPeek = onDismissPeek)
                Controls(state = state, speechAvailable = speechAvailable,
                    onPrepare = onPrepare, onStart = onStart, onStop = onStop)
            }
        }
    }
}

@Composable
private fun TargetSelector(target: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    OutlinedButton(onClick = { query = ""; expanded = true }, enabled = enabled) {
        val shortLabel = Languages.findTarget(target)?.name?.substringBefore(" ·") ?: target
        Text("$shortLabel  ▾", fontSize = 14.sp, maxLines = 1)
    }
    if (expanded) {
        val matches = Languages.search(query)
        val previews = Languages.searchCommunity(query)
        val unavailable = Languages.requested.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Translate to") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Search language") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Text("59 ML Kit languages + 3 limited offline beginner phrasebooks",
                        color = Muted, fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(matches, key = { it.code }) { language ->
                            TextButton(
                                onClick = { expanded = false; onSelect(language.code) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text((if (language.code == target) "✓  " else "") + language.name,
                                    modifier = Modifier.fillMaxWidth(), color = Color.White)
                            }
                        }
                        if (previews.isNotEmpty()) {
                            item(key = "phrasebook-title") {
                                Text("BEGINNER WORDS ONLY · no general translation / voice packs",
                                    color = Color(0xFFFFC88E), fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp))
                            }
                            items(previews, key = { "preview-${it.code}" }) { language ->
                                TextButton(onClick = { expanded = false; onSelect(language.code) },
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text((if (target == language.code) "✓  " else "") + language.name,
                                        color = Color(0xFFFFC88E), modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                        if (unavailable.isNotEmpty()) {
                            item(key = "unavailable-title") {
                                Text("More requested languages · not available offline yet",
                                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                            }
                            items(unavailable, key = { "request-${it.code}" }) { language ->
                                Text("${language.name} · coming later",
                                    color = Muted, fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth().padding(10.dp))
                            }
                        }
                        if (matches.isEmpty() && previews.isEmpty() && unavailable.isEmpty()) {
                            item { Text("No matching language", color = Muted) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { expanded = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun Transcript(
    modifier: Modifier,
    state: LiveState,
    onPeekWord: (String, String?) -> Unit,
    onDismissPeek: () -> Unit,
) {
    var following by remember { mutableStateOf(true) }
    val list = rememberLazyListState()
    val captions = state.conversation.lines
    val active = state.conversation.activeSource.isNotBlank()
    val total = captions.size + (if (active) 1 else 0)
    val manualScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) following = false
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(total, state.conversation.activeSource, state.conversation.activeTranslation,
        captions.lastOrNull()?.translation, following) {
        if (following && total > 0) list.animateScrollToItem(total - 1)
    }
    Box(modifier = modifier.fillMaxWidth()) {
        if (total == 0) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("♪", color = Bright, fontSize = 48.sp)
                Text("Conversations, like lyrics.", color = Color.White,
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Speak naturally. Auto-detect the speaker's language; read it in your chosen language.",
                    color = Muted, fontSize = 14.sp)
                Text("Press and hold a word for a tiny definition preview.", color = Bright, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(manualScroll),
                state = list,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(captions, key = { it.id }) { line ->
                    SubtitleCard(source = line.source, target = line.translation,
                        sourceLanguage = line.sourceLanguage, targetLanguage = line.targetLanguage,
                        timestamp = line.timestamp, active = false,
                        onPeekWord = onPeekWord, onDismissPeek = onDismissPeek)
                }
                if (active) {
                    item(key = "active") {
                        SubtitleCard(source = state.conversation.activeSource,
                            target = state.conversation.activeTranslation.takeIf { it.isNotBlank() },
                            sourceLanguage = state.conversation.activeLanguage,
                            targetLanguage = state.targetLanguage,
                            timestamp = System.currentTimeMillis(), active = true,
                            onPeekWord = onPeekWord, onDismissPeek = onDismissPeek)
                    }
                }
            }
            if (!following) {
                Button(onClick = { following = true },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Bright, contentColor = Background),
                ) { Text("↓ Follow live") }
            }
        }
        // Non-focusable small popup: never navigates and never blocks the underlying press-release.
        state.insight?.let { insight ->
            Popup(alignment = Alignment.TopCenter,
                properties = PopupProperties(focusable = false,
                    dismissOnBackPress = false, dismissOnClickOutside = false)) {
                WordPreview(insight)
            }
        }
    }
}

@Composable
private fun WordPreview(insight: WordInsight) {
    Surface(modifier = Modifier.widthIn(max = 292.dp).padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp), color = Color(0xFF29384A), shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Bright.copy(alpha = 0.55f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            insight.illustration?.let { Text(it, fontSize = 29.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(insight.word, color = Color.White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                insight.translation?.let { translation ->
                    Text(translation, color = Bright, fontSize = 14.sp)
                }
                Text(insight.definition ?: "No bundled definition · translation only",
                    color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
                Text("Release to close", color = Bright, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SubtitleCard(
    source: String,
    target: String?,
    sourceLanguage: String?,
    targetLanguage: String,
    timestamp: Long,
    active: Boolean,
    onPeekWord: (String, String?) -> Unit,
    onDismissPeek: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF18382F) else Raised),
        border = if (active) BorderStroke(1.dp, Bright.copy(alpha = 0.65f)) else null) {
        Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text((if (active) "●  SPEAKING NOW" else "ORIGINAL") +
                    (sourceLanguage?.let { " · ${Languages.find(it)?.name ?: it}" } ?: " · Detecting…"),
                    color = if (active) Bright else Muted, fontSize = 10.sp)
                if (!active) {
                    val time = remember(timestamp) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                    }
                    Text(time, color = Muted, fontSize = 10.sp)
                }
            }
            PressableWordText(source, language = sourceLanguage, active = active,
                onPeekWord = onPeekWord, onDismissPeek = onDismissPeek)
            Text("${if (Languages.isCommunityPhrasebook(targetLanguage)) "BEGINNER WORDS ONLY" else "TRANSLATION"} · ${Languages.findTarget(targetLanguage)?.name ?: targetLanguage}",
                fontSize = 10.sp, color = Muted)
            PressableWordText(target ?: "Identifying / translating…", language = targetLanguage,
                active = false, translated = true, enabled = target != null,
                onPeekWord = onPeekWord, onDismissPeek = onDismissPeek)
        }
    }
}

@Composable
private fun PressableWordText(
    value: String,
    language: String?,
    active: Boolean,
    translated: Boolean = false,
    enabled: Boolean = true,
    onPeekWord: (String, String?) -> Unit,
    onDismissPeek: () -> Unit,
) {
    var layout by remember(value) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = value,
        modifier = if (!enabled) Modifier else Modifier.pointerInput(value, language) {
            detectTapGestures(onPress = { point ->
                val index = layout?.getOffsetForPosition(point)
                val word = index?.let { wordAt(value, it) }
                if (word != null) {
                    onPeekWord(word, language)
                    // onPress suspends until release OR cancellation (e.g. scrolling).
                    tryAwaitRelease()
                    onDismissPeek()
                }
            })
        },
        onTextLayout = { layout = it },
        color = if (translated && enabled) Bright else if (translated) Muted else Color.White,
        fontSize = if (active) 24.sp else 21.sp,
        lineHeight = if (active) 32.sp else 28.sp,
        fontWeight = if (active || translated) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
private fun Controls(
    state: LiveState,
    speechAvailable: Boolean,
    onPrepare: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Raised) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp)
                    .background(if (state.listening) Bright else Muted, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(state.message, fontSize = 12.sp, color = Muted, modifier = Modifier.weight(1f))
            }
            when {
                !speechAvailable -> Text("No local speech recognizer detected. Cloud recognition is disabled.",
                    color = Color(0xFFFFC88E), fontSize = 13.sp)
                !state.prepared -> Button(onClick = onPrepare, enabled = !state.preparing,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.preparing) "Downloading language models…" else "Prepare offline AI models")
                }
                state.listening -> Button(onClick = onStop, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7E8E),
                        contentColor = Background)) { Text("■ Stop listening") }
                else -> Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("● Start live subtitles")
                }
            }
            Text("No account · On-device voice · Additional languages download over Wi-Fi on first use",
                fontSize = 11.sp, color = Muted)
        }
    }
}
