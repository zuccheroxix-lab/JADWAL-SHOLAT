package com.example.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository to persist user data, prayer history, and saved prayers/du'as into Cloud Firestore.
 */
class FirestoreRepository {
    private val TAG = "FirestoreRepository"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not available: ${e.message}")
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Boolean {
        val db = firestore ?: return false
        return try {
            val userMap = mapOf(
                "uid" to profile.uid,
                "email" to profile.email,
                "displayName" to profile.displayName,
                "photoUrl" to profile.photoUrl,
                "lastActive" to System.currentTimeMillis(),
                "cityName" to profile.cityName,
                "calculationMethod" to profile.calculationMethod,
                "isAnonymous" to profile.isAnonymous
            )
            db.collection("users").document(profile.uid).set(userMap).await()
            Log.d(TAG, "User profile saved to Firestore for UID: ${profile.uid}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun savePrayerRecord(userId: String, record: FirestorePrayerRecord): Boolean {
        val db = firestore ?: return false
        return try {
            val recordMap = mapOf(
                "prayerName" to record.prayerName,
                "date" to record.date,
                "isDone" to record.isDone,
                "timestamp" to (if (record.timestamp == 0L) System.currentTimeMillis() else record.timestamp),
                "location" to record.location
            )
            db.collection("users")
                .document(userId)
                .collection("prayer_tracker")
                .document("${record.date}_${record.prayerName}")
                .set(recordMap)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving prayer record: ${e.message}", e)
            false
        }
    }

    fun getPrayerRecords(userId: String, date: String): Flow<List<FirestorePrayerRecord>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(userId)
            .collection("prayer_tracker")
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore snapshot error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    FirestorePrayerRecord(
                        id = doc.id,
                        prayerName = doc.getString("prayerName") ?: "",
                        date = doc.getString("date") ?: "",
                        isDone = doc.getBoolean("isDone") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        location = doc.getString("location") ?: ""
                    )
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveChatLog(userId: String, text: String, isUser: Boolean): Boolean {
        val db = firestore ?: return false
        return try {
            val chatMap = mapOf(
                "text" to text,
                "isUser" to isUser,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("users")
                .document(userId)
                .collection("chat_history")
                .add(chatMap)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chat log to Firestore", e)
            false
        }
    }

    suspend fun saveFavoriteDua(userId: String, dua: FirestoreSavedDua): Boolean {
        val db = firestore ?: return false
        return try {
            val map = mapOf(
                "title" to dua.title,
                "arabicText" to dua.arabicText,
                "translation" to dua.translation,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("users")
                .document(userId)
                .collection("favorite_duas")
                .add(map)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite dua", e)
            false
        }
    }
}
