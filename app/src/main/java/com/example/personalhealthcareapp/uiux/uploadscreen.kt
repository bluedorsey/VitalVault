package com.example.personalhealthcareapp.uiux

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.personalhealthcareapp.ai.TextChunker
import com.example.personalhealthcareapp.db.Embedding
import com.example.personalhealthcareapp.db.MedicalChunck
import com.example.personalhealthcareapp.db.Medicaldata
import com.example.personalhealthcareapp.db.ObjectBox
import com.example.personalhealthcareapp.ui.theme.*
import com.example.personalhealthcareapp.vision.OCR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UploadScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var documentTitle by remember { mutableStateOf("") }

    var savedDocuments by remember { mutableStateOf(ObjectBox.getAllDocuments()) }
    var documentToDelete by remember { mutableStateOf<Medicaldata?>(null) }
    var viewingDocument by remember { mutableStateOf<Medicaldata?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) statusMessage = "Image selected. Ready to process."
    }

    // ── Full-screen image viewer ──────────────────────────────────────────────
    viewingDocument?.let { doc ->
        Dialog(
            onDismissRequest = { viewingDocument = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                // Dismiss tap on background
                Surface(
                    onClick = { viewingDocument = null },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                ) {}

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewingDocument = null }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            doc.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .claySurface(shape = RoundedCornerShape(24.dp), backgroundColor = Color(0xFF1A1A1A))
                            .clip(RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel = remember(doc.imagepath) {
                            if (doc.imagepath.startsWith("/")) File(doc.imagepath)
                            else Uri.parse(doc.imagepath)
                        }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = doc.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val dateStr = remember(doc.timestamp) {
                        SimpleDateFormat("MMMM dd, yyyy  HH:mm", Locale.getDefault())
                            .format(Date(doc.timestamp))
                    }
                    Text(dateStr, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    documentToDelete?.let { doc ->
        AlertDialog(
            containerColor = BackgroundClay,
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document?", color = TealDark, fontWeight = FontWeight.Bold) },
            text = { Text("\"${doc.title}\" and all its indexed data will be permanently deleted.", color = TextDark) },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            // Delete internal image copy if it exists
                            if (doc.imagepath.startsWith("/")) {
                                File(doc.imagepath).delete()
                            }
                            ObjectBox.deleteDocument(doc.id)
                        }
                        savedDocuments = ObjectBox.getAllDocuments()
                        documentToDelete = null
                    }
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel", color = TextLight)
                }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundClay,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.clayCircle(backgroundColor = WhiteCore, elevation = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TealDark)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Medical Vault", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TealDark)
                    Text("${savedDocuments.size} document${if (savedDocuments.size != 1) "s" else ""} indexed",
                        fontSize = 12.sp, color = TextLight)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Saved documents ───────────────────────────────────────────
            if (savedDocuments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .claySurface(shape = RoundedCornerShape(24.dp))
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = TextLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No documents yet", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Upload your first medical report below", color = TextLight, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "MY DOCUMENTS",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = TextDark, letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .background(TealDark, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${savedDocuments.size}",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhiteCore
                            )
                        }
                    }
                }

                items(savedDocuments, key = { it.id }) { doc ->
                    DocumentCard(
                        doc = doc,
                        onClick = { viewingDocument = doc },
                        onDelete = { documentToDelete = doc }
                    )
                }
            }

            // ── Divider ───────────────────────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = TextLight.copy(alpha = 0.3f))
                    Text(
                        "  UPLOAD NEW DOCUMENT  ",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = TextDark, letterSpacing = 1.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = TextLight.copy(alpha = 0.3f))
                }
            }

            // ── Title input ───────────────────────────────────────────────
            item {
                Column {
                    Text(
                        "DOCUMENT TITLE",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = TextDark, letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .claySurface(shape = RoundedCornerShape(24.dp), elevation = 8.dp)
                            .padding(4.dp)
                    ) {
                        TextField(
                            value = documentTitle,
                            onValueChange = { documentTitle = it },
                            placeholder = { Text("e.g. Cardiology Report Jan 2025", color = TextLight, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // ── Image picker ──────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ButtonActionItem(
                        icon = Icons.Default.Image,
                        text = if (selectedImageUri == null) "Pick Image from Gallery" else "Image Selected — Tap to Change",
                        tint = if (selectedImageUri == null) TextLight else TealDark,
                        onClick = { imagePickerLauncher.launch("image/*") }
                    )
                    ButtonActionItem(
                        icon = Icons.Default.CameraAlt,
                        text = "Take Photo",
                        tint = TextLight,
                        onClick = { statusMessage = "Camera: use Pick Image for now." }
                    )
                }
            }

            // ── Scan & Index button ───────────────────────────────────────
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val uri = selectedImageUri ?: return@Button
                            if (documentTitle.isBlank()) {
                                statusMessage = "Please enter a document title first."
                                return@Button
                            }
                            isProcessing = true
                            statusMessage = "Scanning document…"

                            OCR.extraxtText(
                                context, uri,
                                onSuccess = { text ->
                                    statusMessage = "Chunking and embedding…"
                                    coroutineScope.launch {
                                        try {
                                            // Copy image to internal storage for persistent access
                                            val internalPath = withContext(Dispatchers.IO) {
                                                try {
                                                    val fileName = "doc_${System.currentTimeMillis()}.jpg"
                                                    val dest = File(context.filesDir, fileName)
                                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                                        dest.outputStream().use { output -> input.copyTo(output) }
                                                    }
                                                    dest.absolutePath
                                                } catch (e: Exception) {
                                                    uri.toString() // fallback
                                                }
                                            }

                                            val doc = Medicaldata(
                                                title = documentTitle,
                                                imagepath = internalPath
                                            )
                                            val docId = withContext(Dispatchers.IO) {
                                                ObjectBox.store.boxFor(Medicaldata::class.java).put(doc)
                                            }

                                            val chunks = TextChunker.chunkText(text)
                                            chunks.forEachIndexed { i, chunkText ->
                                                statusMessage = "Indexing chunk ${i + 1} of ${chunks.size}…"
                                                // Prefix every chunk with its document title so the
                                                // embedding and stored text are both document-aware.
                                                val taggedChunk = "[$documentTitle]: $chunkText"
                                                val vector = Embedding.generateVector(taggedChunk)
                                                if (vector != null) {
                                                    val chunk = MedicalChunck(
                                                        chunkedtext = taggedChunk,
                                                        documentId = docId,
                                                        textEmbedding = vector
                                                    )
                                                    withContext(Dispatchers.IO) {
                                                        ObjectBox.store.boxFor(MedicalChunck::class.java).put(chunk)
                                                    }
                                                }
                                            }

                                            statusMessage = "Document securely indexed in your vault!"
                                            savedDocuments = ObjectBox.getAllDocuments()
                                            documentTitle = ""
                                            selectedImageUri = null
                                        } catch (e: Exception) {
                                            statusMessage = "Error: ${e.message}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                onFailure = { e ->
                                    statusMessage = "OCR failed: ${e.message}"
                                    isProcessing = false
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        enabled = selectedImageUri != null && !isProcessing && documentTitle.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealDark,
                            disabledContainerColor = TextLight
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        if (isProcessing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = WhiteCore,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(statusMessage.ifEmpty { "Processing…" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Scan & Index Document", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    if (!isProcessing && statusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            statusMessage,
                            fontSize = 12.sp,
                            color = if (statusMessage.startsWith("Error") || statusMessage.startsWith("OCR")) Color.Red.copy(alpha = 0.8f)
                                    else if (statusMessage.contains("indexed")) TealDark
                                    else TextLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isProcessing && statusMessage.isEmpty()) {
                        Text(
                            "Documents are processed locally. No data leaves your device.",
                            fontSize = 11.sp, color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    doc: Medicaldata,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val chunkCount = remember(doc.id) { ObjectBox.getChunkCountForDocument(doc.id) }
    val dateStr = remember(doc.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(doc.timestamp))
    }
    val imageModel = remember(doc.imagepath) {
        if (doc.imagepath.startsWith("/")) File(doc.imagepath)
        else Uri.parse(doc.imagepath)
    }

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .claySurface(shape = RoundedCornerShape(24.dp), elevation = 8.dp)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .claySurface(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = TealVeryLight,
                            elevation = 4.dp
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = TealLight,
                        modifier = Modifier.size(32.dp)
                    )
                    AsyncImage(
                        model = imageModel,
                        contentDescription = doc.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        doc.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dateStr, fontSize = 12.sp, color = TextLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("INDEXED")
                        Pill("$chunkCount CHUNKS")
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .background(TealVeryLight, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealDark, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ButtonActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = TextLight,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .claySurface(shape = RoundedCornerShape(20.dp), elevation = 6.dp)
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BackgroundClay, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextDark)
            }
        }
    }
}
