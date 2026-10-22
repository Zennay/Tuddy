package com.livelingo.android

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight opt-in routing hint for one Android-picked CSV content URI.
 * Never loads a document on the UI thread, follows a URL, stores its text,
 * or changes personal notes. Both destination dialogs still fully validate
 * bounded content and require user confirmation before importing.
 */
internal object PrivateOwnWordCsvUriRoute {
    suspend fun matchesOwnWordHeader(resolver: ContentResolver, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            if (uri.scheme != "content" || uri.authority.isNullOrBlank())
                return@withContext false
            try {
                resolver.openInputStream(uri)?.use { source ->
                    val prefix = ByteArray(64)
                    var length = 0
                    while (length < prefix.size) {
                        val count = source.read(prefix, length, prefix.size - length)
                        if (count <= 0) break
                        length += count
                        if (prefix.take(length).any { it == '\n'.code.toByte() }) break
                    }
                    val text = prefix.copyOf(length).toString(Charsets.UTF_8)
                        .removePrefix("\uFEFF")
                    text.startsWith("language,word,meaning\r\n") ||
                        text.startsWith("language,word,meaning\n")
                } ?: false
            } catch (_: Exception) { false }
        }
}
