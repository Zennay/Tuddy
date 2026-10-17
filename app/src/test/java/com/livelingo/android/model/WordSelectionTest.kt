package com.livelingo.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordSelectionTest {
    @Test fun tapsReturnContiguousWordsNotSurroundingPunctuation() {
        assertEquals("Verstehst", wordAt("Verstehst du?", 4))
        assertEquals("du", wordAt("Verstehst du?", 10))
        assertEquals("l’amour", wordAt("l’amour", 3))
        assertEquals("co-operate", wordAt("co-operate", 4))
        assertEquals("سڵاو", wordAt("سڵاو!", 1))
        assertEquals("پښتو", wordAt("پښتو!", 2))
        assertEquals("water", wordAt("water.", 3))
    }

    @Test fun punctuationWhitespaceAndBoundsNeverChooseAnAdjacentWord() {
        assertNull(wordAt("hi!", 2))
        assertNull(wordAt("hello world", 5))
        assertNull(wordAt("hello world", -1))
        assertNull(wordAt("hello world", 99))
        assertNull(wordAt("'", 0))
        assertNull(wordAt("-", 0))
        assertNull(wordAt("...", 1))
    }

    @Test fun textIsNotAlteredAndNoLanguageAssumptionsAreMade() {
        val original = "  السلام عليكم  "
        assertEquals("السلام", wordAt(original, 4))
        assertEquals("عليكم", wordAt(original, 11))
        assertEquals("  السلام عليكم  ", original)
        assertNull(wordAt("", 0))
    }
}
