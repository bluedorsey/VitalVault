package com.example.personalhealthcareapp.db
import android.content.Context
import io.objectbox.BoxStore
import kotlin.jvm.java

object ObjectBox {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        if (::store.isInitialized && !store.isClosed) return

        store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }
    fun searchSimilarChunks(questionVector: FloatArray, maxResults: Int = 3): List<MedicalChunck> {
        val chunkBox = store.boxFor(MedicalChunck::class.java)

        val query = chunkBox.query()
            .nearestNeighbors(MedicalChunck_.textEmbedding, questionVector, maxResults)
            .build()

        return query.find()
    }
}