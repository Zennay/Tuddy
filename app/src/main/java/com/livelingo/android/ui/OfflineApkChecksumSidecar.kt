package com.livelingo.android.ui

/**
 * Android's manually selected checksum sidecar must match the exact
 * sha256sum format published beside app-debug.apk in the hosted build.
 *
 * A matching checksum is only an identity check against the human-selected
 * artifact: it never certifies who produced the file or approves a device.
 */
internal object OfflineApkChecksumSidecar {
    const val MAX_BYTES = 256

    fun parse(raw: String): String? {
        if (raw.toByteArray(Charsets.UTF_8).size !in 1..MAX_BYTES) return null
        val normalized = raw.removeSuffix("\n").removeSuffix("\r")
        if ('\n' in normalized || '\r' in normalized) return null
        val parts = normalized.split("  ", limit = 2)
        if (parts.size != 2 || parts[1] != "app-debug.apk") return null
        val digest = parts[0]
        if (digest.length != 64 || !digest.all {
                it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
            }) return null
        return digest.lowercase()
    }
}
