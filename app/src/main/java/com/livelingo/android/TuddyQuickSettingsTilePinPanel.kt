package com.livelingo.android

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Only a foreground user tap may ask Android to place the tile. The Android
 * callback reports the actual decision; submitting a request is not success.
 */
internal object TuddyQuickSettingsTilePin {
    fun request(context: Context, onResult: (Int) -> Unit): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return runCatching {
            val statusBar = context.getSystemService(StatusBarManager::class.java)
                ?: return@runCatching false
            statusBar.requestAddTileService(
                ComponentName(context, TuddyOfflineQuickSettingsTile::class.java),
                context.getString(R.string.tuddy_qs_tile_name),
                Icon.createWithResource(context, R.drawable.tuddy_offline_check_tile),
                context.mainExecutor,
                onResult
            )
            true
        }.getOrDefault(false)
    }
}

@Composable
internal fun TuddyQuickSettingsTilePinPanel(enabled: Boolean) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Android-snelinstelling · Offline check",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Open de lokale model- en netwerkcontrole vanuit het snelle " +
                "instellingenpaneel. De knop start geen microfoon, " +
                "downloads of vertaling en toont geen privégegevens.",
            style = MaterialTheme.typography.bodySmall
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(
                enabled = enabled && !pending,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    pending = true
                    notice = "Android-aanvraag verzonden. Bevestig de tegel in " +
                        "het systeemvenster; de uitkomst is nog onbekend."
                    val submitted = TuddyQuickSettingsTilePin.request(context) { result ->
                        pending = false
                        notice = when (result) {
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                "Android bevestigt dat Offline check is toegevoegd."
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                "Offline check staat volgens Android al bij de snelinstellingen."
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                                "De tegel is niet toegevoegd. Je kunt hem zelf via Bewerken plaatsen."
                            else ->
                                "Android kon de tegel niet toevoegen (code $result). " +
                                    "Open het snelle instellingenpaneel en kies Bewerken."
                        }
                    }
                    if (!submitted) {
                        pending = false
                        notice = "Het verzoek kon niet worden gestart. " +
                            "Voeg de tegel handmatig toe via Snelinstellingen → Bewerken."
                    }
                }
            ) {
                Text(if (pending) "Wacht op Android…" else "Voeg Offline check toe aan Snelinstellingen")
            }
        } else {
            Text(
                "Op Android 12 kan Tuddy de tegel niet rechtstreeks aanbieden. " +
                    "Voeg hem zelf toe via Snelinstellingen → Bewerken.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "Handmatig: veeg tweemaal omlaag → Bewerken → " +
                "sleep ‘Tuddy · Offline’ naar de actieve tegels. " +
                "Android bepaalt plaatsing en beschikbaarheid.",
            style = MaterialTheme.typography.bodySmall
        )
        if (notice.isNotBlank()) {
            Text(notice, style = MaterialTheme.typography.bodySmall)
        }
    }
}
