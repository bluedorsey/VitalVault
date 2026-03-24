package com.example.personalhealthcareapp.db

import android.content.Context
import android.util.Log
import io.objectbox.BoxStore

private const val TAG = "VitalVault_DB"

object ObjectBox {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        if (::store.isInitialized && !store.isClosed) return
        store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
        Log.d(TAG, "ObjectBox initialized")
    }

    /** Returns total number of stored chunks. */
    fun chunkCount(): Long = store.boxFor(MedicalChunck::class.java).count()

    /** Returns all saved documents. */
    fun getAllDocuments(): List<Medicaldata> {
        return store.boxFor(Medicaldata::class.java).all
    }

    /** Returns the number of chunks linked to a specific document. */
    fun getChunkCountForDocument(documentId: Long): Long {
        val chunkBox = store.boxFor(MedicalChunck::class.java)
        return chunkBox.query()
            .equal(MedicalChunck_.documentId, documentId)
            .build()
            .count()
    }

    /**
     * Deletes a document and ALL its associated chunks.
     * This is a secure delete — removes both the parent record and all vector data.
     */
    fun deleteDocument(documentId: Long) {
        val chunkBox = store.boxFor(MedicalChunck::class.java)
        val docBox = store.boxFor(Medicaldata::class.java)

        // Delete all chunks linked to this document
        val chunksToDelete = chunkBox.query()
            .equal(MedicalChunck_.documentId, documentId)
            .build()
            .find()

        Log.d(TAG, "Deleting document $documentId with ${chunksToDelete.size} chunks")
        chunkBox.remove(chunksToDelete)
        docBox.remove(documentId)
        Log.d(TAG, "Document $documentId deleted. Remaining chunks: ${chunkBox.count()}")
    }

    fun searchSimilarChunks(questionVector: FloatArray, maxResults: Int = 5): List<MedicalChunck> {
        val chunkBox   = store.boxFor(MedicalChunck::class.java)
        val totalCount = chunkBox.count()

        Log.d(TAG, "searchSimilarChunks called — DB has $totalCount chunks, vector size: ${questionVector.size}")

        if (totalCount == 0L) {
            Log.w(TAG, "ChunkBox is empty — nothing to search")
            return emptyList()
        }

        return try {
            val query = chunkBox.query()
                .nearestNeighbors(MedicalChunck_.textEmbedding, questionVector, maxResults)
                .build()
            val results = query.find()
            Log.d(TAG, "nearestNeighbors returned ${results.size} results")
            results
        } catch (e: Exception) {
            Log.e(TAG, "nearestNeighbors FAILED: ${e.message}")
            Log.e(TAG, "FIX: Make sure @HnswIndex(dimensions = ${questionVector.size}) in vectordb.kt, then uninstall the app to wipe the old DB")
            emptyList()
        }
    }
}