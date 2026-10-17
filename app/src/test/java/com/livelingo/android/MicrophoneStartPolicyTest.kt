package com.livelingo.android

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrophoneStartPolicyTest {
    @Test fun backgroundOrDestroyedActivityCannotOpenMicrophone() {
        assertFalse(MicrophoneStartPolicy.mayStart(Lifecycle.State.INITIALIZED))
        assertFalse(MicrophoneStartPolicy.mayStart(Lifecycle.State.CREATED))
        assertFalse(MicrophoneStartPolicy.mayStart(Lifecycle.State.DESTROYED))
    }

    @Test fun foregroundActivityCanStartOnlyWhenAtLeastStarted() {
        assertTrue(MicrophoneStartPolicy.mayStart(Lifecycle.State.STARTED))
        assertTrue(MicrophoneStartPolicy.mayStart(Lifecycle.State.RESUMED))
    }
}
