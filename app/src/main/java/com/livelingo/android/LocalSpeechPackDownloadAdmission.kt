package com.livelingo.android

/**
 * One process-wide physical admission for Android's system-managed offline
 * speech-pack downloads. Leaving a screen/destroying SpeechRecognizer does
 * not imply that Android canceled a previously submitted request.
 *
 * An immutable lease identity prevents an old system callback from releasing
 * a newer request, even for the same speech locale. Only a terminal Android
 * callback or a subsequent read-only EXACT installed-pack verification
 * releases a submitted lease. Android 13 has no physical completion callback:
 * its slot may remain busy until a verified installed pack or process restart.
 *
 * All state is transient RAM; no utterances, account/network data or model
 * content are retained.
 */
internal object LocalSpeechPackDownloadAdmission {
    internal class Lease internal constructor(val tag: String) {
        internal var submitted: Boolean = false
    }

    private var current: Lease? = null

    @Synchronized
    fun acquire(tag: String): Lease? {
        if (current != null || tag.isBlank() || tag.length > 40) return null
        return Lease(tag).also { current = it }
    }

    @Synchronized
    fun markSubmitted(lease: Lease) {
        if (current === lease) lease.submitted = true
    }

    @Synchronized
    fun releaseBeforeSubmission(lease: Lease) {
        if (current === lease && !lease.submitted) current = null
    }

    /** Android 14+ reported real completion/error for this exact request. */
    @Synchronized
    fun systemCompleted(lease: Lease) {
        if (current === lease && lease.submitted) current = null
    }

    /** Only an exact, independently reported installed locale qualifies. */
    @Synchronized
    fun verifiedInstalled(tag: String) {
        if (current?.tag == tag && current?.submitted == true) current = null
    }

    @Synchronized
    fun pendingTag(): String? = current?.tag
}
