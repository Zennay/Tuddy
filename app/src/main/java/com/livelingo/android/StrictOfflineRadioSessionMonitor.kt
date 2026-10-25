package com.livelingo.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Session-only companion to the opt-in strict radio gate.
 *
 * A preflight snapshot by itself is insufficient: the user/OS can turn Wi-Fi
 * on or attach a network while one SpeechRecognizer turn is still running.
 * Observe system radio broadcasts and network callbacks until Stop/background.
 * Every event re-reads actual Android state; intent payloads are never trusted.
 *
 * Fail closed if observation cannot be registered, and unregister everything
 * on Stop. These Android signals do NOT prove complete network isolation.
 */
internal class StrictOfflineRadioSessionMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val connectivity =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var active = false
    private var receiverRegistered = false
    private var networkRegistered = false
    private var receiver: BroadcastReceiver? = null
    private var networkListener: ConnectivityManager.NetworkCallback? = null
    private var onViolation: ((OfflineRadioSnapshot) -> Unit)? = null

    fun isActive(): Boolean = active

    fun arm(violation: (OfflineRadioSnapshot) -> Unit): Boolean {
        disarm()
        val manager = connectivity ?: return false
        if (!strictlyOffline(OfflineRadioSnapshot.capture(appContext))) return false

        val radioReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                inspect()
            }
        }
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = inspectAsync()
            override fun onLost(network: Network) = inspectAsync()
            override fun onCapabilitiesChanged(
                network: Network, capabilities: NetworkCapabilities
            ) = inspectAsync()
        }
        receiver = radioReceiver
        networkListener = networkCallback
        onViolation = violation
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                // A privileged Android Wi-Fi package may send system events.
                // Export affects event delivery, never trust: re-read OS state.
                appContext.registerReceiver(
                    radioReceiver, filter, Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(radioReceiver, filter)
            }
            receiverRegistered = true

            // Android sends callbacks for available networks in this session.
            // The receiver covers Airplane/Wi-Fi; each callback also checks
            // *all* transports available through OfflineRadioSnapshot.
            manager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
            networkRegistered = true
            active = true

            // Close the gap between the preflight and observer registration.
            if (!strictlyOffline(OfflineRadioSnapshot.capture(appContext))) {
                disarm()
                return false
            }
            return true
        } catch (_: Exception) {
            disarm()
            return false
        }
    }

    private fun inspectAsync() {
        handler.post { inspect() }
    }

    private fun inspect() {
        if (!active) return
        val snapshot = OfflineRadioSnapshot.capture(appContext)
        if (!strictlyOffline(snapshot)) {
            val violation = onViolation
            disarm() // disallow duplicate callbacks before notifying caller
            violation?.invoke(snapshot)
        }
    }

    fun disarm() {
        active = false
        onViolation = null
        if (receiverRegistered) {
            runCatching { receiver?.let(appContext::unregisterReceiver) }
        }
        receiverRegistered = false
        receiver = null
        if (networkRegistered) {
            runCatching {
                networkListener?.let { listener ->
                    connectivity?.unregisterNetworkCallback(listener)
                }
            }
        }
        networkRegistered = false
        networkListener = null
    }

    companion object {
        fun strictlyOffline(snapshot: OfflineRadioSnapshot): Boolean =
            snapshot.airplaneModeOn == true &&
                snapshot.wifiEnabled == false &&
                !snapshot.networkReadFailed &&
                snapshot.visibleTransports.isEmpty()
    }
}
