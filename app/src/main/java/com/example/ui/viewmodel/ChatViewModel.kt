package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChatMessage
import com.example.data.network.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()

    val chatHistory: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val systemInstruction = """
        Kamu adalah asisten spiritual AI khusus bernama "ZUCCHERO Sholat AI". 
        Tugas utamanya:
        1. Membimbing pengguna seputar ibadah Islam: tata cara sholat, wudhu, tayamum, doa sehari-hari, amalan dzikir pagi & petang.
        2. Memberikan motivasi Islami harian, kisah hikmah sahabat nabi, dan penjelasan hadits singkat.
        3. Menjelaskan keutamaan puasa wajib dan sunnah (Senin-Kamis, Ayyamul Bidh).
        4. Menjelaskan cara menentukan arah kiblat, fiqih ibadah lainnya.
        
        Karakteristik Tanggapan:
        - Sopan, penuh bimbingan, empati tinggi, ramah, dan menaruh salam Islami (Assalamu'alaikum).
        - Menampilkan dalil shahih atau penjelasan ringkas berlandaskan Al-Qur'an dan Sunnah yang kredibel.
        - Menggunakan Bahasa Indonesia yang bersih, kekinian, elegan, dan menenangkan.
        - Jawab dengan cepat, langsung ke inti masalah, rapi terbagi per poin atau paragraf senggang agar mudah dibaca.
        - Hindari perdebatan mazhab yang kasar. Berikan solusi moderat dan luas (rahmatan lil'alamin).
    """.trimIndent()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(isUser = true, text = text, timestamp = System.currentTimeMillis())

        viewModelScope.launch {
            // Save user message to DB
            chatDao.insertMessage(userMsg)

            _isSending.value = true

            // Gather context from history (send simple user prompt with conversational history)
            // To prevent exceeding context limits, we combine the system prompt + user question.
            val aiResponse = GeminiClient.generateResponse(text, systemInstruction)

            val systemMsg = ChatMessage(isUser = false, text = aiResponse, timestamp = System.currentTimeMillis())

            // Save AI response message to DB
            chatDao.insertMessage(systemMsg)

            _isSending.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearHistory()
        }
    }
}

// Extension to safely expose StateFlow read-only
private fun <T> MutableStateFlow<T>.asStateFlow(): StateFlow<T> = this
