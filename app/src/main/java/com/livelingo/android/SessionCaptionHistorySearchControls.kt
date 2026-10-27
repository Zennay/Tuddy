package com.livelingo.android

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Isolated Compose subtree for private, RAM-only transcript navigation.
 * Keeping these controls out of TuddyScreen also prevents the parent
 * composition's generated JVM invoke method from exceeding 64 KiB.
 */
@Composable
internal fun SessionCaptionHistorySearchControls(
    statuses: List<String>,
    currentFilter: SessionCaptionStatusFilter,
    onFilterChange: (SessionCaptionStatusFilter) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    searchActive: Boolean,
    matchingCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Text(
        "Spring naar gespreksregels op tekst én status. " +
            "Alle regels blijven zichtbaar; er wordt niets verwijderd of opgeslagen.",
        style = MaterialTheme.typography.bodySmall
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SessionCaptionStatusFilter.entries.forEach { filter ->
            val count = statuses.count {
                SessionCaptionStatusNavigation.matches(it, filter)
            }
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text("${filter.label} (${count})") }
            )
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Zoek tekst, vertaling of eigen uitleg") },
        supportingText = {
            Text("Zoekt ook met veelvoorkomende Arabisch/Perzische toetsenbordvarianten; opgeslagen tekst blijft exact behouden.")
        },
        singleLine = true
    )
    if (searchActive) {
        Text(
            "${matchingCount} van ${statuses.size} gespreksregels passen " +
                "bij jouw selectie. Tik op Vorige of Volgende om een " +
                "bestaande regel te openen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = matchingCount > 0
        ) { Text("← Vorige") }
        OutlinedButton(
            onClick = onNext,
            enabled = matchingCount > 0
        ) { Text("Volgende →") }
    }
}
