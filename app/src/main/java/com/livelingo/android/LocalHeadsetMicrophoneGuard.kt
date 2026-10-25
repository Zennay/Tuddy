package com.livelingo.android

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Optional, explicit Android input-device guard for private live recognition.
 *
 * Presence of a wired/USB/Bluetooth headset INPUT is required before
 * creating a new SpeechRecognizer turn when the preference is enabled.
 * A device disconnect, unknown routing state or failed listener/poll setup
 * ends that turn and prevents an automatic restart in the owner.
 *
 * Android's input-device inventory is NOT a proof of the input chosen by a
 * vendor SpeechRecognizer. Never claim microphone hardware attestation.
 * No device names/addresses, speech, session history or identifiers are saved.
 */
internal class LocalHeadsetMicrophoneGuard(context: Context) {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = appContext.getSharedPreferences(
        "tuddy-headset-microphone-v1", Context.MODE_PRIVATE
    )

    private var active = false
    private var epoch = 0L
    private var callback: AudioDeviceCallback? = null
    private var deviceRegistered = false
    private var pollTask: Runnable? = null
    private var onInterrupted: ((String) -> Unit)? = null

    fun enabled(): Boolean = runCatching {
        prefs.getBoolean(ENABLED_KEY, false)
    }.getOrDefault(true) // Unknown stored policy must not silently remove a safety gate.

    /** Only a setting changed while the microphone is stopped is accepted. */
    fun setEnabled(value: Boolean): Boolean = runCatching {
        prefs.edit().putBoolean(ENABLED_KEY, value).commit()
    }.getOrDefault(false)

    fun isActive(): Boolean = active

    /** Fail closed when Android cannot reliably enumerate input devices. */
    fun currentBlocker(): String? {
        if (!enabled()) return null
        val current = audio ?: return "Androids microfooningangen zijn niet beschikbaar."
        val hasInput = runCatching {
            current.getDevices(AudioManager.GET_DEVICES_INPUTS).any { device ->
                device.isSource && device.type in HEADSET_INPUT_TYPES
            }
        }.getOrNull() ?: return "Android kon de aanwezige headsetmicrofoon niet controleren."
        return if (hasInput) null else
            "Geen door Android herkende aangesloten headsetmicrofoon. " +
            "Sluit een geschikte bedrade, USB- of Bluetooth-microfoon aan."
    }

    /** Called once per actual recognizer instance, including automatic restarts. */
    fun arm(onLost: (String) -> Unit): Boolean {
        disarm()
        if (!enabled()) return true
        if (currentBlocker() != null) return false
        val current = audio ?: return false
        val version = epoch
        onInterrupted = onLost
        try {
            val observer = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    inspect(version)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    inspect(version)
                }
            }
            callback = observer
            current.registerAudioDeviceCallback(observer, handler)
            deviceRegistered = true
            active = true
            // Device could disconnect between admission and registration.
            if (currentBlocker() != null) {
                disarm()
                return false
            }
            schedulePoll(version)
            return active
        } catch (_: Exception) {
            disarm()
            return false
        }
    }

    private fun schedulePoll(version: Long) {
        if (!active || version != epoch) return
        val task = Runnable {
            if (active && version == epoch) {
                inspect(version)
                if (active && version == epoch) schedulePoll(version)
            }
        }
        pollTask = task
        if (!handler.postDelayed(task, POLL_MS)) {
            interrupt("Android kon de headsetmicrofoon niet blijven bewaken.")
        }
    }

    private fun inspect(version: Long) {
        if (!active || version != epoch) return
        val problem = currentBlocker()
        if (problem != null) interrupt(problem)
    }

    private fun interrupt(problem: String) {
        if (!active) return
        val notify = onInterrupted
        disarm()
        notify?.invoke(problem)
    }

    fun disarm() {
        active = false
        epoch++
        onInterrupted = null
        pollTask?.let(handler::removeCallbacks)
        pollTask = null
        if (deviceRegistered) runCatching {
            callback?.let { audio?.unregisterAudioDeviceCallback(it) }
        }
        deviceRegistered = false
        callback = null
    }

    companion object {
        private const val ENABLED_KEY = "headset-input-required"
        private const val POLL_MS = 2_000L
        private val HEADSET_INPUT_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET
        )
    }
}
