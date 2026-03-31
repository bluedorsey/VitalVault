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

                // Step 2: Check if the query targets a specific document by title
                val allDocuments = ObjectBox.getAllDocuments()
                val mentionedDoc = allDocuments.firstOrNull { doc ->
                    doc.title.isNotBlank() && message.contains(doc.title, ignoreCase = true)
                }

                // Step 3: Retrieve context chunks via hybrid search
                //
                // Strategy:
                //  A) Specific doc mentioned → load ALL its chunks (no ranking loss)
                //  B) Cross-vault → vector similarity (top 5)
                //  C) Always ALSO run keyword search on meaningful words from the query
                //     so short/name-based queries ("olivia", "doctor") get direct text hits.
                //  Merge A/B with C, deduplicate by id.

                val vectorChunks: List<com.example.personalhealthcareapp.db.MedicalChunck> = when {
                    mentionedDoc != null -> {
                        val allChunks = ObjectBox.getAllChunksForDocument(mentionedDoc.id)
                        if (allChunks.size > 10 && questionVector != null) {
                            ObjectBox.searchSimilarChunksInDocument(
                                questionVector, mentionedDoc.id, maxResults = 10
                            )
                        } else {
                            allChunks
                        }
                    }
                    questionVector != null ->
                        ObjectBox.searchSimilarChunks(questionVector, maxResults = 5)
                    else -> emptyList()
                }

                // Extract meaningful keywords: words > 3 chars, skip common stop-words
                val stopWords = setOf(
                    "what", "does", "says", "said", "have", "from", "with", "that",
                    "this", "were", "they", "them", "their", "when", "where", "which",
                    "there", "about", "would", "could", "should", "will", "been",
                    "being", "more", "also", "into", "some", "then", "than", "only",
                    "just", "name", "tell", "give", "show", "find", "look", "know"
                )
                val keywords = message
                    .split(" ", ",", "?", "!", ".", "\n")
                    .map { it.trim().lowercase() }
                    .filter { it.length > 3 && it !in stopWords }

                val keywordChunks = ObjectBox.keywordSearchChunks(keywords)

                // Merge: vector results first (ranked), then any new keyword-only hits
                val seen = mutableSetOf<Long>()
                val contextChunks = buildList {
                    for (c in vectorChunks) { if (seen.add(c.id)) add(c) }
                    for (c in keywordChunks) { if (seen.add(c.id)) add(c) }
                }

                // Step 4: Build context grouped by document.
                // Using documentId (always available) to group, then look up title.
                // This gives the LLM clear document boundaries regardless of whether
                // chunks have the new [Title]: prefix or are from old uploads.
                val docById = allDocuments.associateBy { it.id }
                val chunksByDoc = contextChunks.groupBy { it.documentId }

                val context = chunksByDoc.entries.joinToString("\n\n") { (docId, chunks) ->
                    val docTitle = docById[docId]?.title ?: "Unknown Document"
                    val parts = chunks.mapIndexed { i, chunk ->
                        "  [Part ${i + 1}]: ${chunk.chunkedtext}"
                    }.joinToString("\n")
                    "=== REPORT: $docTitle ===\n$parts"
                }

                // Step 5: Build the RAG-grounded prompt
                val scopeNote = if (mentionedDoc != null)
                    "The records below are the complete contents of the \"${mentionedDoc.title}\" report."
                else
                    "The records below are retrieved from the patient's medical vault across multiple reports. " +
                    "Identify which report is most relevant to the question and mention it by name in your answer."

                val formatHint = """
                    |Answer with the relevant details below (only include what is present in the records):
                    |- Source Report: <name of the report this info comes from>
                    |- Doctor Name: <value or "Not mentioned">
                    |- Health Condition / Diagnosis: <value or "Not mentioned">
                    |- Report Date: <value or "Not mentioned">
                    |- Key Findings / Vitals: <value or "Not mentioned">
                    |- Medications / Next Steps: <value or "Not mentioned">
                """.trimMargin()

                val fullPrompt = if (context.isNotEmpty()) {
                    """You are a medical records assistant. Answer based solely on the records provided.
                        |$scopeNote
                        |$formatHint
                        |
                        |MEDICAL RECORDS:
                        |$context
                        |
                        |QUESTION: $message""".trimMargin()
                } else {
                    """No matching medical records were found for this query.
                        |Please answer the following general health question and mention
                        |that no personal records were found.
                        |
                        |QUESTION: $message""".trimMargin()
                }

                // Step 6: Send to LLM
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