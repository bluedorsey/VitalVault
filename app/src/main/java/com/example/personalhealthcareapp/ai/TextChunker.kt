package com.example.personalhealthcareapp.ai

/**
 * Medical-document-aware text chunker.
 *
 * Strategy:
 *  - Character-based (~500 chars target, 100 char overlap)
 *  - Splits on sentence boundaries (period/newline)
 *  - Preserves medical section headers (FINDINGS:, IMPRESSION:, etc.)
 *  - Keeps short texts as single chunks
 */
object TextChunker {

    private const val TARGET_CHUNK_SIZE = 500
    private const val OVERLAP_SIZE = 100

    // Regex for sentence-ending boundaries
    private val SENTENCE_END = Regex("""(?<=[.!?\n])\s+""")

    // Common medical report section headers
    private val SECTION_HEADERS = listOf(
        "FINDINGS:", "IMPRESSION:", "CONCLUSION:", "DIAGNOSIS:",
        "MEDICATIONS:", "HISTORY:", "PROCEDURE:", "RESULTS:",
        "RECOMMENDATIONS:", "CLINICAL INFORMATION:", "INDICATION:",
        "TECHNIQUE:", "COMPARISON:", "REPORT:", "SUMMARY:",
        "CHIEF COMPLAINT:", "ASSESSMENT:", "PLAN:"
    )

    /**
     * Chunks the given [text] into overlapping segments suitable for
     * embedding and vector search.
     *
     * @param text The full OCR-extracted text from a medical document
     * @return A list of text chunks, each roughly [TARGET_CHUNK_SIZE] chars
     */
    fun chunkText(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // If the text is short enough, return as a single chunk
        if (text.length <= TARGET_CHUNK_SIZE) return listOf(text.trim())

        val sentences = splitIntoSentences(text)
        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()

        for (sentence in sentences) {
            // If adding this sentence would exceed target, finalize current chunk
            if (currentChunk.length + sentence.length > TARGET_CHUNK_SIZE && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())

                // Build overlap from the tail of the current chunk
                val overlap = buildOverlap(currentChunk.toString())
                currentChunk.clear()
                currentChunk.append(overlap)
            }

            // If a single sentence is longer than target, split it further
            if (sentence.length > TARGET_CHUNK_SIZE) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }
                chunks.addAll(splitLongSentence(sentence))
            } else {
                currentChunk.append(sentence)
            }
        }

        // Don't forget the last chunk
        if (currentChunk.isNotEmpty()) {
            val trimmed = currentChunk.toString().trim()
            if (trimmed.isNotEmpty()) {
                chunks.add(trimmed)
            }
        }

        return chunks
    }

    /**
     * Splits text into sentences, keeping medical section headers
     * attached to their content.
     */
    private fun splitIntoSentences(text: String): List<String> {
        val parts = text.split(SENTENCE_END)
        val result = mutableListOf<String>()

        var i = 0
        while (i < parts.size) {
            val part = parts[i].trim()
            if (part.isEmpty()) {
                i++
                continue
            }

            // If this part ends with a section header, merge with next part
            val endsWithHeader = SECTION_HEADERS.any { header ->
                part.uppercase().endsWith(header)
            }

            if (endsWithHeader && i + 1 < parts.size) {
                result.add("$part ${parts[i + 1].trim()} ")
                i += 2
            } else {
                result.add("$part ")
                i++
            }
        }
        return result
    }

    /**
     * Builds overlap text from the tail of the current chunk.
     * Tries to break on a sentence boundary within the overlap window.
     */
    private fun buildOverlap(text: String): String {
        if (text.length <= OVERLAP_SIZE) return text

        val tail = text.takeLast(OVERLAP_SIZE)
        // Try to start overlap at a sentence boundary
        val sentenceStart = tail.indexOfFirst { it == '.' || it == '\n' }
        return if (sentenceStart >= 0 && sentenceStart < tail.length - 1) {
            tail.substring(sentenceStart + 1).trimStart()
        } else {
            tail.trimStart()
        }
    }

    /**
     * Splits a very long sentence into smaller chunks at word boundaries.
     */
    private fun splitLongSentence(sentence: String): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0

        while (start < sentence.length) {
            var end = minOf(start + TARGET_CHUNK_SIZE, sentence.length)

            // Try to break at a word boundary
            if (end < sentence.length) {
                val lastSpace = sentence.lastIndexOf(' ', end)
                if (lastSpace > start) {
                    end = lastSpace + 1
                }
            }

            chunks.add(sentence.substring(start, end).trim())

            // Apply overlap
            start = if (end - OVERLAP_SIZE > start) end - OVERLAP_SIZE else end
        }

        return chunks.filter { it.isNotEmpty() }
    }
}
