package com.livelingo.android.ui

import android.content.Intent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PersonalPhrase

@Composable
private fun PhraseSide(text: String, code: String, label: String, large: Boolean) {
    val rtl = code in setOf("ckb", "ps", "ar", "fa", "ur")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (large) 34.sp else 25.sp,
                lineHeight = if (large) 44.sp else 34.sp,
                textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Deliberately visible, all-local presentation of the user's OWN notes.
 * No ASR, ML Kit, TTS or cloud service. Android sharing is opt-in twice.
 */
@Composable
internal fun OfflinePhrasePresenter(
    notes: List<PersonalPhrase>,
    initial: PersonalPhrase,
    onDismiss: () -> Unit
) {
    val cards = remember(notes, initial) {
        notes.filter { it.language == initial.language }
            .sortedWith(compareBy(PersonalPhrase::phrase))
    }
    var selectedIndex by remember(cards, initial) {
        mutableIntStateOf(cards.indexOfFirst { it == initial }.coerceAtLeast(0))
    }
    var large by remember { mutableStateOf(true) }
    var showMeaningFirst by remember { mutableStateOf(false) }
    var pendingShare by remember { mutableStateOf<PersonalPhrase?>(null) }
    var shareNotice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val selected = cards.getOrNull(selectedIndex)
    fun confirmShare(includeOwnMeaning: Boolean) {
        val chosen = pendingShare
        pendingShare = null
        if (chosen == null || chosen != selected || chosen !in notes) {
            shareNotice = "De kaart is gewijzigd. Kies opnieuw delen."
            return
        }
        val content = if (includeOwnMeaning) {
            chosen.language.uppercase() + ": " + chosen.phrase + "\n" +
                "Mijn eigen betekenis (" + chosen.meaningLanguage.uppercase() + "): " +
                chosen.ownMeaning + "\n" +
                "(Door mij geschreven; niet gecontroleerd door Tuddy)"
        } else chosen.phrase
        if (content.isBlank() || content.length > 400) {
            shareNotice = "Deze eigen zin kan niet veilig worden gedeeld."
            return
        }
        val started = runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(
                send, "Kies waar je jouw eigen zin deelt"
            ))
            true
        }.getOrDefault(false)
        shareNotice = if (started)
            "Android-deelvenster geopend. Jij kiest of en waar je de zin verstuurt."
        else "Delen niet beschikbaar. Er is geen zin verzonden."
    }
    val label = when (initial.language) {
        "kmr" -> "Kurmanji"
        "ckb" -> "Sorani"
        "ps" -> "Pasjtoe"
        else -> initial.language.uppercase()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.86f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mijn gesprekskaart · " + label,
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text(
                    "Door jou geschreven · geen geverifieerde vertaling. " +
                        "Toon alleen zinnen die je met iemand wilt delen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (shareNotice.isNotBlank()) {
                    Text(shareNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider()
                if (selected == null) {
                    Text("Deze opgeslagen zin is niet meer beschikbaar.")
                    Spacer(Modifier.weight(1f))
                } else {
                    Column(
                        modifier = Modifier.weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        if (showMeaningFirst) {
                            PhraseSide(selected.ownMeaning, selected.meaningLanguage,
                                "Eigen betekenis (" + selected.meaningLanguage.uppercase() + ")", large)
                            HorizontalDivider()
                            PhraseSide(selected.phrase, selected.language,
                                "Eigen zin (" + label + ")", large)
                        } else {
                            PhraseSide(selected.phrase, selected.language,
                                "Eigen zin (" + label + ")", large)
                            HorizontalDivider()
                            PhraseSide(selected.ownMeaning, selected.meaningLanguage,
                                "Eigen betekenis (" + selected.meaningLanguage.uppercase() + ")", large)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = { showMeaningFirst = !showMeaningFirst }) {
                        Text("Wissel volgorde")
                    }
                    TextButton(onClick = { large = !large }) {
                        Text(if (large) "Kleiner" else "Groter")
                    }
                    TextButton(enabled = selected != null,
                        onClick = {
                            pendingShare = selected
                            shareNotice = ""
                        }) { Text("Deel…") }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        enabled = selectedIndex > 0,
                        onClick = { selectedIndex-- }
                    ) { Text("← Vorige") }
                    Text((if (selected == null) 0 else selectedIndex + 1).toString() +
                        " / " + cards.size,
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        enabled = selectedIndex < cards.lastIndex,
                        onClick = { selectedIndex++ }
                    ) { Text("Volgende →") }
                }
            }
        }
    }
    val request = pendingShare
    if (request != null) {
        AlertDialog(
            onDismissRequest = { pendingShare = null },
            title = { Text("Mijn eigen zin extern delen?") },
            text = {
                Text("Je deelt een door jou geschreven zin via een externe " +
                    "Android-app. Die app kan de tekst bewaren, uploaden of met " +
                    "anderen delen. Tuddy kan dit daarna niet terugdraaien. " +
                    "De eigen betekenis is NIET door Tuddy geverifieerd. " +
                    "Kies bewust wat je wilt versturen.")
            },
            confirmButton = {
                Column {
                    TextButton(onClick = { confirmShare(false) }) {
                        Text("Deel alleen mijn zin")
                    }
                    TextButton(onClick = { confirmShare(true) }) {
                        Text("Deel zin en eigen betekenis")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingShare = null }) {
                    Text("Annuleren")
                }
            }
        )
    }
}
