package com.livelingo.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

/**
 * Optional Android device-protection boundary for foreground speech capture.
 *
 * Never records battery history, temperatures, locations, utterances or user
 * identifiers. No wake locks, alarms, background workers or permissions.
 * Only active for a user-started microphone session when explicitly enabled.
 *
 * Android battery and thermal signals are advisory; they are not a physical
 * measurement, and some vendors may delay/omit them. A periodic snapshot
 * closes gaps when the service does not deliver a change event.
 */
internal class LocalSpeechPowerGuard(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "tuddy-private-speech-power-v1", Context.MODE_PRIVATE
    )
    private val handler = Handler(Looper.getMainLooper())
    private val powerManager = appContext.getSystemService(PowerManager::class.java)

    private var active = false
    private var receiverRegistered = false
    private var thermalRegistered = false
    private var receiver: BroadcastReceiver? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
    private var pendingPoll: Runnable? = null
    private var onInterrupted: ((String) -> Unit)? = null
    private var epoch = 0L

    fun enabled(): Boolean =
        runCatching { preferences.getBoolean(ENABLED_KEY, false) }.getOrDefault(false)

    fun setEnabled(value: Boolean): Boolean = runCatching {
        preferences.edit().putBoolean(ENABLED_KEY, value).commit()
    }.getOrDefault(false)

    fun isActive(): Boolean = active

    /** A known severe condition blocks Start/restart. Unknown data is not proof of safety. */
    fun currentBlocker(): String? {
        val thermal = runCatching { powerManager?.currentThermalStatus }.getOrNull()
        if (thermal != null && thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
            return "Android meldt ernstige thermische belasting. Laat het toestel afkoelen."
        }
        val battery = runCatching {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }.getOrNull()
        if (battery != null) {
            val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val charging = plugged != 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0 &&
                (level * 100L / scale) <= CRITICAL_BATTERY_PERCENT && !charging) {
                return "Accu bijna leeg (maximaal 5%) en niet aan het laden. " +
                    "Laad het toestel op voor je doorgaat."
            }
        }
        return null
    }

    fun arm(interrupted: (String) -> Unit): Boolean {
        disarm()
        if (!enabled()) return true
        if (currentBlocker() != null) return false
        val power = powerManager ?: return false
        val version = epoch
        onInterrupted = interrupted
        try {
            val observer = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    // Never trust intent extras: inspect the current Android state.
                    inspect(version)
                }
            }
            receiver = observer
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(observer, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(observer, filter)
            }
            receiverRegistered = true
            val thermal = PowerManager.OnThermalStatusChangedListener {
                inspect(version)
            }
            thermalListener = thermal
            power.addThermalStatusListener(appContext.mainExecutor, thermal)
            thermalRegistered = true
            active = true
            // Check again after listeners are installed to close the race.
            if (currentBlocker() != null) {
                disarm()
                return false
            }
            poll(version)
            return true
        } catch (_: Exception) {
            disarm()
            return false
        }
    }

    private fun poll(version: Long) {
        if (!active || version != epoch) return
        val task = Runnable {
            if (active && version == epoch) {
                inspect(version)
                if (active && version == epoch) poll(version)
            }
        }
        pendingPoll = task
        if (!handler.postDelayed(task, POLL_INTERVAL_MS)) {
            interrupt("Android kon de energiebewaking niet voortzetten. " +
                "De microfoon is uit voorzorg gestopt.")
        }
    }

    private fun inspect(version: Long) {
        if (!active || version != epoch) return
        val reason = currentBlocker()
        if (reason != null) interrupt(reason)
    }

    private fun interrupt(reason: String) {
        if (!active) return
        val callback = onInterrupted
        disarm()
        callback?.invoke(reason)
    }

    fun disarm() {
        epoch++
        active = false
        onInterrupted = null
        pendingPoll?.let(handler::removeCallbacks)
        pendingPoll = null
        if (receiverRegistered) runCatching {
            receiver?.let(appContext::unregisterReceiver)
        }
        receiverRegistered = false
        receiver = null
        if (thermalRegistered) runCatching {
            thermalListener?.let { powerManager?.removeThermalStatusListener(it) }
        }
        thermalRegistered = false
        thermalListener = null
    }

    companion object {
        private const val ENABLED_KEY = "protect-microphone-thermal-critical-battery"
        private const val CRITICAL_BATTERY_PERCENT = 5L
        private const val POLL_INTERVAL_MS = 12_000L
    }
}
