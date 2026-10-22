package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.SessionTranslationCorrections
import com.livelingo.android.SessionCorrectionLanguageCatalog
import com.livelingo.android.TargetCatalog

/**
 * User-authored exact-match source + translation without starting a microphone
 * or adding synthetic captions. The caller must reject a stale snapshot.
 */
@Composable
internal fun SessionCorrectionManualAddDialog(
    entries: List<SessionTranslationCorrection>,
    initialTargetCode: String? = null,
    onAdd: (List<SessionTranslationCorrection>, SessionTranslationCorrection) -> Boolean,
    onDismiss: () -> Unit
) {
    val snapshot = remember { entries.toList() }
    var sourceCode by remember { mutableStateOf("nl") }
    var targetCode by remember {
        mutableStateOf(initialTargetCode?.takeIf {
            it in SessionCorrectionLanguageCatalog.manualOnlyCodes
        } ?: "en")
    }
    var sourceMenuOpen by remember { mutableStateOf(false) }
    var targetMenuOpen by remember { mutableStateOf(false) }
    var original by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val codeLabel = SessionCorrectionLanguageCatalog.targets.associate { it.code to it.title }
    val candidate = SessionTranslationCorrection(
        original, translation, sourceCode, targetCode
    )
    val list = snapshot + candidate
    val valid = original.isNotBlank() && translation.isNotBlank() &&
        sourceCode != targetCode && SessionTranslationCorrections.validEntries(list)
    val duplicate = snapshot.any {
        it.original == original && it.sourceCode == sourceCode &&
            it.targetCode == targetCode
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schrijf mijn eigen vertaalcorrectie") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Schrijf zelf de exacte originele zin en je eigen vertaling. " +
                        "Dit is geen gecontroleerde vertaling: Tuddy gebruikt " +
                        "geen spraakherkenning of vertaalmodel voor deze stap. " +
                        "De correctie blijft alleen in deze sessie, wordt later " +
                        "uitsluitend als voorstel getoond en nooit automatisch toegepast.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (sourceCode in SessionCorrectionLanguageCatalog.manualOnlyCodes ||
                    targetCode in SessionCorrectionLanguageCatalog.manualOnlyCodes) {
                    Text(
                        "Handmatig phrasebook: deze taalcode is alleen voor jouw eigen " +
                            "tekstcorrecties. Er is geen geactiveerd spraak- of vertaalmodel; " +
                            "deze invoer verandert de live vertaalroute niet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Box {
                    TextButton(onClick = { sourceMenuOpen = true }) {
                        Text("Brontaal: " + (codeLabel[sourceCode] ?: sourceCode) + " ▾")
                    }
                    DropdownMenu(
                        expanded = sourceMenuOpen,
                        onDismissRequest = { sourceMenuOpen = false }
                    ) {
                        SessionCorrectionLanguageCatalog.targets.forEach { target ->
                            DropdownMenuItem(
                                text = { Text(target.title + " (" + target.code + ")") },
                                onClick = {
                                    sourceCode = target.code
                                    sourceMenuOpen = false
                                    notice = ""
                                }
                            )
                        }
                    }
                }
                Box {
                    TextButton(onClick = { targetMenuOpen = true }) {
                        Text("Doeltaal: " + (codeLabel[targetCode] ?: targetCode) + " ▾")
                    }
                    DropdownMenu(
                        expanded = targetMenuOpen,
                        onDismissRequest = { targetMenuOpen = false }
                    ) {
                        TargetCatalog.targets.forEach { target ->
                            DropdownMenuItem(
                                text = { Text(target.title + " (" + target.code + ")") },
                                onClick = {
                                    targetCode = target.code
                                    targetMenuOpen = false
                                    notice = ""
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = original,
                    onValueChange = {
                        original = it.take(SessionTranslationCorrections.MAX_TEXT_UNITS)
                        notice = ""
                    },
                    label = { Text("Exacte brontekst, zelf geschreven") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                OutlinedTextField(
                    value = translation,
                    onValueChange = {
                        translation = it.take(SessionTranslationCorrections.MAX_TEXT_UNITS)
                        notice = ""
                    },
                    label = { Text("Mijn eigen, niet-geverifieerde vertaling") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Text(
                    "Bewust toevoegen: " + snapshot.size + " van maximaal " +
                        SessionTranslationCorrections.MAX_ENTRIES + " bestaande " +
                        "correcties. Er wordt niets stilzwijgend overschreven of gewist.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (sourceCode == targetCode) Text(
                    "Bron- en doeltaal moeten verschillen.",
                    color = MaterialTheme.colorScheme.error
                )
                if (duplicate) Text(
                    "Voor deze exacte zin en taalrichting bestaat al een correctie. " +
                        "Bewerk die rechtstreeks in de lijst.",
                    color = MaterialTheme.colorScheme.error
                )
                if (notice.isNotBlank()) Text(
                    notice,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (onAdd(snapshot, candidate)) onDismiss()
                        else notice = "Niet toegevoegd: lijst gewijzigd, limiet bereikt " +
                            "of ongeldige invoer. Er is niets vervangen."
                    }
                ) { Text("Ja, onthoud mijn eigen zin tijdelijk") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
