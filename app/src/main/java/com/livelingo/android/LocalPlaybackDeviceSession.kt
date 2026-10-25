package com.livelingo.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Permission-free, short-lived Android audio session for explicitly requested
 * offline speech. Audio focus is required; any focus loss stops the current
 * utterance rather than allowing Tuddy to speak across a phone call or another
 * app. Headset output is monitored to reduce unexpected loudspeaker playback
 * after disconnection. No route, recording or audio content is persisted.
 *
 * Connected devices and AUDIO_BECOMING_NOISY are best-effort Android hints:
 * this cannot prove which physical speaker rendered audio, nor eliminate the
 * short window before the system delivers a device/focus event.
 */
internal class LocalPlaybackDeviceSession(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val manager: AudioManager? =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var active = false
    private var everHadHeadset = false
    private var focus: AudioFocusRequest? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var noisyReceiver: BroadcastReceiver? = null
    private var receiverRegistered = false
    private var deviceRegistered = false
    private var interruption: ((String) -> Unit)? = null

    fun begin(onInterrupted: (String) -> Unit): Boolean {
        end()
        val audio = manager ?: return false
        // Unknown route metadata is a failure, not implied permission to speak.
        val initiallyConnected = visibleHeadset(audio) ?: return false
        everHadHeadset = initiallyConnected
        interruption = onInterrupted
        val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                interrupt("Android gaf de audiofocus aan een andere app of oproep. Voorlezen gestopt.")
            }
        }
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(SPEECH_AUDIO_ATTRIBUTES)
            .setOnAudioFocusChangeListener(focusListener, main)
            .build()
        try {
            if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                end()
                return false
            }
            focus = request
            val routeCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    inspectRoute()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    inspectRoute()
                }
            }
            audioDeviceCallback = routeCallback
            audio.registerAudioDeviceCallback(routeCallback, main)
            deviceRegistered = true
            val noisy = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    // Ignore untrusted payloads: stopping TTS is safe, and
                    // Android route state is checked at the next explicit tap.
                    if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                        interrupt("De audio-uitvoer kan naar de luidspreker wisselen. Voorlezen gestopt.")
                    }
                }
            }
            noisyReceiver = noisy
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(noisy, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(noisy, filter)
            }
            receiverRegistered = true
            active = true
            // Route may have changed between the original user tap and
            // listener registration. Never start speaking after that gap.
            val nowConnected = visibleHeadset(audio)
            if (nowConnected == null || (initiallyConnected && !nowConnected)) {
                end()
                return false
            }
            everHadHeadset = everHadHeadset || nowConnected
            return true
        } catch (_: RuntimeException) {
            end()
            return false
        }
    }

    private fun inspectRoute() {
        if (!active) return
        val available = manager?.let(::visibleHeadset)
        when {
            available == null ->
                interrupt("Android kan de huidige audio-uitvoer niet controleren. Voorlezen gestopt.")
            everHadHeadset && !available ->
                interrupt("De hoofdtelefoon is losgekoppeld. Voorlezen gestopt vóór verder afspelen.")
            available -> everHadHeadset = true
        }
    }

    private fun interrupt(reason: String) {
        if (!active) return
        val notify = interruption
        end()
        notify?.invoke(reason)
    }

    fun end() {
        active = false
        interruption = null
        everHadHeadset = false
        if (receiverRegistered) {
            runCatching { noisyReceiver?.let(appContext::unregisterReceiver) }
        }
        receiverRegistered = false
        noisyReceiver = null
        if (deviceRegistered) {
            runCatching { audioDeviceCallback?.let { manager?.unregisterAudioDeviceCallback(it) } }
        }
        deviceRegistered = false
        audioDeviceCallback = null
        focus?.let { request ->
            runCatching { manager?.abandonAudioFocusRequest(request) }
        }
        focus = null
    }

    private fun visibleHeadset(audio: AudioManager): Boolean? =
        runCatching {
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
                device.type in HEADSET_DEVICE_TYPES
            }
        }.getOrNull()

    companion object {
        val SPEECH_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        private val HEADSET_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_BLE_HEADSET
        )
    }
}
