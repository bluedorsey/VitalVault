package com.example.personalhealthcareapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.personalhealthcareapp.db.Embedding
import com.example.personalhealthcareapp.db.ObjectBox
import com.example.personalhealthcareapp.ui.theme.PersonalHealthCareAppTheme
import com.example.personalhealthcareapp.uiux.MainPagerScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ObjectBox synchronously (fast, required before any DB access)
        ObjectBox.init(this)

        // Initialize embedder on background thread to avoid ANR
        CoroutineScope(Dispatchers.IO).launch {
            Embedding.initEmbedder(this@MainActivity)
        }

        setContent {
            PersonalHealthCareAppTheme {
                MainPagerScreen()
            }
        }
    }
}
