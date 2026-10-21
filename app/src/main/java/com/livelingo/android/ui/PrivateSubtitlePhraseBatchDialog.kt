package com.livelingo.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.livelingo.android.PersonalPhrase
import com.livelingo.android.PrivatePhraseTextPolicy
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleRowSelection
import java.util.Locale

/**
 * Manual, bounded, whole-batch study-card creation from authored subtitles.
 * No auto language inference, MT, ASR, transcript capture or partial storage.
 */
@Composable
internal fun PrivateSubtitlePhraseBatchDialog(
    cues: List<PrivateSubtitleCue>,
    onSaveBatch: (List<PersonalPhrase>) -> Boolean,
    onSaved: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember(cues) {
        mutableStateOf(if (cues.size == 1) "1" else "1-${minOf(8, cues.size)}")
    }
    var meaningLanguage by remember(cues) { mutableStateOf("nl") }
    // Keys are exact 0-based original cue positions, not filtered display rows.
    var authored by remember(cues, expression) {
        mutableStateOf<Map<Int, String>>(emptyMap())
    }
    var meanings by remember(cues, expression) {
        mutableStateOf<Map<Int, String>>(emptyMap())
    }
    var languages by remember(cues, expression) {
        mutableStateOf<Map<Int, String>>(emptyMap())
    }
    var approved by remember(cues, expression) { mutableStateOf(false) }
    var error by remember(cues, expression) { mutableStateOf("") }
    val selected = remember(cues, expression) {
        PrivateSubtitleRowSelection.parse(expression, cues.size)
            ?.takeIf { it.size in 1..8 }
    }
    val proposals = selected?.map { index ->
        PersonalPhrase(
            languages[index].orEmpty(),
            authored[index] ?: cues[index].text,
            meaningLanguage,
            meanings[index].orEmpty()
        )
    }
    val duplicateKeys = proposals?.map {
        it.language + "|" + it.phrase.lowercase(Locale.ROOT)
    }?.let { it.toSet().size != it.size } ?: false
    val valid = proposals != null && !duplicateKeys && proposals.all {
        it.language in setOf("kmr", "ckb", "ps") &&
            PrivatePhraseTextPolicy.valid(it.phrase, 120) &&
            PrivatePhraseTextPolicy.valid(it.ownMeaning, 180)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meerdere eigen ondertitelzinnen bewaren") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Kies zelf maximaal acht ORIGINELE ondertitelregelnummers. " +
                    "Iedere zin krijgt een eigen handmatig taallabel en " +
                    "door jou geschreven betekenis. Tuddy herkent of vertaalt niets.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = expression,
                    onValueChange = {
                        if (it.length <= 120) expression = it
                        approved = false
                        error = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bronregels, bijvoorbeeld 1-3,7,10") },
                    singleLine = true
                )
                if (selected == null) {
                    Text("Kies 1 tot 8 bestaande bronregels zonder herhalingen. " +
                        "Een ongeldige keuze bewaart niets.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("${selected.size} originele regels gekozen. " +
                        "Wijziging van je selectie wist alleen deze tijdelijke invoer.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Ik schrijf de betekenissen in:",
                    style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("nl" to "Nederlands", "en" to "English").forEach { (code, label) ->
                        TextButton(onClick = {
                            meaningLanguage = code
                            approved = false
                            error = ""
                        }) { Text((if (meaningLanguage == code) "✓ " else "") + label) }
                    }
                }
                selected?.forEach { index ->
                    HorizontalDivider()
                    Text("Bronregel ${index + 1} · ${cues[index].startLabel} → " +
                        cues[index].endLabel,
                        style = MaterialTheme.typography.titleSmall)
                    Text("Mijn eigen keuze voor de brontaal:",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf("kmr" to "Kurmanji", "ckb" to "Sorani",
                            "ps" to "Pasjtoe").forEach { (code, label) ->
                            TextButton(onClick = {
                                languages = languages + (index to code)
                                approved = false
                                error = ""
                            }) {
                                Text((if (languages[index] == code) "✓ " else "") + label,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    val phrase = authored[index] ?: cues[index].text
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = {
                            if (it.length <= 2400 && PrivatePhraseTextPolicy.wellFormed(it))
                                authored = authored + (index to it)
                            approved = false
                            error = ""
                        },
                        label = { Text("Mijn nagelezen zin · max. 120 tekens, één regel") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl),
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                    if (!PrivatePhraseTextPolicy.valid(phrase, 120)) Text(
                        "Pas deze zin zelf aan tot één geldige regel. " +
                            "De volledige originele cue is niet afgekapt.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = meanings[index].orEmpty(),
                        onValueChange = {
                            if (it.length <= 180 && PrivatePhraseTextPolicy.wellFormed(it))
                                meanings = meanings + (index to it)
                            approved = false
                            error = ""
                        },
                        label = { Text("Mijn eigen betekenis · max. 180 tekens") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl),
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                }
                if (duplicateKeys) Text(
                    "De gekozen regels bevatten dubbele eigen zinnen in dezelfde taal. " +
                        "Maak ze verschillend of pas de bronregelselectie aan.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text("Alle kaarten worden samen bewaard of helemaal niet. " +
                    "Bestaande zinnen worden nooit vervangen; geen audit of " +
                    "native-speakercontrole en geen automatische vertaling.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Checkbox(checked = approved, enabled = valid,
                        onCheckedChange = { approved = it; error = "" })
                    Text("Ik heb elke taal, zin en zelfgeschreven betekenis " +
                        "nagelezen en wil deze volledige selectie privé opslaan.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (error.isNotBlank()) Text(error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = valid && approved, onClick = {
                val fresh = selected?.map { index ->
                    PersonalPhrase(languages[index].orEmpty(),
                        authored[index] ?: cues[index].text,
                        meaningLanguage, meanings[index].orEmpty())
                }
                val safe = fresh != null && fresh.size in 1..8 &&
                    fresh.all {
                        it.language in setOf("kmr", "ckb", "ps") &&
                            PrivatePhraseTextPolicy.valid(it.phrase, 120) &&
                            PrivatePhraseTextPolicy.valid(it.ownMeaning, 180)
                    } && fresh.map {
                        it.language + "|" + it.phrase.lowercase(Locale.ROOT)
                    }.toSet().size == fresh.size
                if (safe && fresh != null && approved && onSaveBatch(fresh))
                    onSaved(fresh.size)
                else {
                    approved = false
                    error = "Niets opgeslagen: een zin bestaat mogelijk al, " +
                        "een regel is veranderd of de privé-zinnenlijst is vol."
                }
            }) { Text("Bewaar de volledige eigen selectie") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleer · niets bewaren") }
        }
    )
}
