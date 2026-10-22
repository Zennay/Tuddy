package com.livelingo.android

import java.text.Normalizer
import java.util.Locale

/**
 * Display-only search tolerance for user-authored Kurmanji, Sorani and Pashto
 * cards. Never call this when saving, deduplicating, exporting or translating:
 * the user's exact original script and distinctions must remain intact.
 *
 * NFKC handles presentation-form keyboards; the tiny explicit table handles
 * Arabic/Persian keyboard variants of yeh and kaf. Harakat, elongation and
 * ZWNJ can be omitted while *searching*. Letters such as ه/ە and hamza forms
 * deliberately remain distinct rather than inventing a translation.
 */
internal object PrivatePhraseScriptSearch {
    fun fold(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
        return buildString(normalized.length) {
            normalized.forEach { character ->
                when (character) {
                    '\u064A', '\u0649' -> append('\u06CC') // Arabic yeh/alef maksura → Farsi yeh
                    '\u0643' -> append('\u06A9') // Arabic kaf → keheh
                    '\u0640', '\u200C', '\u0670' -> Unit // tatweel, ZWNJ, superscript alef
                    in '\u064B'..'\u065F' -> Unit // optional Arabic vowel marks
                    else -> append(character)
                }
            }
        }
    }

    fun contains(text: String, foldedQuery: String): Boolean =
        fold(text).contains(foldedQuery)

    fun beginsWith(text: String, foldedQuery: String): Boolean =
        fold(text).startsWith(foldedQuery)

    fun equalsTo(text: String, foldedQuery: String): Boolean =
        fold(text) == foldedQuery
}
