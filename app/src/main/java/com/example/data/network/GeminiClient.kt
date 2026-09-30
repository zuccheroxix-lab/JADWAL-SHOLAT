package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(prompt: String, systemInstruction: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is a placeholder or empty!")
            return@withContext "Kunci API Gemini belum diatur. Silakan masukkan kunci API Anda di panel Rahasia / Secrets AI Studio."
        }

        try {
            val root = JSONObject()
            
            // Build contents array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            
            root.put("contents", contentsArray)

            // Build system instruction
            if (systemInstruction.isNotEmpty()) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                root.put("systemInstruction", sysInstObj)
            }

            val requestBodyJson = root.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Response failed: Code ${response.code}, Body: $errBody")
                    return@withContext "Maaf, terjadi kesalahan saat menghubungi AI (Error ${response.code})."
                }

                val resBody = response.body?.string() ?: return@withContext "Gagal mendapatkan respon (Response Body kosong)."
                val jsonRes = JSONObject(resBody)
                val candidates = jsonRes.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Respon kosong.")
                    }
                }
                return@withContext "AI tidak dapat memproses jawaban saat ini."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call: ", e)
            return@withContext "Eror: Gagal menghubungi server atau koneksi terputus. Detail: ${e.localizedMessage}"
        }
    }
}
