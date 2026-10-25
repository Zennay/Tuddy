package com.livelingo.android

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

/**
 * A strictly session-scoped Android media STOP boundary for private local TTS.
 *
 * Headset media pause/stop and Android's active media transport can cancel a
 * user-started utterance without reopening Tuddy. PLAY/RESUME is deliberately
 * never offered or implemented; no external event can start the microphone,
 * resume speech or recover private text after the app has stopped.
 *
 * The session publishes no title, artwork, duration, subtitle, utterance,
 * source app, caller or other metadata. No notification, service or persistent
 * media session is created. All callbacks are generation fenced and destroyed
 * on Stop, normal completion, background or a new playback.
 */
internal class PrivatePlaybackMediaControls(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var session: MediaSession? = null
    private var epoch = 0L

    /**
     * Fail closed if Android cannot establish a cancellable active session.
     * Only the stop callback, never speech/text, crosses this boundary.
     */
    fun open(onStopRequested: () -> Unit): Boolean {
        close()
        val capturedEpoch = epoch
        var created: MediaSession? = null
        return try {
            val media = MediaSession(appContext, "tuddy-local-private-tts")
            created = media
            val callback = object : MediaSession.Callback() {
                private fun stopCurrentSession() {
                    if (epoch == capturedEpoch && session === media) {
                        onStopRequested()
                    }
                }

                override fun onPause() = stopCurrentSession()
                override fun onStop() = stopCurrentSession()

                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(
                        Intent.EXTRA_KEY_EVENT
                    ) ?: return false
                    val stopKey = when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_STOP,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_HEADSETHOOK -> true
                        else -> false
                    }
                    if (!stopKey) return false
                    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                        stopCurrentSession()
                    }
                    // Consume UP as well, so a single click cannot also play
                    // a stale queued utterance. PLAY-only is never handled.
                    return true
                }
            }
            media.setCallback(callback, main)
            media.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
            media.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
                    .setState(
                        PlaybackState.STATE_PLAYING,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                        1.0f
                    )
                    .build()
            )
            session = media
            media.isActive = true
            true
        } catch (_: RuntimeException) {
            created?.let { orphan ->
                if (orphan !== session) runCatching { orphan.release() }
            }
            close()
            false
        }
    }

    fun close() {
        epoch++
        val previous = session
        session = null
        if (previous != null) {
            runCatching { previous.isActive = false }
            runCatching { previous.setCallback(null) }
            runCatching { previous.release() }
        }
    }
}
