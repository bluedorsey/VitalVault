package com.example.personalhealthcareapp.uiux

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.personalhealthcareapp.ai.TextChunker
import com.example.personalhealthcareapp.db.Embedding
import com.example.personalhealthcareapp.db.MedicalChunck
import com.example.personalhealthcareapp.db.Medicaldata
import com.example.personalhealthcareapp.db.ObjectBox
import com.example.personalhealthcareapp.vision.OCR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Upload screen for scanning and indexing medical documents.
 * Flow: Pick image → OCR → Chunk → Embed → Store in ObjectBox
 * Also shows saved documents with delete capability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Select a medical document to scan") }
    var isProcessing by remember { mutableStateOf(false) }
    var documentTitle by remember { mutableStateOf("") }
    var processingLog by remember { mutableStateOf<List<String>>(emptyList()) }

    // Saved documents state
    var savedDocuments by remember { mutableStateOf(ObjectBox.getAllDocuments()) }
    var documentToDelete by remember { mutableStateOf<Medicaldata?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        ocrText = ""
        processingLog = emptyList()
        if (uri != null) {
            statusMessage = "Image selected. Tap 'Scan & Index' to process."
        }
    }

    // Delete confirmation dialog
    if (documentToDelete != null) {
        val doc = documentToDelete!!
        val chunkCount = remember(doc.id) {
            ObjectBox.getChunkCountForDocument(doc.id)
        }

        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document?") },
            text = {
                Text("\"${doc.title}\" and its $chunkCount chunks will be permanently deleted. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                ObjectBox.deleteDocument(doc.id)
                            }
                            savedDocuments = ObjectBox.getAllDocuments()
                            documentToDelete = null
                            statusMessage = "\"${doc.title}\" deleted."
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Document") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Saved Documents Section ──
            if (savedDocuments.isNotEmpty()) {
                item {
                    Text(
                        "Saved Documents (${savedDocuments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                items(savedDocuments, key = { it.id }) { doc ->
                    SavedDocumentCard(
                        document = doc,
                        onDelete = { documentToDelete = doc }
                    )
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            // ── Upload New Document Section ──
            item {
                Text(
                    "Upload New Document",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Title input
            item {
                OutlinedTextField(
                    value = documentTitle,
                    onValueChange = { documentTitle = it },
                    label = { Text("Document Title") },
                    placeholder = { Text("e.g. Blood Test Report, X-Ray") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Image preview
            item {
                if (selectedImageUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected document",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Image")
                    }

                    Button(
                        onClick = {
                            val uri = selectedImageUri ?: return@Button
                            isProcessing = true
                            processingLog = listOf("Starting OCR…")
                            statusMessage = "Processing…"

                            OCR.extraxtText(
                                context = context,
                                imageUri = uri,
                                onSuccess = { text ->
                                    ocrText = text
                                    processingLog = processingLog + "OCR complete: ${text.length} chars"

                                    // Chunk → Embed → Store
                                    coroutineScope.launch {
                                        try {
                                            val doc = Medicaldata(
                                                title = documentTitle.ifBlank { "Untitled Document" },
                                                imagepath = uri.toString()
                                            )
                                            val docId = withContext(Dispatchers.IO) {
                                                ObjectBox.store.boxFor(Medicaldata::class.java).put(doc)
                                            }
                                            processingLog = processingLog + "Document saved (id=$docId)"

                                            val chunks = TextChunker.chunkText(text)
                                            processingLog = processingLog + "Created ${chunks.size} chunks"

                                            chunks.forEachIndexed { index, chunkText ->
                                                val vector = Embedding.generateVector(chunkText)
                                                if (vector != null) {
                                                    val chunk = MedicalChunck(
                                                        chunkedtext = chunkText,
                                                        documentId = docId,
                                                        textEmbedding = vector
                                                    )
                                                    withContext(Dispatchers.IO) {
                                                        ObjectBox.store.boxFor(MedicalChunck::class.java).put(chunk)
                                                    }
                                                    processingLog = processingLog +
                                                            "Chunk ${index + 1}/${chunks.size} embedded (${vector.size}d)"
                                                } else {
                                                    processingLog = processingLog +
                                                            "⚠ Chunk ${index + 1} embedding failed"
                                                }
                                            }

                                            val totalChunks = ObjectBox.chunkCount()
                                            statusMessage = "Done! ${chunks.size} chunks indexed. Total in DB: $totalChunks"
                                            processingLog = processingLog + "✅ Complete!"

                                            // Refresh the document list
                                            savedDocuments = ObjectBox.getAllDocuments()
                                            documentTitle = ""
                                            selectedImageUri = null
                                        } catch (e: Exception) {
                                            Log.e("Upload", "Pipeline error: ${e.message}")
                                            statusMessage = "Error: ${e.message}"
                                            processingLog = processingLog + "❌ Error: ${e.message}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                onFailure = { e ->
                                    statusMessage = "OCR failed: ${e.message}"
                                    processingLog = processingLog + "❌ OCR failed: ${e.message}"
                                    isProcessing = false
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedImageUri != null && !isProcessing
                    ) {
                        Text("Scan & Index")
                    }
                }
            }

            // Status
            item {
                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Processing log
            if (processingLog.isNotEmpty()) {
                item {
                    Text(
                        "Processing Log",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                items(processingLog) { logLine ->
                    Text(
                        text = logLine,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // OCR preview
            if (ocrText.isNotEmpty()) {
                item {
                    Text(
                        "Extracted Text Preview",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Text(
                            text = ocrText.take(500) + if (ocrText.length > 500) "…" else "",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Card displaying a saved document with its title, date, chunk count, and a delete button. */
@Composable
private fun SavedDocumentCard(
    document: Medicaldata,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val chunkCount = remember(document.id) { ObjectBox.getChunkCountForDocument(document.id) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$chunkCount chunks · ${dateFormatter.format(Date(document.timestamp))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete document",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

