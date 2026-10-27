package com.livelingo.android.ui

import androidx.compose.runtime.mutableStateOf
import com.livelingo.android.OfflineSessionDiagnostics

/**
 * RAM-only acceptance draft that survives leaving the startcheck for a real
 * microphone turn. Owned by the active conversation composition, not disk,
 * preferences or a process-wide singleton. No utterances or transcripts.
 *
 * Android activity/process recreation discards it. Explicit SAF checkpoints
 * remain the only user-controlled route for saving observed outcomes to disk.
 */
internal class OfflineAcceptanceDraft {
    val baseline = mutableStateOf<ByteArray?>(null)
    val recordedAt = mutableStateOf<Long>(0L)
    val opened = mutableStateOf<Boolean>(false)
    val confirmDiscard = mutableStateOf<Boolean>(false)
    val message = mutableStateOf<String>("")
    val expectedApkSha256 = mutableStateOf<String>("")
    val sidecarNotice = mutableStateOf<String>("")
    val disconnectedConfirmed = mutableStateOf<Boolean>(false)
    val permissionAndStopConfirmed = mutableStateOf<Boolean>(false)
    val noCloudFallbackConfirmed = mutableStateOf<Boolean>(false)
    val privacyClearingConfirmed = mutableStateOf<Boolean>(false)
    val attempts = mutableStateOf<List<OfflineObservedAttempt>>(emptyList())
    val chosenDirection = mutableStateOf<OfflineObservationDirection>(OfflineObservationDirection.NL_EN)
    val observedSpeech = mutableStateOf<Boolean>(false)
    val observedTranslation = mutableStateOf<Boolean>(false)
    val attemptLatency = mutableStateOf<String>("")
    val attemptMessage = mutableStateOf<String>("")
    val pendingReport = mutableStateOf<ByteArray?>(null)
    val pendingBaseline = mutableStateOf<ByteArray?>(null)
    val pendingCheckpoint = mutableStateOf<ByteArray?>(null)
    val checkpointImported = mutableStateOf<Boolean>(false)
    val checkpointNotice = mutableStateOf<String>("")
    // Baseline of actual on-device event counters at the START of the draft.
    val startedDiagnostics = mutableStateOf<OfflineSessionDiagnostics?>(null)
    // Imported checkpoint rows cannot be represented as freshly observed turns.
    val newAttemptFlags = mutableStateOf<List<Boolean>>(emptyList())
}
