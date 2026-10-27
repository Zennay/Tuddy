package com.livelingo.android

/**
 * One process-wide admission slot for Android's *system-managed* offline
 * speech-pack download requests, shared by every transient UI controller.
 *
 * Destroying a SpeechRecognizer, leaving a screen, or losing a callback is
 * NOT proof that Android stopped a previously submitted model download.
 * Once a request is submitted, only a terminal system callback (Android 14+)
 * or a subsequent read-only EXACT installed-pack verification releases it.
 * Android 13 cannot observe physical completion; keep the slot occupied until
 * a separately observed installed state or app-process restart.
 *
 * Stores only a single speech locale in RAM, never an account, audio, text,
 * network address, model bytes, or persistent installation claim.
 */
internal object LocalSpeechPackDownloadAdmission {
    private var heldTag: String? = null
    private var submitted = false

    @Synchronized
    fun acquire(tag: String): Boolean {
        if (heldTag != null || tag.isBlank() || tag.length > 40) return false
        heldTag = tag
        submitted = false
        return true
    }

    @Synchronized
    fun markSubmitted(tag: String) {
        if (heldTag == tag) submitted = true
    }

    /** Only before asking the Android service to download any model. */
    @Synchronized
    fun releaseBeforeSubmission(tag: String) {
        if (heldTag == tag && !submitted) {
            heldTag = null
            submitted = false
        }
    }

    /** Android 14+ actually reported success or failure; never a UI cancel. */
    @Synchronized
    fun systemCompleted(tag: String) {
        if (heldTag == tag && submitted) {
            heldTag = null
            submitted = false
        }
    }

    /** Exact, independently reported INSTALLED pack, never PENDING or READY variant. */
    @Synchronized
    fun verifiedInstalled(tag: String) {
        if (heldTag == tag && submitted) {
            heldTag = null
            submitted = false
        }
    }

    @Synchronized
    fun pendingTag(): String? = heldTag
}
