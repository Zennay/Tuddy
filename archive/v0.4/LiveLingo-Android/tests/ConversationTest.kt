import com.livelingo.android.model.Conversation

fun main() {
    val empty = Conversation()
    check(empty.lines.isEmpty())
    val a = empty.partial("Do you")
    check(a.activeSource == "Do you")
    check(a.revision == 1L)
    val same = a.partial("Do you")
    check(same === a)
    val b = a.partial("Do you understand?")
    val obsolete = b.interimTranslation(a.revision, "Du")
    check(obsolete.activeTranslation.isEmpty()) { "Stale translation applied" }
    val updated = obsolete.interimTranslation(b.revision, "Verstehst du?")
    check(updated.activeTranslation == "Verstehst du?")
    val finalized = updated.finish("Do you understand?", 123L)
    check(finalized.activeSource.isEmpty())
    check(finalized.lines.single().source == "Do you understand?")
    check(finalized.lines.single().id == 1L)
    val translated = finalized.finalTranslation(1L, "Verstehst du?")
    check(translated.lines.single().translation == "Verstehst du?")
    check(translated.interimTranslation(b.revision, "wrong") == translated)
    val second = translated.finish("Hello", 456L)
    check(second.lines.last().id == 2L)
    check(second.lines.size == 2)
    check(second.clear().lines.isEmpty())
    var large = Conversation()
    repeat(505) { large = large.finish("line $it", it.toLong()) }
    check(large.lines.size == 500)
    check(large.lines.first().id == 6L)
    println("PASS 14 checks: live interim, stale results, final alignment, bounded history and clear")
}
