package com.livelingo.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings

/**
 * A privacy-safe, user-visible snapshot of Android's connection indicators.
 * It cannot prove that every radio is disabled or that a speech service
 * never transmitted bytes. No SSID, addresses, endpoints or identifiers.
 */
internal data class OfflineRadioSnapshot(
    val airplaneModeOn: Boolean?,
    val wifiEnabled: Boolean?,
    val visibleTransports: Set<String>,
    val networkReadFailed: Boolean
) {
    fun display(): String = buildString {
        append("Vliegtuigmodus: ")
        append(when (airplaneModeOn) {
            true -> "aan volgens Android"
            false -> "uit volgens Android"
            null -> "niet te lezen"
        })
        append("\nWifi-radio: ")
        append(when (wifiEnabled) {
            true -> "aan (kan ook in vliegtuigmodus actief zijn)"
            false -> "uit volgens Android"
            null -> "niet te lezen"
        })
        append("\nAndroid-netwerkverbindingen: ")
        append(when {
            networkReadFailed -> "status onbekend"
            visibleTransports.isEmpty() -> "geen transport gerapporteerd"
            else -> visibleTransports.sorted().joinToString(", ")
        })
        if (airplaneModeOn != true || wifiEnabled != false ||
            visibleTransports.isNotEmpty() || networkReadFailed) {
            append("\nGeen bevestigde netwerkisolatie. Controleer de instellingen en verbindingen zelf.")
        } else {
            append("\nGeen actieve netwerktransporten waargenomen. Dit bewijst geen volledige radio- of netwerkisolatie.")
        }
    }

    fun reportLines(): List<String> = listOf(
        "Wifi radio enabled (Android reported): ${wifiEnabled ?: "unknown"}",
        "Network transport read failed: $networkReadFailed",
        "Network transports Android reports: ${if (networkReadFailed) "unknown" else visibleTransports.sorted().joinToString(", ").ifBlank { "none reported" }}",
        "Connection snapshot independently proves isolation: no"
    )

    companion object {
        fun capture(context: Context): OfflineRadioSnapshot {
            val airplane = runCatching {
                Settings.Global.getInt(context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON) == 1
            }.getOrNull()
            val wifi = runCatching {
                (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                    .isWifiEnabled
            }.getOrNull()
            val network = runCatching {
                val manager = context.getSystemService(ConnectivityManager::class.java)
                requireNotNull(manager)
                manager.allNetworks.mapNotNull { manager.getNetworkCapabilities(it) }
                    .flatMap { capabilities ->
                        listOfNotNull(
                            "wifi".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) },
                            "mobiel".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) },
                            "ethernet".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) },
                            "vpn".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) },
                            "bluetooth-netwerk".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) }
                        )
                    }.toSet()
            }
            return OfflineRadioSnapshot(
                airplaneModeOn = airplane,
                wifiEnabled = wifi,
                visibleTransports = network.getOrNull() ?: emptySet(),
                networkReadFailed = network.isFailure
            )
        }
    }
}
