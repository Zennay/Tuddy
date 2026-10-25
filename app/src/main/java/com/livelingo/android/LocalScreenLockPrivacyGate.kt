package com.livelingo.android

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager

/**
 * Device-level complement to Activity ON_PAUSE. Some Android vendors deliver
 * screen-off before the Activity lifecycle transitions; fail closed on the
 * system's protected ACTION_SCREEN_OFF broadcast instead of continuing
 * microphone capture, TTS or a private selected-text workflow until ON_PAUSE.
 *
 * The receiver is registered only while a foreground Activity is RESUMED.
 * It stores no captions, audio, lock timestamps or biometric data, changes
 * no device settings and never resumes a cancelled operation on unlock.
 *
 * Broadcasts do not independently attest to physical lock isolation.
 */
internal class LocalScreenLockPrivacyGate(context: Context) {
    private val appContext = context.applicationContext
    private val power = appContext.getSystemService(PowerManager::class.java)
    private val keyguard = appContext.getSystemService(KeyguardManager::class.java)

    private var receiver: BroadcastReceiver? = null
    private var registered = false
    private var onInterrupted: (() -> Unit)? = null

    fun isInteractiveAndUnlocked(): Boolean = runCatching {
        power?.isInteractive == true && keyguard?.isKeyguardLocked == false
    }.getOrDefault(false)

    fun isArmed(): Boolean = registered

    /**
     * The foreground owner supplies a synchronous Stop/redaction callback.
     * Registration is fail-closed: an unavailable OS service or receiver
     * prevents a private session from being admitted.
     */
    fun arm(interrupted: () -> Unit): Boolean {
        disarm()
        if (!isInteractiveAndUnlocked()) return false
        val observer = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_SCREEN_OFF || !registered) return
                val notify = onInterrupted
                disarm() // Exactly one interruption for this foreground session.
                notify?.invoke()
            }
        }
        onInterrupted = interrupted
        receiver = observer
        try {
            // ACTION_SCREEN_OFF is a protected *system-only* broadcast.
            // Android requires context registration (never a manifest receiver).
            // The system-only receiver is exempt from exported receiver flags.
            appContext.registerReceiver(
                observer,
                IntentFilter(Intent.ACTION_SCREEN_OFF)
            )
            registered = true
            // Detect a lock between the first snapshot and registration.
            if (!isInteractiveAndUnlocked()) {
                disarm()
                return false
            }
            return true
        } catch (_: RuntimeException) {
            disarm()
            return false
        }
    }

    fun disarm() {
        val observer = receiver
        if (registered && observer != null) {
            runCatching { appContext.unregisterReceiver(observer) }
        }
        registered = false
        receiver = null
        onInterrupted = null
    }
}
