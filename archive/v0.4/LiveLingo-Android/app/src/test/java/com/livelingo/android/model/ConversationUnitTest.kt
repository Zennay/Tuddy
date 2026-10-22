package com.livelingo.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationUnitTest {
    @Test fun interimTranslationIgnoresStaleResults() {
        val first = Conversation().partial("Do you")
        val second = first.partial("Do you understand")
        assertEquals("", second.interimTranslation(first.revision, "Stale").activeTranslation)
        assertEquals("Verstehst du", second.interimTranslation(second.revision, "Verstehst du").activeTranslation)
    }

    @Test fun finalCaptionsStayPairedWithTheirOwnTranslation() {
        val first = Conversation().finish("Hello", 100L)
        val second = first.finish("Do you understand?", 200L)
        val delayedFirst = second.finalTranslation(1L, "Hallo")
        val reordered = delayedFirst.finalTranslation(2L, "Verstehst du?")
        assertEquals(1L, reordered.lines[0].id)
        assertEquals("Hallo", reordered.lines[0].translation)
        assertEquals("Verstehst du?", reordered.lines[1].translation)
    }

    @Test fun currentUtteranceDoesNotDuplicateFinal() {
        val spoken = Conversation().partial("Hi").partial("Hi there")
        val completed = spoken.finish("Hi there", 11L)
        assertEquals(1, completed.lines.size)
        assertEquals("", completed.activeSource)
        assertFalse(completed.activeTranslation.isNotEmpty())
    }

    @Test fun boundedAndErasableHistory() {
        var session = Conversation()
        repeat(550) { session = session.finish("Line $it", it.toLong()) }
        assertEquals(500, session.lines.size)
        assertEquals(51L, session.lines.first().id)
        assertTrue(session.clear().lines.isEmpty())
    }
}
