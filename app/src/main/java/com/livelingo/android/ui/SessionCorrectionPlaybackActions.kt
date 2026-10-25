package com.livelingo.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.livelingo.android.PrivatePlaybackRoutingPreference
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.SessionTranslationCorrection

/**
 * Explicit listening practice for a single human-authored correction.
 *
 * The existing speaker admits only installed, non-network Android TTS voices;
 * unsupported languages fail closed with a notice, never a network fallback.
 */
@Composable
internal fun SessionCorrectionPlaybackActions(
    entry: SessionTranslationCorrection,
    speaker: LocalCaptionSpeaker
) {
    val context = LocalContext.current
    var notice by remember(entry) { mutableStateOf("") }
    var headphonesOnly by remember(context, entry) {
        mutableStateOf(PrivatePlaybackRoutingPreference.headphonesOnly(context))
    }
    LocalCaptionPlaybackMenu(
        hasCompletedTranslation = true,
        enabled = true,
        headphonesOnly = headphonesOnly,
        onHeadphonesOnlyChange = { selected ->
            speaker.stop()
            val saved = PrivatePlaybackRoutingPreference.setHeadphonesOnly(context, selected)
            headphonesOnly = PrivatePlaybackRoutingPreference.headphonesOnly(context)
            notice = if (!saved)
                "De audio-privacyvoorkeur kon niet worden opgeslagen."
            else if (selected)
                "Privéstand aan: Android moet een aangesloten hoofdtelefoon melden."
            else "Privéstand uit: luidspreker kan worden gebruikt na bewust afspelen."
        },
        onPlayOriginal = { pace ->
            speaker.speak(entry.original, entry.sourceCode, pace) { notice = it }
        },
        onPlayTranslation = { pace ->
            speaker.speak(entry.translation, entry.targetCode, pace) { notice = it }
        },
        onStop = {
            speaker.stop()
            notice = "Lokale voorlezing gestopt."
        }
    )
    if (notice.isNotEmpty()) {
        Text(notice, style = MaterialTheme.typography.bodySmall)
    }
}
