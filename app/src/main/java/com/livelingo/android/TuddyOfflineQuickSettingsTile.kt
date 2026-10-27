package com.livelingo.android

import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * A user-installed Quick Settings route into the existing offline readiness
 * screen. A tile tap is never permission to record, download a model or read
 * a private transcript. No work runs while the shade is merely displayed.
 */
class TuddyOfflineQuickSettingsTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            // A navigation action is not a switch and must not imply that
            // offline recognition or translation has been verified.
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tuddy_qs_tile_name)
            contentDescription = getString(R.string.tuddy_qs_tile_accessibility)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // Quick Settings may be accessible on the lock screen. Never reveal
        // Tuddy's private in-app state until Android has unlocked the device.
        if (isLocked) {
            unlockAndRun { openOfflineCheck() }
        } else {
            openOfflineCheck()
        }
    }

    private fun openOfflineCheck() {
        val route = TuddyLauncherShortcuts.intentFor(
            this, TuddyLauncherDestination.OFFLINE_STARTCHECK
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0x7AD1,
                route,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(route)
        }
    }
}
