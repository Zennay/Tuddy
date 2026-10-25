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
 * Active only during a user's explicit strict-airplane-mode microphone turn.
 *
 * Android broadcasts and ConnectivityManager callbacks can be delayed, lost
 * or coalesced. Independently re-read airplane mode, Wi-Fi and all visible
 * network transports every three seconds while the recognizer is active.
 * There is no network request, service, wake lock, storage or model download.
 *
 * No Android-reported state proves complete physical radio isolation.
 */
internal class StrictOfflineRadioSessionMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val connectivity =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var active = false
    private var epoch = 0L
    private var receiverRegistered = false
    private var networkRegistered = false
    private var receiver: BroadcastReceiver? = null
    private var networkListener: ConnectivityManager.NetworkCallback? = null
    private var periodicCheck: Runnable? = null
    private var onViolation: ((String) -> Unit)? = null

    fun isActive(): Boolean = active

    /** Failure to install *either* event observers or the polling fallback is
     * a refusal to start listening; no best-effort silent downgrade. */
    fun arm(violation: (String) -> Unit): Boolean {
        disarm()
        val manager = connectivity ?: return false
        if (!strictlyOffline(OfflineRadioSnapshot.capture(appContext))) return false
        val version = epoch

        val radioReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Ignore broadcast extras; inspect fresh OS state instead.
                inspect(version)
            }
        }
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = inspect(version)
            override fun onLost(network: Network) = inspect(version)
            override fun onCapabilitiesChanged(
                network: Network, capabilities: NetworkCapabilities
            ) = inspect(version)
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
                // System events are re-read, not trusted as authoritative.
                appContext.registerReceiver(
                    radioReceiver, filter, Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(radioReceiver, filter)
            }
            receiverRegistered = true

            // Run network callbacks on the same main Handler as lifecycle,
            // poll and receiver. Old callbacks are fenced by version.
            manager.registerNetworkCallback(
                NetworkRequest.Builder().build(), networkCallback, handler
            )
            networkRegistered = true
            active = true

            // Recheck after registrations to close the initial admission gap.
            if (!strictlyOffline(OfflineRadioSnapshot.capture(appContext)) ||
                !scheduleNextCheck(version)) {
                disarm()
                return false
            }
            return true
        } catch (_: Exception) {
            disarm()
            return false
        }
    }

    private fun scheduleNextCheck(version: Long): Boolean {
        if (!active || version != epoch) return false
        val task = Runnable {
            if (!active || version != epoch) return@Runnable
            inspect(version)
            if (active && version == epoch && !scheduleNextCheck(version)) {
                interrupt("Android kon de periodieke offline-radiocontrole niet voortzetten.")
            }
        }
        periodicCheck = task
        val scheduled = runCatching {
            handler.postDelayed(task, CHECK_INTERVAL_MS)
        }.getOrDefault(false)
        if (!scheduled) periodicCheck = null
        return scheduled
    }

    private fun inspect(version: Long) {
        if (!active || version != epoch) return
        val snapshot = OfflineRadioSnapshot.capture(appContext)
        if (!strictlyOffline(snapshot)) {
            interrupt("Android meldt een actieve of onbekende radio of netwerkverbinding.")
        }
    }

    private fun interrupt(reason: String) {
        if (!active) return
        val notify = onViolation
        disarm() // Block duplicate/stale callbacks before stopping the owner.
        notify?.invoke(reason)
    }

    fun disarm() {
        epoch++
        active = false
        onViolation = null
        periodicCheck?.let(handler::removeCallbacks)
        periodicCheck = null

        if (receiverRegistered) {
            runCatching { receiver?.let(appContext::unregisterReceiver) }
        }
        receiverRegistered = false
        receiver = null

        if (networkRegistered) {
            runCatching {
                networkListener?.let { callback ->
                    connectivity?.unregisterNetworkCallback(callback)
                }
            }
        }
        networkRegistered = false
        networkListener = null
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 3_000L

        fun strictlyOffline(snapshot: OfflineRadioSnapshot): Boolean =
            snapshot.airplaneModeOn == true &&
                snapshot.wifiEnabled == false &&
                !snapshot.networkReadFailed &&
                snapshot.visibleTransports.isEmpty()
    }
}
