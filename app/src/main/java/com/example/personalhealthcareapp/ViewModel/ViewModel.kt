package com.example.personalhealthcareapp.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalhealthcareapp.LLMinference.LLMInferenceManager
import com.example.personalhealthcareapp.chat_managment.Chat_message
import com.example.personalhealthcareapp.db.Embedding
import com.example.personalhealthcareapp.db.ObjectBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    /** Tracks the model loading lifecycle */
    enum class ModelState { LOADING, READY, ERROR }

    private val _modelState = MutableStateFlow(ModelState.LOADING)
    val modelState = _modelState.asStateFlow()

    private val _responseStatus = MutableStateFlow("AI is loading")
    val responseStatus = _responseStatus.asStateFlow()

    private val _chathistory = MutableStateFlow<List<Chat_message>>(emptyList())
    val chathistory = _chathistory.asStateFlow()

    init {
        // Load model on background thread to avoid ANR
        viewModelScope.launch(Dispatchers.IO) {
            try {
                LLMInferenceManager.initModel(application)
                _modelState.value = ModelState.READY
                _responseStatus.value = "AI is ready"

                // Collect streaming partial results from LLM
                LLMInferenceManager.partialresult.collect { word ->
                    appendMessage(word)
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Model loading failed: ${e.message}")
                _modelState.value = ModelState.ERROR
                _responseStatus.value = "AI failed to load"
            }
        }
    }

    /**
     * Sends a user message through the RAG pipeline:
     * 1. Add user message to chat
     * 2. Embed the question
     * 3. Search ObjectBox for relevant chunks
     * 4. Build context-aware prompt
     * 5. Send to Gemma for generation
     */
    fun sendMessage(message: String) {
        if (_modelState.value != ModelState.READY) return

        _responseStatus.value = "Thinking…"
        val currentList = _chathistory.value.toMutableList()
        currentList.add(Chat_message(message, isUser = true))
        currentList.add(Chat_message("Searching your records…", isUser = false))
        _chathistory.value = currentList

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Embed the question
                val questionVector = Embedding.generateVector(message)

                // Step 2: Search for relevant chunks (RAG retrieval)
                val contextChunks = if (questionVector != null) {
                    ObjectBox.searchSimilarChunks(questionVector, maxResults = 5)
                } else {
                    emptyList()
                }

                // Step 3: Build context string from top 3 results
                val context = if (contextChunks.isNotEmpty()) {
                    val topChunks = contextChunks.take(3)
                    topChunks.mapIndexed { index, chunk ->
                        "[Record ${index + 1}]: ${chunk.chunkedtext}"
                    }.joinToString("\n\n")
                } else {
                    ""
                }

                // Step 4: Build the RAG-grounded prompt
                val fullPrompt = if (context.isNotEmpty()) {
                    """Based on the following medical records, answer the question.
                        |If the records don't contain relevant information, say so.
                        |
                        |MEDICAL RECORDS:
                        |$context
                        |
                        |QUESTION: $message""".trimMargin()
                } else {
                    """No medical records found in the database for this query.
                        |Please answer the following general health question, and mention
                        |that no personal records were found.
                        |
                        |QUESTION: $message""".trimMargin()
                }

                // Step 5: Send to LLM
                LLMInferenceManager.generateResponse(fullPrompt)

            } catch (e: Exception) {
                Log.e("ChatVM", "RAG pipeline error: ${e.message}")
                val errorList = _chathistory.value.toMutableList()
                if (errorList.isNotEmpty() && !errorList.last().isUser) {
                    errorList[errorList.size - 1] =
                        Chat_message("Sorry, something went wrong. Please try again.", isUser = false)
                    _chathistory.value = errorList
                }
            }
        }
    }

    /**
     * Appends streaming LLM output to the last AI message bubble.
     */
    private fun appendMessage(response: String) {
        val currentList = _chathistory.value.toMutableList()
        if (currentList.isEmpty()) return

        val lastMessage = currentList.last()
        if (!lastMessage.isUser) {
            val newText = if (lastMessage.text == "Searching your records…" || lastMessage.text == "thinking") {
                response
            } else {
                lastMessage.text + response
            }
            currentList[currentList.size - 1] = lastMessage.copy(text = newText)
            _chathistory.value = currentList
        }
    }
}