package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestorePrayerRecord
import com.example.data.firebase.FirestoreRepository
import com.example.data.firebase.UserProfile
import com.example.data.preferences.SettingsManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepo = FirestoreRepository()
    private val settings = SettingsManager(application)

    val currentUser: StateFlow<FirebaseUser?> = FirebaseManager.currentUser
    val isAuthLoading: StateFlow<Boolean> = FirebaseManager.isAuthLoading
    val authErrorMessage: StateFlow<String?> = FirebaseManager.authErrorMessage

    private val _syncStatus = MutableStateFlow("Tersinkronisasi")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _todayPrayerChecks = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val todayPrayerChecks: StateFlow<Map<String, Boolean>> = _todayPrayerChecks.asStateFlow()

    init {
        FirebaseManager.init(application)
        observeUserAndSync()
    }

    private fun observeUserAndSync() {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    val profile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: "guest@zucchero.app",
                        displayName = user.displayName ?: "Hamba Allah",
                        photoUrl = user.photoUrl?.toString() ?: "",
                        lastActive = System.currentTimeMillis(),
                        cityName = settings.cityName,
                        calculationMethod = settings.calculationMethod,
                        isAnonymous = user.isAnonymous
                    )
                    firestoreRepo.saveUserProfile(profile)
                    loadTodayPrayerChecks(user.uid)
                }
            }
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun loadTodayPrayerChecks(userId: String) {
        viewModelScope.launch {
            firestoreRepo.getPrayerRecords(userId, getTodayDateString()).collect { records ->
                val map = records.associate { it.prayerName to it.isDone }
                _todayPrayerChecks.value = map
            }
        }
    }

    fun togglePrayerDone(prayerName: String) {
        val user = currentUser.value ?: return
        val current = _todayPrayerChecks.value[prayerName] ?: false
        val newStatus = !current

        val updatedMap = _todayPrayerChecks.value.toMutableMap()
        updatedMap[prayerName] = newStatus
        _todayPrayerChecks.value = updatedMap

        viewModelScope.launch {
            val record = FirestorePrayerRecord(
                prayerName = prayerName,
                date = getTodayDateString(),
                isDone = newStatus,
                timestamp = System.currentTimeMillis(),
                location = settings.cityName
            )
            firestoreRepo.savePrayerRecord(user.uid, record)
        }
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        val client = FirebaseManager.getGoogleSignInClient(context)
        return client.signInIntent
    }

    fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                FirebaseManager.signInWithGoogleIdToken(idToken) { success, err ->
                    if (success) {
                        _syncStatus.value = "Berhasil Masuk Google"
                    }
                }
            } else {
                // If ID Token not available without Web Client ID, use user profile directly
                account?.let { acc ->
                    _syncStatus.value = "Terhubung: ${acc.displayName}"
                }
            }
        } catch (e: Exception) {
            FirebaseManager.signInAnonymously { _, _ -> }
        }
    }

    fun signInAsGuest() {
        FirebaseManager.signInAnonymously { success, _ ->
            if (success) {
                _syncStatus.value = "Masuk sebagai Tamu"
            }
        }
    }

    fun signOut() {
        FirebaseManager.signOut()
    }
}
