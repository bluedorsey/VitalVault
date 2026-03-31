package com.example.personalhealthcareapp.uiux

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalhealthcareapp.ViewModel.ChatViewModel
import com.example.personalhealthcareapp.chat_managment.Chat_message
import com.example.personalhealthcareapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onNavigateToUpload: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val chatHistory by viewModel.chathistory.collectAsState()
    val modelState by viewModel.modelState.collectAsState()

    Scaffold(
        containerColor = BackgroundClay,
        topBar = { TopHeader() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            AnimatedContent(
                targetState = chatHistory.isEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(tween(600)) + slideInVertically(tween(600), initialOffsetY = { -it / 8 })) togetherWith
                                (fadeOut(tween(400)) + slideOutVertically(tween(400), targetOffsetY = { -it / 8 }))
                    } else {
                        (fadeIn(tween(600)) + slideInVertically(tween(600), initialOffsetY = { it / 8 })) togetherWith
                                fadeOut()
                    }
                },
                label = "ChatContentTransition"
            ) { isEmpty ->
                if (isEmpty) EmptyHeroState()
                else ChatList(chatHistory)
            }

            // Chat Input bottom bar
            ClayChatInput(
                enabled = modelState == ChatViewModel.ModelState.READY,
                onSend = { text -> viewModel.sendMessage(text) }
            )
        }
    }
}

@Composable
private fun TopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = "Logo", tint = TealDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("VITALVAULT", fontWeight = FontWeight.Bold, color = TealDark, fontSize = 20.sp, letterSpacing = 2.sp)
            }
            Text("AI READY", color = TextLight, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(start = 32.dp))
        }
        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = TextDark, modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun EmptyHeroState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Central Huge Clay Circle
        Box(
            modifier = Modifier
                .size(260.dp)
                .clayCircle(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Heart",
                    modifier = Modifier.size(84.dp),
                    tint = TealDark
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .claySurface(shape = RoundedCornerShape(16.dp), backgroundColor = TealVeryLight, elevation = 4.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SYSTEM SYNCHRONIZED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealDark)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text("Your health, sculpted by intelligence.", fontSize = 18.sp, color = TextDark, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "VitalVault organizes your records into a cohesive bio-narrative, securing your data in our tactile-encryption sanctuary.",
            fontSize = 14.sp, color = TextLight, textAlign = TextAlign.Center, lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(modifier = Modifier.weight(1f), title = "TOTAL RECORDS", value = "142", icon = Icons.Default.Folder)
            StatCard(modifier = Modifier.weight(1f), title = "HEALTH SCORE", value = "94%", icon = Icons.Default.ShowChart)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = modifier
            .claySurface()
            .padding(20.dp)
    ) {
        Column {
            Icon(icon, contentDescription = title, tint = TealDark, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.5.sp)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }
    }
}

@Composable
private fun ChatList(history: List<Chat_message>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        items(history) { msg ->
            val isUser = msg.isUser
            val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
            val bgColor = if (isUser) TealDark else WhiteCore
            val textColor = if (isUser) WhiteCore else TextDark

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .claySurface(
                            backgroundColor = bgColor,
                            shape = RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = if (isUser) 24.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 24.dp
                            ),
                            elevation = 8.dp
                        )
                        .padding(20.dp)
                ) {
                    Text(msg.text, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun ClayChatInput(enabled: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .claySurface(shape = RoundedCornerShape(32.dp), elevation = 16.dp)
                .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if(enabled) "Ask about your health rec..." else "Initializing AI...", color = TextLight, fontSize = 15.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3
                )
                
                // Send button
                IconButton(
                    onClick = { if(text.isNotBlank()) { onSend(text.trim()); text = "" } },
                    enabled = enabled && text.isNotBlank(),
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (text.isNotBlank()) TealDark else TextLight, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = WhiteCore, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
