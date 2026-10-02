package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChatMessage
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestoreRepository
import com.example.data.network.GeminiChatResult
import com.example.data.network.GeminiClient
import com.example.data.network.GeminiModelTier
import com.example.data.preferences.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReligiousCategory(
    val id: String,
    val title: String,
    val icon: String,
    val promptSuggestions: List<String>
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()
    private val firestoreRepo = FirestoreRepository()
    private val settings = SettingsManager(application)

    val chatHistory: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Gemini Model Tier Selection (3.5-flash for General, 3.1-pro for Complex, 3.1-flash-lite for Fast)
    private val _selectedModelTier = MutableStateFlow(GeminiModelTier.GENERAL)
    val selectedModelTier: StateFlow<GeminiModelTier> = _selectedModelTier.asStateFlow()

    // Google Maps Grounding Toggle
    private val _isMapsGroundingEnabled = MutableStateFlow(false)
    val isMapsGroundingEnabled: StateFlow<Boolean> = _isMapsGroundingEnabled.asStateFlow()

    private val _lastModelUsed = MutableStateFlow("")
    val lastModelUsed: StateFlow<String> = _lastModelUsed.asStateFlow()

    val categories = listOf(
        ReligiousCategory(
            id = "Semua",
            title = "Semua",
            icon = "🕌",
            promptSuggestions = listOf(
                "Bagaimana niat dan tata cara sholat Tahajjud serta Witir?",
                "Doa pembuka pintu rezeki yang berkah dan halal",
                "Apa saja syarat dan ketentuan sholat jamak & qashar bagi musafir?",
                "Bagaimana hukum dan tata cara sujud sahwi saat ragu rakaat sholat?"
            )
        ),
        ReligiousCategory(
            id = "Masjid & Peta",
            title = "Peta & Masjid",
            icon = "📍",
            promptSuggestions = listOf(
                "Cari masjid atau musholla terdekat dari koordinat saya",
                "Rekomendasi tempat makan halal terdekat untuk berbuka puasa",
                "Di mana toko busana muslim atau perlengkapan ibadah di sekitar sini?",
                "Info pusat kajian Islam atau pesantren terdekat"
            )
        ),
        ReligiousCategory(
            id = "Fiqih Sholat",
            title = "Fiqih Sholat",
            icon = "🕋",
            promptSuggestions = listOf(
                "Apa saja rukun sholat yang wajib dipenuhi dan pembatal sholat?",
                "Bagaimana cara sholat sambil duduk atau berbaring saat sakit?",
                "Kapan waktu utama sholat Dhuha dan berapa rakaat yang dianjurkan?",
                "Hukum makmum tertinggal (masbuq) dalam sholat berjamaah"
            )
        ),
        ReligiousCategory(
            id = "Doa & Dzikir",
            title = "Doa & Dzikir",
            icon = "🤲",
            promptSuggestions = listOf(
                "Dzikir setelah sholat fardhu yang diajarkan Rasulullah SAW",
                "Doa ketika hati cemas, gelisah, atau tertimpa kesulitan hidup",
                "Bimbingan dzikir pagi dan petang (Al-Ma'tsurat)",
                "Doa untuk kedua orang tua baik yang masih hidup maupun telah wafat"
            )
        ),
        ReligiousCategory(
            id = "Puasa & Zakat",
            title = "Puasa & Zakat",
            icon = "🌙",
            promptSuggestions = listOf(
                "Hukum dan tata cara mengganti (qadha) puasa Ramadhan",
                "Keutamaan puasa sunnah Senin-Kamis dan puasa Ayyamul Bidh",
                "Bagaimana cara menghitung zakat penghasilan dan zakat mal?",
                "Siapa saja 8 golongan (asnaf) yang berhak menerima zakat?"
            )
        ),
        ReligiousCategory(
            id = "Al-Qur'an & Hadits",
            title = "Al-Qur'an & Hadits",
            icon = "📖",
            promptSuggestions = listOf(
                "Tafsir dan keutamaan membaca Surat Al-Kahfi di hari Jumat",
                "Hadits shahih tentang pentingnya menjaga sholat lima waktu",
                "Kisah keteladanan Nabi Yunus AS saat berada di dalam perut ikan paus",
                "Adab dan keutamaan mengkhatamkan Al-Qur'an"
            )
        ),
        ReligiousCategory(
            id = "Thaharah & Wudhu",
            title = "Thaharah",
            icon = "💧",
            promptSuggestions = listOf(
                "Tata cara wudhu yang sempurna sesuai sunnah Rasulullah SAW",
                "Kapan seseorang diperbolehkan tayamum dan bagaimana caranya?",
                "Rukun dan niat mandi wajib (junub) bagi pria dan wanita",
                "Perbedaan antara najis mukhaffafah, mutawassithah, dan mughalladhah"
            )
        )
    )

    fun selectCategory(categoryId: String) {
        _selectedCategory.value = categoryId
        if (categoryId == "Masjid & Peta") {
            _isMapsGroundingEnabled.value = true
        }
    }

    fun selectModelTier(tier: GeminiModelTier) {
        _selectedModelTier.value = tier
    }

    fun toggleMapsGrounding() {
        _isMapsGroundingEnabled.value = !_isMapsGroundingEnabled.value
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isSending.value) return

        val userText = text.trim()
        val userMsg = ChatMessage(isUser = true, text = userText, timestamp = System.currentTimeMillis())

        viewModelScope.launch {
            chatDao.insertMessage(userMsg)
            _isSending.value = true

            // Sync user question to Firestore if logged in
            val currentUser = FirebaseManager.currentUser.value
            if (currentUser != null) {
                firestoreRepo.saveChatLog(currentUser.uid, userText, isUser = true)
            }

            try {
                val historySnapshot = chatHistory.value.takeLast(6).map { it.isUser to it.text }
                val isMapsNeeded = _isMapsGroundingEnabled.value || 
                    userText.contains("masjid", ignoreCase = true) ||
                    userText.contains("terdekat", ignoreCase = true) ||
                    userText.contains("makanan halal", ignoreCase = true) ||
                    userText.contains("lokasi", ignoreCase = true)

                val result: GeminiChatResult = GeminiClient.generateChatResponse(
                    prompt = userText,
                    history = historySnapshot,
                    modelTier = _selectedModelTier.value,
                    enableMapsGrounding = isMapsNeeded,
                    userLatitude = settings.latitude.toDouble(),
                    userLongitude = settings.longitude.toDouble()
                )

                _lastModelUsed.value = result.modelUsed

                var formattedReply = result.replyText
                if (result.isMapsGrounded && result.groundingSources.isNotEmpty()) {
                    val sourcesText = result.groundingSources.take(3).joinToString("\n") { src ->
                        "• ${src.title}${if (!src.uri.isNullOrEmpty()) " (${src.uri})" else ""}"
                    }
                    formattedReply += "\n\n📍 Sumber Google Maps:\n$sourcesText"
                }

                val aiMsg = ChatMessage(
                    isUser = false,
                    text = formattedReply,
                    timestamp = System.currentTimeMillis()
                )

                chatDao.insertMessage(aiMsg)

                // Sync AI reply to Firestore if logged in
                if (currentUser != null) {
                    firestoreRepo.saveChatLog(currentUser.uid, formattedReply, isUser = false)
                }

            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    isUser = false,
                    text = "Mohon maaf, terjadi kesalahan: ${e.localizedMessage}",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(errorMsg)
            } finally {
                _isSending.value = false
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearHistory()
        }
    }
}
