package com.livelingo.android

/**
 * A process-level admission fence for *explicitly user-authorized* ML Kit
 * Wi-Fi text-model preparation. An Android download may continue after its
 * calling screen is dismissed or its UI deadline expires. Do not start
 * another download until that real Task reports completion.
 *
 * A callback that never arrives leaves this admission locked for this app
 * process rather than guessing that ML Kit stopped downloading. No sentence,
 * user identity, downloaded model list, or persistent state is held here.
 */
internal class LocalTextModelDownloadAdmission private constructor() {
    private var occupied = false

    class Lease internal constructor(private val owner: LocalTextModelDownloadAdmission) {
        private var released = false
        fun release() {
            if (released) return
            released = true
            owner.occupied = false
        }
    }

    fun acquire(): Lease? {
        if (occupied) return null
        occupied = true
        return Lease(this)
    }

    fun isBusy(): Boolean = occupied

    companion object {
        val shared = LocalTextModelDownloadAdmission()
    }
}
