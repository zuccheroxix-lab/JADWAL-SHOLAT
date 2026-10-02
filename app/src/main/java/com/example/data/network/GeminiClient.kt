package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GeminiModelTier(val modelId: String, val displayName: String, val description: String) {
    GENERAL("gemini-3.5-flash", "Gemini 3.5 Flash", "Tugas umum, doa, panduan ibadah & grounding peta"),
    COMPLEX("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Analisis fiqih mendalam, komparasi mazhab & tafsir"),
    FAST("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Tanya jawab kilat & jawaban ringkas")
}

data class GroundingSource(
    val title: String,
    val uri: String? = null
)

data class GeminiChatResult(
    val replyText: String,
    val modelUsed: String,
    val groundingSources: List<GroundingSource> = emptyList(),
    val isMapsGrounded: Boolean = false
)

/**
 * Client for interacting with Google Generative AI models.
 * Supports:
 * 1. gemini-3.5-flash (General tasks & Google Maps grounding)
 * 2. gemini-3.1-pro-preview (Complex theological & detailed fiqih reasoning)
 * 3. gemini-3.1-flash-lite-preview (Fast instant answers)
 * 4. Google Maps Grounding tool for finding mosques, halal food & Islamic sites.
 */
object GeminiClient {
    private const val TAG = "GeminiClient"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    const val DEFAULT_SYSTEM_INSTRUCTION = """
        Anda adalah "Asisten Syariah & Penasihat Ibadah AI" terpercaya di aplikasi ZUCCHERO Waktu Sholat.
        Tugas utama Anda adalah membimbing pengguna seputar ajaran Islam, fiqih ibadah, tata cara sholat, wudhu, doa harian, zakat, puasa, dan akhlak Islami.
        
        Karakteristik Tanggapan:
        1. Awali dengan salam hangat Islami ("Assalamu'alaikum Warahmatullahi Wabarakatuh") pada percakapan awal.
        2. Berikan dalil shahih dari Al-Qur'an dan Sunnah/Hadits dengan sanad yang jelas.
        3. Jelaskan rukun dan syarat ibadah secara terstruktur menggunakan penomoran atau poin ringkas.
        4. Sikap wasathiyah (moderat), santun, menyejukkan, dan tidak memicu perselisihan mazhab.
        5. Jika terdapat pertanyaan mengenai lokasi masjid atau tempat ibadah di sekitar, sertakan info alamat atau nama masjid secara akurat dari Google Maps.
    """

    /**
     * Send multi-turn prompt to Gemini with support for Model Selection & Google Maps Grounding
     */
    suspend fun generateChatResponse(
        prompt: String,
        history: List<Pair<Boolean, String>> = emptyList(),
        modelTier: GeminiModelTier = GeminiModelTier.GENERAL,
        enableMapsGrounding: Boolean = false,
        userLatitude: Double? = null,
        userLongitude: Double? = null,
        systemInstruction: String = DEFAULT_SYSTEM_INSTRUCTION
    ): GeminiChatResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is empty")
            return@withContext GeminiChatResult(
                replyText = "Kunci API Gemini belum diatur. Harap masukkan GEMINI_API_KEY Anda di Secrets AI Studio agar asisten dapat aktif.",
                modelUsed = modelTier.modelId
            )
        }

        // When Maps Grounding is requested, or if using direct REST capabilities:
        if (enableMapsGrounding) {
            return@withContext callGeminiRestWithMaps(
                apiKey = apiKey,
                prompt = prompt,
                history = history,
                systemInstruction = systemInstruction,
                userLatitude = userLatitude,
                userLongitude = userLongitude
            )
        }

        // Standard SDK call with chosen model tier
        try {
            val config = generationConfig {
                temperature = if (modelTier == GeminiModelTier.COMPLEX) 0.5f else 0.7f
                topP = 0.95f
                topK = 40
            }

            val generativeModel = GenerativeModel(
                modelName = modelTier.modelId,
                apiKey = apiKey,
                generationConfig = config,
                systemInstruction = content { text(systemInstruction) }
            )

            val sdkHistory = history.map { (isUser, text) ->
                content(role = if (isUser) "user" else "model") {
                    text(text)
                }
            }

            val chat = generativeModel.startChat(history = sdkHistory)
            val response = chat.sendMessage(prompt)
            val text = response.text ?: "Afwan, respon kosong dari server."

            return@withContext GeminiChatResult(
                replyText = text,
                modelUsed = modelTier.modelId
            )
        } catch (e: Exception) {
            Log.e(TAG, "GenerativeModel SDK failed with tier $modelTier, attempting REST fallback: ${e.message}")
            // Fallback via REST endpoint
            return@withContext callGeminiRestFallback(
                apiKey = apiKey,
                prompt = prompt,
                history = history,
                modelTier = modelTier,
                systemInstruction = systemInstruction
            )
        }
    }

    /**
     * Call Gemini 3.5 Flash REST API with Google Maps tool enabled
     */
    private fun callGeminiRestWithMaps(
        apiKey: String,
        prompt: String,
        history: List<Pair<Boolean, String>>,
        systemInstruction: String,
        userLatitude: Double?,
        userLongitude: Double?
    ): GeminiChatResult {
        val model = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val root = JSONObject()

            // System Instruction
            val sysObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject().put("text", systemInstruction)
            sysParts.put(sysPart)
            sysObj.put("parts", sysParts)
            root.put("systemInstruction", sysObj)

            // Contents with conversation history
            val contentsArray = JSONArray()
            for ((isUser, text) in history) {
                val item = JSONObject()
                item.put("role", if (isUser) "user" else "model")
                val parts = JSONArray().put(JSONObject().put("text", text))
                item.put("parts", parts)
                contentsArray.put(item)
            }

            // Current prompt enriched with coordinates if available
            val promptWithLocation = if (userLatitude != null && userLongitude != null) {
                "$prompt\n\n[Lokasi GPS Pengguna Terkini: Latitude $userLatitude, Longitude $userLongitude]"
            } else {
                prompt
            }

            val userItem = JSONObject()
            userItem.put("role", "user")
            userItem.put("parts", JSONArray().put(JSONObject().put("text", promptWithLocation)))
            contentsArray.put(userItem)
            root.put("contents", contentsArray)

            // Add Google Maps Tool Grounding
            val toolsArray = JSONArray()
            val mapsTool = JSONObject()
            mapsTool.put("googleMaps", JSONObject())
            toolsArray.put(mapsTool)
            root.put("tools", toolsArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w(TAG, "Maps Grounding failed (${response.code}), falling back to standard: $bodyStr")
                    // If Google Maps tool is unavailable for this key, fall back to standard call
                    return callGeminiRestFallback(apiKey, prompt, history, GeminiModelTier.GENERAL, systemInstruction)
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val cand = candidates.getJSONObject(0)
                    val content = cand.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val reply = if (parts != null && parts.length() > 0) {
                        parts.getJSONObject(0).optString("text", "")
                    } else ""

                    // Extract grounding metadata if present
                    val sources = mutableListOf<GroundingSource>()
                    val groundingMeta = cand.optJSONObject("groundingMetadata")
                    val chunks = groundingMeta?.optJSONArray("groundingChunks")
                    if (chunks != null) {
                        for (i in 0 until chunks.length()) {
                            val chunk = chunks.optJSONObject(i)
                            val web = chunk?.optJSONObject("web")
                            val title = web?.optString("title") ?: "Google Maps Data"
                            val uri = web?.optString("uri")
                            sources.add(GroundingSource(title, uri))
                        }
                    }

                    return GeminiChatResult(
                        replyText = reply.ifBlank { "Tidak ada jawaban yang dihasilkan." },
                        modelUsed = model,
                        groundingSources = sources,
                        isMapsGrounded = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Maps Grounding call: ${e.message}", e)
        }

        return callGeminiRestFallback(apiKey, prompt, history, GeminiModelTier.GENERAL, systemInstruction)
    }

    /**
     * Fallback REST call for any Gemini tier
     */
    private fun callGeminiRestFallback(
        apiKey: String,
        prompt: String,
        history: List<Pair<Boolean, String>>,
        modelTier: GeminiModelTier,
        systemInstruction: String
    ): GeminiChatResult {
        val model = modelTier.modelId
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val root = JSONObject()
            val sysObj = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            root.put("systemInstruction", sysObj)

            val contentsArray = JSONArray()
            for ((isUser, text) in history) {
                contentsArray.put(
                    JSONObject()
                        .put("role", if (isUser) "user" else "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", text)))
                )
            }
            contentsArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            )
            root.put("contents", contentsArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return GeminiChatResult(
                        replyText = "Afwan, terjadi kendala saat menghubungi AI ($model): Error ${response.code}",
                        modelUsed = model
                    )
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "Jawaban kosong.") ?: "Jawaban kosong."

                return GeminiChatResult(
                    replyText = text,
                    modelUsed = model
                )
            }
        } catch (e: Exception) {
            return GeminiChatResult(
                replyText = "Gagal memproses pertanyaan: ${e.localizedMessage}",
                modelUsed = model
            )
        }
    }
}
