package com.livelingo.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Read-only, fail-closed network preflight for a NEW, user-approved offline
 * model download. Repeated checks are intentional: Wi-Fi can become metered,
 * captive, disconnected or replaced between dialog display and submit.
 *
 * Android network capabilities are advisory. ML Kit's requireWifi() remains
 * mandatory on all text-model requests; only Android's speech service manages
 * the payload of a speech-pack request. This never downloads, redirects, or
 * persists network data, and never examines SSID, IP or user traffic.
 */
internal object LocalModelNetworkAdmission {
    internal data class Receipt(val canBegin: Boolean, val message: String)

    fun inspect(context: Context): Receipt {
        val caps = runCatching {
            val connectivity = context.applicationContext.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as? ConnectivityManager ?: return@runCatching null
            val active = connectivity.activeNetwork ?: return@runCatching null
            connectivity.getNetworkCapabilities(active)
        }.getOrNull() ?: return Receipt(
            false,
            "Android kan de actieve wifi-verbinding niet bevestigen. " +
                "Geen nieuwe modeldownload gestart; controleer je netwerk."
        )
        return when {
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Receipt(
                false, "Alleen via wifi: Android meldt nu geen actieve wifi-verbinding. " +
                    "Mobiele data en andere netwerktypen zijn niet toegestaan."
            )
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> Receipt(
                false, "Deze wifi-verbinding meldt geen internettoegang. " +
                    "Er is geen modeldownload aangevraagd."
            )
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> Receipt(
                false, "Android heeft de internettoegang op wifi nog niet gevalideerd. " +
                    "Voltooi zo nodig eerst de wifi-inlogpagina."
            )
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> Receipt(
                false, "Android markeert deze wifi-verbinding als gemeten of beperkt. " +
                    "Tuddy start hier geen grote offline modeldownload. " +
                    "Gebruik een door Android als ongemeten gemarkeerd wifi-netwerk."
            )
            else -> Receipt(
                true, "Android meldt actieve, gevalideerde en ongemeten wifi. " +
                    "Dit is alleen toelating voor een bewuste download, geen " +
                    "garantie dat de verbinding later niet verandert."
            )
        }
    }

    fun blocker(context: Context): String? =
        inspect(context).takeUnless { it.canBegin }?.message
}
