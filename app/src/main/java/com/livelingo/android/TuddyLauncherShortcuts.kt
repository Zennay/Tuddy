package com.livelingo.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon

/**
 * Android launcher shortcuts never carry speech, translations, private input,
 * device IDs or permissions. A shortcut may only open an existing user-facing
 * workflow; microphone capture and model downloads remain explicit taps.
 *
 * This router is also used for cold-start and onNewIntent delivery, so a
 * shortcut cannot be silently consumed only on the first app launch.
 */
internal enum class TuddyLauncherDestination(val shortcutId: String) {
    TYPE_TEXT("type-text"),
    OFFLINE_STARTCHECK("offline-startcheck"),
    ONE_SENTENCE("one-sentence");

    companion object {
        fun fromId(value: String?): TuddyLauncherDestination? =
            entries.firstOrNull { it.shortcutId == value }
    }
}

internal object TuddyLauncherShortcuts {
    private const val ACTION_OPEN = "com.livelingo.android.action.OPEN_SHORTCUT"
    private const val EXTRA_DESTINATION = "com.livelingo.android.extra.LAUNCHER_DESTINATION"

    /** Reject ordinary SEND/VIEW/PROCESS_TEXT intents and malformed actions. */
    fun requestedDestination(intent: Intent?): TuddyLauncherDestination? {
        if (intent?.action != ACTION_OPEN || intent.data != null ||
            intent.component?.className != MainActivity::class.java.name ||
            intent.hasExtra(Intent.EXTRA_TEXT) ||
            intent.hasExtra(Intent.EXTRA_STREAM) ||
            intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) return null
        return runCatching {
            TuddyLauncherDestination.fromId(
                intent.getStringExtra(EXTRA_DESTINATION)
            )
        }.getOrNull()
    }

    /** Remove the route immediately so Activity recreation cannot replay it. */
    fun clearReceivedRoute(intent: Intent?) {
        intent?.removeExtra(EXTRA_DESTINATION)
    }

    /**
     * Shared, narrowly validated entrypoint for dynamic launcher shortcuts and
     * the optional home-screen widget. Intent extras contain a route identifier
     * only: no pasted text, microphone sample or remembered translation.
     */
    fun intentFor(context: Context, route: TuddyLauncherDestination): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(EXTRA_DESTINATION, route.shortcutId)
        }

    private fun shortcut(context: Context, route: TuddyLauncherDestination): ShortcutInfo {
        val (short, long, drawable) = when (route) {
            TuddyLauncherDestination.TYPE_TEXT ->
                Triple("Typ lokaal", "Typ tekst en vertaal met reeds aanwezige modellen",
                    android.R.drawable.ic_menu_edit)
            TuddyLauncherDestination.OFFLINE_STARTCHECK ->
                Triple("Offline check", "Controleer lokale spraak, tekstmodellen en vliegtuigmodus",
                    android.R.drawable.ic_menu_info_details)
            TuddyLauncherDestination.ONE_SENTENCE ->
                Triple("Eén zin", "Zet één-zinsherkenning klaar; start de microfoon zelf",
                    android.R.drawable.ic_btn_speak_now)
        }
        val open = intentFor(context, route)
        return ShortcutInfo.Builder(context, "tuddy-" + route.shortcutId)
            .setShortLabel(short)
            .setLongLabel(long)
            .setIcon(Icon.createWithResource("android", drawable))
            .setIntent(open)
            .build()
    }

    /** Safe to call on app startup; Android may decline based on launcher policy. */
    fun publish(context: Context): Boolean = runCatching {
        val shortcuts = context.getSystemService(ShortcutManager::class.java)
            ?: return@runCatching false
        shortcuts.setDynamicShortcuts(
            TuddyLauncherDestination.entries.map { shortcut(context, it) }
        )
    }.getOrDefault(false)

    /**
     * Android owns the actual launcher confirmation; true means it accepted
     * the PIN REQUEST, not that the user has installed the shortcut.
     */
    fun requestPin(context: Context, route: TuddyLauncherDestination): Boolean =
        runCatching {
            val manager = context.getSystemService(ShortcutManager::class.java)
                ?: return@runCatching false
            manager.isRequestPinShortcutSupported &&
                manager.requestPinShortcut(shortcut(context, route), null)
        }.getOrDefault(false)
}