package com.example.personalhealthcareapp.db

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

/** Parent document — one per scanned medical report. */
@Entity
data class Medicaldata(
    @Id var id: Long = 0,
    val title: String = "",          // e.g. "Blood Test", "X-Ray Report"
    val imagepath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/** A single text chunk embedded as a vector. Links back to its parent document. */
@Entity
data class MedicalChunck(
    @Id var id: Long = 0,
    val chunkedtext: String = "",
    val documentId: Long = 0,        // FK → Medicaldata.id

    // USE TFLite outputs 100-dimensional embeddings
    @HnswIndex(dimensions = 100)
    var textEmbedding: FloatArray? = null
)

