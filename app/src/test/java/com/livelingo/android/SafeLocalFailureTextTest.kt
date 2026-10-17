package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SafeLocalFailureTextTest {
    @Test fun speechFailureContainsOnlyStaticActionableText() {
        assertEquals(
            "Offline spraakdienst kon niet starten. Controleer de microfoontoestemming en lokale spraakpakketten.",
            SafeLocalFailureText.SPEECH_START
        )
    }

    @Test fun translationFailureContainsOnlyStaticActionableText() {
        assertEquals(
            "Vertalen mislukt. Controleer het lokale taalmodel en probeer opnieuw.",
            SafeLocalFailureText.TRANSLATION
        )
    }

    @Test fun neitherFailureFieldCanAccidentallyRevealProviderContext() {
        for (safe in listOf(SafeLocalFailureText.SPEECH_START, SafeLocalFailureText.TRANSLATION)) {
            assertFalse(safe.contains("https://", ignoreCase = true))
            assertFalse(safe.contains("token", ignoreCase = true))
            assertFalse(safe.contains("Bearer", ignoreCase = true))
            assertFalse(safe.contains("Exception", ignoreCase = true))
            assertFalse(safe.contains("@"))
            assertFalse(safe.length > 150)
        }
    }
}
