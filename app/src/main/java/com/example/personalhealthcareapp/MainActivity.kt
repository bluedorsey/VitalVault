package com.example.personalhealthcareapp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.personalhealthcareapp.db.Embedding
import com.example.personalhealthcareapp.db.ObjectBox
import com.example.personalhealthcareapp.uiux.ChatScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //turning on the engines of embedding and objectbox to store vector db
        Embedding.initEmbedder(this)
        ObjectBox.init(this)
        setContent {
            MaterialTheme {
          ChatScreen()            }
        }
    }
}

