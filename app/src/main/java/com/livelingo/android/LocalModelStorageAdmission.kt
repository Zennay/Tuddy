package com.livelingo.android

import android.os.Environment
import android.os.StatFs

/**
 * Lightweight storage preflight only for a NEW, explicitly user-approved
 * offline model download. All model inventory and translation operations
 * remain available when storage is low.
 *
 * There is no reliable model size estimate or promise that Android can reserve
 * space for the requested model. A conservative free-space buffer avoids
 * starting a discretionary download into an almost full /data partition.
 * Never persists free bytes or device information.
 */
internal object LocalModelStorageAdmission {
    private const val MIN_FREE_BYTES = 512L * 1024L * 1024L
    private const val MIB = 1024L * 1024L

    internal data class Receipt(
        val canBegin: Boolean,
        val availableMiB: Long?,
        val message: String
    )

    fun inspect(): Receipt {
        val freeBytes = runCatching {
            StatFs(Environment.getDataDirectory().absolutePath).availableBytes
        }.getOrNull()
        if (freeBytes == null || freeBytes < 0L) {
            return Receipt(false, null,
                "Android kon de vrije interne opslagruimte niet betrouwbaar lezen. " +
                    "Geen nieuw offline model aangevraagd; controleer de opslag in Android-instellingen.")
        }
        val available = freeBytes / MIB
        if (freeBytes < MIN_FREE_BYTES) {
            return Receipt(false, available,
                "Interne opslag bijna vol: ongeveer $available MiB vrij. " +
                    "Tuddy start geen nieuwe offline modeldownload onder 512 MiB vrije ruimte. " +
                    "Maak veilig ruimte vrij via Android-instellingen en probeer bewust opnieuw.")
        }
        return Receipt(true, available,
            "Android meldt ongeveer $available MiB vrije interne opslag. " +
                "Dit is alleen een startvoorwaarde, geen garantie dat het model past of zal installeren.")
    }

    /** Read the live amount again immediately before each submitted request. */
    fun blocker(): String? = inspect().takeUnless { it.canBegin }?.message
}
