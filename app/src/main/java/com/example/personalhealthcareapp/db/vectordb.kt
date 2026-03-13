package com.example.personalhealthcareapp.db

import com.google.protobuf.Timestamp
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
// patient doc
@Entity
data class Medicaldata(
    @Id var id: Long=0,
    val tittle:String="",//tittle like blood test , date
    val imagepath: String="",
    val timestamp : Long= System.currentTimeMillis()
)

@Entity
data class MedicalChunck(
    @Id var id: Long = 0,
    val chunkedtext: String="",

    //vector Engine
    @HnswIndex(dimensions = 100)
    var textEmbedding: FloatArray? = null
)

