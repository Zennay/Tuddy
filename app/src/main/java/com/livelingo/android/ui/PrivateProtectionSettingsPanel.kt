package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Keep device-only visual privacy choices together and outside TuddyScreen's
 * already large Compose bytecode method. The host Activity alone applies
 * Android window flags and the user-selected foreground idle timer.
 */
@Composable
internal fun PrivateProtectionSettingsPanel(
    shieldEnabled: Boolean,
    shieldAvailable: Boolean,
    shieldNotice: String,
    idleMinutes: Int,
    idleNotice: String,
    onShieldChange: (Boolean) -> Unit,
    onIdleChange: (Int) -> Unit
) {
    Text("Privacy · privéweergave", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            if (shieldEnabled) "Screenshots en schermopname blokkeren · aan"
            else "Screenshots en schermopname blokkeren · uit",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Switch(
            checked = shieldEnabled,
            enabled = shieldAvailable,
            onCheckedChange = onShieldChange
        )
    }
    Text(
        "Android probeert screenshots, schermopnames en recente-appsvoorbeelden " +
            "te blokkeren zolang dit aanstaat. Dit kan ook zelf screenshots " +
            "maken verhinderen; externe camera's blijven mogelijk. " +
            "Er wordt geen gesprekstekst voor opgeslagen.",
        style = MaterialTheme.typography.bodySmall
    )
    if (shieldNotice.isNotBlank()) {
        Text(shieldNotice, style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider()
    PrivateIdleCurtainControls(
        minutes = idleMinutes,
        notice = idleNotice,
        onChange = onIdleChange
    )
}
