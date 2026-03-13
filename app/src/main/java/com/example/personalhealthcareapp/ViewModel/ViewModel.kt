package com.example.personalhealthcareapp.ViewModel

import android.app.Application
import androidx.compose.runtime.currentRecomposeScope
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalhealthcareapp.LLMinference.LLMInferenceManager
import com.example.personalhealthcareapp.chat_managment.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewModel(application: Application): AndroidViewModel(application) {
private val _responseStatus = MutableStateFlow("Ai is loading")
    val responseStatus=_responseStatus.asStateFlow()
    private  val _chathistory= MutableStateFlow<List<Chat>>(emptyList())
    val chathistory=_chathistory.asStateFlow()

    init {
        viewModelScope.launch (Dispatchers.IO)//dispatcher to do the work in background thread
        {
            LLMInferenceManager.initModel(application)
            _responseStatus.value = "Ai is ready to use"
// adding chucked words that can from LLM
            LLMInferenceManager.partialresult.collect { word ->
                appended_message(word)
                _responseStatus.value = word
            }
            _chathistory.value=listOf(Chat("what the message", isUser = false))

        }

    }
    fun sendMessage(message: String) {
        _responseStatus.value = "Thinking..."
        val currentList = _chathistory.value.toMutableList()
        currentList.add(Chat(message, isUser = true))
        currentList.add(Chat("thinking", isUser = false))

        _chathistory.value = currentList
        LLMInferenceManager.generateResponse(message)


    }

    private fun appended_message(reponse: String){
        val currentList =_chathistory.value.toMutableList()

        val lastword=currentList.last()
        if (!lastword.isUser){
            val newtext= if(lastword.text=="thinking") reponse else lastword.text + reponse
            currentList[currentList.size - 1] = lastword.copy(text = newtext)
            _chathistory.value= currentList
        }
    }
}