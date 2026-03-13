package com.example.personalhealthcareapp.uiux

import com.example.personalhealthcareapp.db.Embedding
import com.example.personalhealthcareapp.vision.OCR
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.personalhealthcareapp.db.MedicalChunck
import com.example.personalhealthcareapp.db.Medicaldata
import com.example.personalhealthcareapp.db.ObjectBox
import kotlinx.coroutines.launch


@Composable
fun UploadScreen() {
    val context = LocalContext.current
    // We need a coroutine scope to call our suspend function for the Embedder
    val coroutineScope = rememberCoroutineScope()

    // UI State variables
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var statusText by remember { mutableStateOf("Ready to upload a medical report.") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                statusText = "Image selected. Ready to process."
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // BUTTON 1: Pick the Photo
        Button(onClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }) {
            Text("Select Medical Report")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Only show the Process button IF a photo is selected
        if (selectedImageUri != null) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                onClick = {
                    statusText = "1. Scanning image for text..."

                    // Trigger Pipeline A
                    OCR.extraxtText(
                        context = context,
                        imageUri = selectedImageUri!!,
                        onSuccess = { extractedText ->
                            statusText = "2. Found text! Generating AI Vector..."

                            // Switch to background thread to do the heavy math
                            coroutineScope.launch {
//
                                // Turn on the engine
                                Embedding.initEmbedder(context)

                                //vector generator
                                val vector = Embedding.generateVector(extractedText)

                                //create a folder name parentbox to store medical data
                                val parentbox= ObjectBox.store.boxFor(Medicaldata::class.java)
                                val newDocument= Medicaldata(tittle ="Scanned data", imagepath = selectedImageUri.toString())
                                val parentID=parentbox.put(newDocument)

                                //chunk the text
                                val textChunk=extractedText.chunkText(200)
                                statusText = "Created ${textChunk.size} chunks. Generating AI vectors..."


                                //create a folder name chuckbox to store the medical chunked data
                                val ChunkBox = ObjectBox.store.boxFor(MedicalChunck::class.java)
                                var savecount=0
                                for (chunk in textChunk){
                                    val vector= Embedding.generateVector(chunk)
                                    if (vector!=null){
                                        val newChunk= MedicalChunck(chunkedtext = chunk, textEmbedding = vector)
                                        ChunkBox.put(newChunk)
                                        savecount++
                                    }

                                }
                                statusText = "3. SUCCESS! Saved document with $savecount searchable pieces."
                                Log.d("AI_PIPELINE", "Saved Parent ID: $parentID with $savecount chunks.")


                            }
                        },
                        onFailure = { error ->
                            statusText = "OCR Failed: ${error.message}"
                        }
                    )
                }
            ) {
                Text("Process & Save to Memory")
            }
        }
    }
}
// criteria to chunk
fun String.chunkText(MaxLine: Int =15,Overlaping : Int= 3): List<String> {
    //filtering the line to text like leaving blank line extracting text
   val allLine=this.trim().lines().filter { it.isNotBlank() }
    if (allLine.isEmpty())return emptyList()
    if(allLine.size<=MaxLine)return listOf(this)

    val chunks = mutableListOf<String>()//store the filtered text
    val stepsize=MaxLine-Overlaping//to reduce context loss overlapping is done and re read previous 3 lines and make a buffer section to reread


    for (i in allLine.indices step stepsize){
        val endIndex = minOf(i+MaxLine,allLine.size)//where to make cut
        val chunk = allLine.subList(i,endIndex).joinToString("/n")
        chunks.add(chunk)
        if(endIndex==allLine.size)break
    }
    return chunks

}