package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.LocalCaptionSpeaker

/**
 * Explicitly presented, user-written words only. Keeps exactly the caller's
 * deck/filter and selection; never imports/sends notes or claims human review.
 * Audio is possible only on a deliberate tap with an already-installed
 * Android voice that reports no network dependency. The saved cards never change.
 */
@Composable
internal fun FaceToFacePersonalPhraseDialog(
    cards: List<PersonalPhrase>,
    initialIndex: Int,
    onBack: (Int) -> Unit
) {
    var index by remember(cards, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, cards.lastIndex.coerceAtLeast(0)))
    }
    var originalOnFarSide by remember { mutableStateOf(true) }
    var large by remember { mutableStateOf(true) }
    var practice by remember { mutableStateOf(false) }
    var revealed by remember(index, practice) { mutableStateOf(false) }
    var audioNotice by remember { mutableStateOf("") }
    val selected = cards.getOrNull(index)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    DisposableEffect(lifecycle, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) speaker.stop()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    LaunchedEffect(index, originalOnFarSide, practice, selected) {
        speaker.stop()
        audioNotice = ""
    }
    val meaningHidden = practice && !revealed
    fun direction(lang: String): TextDirection =
        if (lang in setOf("ckb", "ps", "ar", "fa", "ur", "he"))
            TextDirection.Rtl else TextDirection.Ltr
    fun returnToCard() { speaker.stop(); onBack(index) }
    Dialog(onDismissRequest = { returnToCard() },
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer { rotationZ = 180f }
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val text = if (originalOnFarSide) selected?.phrase
                            else if (meaningHidden) "Eigen betekenis verborgen" else selected?.ownMeaning
                        val language = if (originalOnFarSide) selected?.language
                            else selected?.meaningLanguage
                        Text(if (originalOnFarSide) "TEGENOVER · EIGEN ZIN"
                            else "TEGENOVER · EIGEN BETEKENIS",
                            style = MaterialTheme.typography.labelLarge)
                        Text(text ?: "Geen kaart geselecteerd.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = direction(language.orEmpty())),
                            fontSize = if (large) 32.sp else 24.sp,
                            lineHeight = if (large) 41.sp else 31.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
                        Text(language?.uppercase() ?: "—",
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = index > 0,
                            onClick = { index-- }) { Text("← Vorige") }
                        Text("${if (selected == null) 0 else index + 1}/${cards.size}",
                            modifier = Modifier.padding(top = 14.dp),
                            style = MaterialTheme.typography.labelMedium)
                        TextButton(enabled = index < cards.lastIndex,
                            onClick = { index++ }) { Text("Volgende →") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            originalOnFarSide = !originalOnFarSide
                        }) { Text("↕ Kanten") }
                        TextButton(onClick = { large = !large }) {
                            Text(if (large) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(onClick = { returnToCard() }) {
                            Text("Terug")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { practice = !practice }) {
                            Text(if (practice) "✓ Oefenmodus" else "○ Oefenmodus")
                        }
                        TextButton(enabled = meaningHidden && selected != null,
                            onClick = { revealed = true }) {
                            Text(if (meaningHidden) "Toon betekenis" else "Betekenis zichtbaar")
                        }
                    }
                    if (selected != null) {
                        OfflinePhraseSpeechActions(
                            original = selected.phrase,
                            originalLanguage = selected.language,
                            meaning = selected.ownMeaning,
                            meaningLanguage = selected.meaningLanguage,
                            originalLabel = "Mijn zin",
                            meaningLabel = "Mijn betekenis",
                            speaker = speaker,
                            meaningReady = !meaningHidden,
                            onNotice = { audioNotice = it }
                        )
                    }
                    if (audioNotice.isNotBlank()) {
                        Text(audioNotice,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center)
                    }
                    Text("Voorlezen alleen na jouw tik met een door Android als lokaal " +
                        "gemelde, geïnstalleerde stem. Ontbreekt die, dan geen cloudfallback.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center)
                    PrivateTableScreenAwakeControl()
                    Text("Door jou geschreven, niet door Tuddy gecontroleerd. " +
                        "Geen delen, opslagwijziging of netwerkactie.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center)
                }
                Surface(modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val text = if (originalOnFarSide)
                            if (meaningHidden) "Eigen betekenis verborgen" else selected?.ownMeaning
                        else selected?.phrase
                        val language = if (originalOnFarSide) selected?.meaningLanguage
                            else selected?.language
                        Text(if (originalOnFarSide) "HIER · EIGEN BETEKENIS"
                            else "HIER · EIGEN ZIN",
                            style = MaterialTheme.typography.labelLarge)
                        Text(text ?: "Je hebt nog geen eigen kaart gemaakt.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = direction(language.orEmpty())),
                            fontSize = if (large) 32.sp else 24.sp,
                            lineHeight = if (large) 41.sp else 31.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
                        Text(language?.uppercase() ?: "—",
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
