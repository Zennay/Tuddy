package com.livelingo.android

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Reads only an explicitly user-selected Android document into a bounded RAM
 * buffer. This does not persist URI grants, create copies or parse external links.
 * A document provider MAY be remote: users choose that provider themselves.
 */
internal object PrivateSubtitleImportDocument {
    sealed class Result {
        data class Ready(val text: String, val sizeBytes: Int) : Result()
        data class Rejected(val reason: String) : Result()
    }

    fun read(resolver: ContentResolver, uri: Uri): Result {
        return try {
            val stream = resolver.openInputStream(uri)
                ?: return Result.Rejected("De gekozen provider heeft geen leesbaar document geleverd.")
            val bytes = stream.use { input ->
                ByteArrayOutputStream().use { output ->
                    val chunk = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(chunk)
                        if (read < 0) break
                        if (read == 0) continue
                        total += read
                        if (total > PrivateSubtitleFileReader.MAX_BYTES)
                            return Result.Rejected(
                                "Document groter dan 256 KiB; het bestand is niet ingeladen."
                            )
                        output.write(chunk, 0, read)
                    }
                    output.toByteArray()
                }
            }
            if (bytes.isEmpty()) return Result.Rejected("Het gekozen document is leeg.")
            val decoded = try {
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
            } catch (_: Exception) {
                return Result.Rejected("Alleen volledig geldige UTF-8 tekstbestanden worden geaccepteerd.")
            }
            if (decoded.any { it == '\u0000' ||
                (it.isISOControl() && it != '\n' && it != '\r' && it != '\t') }) {
                return Result.Rejected("Document bevat binaire of onveilige controletekens.")
            }
            Result.Ready(decoded, bytes.size)
        } catch (_: SecurityException) {
            Result.Rejected("Geen toegang tot dit document. Kies het opnieuw via Android.")
        } catch (_: Exception) {
            Result.Rejected("Document kon niet worden gelezen via de gekozen provider.")
        }
    }
}
