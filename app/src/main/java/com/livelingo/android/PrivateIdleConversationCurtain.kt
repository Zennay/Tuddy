package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Explicit opt-in privacy cover for an unattended foreground conversation.
 *
 * Only a duration preference is persisted. The timer holds no captions,
 * audio, Android identifiers, timestamps or usage history. Its callback owns
 * the existing stop-and-cover action; it does not destroy unsaved transcript.
 * Android's onUserInteraction resets the deadline for touch/key interactions.
 */
internal class PrivateIdleConversationCurtain(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var foreground = false
    private var callback: (() -> Unit)? = null
    private var task: Runnable? = null
    private var epoch = 0L

    fun selectedMinutes(): Int = readMinutes(appContext)

    fun changeMinutes(minutes: Int): Boolean {
        if (minutes !in CHOICES) return false
        val saved = runCatching {
            appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MINUTES, minutes).commit()
        }.getOrDefault(false)
        if (saved) restart()
        return saved
    }

    fun attach(onIdle: () -> Unit) {
        callback = onIdle
        restart()
    }

    fun detach() {
        callback = null
        cancel()
    }

    fun onForeground() {
        foreground = true
        restart()
    }

    fun onBackground() {
        foreground = false
        cancel()
    }

    fun onUserInteraction() {
        if (foreground && callback != null) restart()
    }

    private fun cancel() {
        epoch++
        task?.let(handler::removeCallbacks)
        task = null
    }

    private fun restart() {
        cancel()
        val minutes = selectedMinutes()
        if (!foreground || callback == null || minutes == 0) return
        val current = epoch
        val timeout = Runnable {
            if (epoch != current || !foreground) return@Runnable
            task = null
            // Disarm first: a slow callback or Compose recomposition cannot
            // cover twice, and returning from another Activity never runs it.
            val cover = callback
            callback = null
            epoch++
            cover?.invoke()
        }
        task = timeout
        if (!handler.postDelayed(timeout, minutes * 60_000L)) {
            task = null
            // The privacy timer is best-effort: the user must not be told
            // the device is definitely protected if Android rejects a timer.
        }
    }

    companion object {
        val CHOICES = setOf(0, 5, 15, 30)
        private const val PREFERENCES = "tuddy-private-idle-curtain-v1"
        private const val KEY_MINUTES = "minutes"

        fun readMinutes(context: Context): Int = runCatching {
            context.applicationContext.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE
            ).getInt(KEY_MINUTES, 0)
        }.getOrDefault(0).takeIf { it in CHOICES } ?: 0
    }
}
