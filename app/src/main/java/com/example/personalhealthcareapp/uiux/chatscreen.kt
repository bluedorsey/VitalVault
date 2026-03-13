package com.example.personalhealthcareapp.uiux // Adjust to match your package

import android.util.Log // 1. IMPORT LOG
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalhealthcareapp.db.Embedding
import com.example.personalhealthcareapp.db.ObjectBox.searchSimilarChunks
import kotlinx.coroutines.launch
import kotlin.collections.forEachIndexed

data class ChatMessage(val text: String, val isUser: Boolean)

// 2. DEFINE A CONSTANT TAG FOR FILTERING IN LOGCAT
private const val TAG = "HealthApp_ChatPipeline"

@Composable
fun ChatScreen() {
    var questionText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // 1. THE CHAT HISTORY (The scrollable list of messages)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. THE INPUT BAR (Where you type your question)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your health records...") },
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (questionText.isBlank()) {
                    Log.w(TAG, "User clicked Ask with empty input.") // Warning Log
                    return@Button
                }

                val userQuestion = questionText
                questionText = "" // Clear the text box instantly

                Log.d(TAG, "--- NEW SEARCH PIPELINE STARTED ---")
                Log.d(TAG, "User Input: '$userQuestion'") // Debug Log

                // Add the user's question to the screen
                messages = messages + ChatMessage(userQuestion, true)

                // --- TRIGGER THE AI SEARCH PIPELINE ---
                coroutineScope.launch {
                    try {
                        Log.d(TAG, "Step 1: Generating embedding vector for input...")

                        // 1. Convert the user's question into math
                        val questionVector = Embedding.generateVector(userQuestion)

                        if (questionVector != null) {
                            Log.d(TAG, "Step 1 SUCCESS: Vector generated. Length: ${questionVector.size}")
                            Log.d(TAG, "Step 2: Searching ObjectBox for similar chunks...")

                            // 2. Search the database for the top 3 matching medical chunks
                            val topChunks = searchSimilarChunks(questionVector, maxResults = 3)

                            Log.d(TAG, "Step 2 SUCCESS: Found ${topChunks.size} matching chunks.")

                            // 3. Process the results
                            var botResponse = "🔍 **Database Search Results:**\n\n"

                            if (topChunks.isEmpty()) {
                                Log.i(TAG, "Result: No chunks found in the database.") // Info Log
                                botResponse = "I couldn't find any medical records in your vault matching that question."
                            } else {
                                topChunks.forEachIndexed { index, chunk ->
                                    val chunkPreview = chunk.chunkedtext?.take(50) ?: "null"
                                    Log.v(TAG, "Match ${index + 1} | Score: [If Available] | Text Preview: $chunkPreview...") // Verbose Log

                                    botResponse += "${index + 1}. ${chunk.chunkedtext}\n\n"
                                }
                            }

                            // Add the database results to the screen
                            messages = messages + ChatMessage(botResponse, false)

                        } else {
                            // The vector was null
                            Log.e(TAG, "Step 1 FAILED: questionVector returned null.") // Error Log
                            messages = messages + ChatMessage("Error: Could not convert question to AI vector.", false)
                        }
                    } catch (e: Exception) {
                        // Catching any crashes in the coroutine (like ObjectBox database being closed)
                        Log.e(TAG, "CRITICAL ERROR in Search Pipeline", e)
                        messages = messages + ChatMessage("An unexpected error occurred. Check logs.", false)
                    }
                }
            }) {
                Text("Ask")
            }
        }
    }
}

// A helper UI component to make the chat look like a real messaging app
@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            color = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}