package com.example.personalhealthcareapp.db

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object Embedding{
   private var textEmbedder: TextEmbedder? = null
    const val MODLE_NAME ="universal_sentence_encoder.tflite"

   fun initEmbedder(context: Context) {
      Log.d("Embedder", "STEP 1: initEmbedder function was triggered!")

      if (textEmbedder != null) {
         Log.d("Embedder", "STEP 2: Embedder is already running.")
         return
      }

      try {
         Log.d("Embedder", "STEP 3: Attempting to load the .tflite file...")

         val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODLE_NAME)
            .build()

         val options = TextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .build()

         textEmbedder = TextEmbedder.createFromOptions(context, options)
         Log.d("Embedder", "STEP 4: SUCCESS! Engine is on.")

      } catch (e: Exception) {
         // By passing 'e' at the end, Android Studio will print the exact reason it crashed!
         Log.e("Embedder", "CRASH: Failed to turn on the engine!", e)
      }
   }
   //generate the vectors for the text
   // We add the 'suspend' keyword so it runs asynchronously
   suspend fun generateVector(text: String): FloatArray? = withContext(Dispatchers.Default) {
      if (textEmbedder == null) {
         Log.e("Embedder", "Cannot generate vector. Embedder is null!")
         return@withContext null
      }

      Log.d("Embedder", "Generating vector on thread: ${Thread.currentThread().name}")

      //  Ask MediaPipe to embed the text
      val result = textEmbedder?.embed(text)

      // 2. Dig through the MediaPipe result object to pull out the raw FloatArray
      return@withContext result?.embeddingResult()?.embeddings()?.firstOrNull()?.floatEmbedding()
   }
}