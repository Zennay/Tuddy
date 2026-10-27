package com.livelingo.android

/**
 * Bounded, process-only OCR block review before explicit offline translation.
 * No file, image, intent extra, clipboard or speech data is ever written here.
 * Keeping individual blocks lets users select a useful 8,000-character excerpt
 * from a longer Latin-script image instead of discarding all recognized text.
 */
internal data class PrivateImageTextBlock(val index: Int, val text: String)

internal object PrivateImageTextBlocks {
    const val MAX_HANDOFF_CHARS = 8_000
    private const val MAX_BLOCKS = 100
    private const val MAX_OCR_CHARS = 80_000

    fun admit(raw: List<String>): List<PrivateImageTextBlock>? {
        if (raw.isEmpty() || raw.size > MAX_BLOCKS) return null
        var total = 0
        val admitted = mutableListOf<PrivateImageTextBlock>()
        for (part in raw) {
            if (part.length > MAX_OCR_CHARS - total) return null
            total += part.length
            val normalized = part.trim()
            if (normalized.isBlank()) continue
            // Reject non-printable contents rather than forwarding unexpected
            // binary or control payloads from the device OCR implementation.
            if (normalized.any { char ->
                    char == '\u0000' || (char.isISOControl() && char != '\n' &&
                        char != '\r' && char != '\t')
                }) return null
            admitted.add(PrivateImageTextBlock(admitted.size, normalized))
        }
        return admitted.takeIf { it.isNotEmpty() }
    }

    fun assemble(
        blocks: List<PrivateImageTextBlock>,
        selected: Set<Int>
    ): String? {
        if (selected.isEmpty()) return null
        if (selected.any { id -> blocks.none { it.index == id } }) return null
        val output = StringBuilder()
        for (block in blocks) {
            if (block.index !in selected) continue
            // Avoid allocating an oversized string for every checkbox tap.
            val added = block.text.length + if (output.isNotEmpty()) 2 else 0
            if (added > MAX_HANDOFF_CHARS - output.length) return null
            if (output.isNotEmpty()) output.append("\n\n")
            output.append(block.text)
        }
        val text = output.toString()
        return text.takeIf { it.isNotBlank() }
    }
}
