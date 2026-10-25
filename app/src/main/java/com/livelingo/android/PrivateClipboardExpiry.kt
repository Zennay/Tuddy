package com.livelingo.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import java.security.MessageDigest
import java.util.UUID

/**
 * Opt-in best-effort expiration for explicitly copied Android text.
 *
 * The system clipboard is outside Tuddy: once copied, other apps can read or
 * retain text immediately. This only attempts to clear Tuddy's own latest
 * unchanged clip if the process remains active and Android allows clipboard
 * inspection when the timer fires. It never clears another app's subsequent
 * copy or promises deletion from other apps/clipboard history.
 */
internal object PrivateClipboardExpiry {
    private const val PREFERENCES = "tuddy-private-clipboard-v1"
    private const val KEY_SECONDS = "expiry-seconds"
    private const val MAX_TEXT_CHARS = 131_072
    val choices = setOf(0, 60, 300)

    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    fun selectedSeconds(context: Context): Int =
        runCatching {
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getInt(KEY_SECONDS, 0)
        }.getOrDefault(0).takeIf { it in choices } ?: 0

    fun changeSeconds(context: Context, seconds: Int): Boolean {
        if (seconds !in choices) return false
        val saved = runCatching {
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putInt(KEY_SECONDS, seconds).commit()
        }.getOrDefault(false)
        if (saved) cancelPending() // New policy applies to the next explicit copy.
        return saved
    }

    fun copy(context: Context, label: String, text: String): Boolean {
        if (text.isEmpty() || text.length > MAX_TEXT_CHARS) return false
        val clipboard = context.applicationContext.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as? ClipboardManager ?: return false

        val marker = "Tuddy · " + label.take(60) + " · " + UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(marker, text)
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        val written = runCatching { clipboard.setPrimaryClip(clip); true }
            .getOrDefault(false)
        if (!written) return false

        cancelPending()
        val seconds = selectedSeconds(context)
        if (seconds == 0) return true

        // No raw transcript retained in timers, SharedPreferences, or logs.
        val fingerprint = sha256(text)
        val task = Runnable {
            pending = null
            runCatching {
                val current = clipboard.primaryClip ?: return@runCatching
                if (current.itemCount != 1 ||
                    current.description.label?.toString() != marker ||
                    current.description.getMimeType(0) != "text/plain"
                ) return@runCatching
                val currentText = current.getItemAt(0).text?.toString()
                    ?: return@runCatching
                if (sha256(currentText) == fingerprint) {
                    clipboard.clearPrimaryClip()
                }
            }
            // Access is sometimes denied by Android when the app is not
            // foreground. This expiry is explicitly best-effort, not a guarantee.
        }
        pending = task
        if (!handler.postDelayed(task, seconds * 1000L)) {
            pending = null
        }
        return true
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun cancelPending() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }
}
