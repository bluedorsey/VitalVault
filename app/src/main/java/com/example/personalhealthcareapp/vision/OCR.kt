package com.example.personalhealthcareapp.vision

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.security.PrivilegedExceptionAction

//OCR optical Character Recognition
object OCR {
    private val recorgination = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
// image click function work text extraction save in db
    fun extraxtText(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try{
            Log.d("OCR", "Input the image ")
            val image = InputImage.fromFilePath(context, imageUri)
            Log.d("OCR", "Input the image ")
            recorgination.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    Log.d("OCR", "Success fully read the text ${text.length}")
                    onSuccess(text)
                }
                .addOnFailureListener { e ->
                    Log.d("OCR","Failed to read the text")
                    onFailure(e)
                }

        }
        catch (e:Exception){
            Log.d("OCR","Could not load the file ${e.message}")
            onFailure(e)

        }

    }

}