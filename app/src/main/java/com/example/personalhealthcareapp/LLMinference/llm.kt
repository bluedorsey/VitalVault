package com.example.personalhealthcareapp.LLMinference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/**
 * Singleton that manages Gemma model loading and inference.
 * All medical content is stripped from logs in release builds.
 */
object LLMInferenceManager {
    private var llmInference: LlmInference? = null
    private const val MODEL_NAME = "gemma-1.1-2b-it-cpu-int4.bin"

    private val _partialresult = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val partialresult: SharedFlow<String> = _partialresult.asSharedFlow()

    fun initModel(context: Context) {
        if (llmInference != null) return

        Log.d("AiBot", "Starting model load…")

        try {
            val modelFile = File(context.cacheDir, MODEL_NAME)
            if (!modelFile.exists()) {
                Log.d("AiBot", "Copying model from assets to cache…")
                context.assets.open(MODEL_NAME).use { inputStream ->
                    modelFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            Log.d("AiBot", "Configuring LLM engine…")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setResultListener { partialResult, done ->
                    // SECURITY: Do NOT log partialResult — it may contain medical content
                    Log.d("AiBot", "Partial result received (done=$done)")
                    _partialresult.tryEmit(partialResult)
                }
                .build()

            Log.d("AiBot", "Loading model into RAM…")
            llmInference = LlmInference.createFromOptions(context, options)
            Log.d("AiBot", "Model loaded successfully.")

        } catch (e: Exception) {
            Log.e("AiBot", "CRITICAL: Model load failed: ${e.message}")
            throw e  // Re-throw so ViewModel can set ERROR state
        }
    }

    fun generateResponse(prompt: String) {
        if (llmInference == null) {
            Log.e("AiBot", "Cannot generate: model not loaded")
            return
        }

        // SECURITY: Do NOT log the prompt — it contains medical records
        Log.d("AiBot", "Generating response…")
        val formattedPrompt = "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"

        try {
            llmInference?.generateResponseAsync(formattedPrompt)
        } catch (e: Exception) {
            Log.e("AiBot", "Generation error: ${e.message}")
        }
    }
}