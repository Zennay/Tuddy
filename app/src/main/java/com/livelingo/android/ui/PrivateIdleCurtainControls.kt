package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Optional privacy preference only. Android interaction/lifecycle timing is
 * managed by MainActivity rather than Compose or a background service.
 */
@Composable
internal fun PrivateIdleCurtainControls(
    minutes: Int,
    notice: String,
    onChange: (Int) -> Unit
) {
    Text(
        "Privacy · gesprek automatisch afdekken bij inactiviteit",
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        "Optioneel: na een periode zonder aanraking of toetsbediening " +
            "stopt Tuddy de microfoon en het voorlezen en bedekt het tijdelijke gesprek. " +
            "Het gesprek wordt niet verwijderd. Standaard uit. " +
            "Deze timer geldt alleen zolang Tuddy vooraan staat; " +
            "ontgrendelen en verder luisteren vereisen een bewuste handeling.",
        style = MaterialTheme.typography.bodySmall
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(0 to "Uit", 5 to "5 min", 15 to "15 min", 30 to "30 min")
            .forEach { (value, title) ->
                TextButton(onClick = { onChange(value) }) {
                    Text(if (minutes == value) "✓ $title" else title)
                }
            }
    }
    if (notice.isNotBlank()) {
        Text(notice, style = MaterialTheme.typography.bodySmall)
    }
    Text(
        "De Android-interactieklok is geen onafhankelijke beveiligings- of " +
            "aanwezigheidsmeting. Als de app naar de achtergrond gaat, " +
            "gelden al de bestaande stop- en privacyregels.",
        style = MaterialTheme.typography.labelSmall
    )
}
