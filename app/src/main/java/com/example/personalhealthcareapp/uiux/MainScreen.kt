package com.example.personalhealthcareapp.uiux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalhealthcareapp.ViewModel.ChatViewModel
import com.example.personalhealthcareapp.chat_managment.Chat_message
import kotlinx.coroutines.launch

/**
 * Main chat screen for VitalVault.
 * Displays conversation history, status bar, and message input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToUpload: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val chatHistory by viewModel.chathistory.collectAsState()
    val responseStatus by viewModel.responseStatus.collectAsState()
    val modelState by viewModel.modelState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VitalVault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = when (modelState) {
                                ChatViewModel.ModelState.LOADING -> "Loading AI model…"
                                ChatViewModel.ModelState.READY -> "AI ready"
                                ChatViewModel.ModelState.ERROR -> "AI failed to load"
                            },
                            fontSize = 12.sp,
                            color = when (modelState) {
                                ChatViewModel.ModelState.LOADING -> MaterialTheme.colorScheme.tertiary
                                ChatViewModel.ModelState.READY -> MaterialTheme.colorScheme.primary
                                ChatViewModel.ModelState.ERROR -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToUpload) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload Document"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat message list
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            // Auto-scroll when new messages arrive
            LaunchedEffect(chatHistory.size) {
                if (chatHistory.isNotEmpty()) {
                    listState.animateScrollToItem(chatHistory.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(chatHistory) { message ->
                    ChatBubble(message)
                }
            }

            // Input area
            MessageInput(
                enabled = modelState == ChatViewModel.ModelState.READY,
                onSend = { text ->
                    viewModel.sendMessage(text)
                    coroutineScope.launch {
                        if (chatHistory.isNotEmpty()) {
                            listState.animateScrollToItem(chatHistory.size)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatBubble(chat: Chat_message) {
    val isUser = chat.isUser
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isUser) "You" else "VitalVault AI",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = chat.text,
                fontSize = 15.sp,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

@Composable
private fun MessageInput(
    enabled: Boolean,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (enabled) "Ask about your health records…"
                        else "Waiting for AI to load…"
                    )
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}
