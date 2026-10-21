package com.livelingo.android.ui

import androidx.compose.ui.text.style.TextDirection

/**
 * Pick direction from the first Unicode strong character, not the phone UI
 * locale or a guessed language label. RTL fallback applies only when a string
 * has no strong directional characters (for example, a number-only caption).
 */
internal object PrivateTextDirection {
    fun forContent(text: CharSequence, fallbackRtl: Boolean = false): TextDirection {
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            when (Character.getDirectionality(codePoint)) {
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return TextDirection.Ltr
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return TextDirection.Rtl
            }
            index += Character.charCount(codePoint)
        }
        return if (fallbackRtl) TextDirection.Rtl else TextDirection.Ltr
    }
}
