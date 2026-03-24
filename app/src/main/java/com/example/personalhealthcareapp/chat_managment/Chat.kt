package com.example.personalhealthcareapp.chat_managment

/**
 * Represents a single message in the chat conversation.
 * @param text The message content
 * @param isUser true if the message was sent by the user, false if from AI
 */
data class Chat_message(
    val text: String,
    val isUser: Boolean
)
