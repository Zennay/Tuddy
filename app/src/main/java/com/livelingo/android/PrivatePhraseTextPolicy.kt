package com.livelingo.android

/** One safe admission rule for explicitly user-authored offline phrase notes.
 * Accept intact supplementary letters, but no isolated UTF-16 surrogates,
 * bidi-override/format controls (except script ZWNJ), or control characters.
 * No language recognition, translation or network access is performed.
 */
internal object PrivatePhraseTextPolicy {
    fun wellFormed(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (Character.isLowSurrogate(char)) return false
            if (Character.isHighSurrogate(char)) {
                if (index + 1 >= value.length ||
                    !Character.isLowSurrogate(value[index + 1])) return false
                index += 2
            } else index++
        }
        return true
    }

    fun valid(value: String, max: Int): Boolean {
        if (value.isBlank() || value.length > max ||
            value != value.trim() || !wellFormed(value)) return false
        var offset = 0
        while (offset < value.length) {
            val codepoint = Character.codePointAt(value, offset)
            if (Character.isISOControl(codepoint) ||
                (Character.getType(codepoint) == Character.FORMAT.toInt() &&
                    codepoint != 0x200C) ||
                codepoint == 0x2028 || codepoint == 0x2029) return false
            offset += Character.charCount(codepoint)
        }
        return true
    }
}
