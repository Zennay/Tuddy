package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleDraftEditor
import com.livelingo.android.SubtitleDraftFormat
import com.livelingo.android.TargetCatalog
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

/** Explicitly opened, ephemeral subtitle document editor, never the live transcript. */
@Composable
internal fun PrivateSubtitleFileDialog(
    cues: List<PrivateSubtitleCue>,
    onDismiss: () -> Unit,
    onSaveCopy: (List<PrivateSubtitleCue>, SubtitleDraftFormat) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var shownIndex by remember { mutableIntStateOf(-1) }
    var large by remember { mutableStateOf(true) }
    var draft by remember(cues) { mutableStateOf(cues) }
    var edited by remember(cues) { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var splitIndex by remember { mutableStateOf<Int?>(null) }
    var mergeIndex by remember { mutableStateOf<Int?>(null) }
    var splitFirst by remember { mutableStateOf("") }
    var splitSecond by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }
    var requestSave by remember { mutableStateOf<SubtitleDraftFormat?>(null) }
    var notice by remember { mutableStateOf("") }
    val found = draft.mapIndexed { idx, row -> idx to row }
        .filter { (_, row) -> row.text.contains(query.trim(), ignoreCase = true) }
    val current = found.getOrNull(shownIndex)?.second
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    var chosenVoiceLanguage by remember { mutableStateOf<String?>(null) }
    var voicePickerOpen by remember { mutableStateOf(false) }
    var spokenNotice by remember { mutableStateOf("") }
    DisposableEffect(speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE)
                speaker.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); speaker.close() }
    }
    LaunchedEffect(current?.text, current?.startMillis, current?.endMillis) {
        speaker.stop()
        spokenNotice = ""
    }
    fun pronounceCue(slow: Boolean) {
        val lang = chosenVoiceLanguage ?: return
        val safe = current?.text?.replace('\n', ' ')?.trim().orEmpty()
        if (safe.isBlank() || safe.length > 400) {
            spokenNotice = "Alleen geselecteerde tekst tot 400 tekens kan lokaal worden voorgelezen."
        } else speaker.speak(safe, lang,
            if (slow) LocalSpeechPace.SLOW else LocalSpeechPace.NORMAL) {
            spokenNotice = it
        }
    }

    fun nudgeSelected(start: Long, end: Long) {
        val selected = found.getOrNull(shownIndex)?.first ?: return
        val updated = PrivateSubtitleDraftEditor.adjustCue(draft, selected, start, end)
        if (updated == null) notice = "Tijd geweigerd: begin/eind of volgorde ongeldig."
        else { draft = updated; edited = true; notice = "Tijd van deze regel aangepast." }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Eigen ondertitelconcept · SRT/WebVTT",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text("Alle tekst en tijdcodes komen uit je gekozen bestand. " +
                    "Geen geverifieerde vertaling of audio-synchronisatie.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = query, onValueChange = {
                    query = it.take(100)
                    shownIndex = -1
                }, label = { Text("Zoek in dit document") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("${found.size} van ${draft.size} regels" +
                    (if (edited) " · niet bewaarde wijzigingen" else ""),
                    style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        val next = PrivateSubtitleDraftEditor.shift(draft, -500L)
                        if (next == null) notice = "Kan niet eerder: negatieve tijdcode."
                        else { draft = next; edited = true; notice = "0,5 s eerder gezet." }
                    }) { Text("−0,5 s") }
                    TextButton(onClick = {
                        val next = PrivateSubtitleDraftEditor.shift(draft, 500L)
                        if (next == null) notice = "Kan niet later: tijdgrens bereikt."
                        else { draft = next; edited = true; notice = "0,5 s later gezet." }
                    }) { Text("+0,5 s") }
                    TextButton(enabled = edited, onClick = {
                        draft = cues
                        edited = false
                        shownIndex = -1
                        notice = "Bewerkingen weggegooid; origineel ongewijzigd."
                    }) { Text("Herstel") }
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.SRT }) {
                        Text("Bewaar kopie SRT…")
                    }
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.WEBVTT }) {
                        Text("Bewaar kopie VTT…")
                    }
                }
                if (notice.isNotBlank()) Text(notice,
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                if (current == null) {
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        itemsIndexed(found) { index, (_, cue) ->
                            Column {
                                TextButton(onClick = { shownIndex = index }) {
                                    Text("${index + 1}. ${cue.startLabel} → ${cue.endLabel} · Open")
                                }
                                Text(cue.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrRtl),
                                    maxLines = 2)
                                HorizontalDivider()
                            }
                        }
                    }
                } else {
                    Text("${current.startLabel} → ${current.endLabel}",
                        style = MaterialTheme.typography.labelLarge)
                    Text(current.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl),
                        fontSize = if (large) 30.sp else 22.sp,
                        lineHeight = if (large) 40.sp else 30.sp,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()))
                    Text("Uitspraak · kies de taal van deze bestandsregel zelf",
                        style = MaterialTheme.typography.labelSmall)
                    Box {
                        TextButton(onClick = { voicePickerOpen = true }) {
                            val label = TargetCatalog.targets.firstOrNull {
                                it.code == chosenVoiceLanguage
                            }?.title ?: "Kies eerst taal voor lokaal voorlezen…"
                            Text(label)
                        }
                        DropdownMenu(
                            expanded = voicePickerOpen,
                            onDismissRequest = { voicePickerOpen = false }
                        ) {
                            TargetCatalog.targets.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.title) },
                                    onClick = {
                                        speaker.stop()
                                        chosenVoiceLanguage = language.code
                                        spokenNotice = ""
                                        voicePickerOpen = false
                                    }
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = chosenVoiceLanguage != null &&
                            current.text.length <= 400,
                            onClick = { pronounceCue(false) }) { Text("🔊 Hoor lokaal") }
                        TextButton(enabled = chosenVoiceLanguage != null &&
                            current.text.length <= 400,
                            onClick = { pronounceCue(true) }) { Text("Rustig") }
                        TextButton(onClick = {
                            speaker.stop()
                            spokenNotice = "Uitspraak gestopt."
                        }) { Text("■ Stop") }
                    }
                    if (spokenNotice.isNotBlank()) {
                        Text(spokenNotice, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = {
                        editIndex = found[shownIndex].first
                        editValue = current.text
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("✎ Corrigeer deze regel handmatig")
                    }
                    Text("Tijden van alleen deze regel · stap 0,5 s",
                        style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { nudgeSelected(-500L, 0L) }) {
                            Text("Begin −0,5 s")
                        }
                        TextButton(onClick = { nudgeSelected(500L, 0L) }) {
                            Text("Begin +0,5 s")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { nudgeSelected(0L, -500L) }) {
                            Text("Eind −0,5 s")
                        }
                        TextButton(onClick = { nudgeSelected(0L, 500L) }) {
                            Text("Eind +0,5 s")
                        }
                        TextButton(enabled = draft.size > 1,
                            onClick = { deleteIndex = found[shownIndex].first }) {
                            Text("Wis regel…")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = draft.size < 500,
                            onClick = {
                                val rowIndex = found[shownIndex].first
                                val cut = current.text.indexOf(' ', current.text.length / 2)
                                splitFirst = if (cut > 0) current.text.substring(0, cut) else current.text
                                splitSecond = if (cut > 0) current.text.substring(cut + 1) else ""
                                splitIndex = rowIndex
                            }) { Text("Splits deze regel…") }
                        TextButton(enabled = found[shownIndex].first < draft.lastIndex,
                            onClick = { mergeIndex = found[shownIndex].first }) {
                            Text("Combineer volgende…")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = shownIndex > 0,
                            onClick = { shownIndex-- }) { Text("← Vorige") }
                        TextButton(onClick = { large = !large }) {
                            Text(if (large) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(enabled = shownIndex < found.lastIndex,
                            onClick = { shownIndex++ }) { Text("Volgende →") }
                        TextButton(onClick = { shownIndex = -1 }) { Text("Lijst") }
                    }
                }
                Text("Deze bewerkingen blijven in RAM totdat je bewust een nieuw " +
                    "bestand bewaart. Het geopende origineel wordt nooit overschreven. " +
                    "Android-bestandsproviders kunnen extern synchroniseren.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    val splitting = splitIndex
    if (splitting != null) {
        AlertDialog(onDismissRequest = { splitIndex = null },
            title = { Text("Splits één ondertitel in twee") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kies zelf twee zinsdelen. De bestaande tijdsduur " +
                        "wordt gelijk gedeeld; dit is geen gecontroleerde audiotiming.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = splitFirst,
                        onValueChange = { if (it.length <= 2400) splitFirst = it },
                        label = { Text("Eerste deel") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 4)
                    OutlinedTextField(
                        value = splitSecond,
                        onValueChange = { if (it.length <= 2400) splitSecond = it },
                        label = { Text("Tweede deel") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 4)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = PrivateSubtitleDraftEditor.splitCue(
                        draft, splitting, splitFirst.trim(), splitSecond.trim())
                    if (next == null) {
                        notice = "Splitsing geweigerd: tekst/tijdsduur of cuevolgorde ongeldig."
                    } else {
                        draft = next
                        edited = true
                        shownIndex = -1
                        notice = "Regel in twee bewerkbare regels gesplitst."
                    }
                    splitIndex = null
                }) { Text("Splits tijdelijk") }
            },
            dismissButton = {
                TextButton(onClick = { splitIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val merging = mergeIndex
    if (merging != null) {
        AlertDialog(onDismissRequest = { mergeIndex = null },
            title = { Text("Volgende ondertitel samenvoegen?") },
            text = { Text("De twee teksten worden samengevoegd in één cue, " +
                "van het eerste begin tot het tweede einde. Dit is alleen " +
                "een wijziging in je tijdelijke document, geen audio-uitlijning. " +
                "Herstel zet het oorspronkelijke bestand terug.") },
            confirmButton = {
                TextButton(onClick = {
                    val next = PrivateSubtitleDraftEditor.mergeWithNext(draft, merging)
                    if (next == null) notice = "Samenvoegen geweigerd: te veel tekst of ongeldige tijd."
                    else {
                        draft = next
                        edited = true
                        shownIndex = -1
                        notice = "Beide regels tot één tijdelijke cue samengevoegd."
                    }
                    mergeIndex = null
                }) { Text("Voeg samen") }
            },
            dismissButton = {
                TextButton(onClick = { mergeIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val requestedDelete = deleteIndex
    if (requestedDelete != null) {
        AlertDialog(onDismissRequest = { deleteIndex = null },
            title = { Text("Deze ondertitelregel verwijderen?") },
            text = { Text("Alleen de tijdelijke documentkopie verandert. " +
                "Het originele bestand blijft ongewijzigd; Herstel kan dit terugdraaien.") },
            confirmButton = {
                TextButton(onClick = {
                    val newDraft = PrivateSubtitleDraftEditor.dropCue(draft, requestedDelete)
                    if (newDraft != null) {
                        draft = newDraft
                        edited = true
                        shownIndex = -1
                        notice = "Regel verwijderd uit tijdelijke kopie."
                    } else notice = "De laatste regel kan niet worden verwijderd."
                    deleteIndex = null
                }) { Text("Wis alleen deze regel") }
            },
            dismissButton = {
                TextButton(onClick = { deleteIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val position = editIndex
    if (position != null) {
        AlertDialog(onDismissRequest = { editIndex = null },
            title = { Text("Handmatige tekstcorrectie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("De tijdcode blijft gelijk. Tuddy controleert of vertaalt " +
                        "de nieuwe tekst niet automatisch.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = editValue,
                        onValueChange = { if (it.length <= 2400) editValue = it },
                        label = { Text("Ondertiteltekst") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = PrivateSubtitleDraftEditor.revise(draft, position, editValue)
                    if (next == null) {
                        notice = "Ongeldige tekst. Maximaal acht regels, zonder controlecodes."
                    } else {
                        draft = next
                        edited = true
                        notice = "Tekst aangepast in tijdelijk concept."
                        shownIndex = -1
                    }
                    editIndex = null
                }) { Text("Gebruik correctie") }
            },
            dismissButton = {
                TextButton(onClick = { editIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val chosenFormat = requestSave
    if (chosenFormat != null) {
        AlertDialog(onDismissRequest = { requestSave = null },
            title = { Text("Nieuw ondertitelbestand bewaren?") },
            text = { Text("Je kiest bewust een Android-bestandsprovider. " +
                "Die kan jouw tekst buiten Tuddy synchroniseren. Tijdcodes zijn " +
                "handmatige schattingen en teksten zijn niet geverifieerd. " +
                "Het eerder geopende bestand blijft ongewijzigd.") },
            confirmButton = {
                TextButton(onClick = {
                    requestSave = null
                    if (PrivateSubtitleDraftEditor.snapshot(draft, chosenFormat) == null)
                        notice = "Ongeldig concept: kan geen ondertitelbestand maken."
                    else onSaveCopy(draft.toList(), chosenFormat)
                }) { Text("Kies opslaglocatie…") }
            },
            dismissButton = {
                TextButton(onClick = { requestSave = null }) { Text("Annuleren") }
            }
        )
    }
}
