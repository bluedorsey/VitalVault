package com.example.personalhealthcareapp.LLMinference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
//object created to run llm in background
object LLMInferenceManager{
    private var llmInference: LlmInference? = null//check that if modle is running or not cause loading heavy modle every time makes tha app very heavy
    private const val MODEL_NAME="gemma-1.1-2b-it-cpu-int4.bin"
// creating the pipeline that connect modle to ui
    private val _partialresult = MutableSharedFlow<String>(//privately store the input and pass it into modle
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val partialresult: SharedFlow<String> = _partialresult.asSharedFlow()// store the result

    fun initModel(context: Context) {
        if (llmInference != null) return

        Log.d("AiBot", "1. Starting to load model...")

        try {
            val modelFile = File(context.cacheDir, MODEL_NAME)
            if (!modelFile.exists()) {
                Log.d("AiBot", "2. Copying file from Assets to Cache...")
                context.assets.open(MODEL_NAME).use { inputStream ->
                    modelFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            Log.d("AiBot", "3. File ready. Configuring engine...")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setResultListener { partialResult, done ->
                    Log.d("AiBot", "AI said: $partialResult") // Watch the AI talk in logs
                    _partialresult.tryEmit(partialResult)
                }
                .build()

            Log.d("AiBot", "4. Loading into RAM (This is the heavy part)...")
            llmInference = LlmInference.createFromOptions(context, options)
            Log.d("AiBot", "5. SUCCESS! Brain is alive.")

        } catch (e: Exception) {
            // If it fails, this will print the EXACT reason in red text
            Log.e("AiBot", "CRITICAL ERROR LOADING AI: ${e.message}")
        }
    }

    fun generateResponse(prompt: String) {
        if (llmInference == null) {
            Log.e("AiBot", "Cannot generate response. The Brain is null!")
            return
        }

        Log.d("AiBot", "6. Sending message to AI: $prompt")
        val formattedPrompt = "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"

        try {
            llmInference?.generateResponseAsync(formattedPrompt)
        } catch (e: Exception) {
            Log.e("AiBot", "Error during generation: ${e.message}")
        }
    }}