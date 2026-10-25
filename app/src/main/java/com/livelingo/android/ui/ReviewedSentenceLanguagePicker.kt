package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.TargetCatalog

/**
 * Chooses a supported ML Kit *text* language for one privately reviewed
 * Android speech result. It never changes the microphone language, never
 * prepares/downloads a model, and does not persist a language preference.
 */
@Composable
internal fun ReviewedSentenceLanguagePicker(
    which: String,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember(which) { mutableStateOf("") }
    val matches = TargetCatalog.search(query)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teksttaal kiezen · $which") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Kies één ondersteunde lokale ML Kit-teksttaal. " +
                        "Dit verandert alleen deze gecontroleerde zin, " +
                        "niet de Android-microfoon of volgende gesprekken.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(64) },
                    label = { Text("Zoek taal of taalcode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (matches.isEmpty()) {
                    Text("Geen ondersteunde teksttaal gevonden.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(matches, key = { it.code }) { language ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onSelect(language.code)
                                    onDismiss()
                                }
                            ) {
                                Text(
                                    language.title + " (${language.code})" +
                                        if (language.code == selectedCode) " · gekozen" else ""
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terug naar zin") }
        }
    )
}
