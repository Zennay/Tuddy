package com.livelingo.android

import android.content.Intent
import android.os.SystemClock

/**
 * One-shot, process-local OCR -> translator handoff. Only an opaque token
 * crosses Activity intents; private recognized text never enters Intent
 * extras, saved state, preferences, files or the clipboard.
 *
 * A process death or expired token fails closed and asks the user to pick
 * the image again. QuickTranslateActivity is not exported.
 */
internal object PrivateImageTextHandoff {
    const val ACTION = "com.livelingo.android.action.PRIVATE_IMAGE_TEXT"
    private const val EXTRA_TOKEN = "com.livelingo.android.extra.PRIVATE_IMAGE_TOKEN"
    private const val MAX_TEXT = 8_000
    private const val LIFETIME_MS = 15_000L

    private var nextToken = 0L
    private var stagedToken = 0L
    private var stagedText: String? = null
    private var expiresAt = 0L

    @Synchronized
    fun offer(text: String): Intent? {
        val accepted = text.trim()
        if (accepted.isEmpty() || accepted.length > MAX_TEXT) return null
        nextToken++
        stagedToken = nextToken
        stagedText = accepted
        expiresAt = SystemClock.elapsedRealtime() + LIFETIME_MS
        return Intent(ACTION).apply {
            putExtra(EXTRA_TOKEN, stagedToken)
        }
    }

    @Synchronized
    fun consume(intent: Intent?): String? {
        if (intent?.action != ACTION) return null
        val token = runCatching {
            intent.getLongExtra(EXTRA_TOKEN, 0L)
        }.getOrDefault(0L)
        intent.removeExtra(EXTRA_TOKEN)
        val result = if (token != 0L && token == stagedToken &&
            SystemClock.elapsedRealtime() <= expiresAt) stagedText else null
        stagedText = null
        stagedToken = 0L
        expiresAt = 0L
        return result
    }
}
