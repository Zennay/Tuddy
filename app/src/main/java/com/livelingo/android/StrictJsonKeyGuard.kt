package com.livelingo.android

import android.util.JsonReader
import android.util.JsonToken
import java.io.StringReader

/**
 * Strictly scan user-supplied JSON before JSONObject/JSONArray parsing.
 *
 * org.json keeps only one value when an object repeats a key, which makes an
 * otherwise exact import schema ambiguous. This scanner rejects duplicate
 * decoded keys at every nesting level, malformed/trailing JSON, excessive
 * nesting and pathological token counts before any import preview is offered.
 */
internal object StrictJsonKeyGuard {
    private const val MAX_DEPTH = 32
    private const val MAX_TOKENS = 40_000

    fun accepts(source: String): Boolean = try {
        JsonReader(StringReader(source)).use { reader ->
            reader.isLenient = false
            val budget = Budget()
            consume(reader, depth = 0, budget = budget) &&
                reader.peek() == JsonToken.END_DOCUMENT
        }
    } catch (_: Exception) {
        false
    }

    private class Budget(var tokens: Int = 0)

    private fun consume(reader: JsonReader, depth: Int, budget: Budget): Boolean {
        if (depth > MAX_DEPTH || ++budget.tokens > MAX_TOKENS) return false
        return when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                val names = HashSet<String>()
                while (reader.hasNext()) {
                    if (++budget.tokens > MAX_TOKENS) return false
                    val name = reader.nextName()
                    if (!names.add(name) || !consume(reader, depth + 1, budget))
                        return false
                }
                reader.endObject()
                true
            }
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (!consume(reader, depth + 1, budget)) return false
                }
                reader.endArray()
                true
            }
            JsonToken.STRING -> {
                reader.nextString()
                true
            }
            JsonToken.NUMBER -> {
                reader.nextString()
                true
            }
            JsonToken.BOOLEAN -> {
                reader.nextBoolean()
                true
            }
            JsonToken.NULL -> {
                reader.nextNull()
                true
            }
            else -> false
        }
    }
}
