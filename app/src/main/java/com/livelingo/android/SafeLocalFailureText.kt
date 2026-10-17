package com.livelingo.android

/**
 * Android SpeechRecognizer and ML Kit errors may contain a request URI,
 * provider endpoint, local filesystem path or other sensitive user context.
 * Such exception messages must never be rendered as subtitles or UI errors.
 *
 * Only reviewed, static Dutch recovery guidance crosses the view boundary.
 * No Throwable is accepted here by design.
 */
internal object SafeLocalFailureText {
    const val SPEECH_START =
        "Offline spraakdienst kon niet starten. Controleer de microfoontoestemming en lokale spraakpakketten."
    const val TRANSLATION =
        "Vertalen mislukt. Controleer het lokale taalmodel en probeer opnieuw."
}
