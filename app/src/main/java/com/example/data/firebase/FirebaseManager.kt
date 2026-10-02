package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isFirebaseAvailable = false

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            isFirebaseAvailable = true
            val auth = FirebaseAuth.getInstance()
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
                Log.d(TAG, "Firebase Auth State changed: User=${firebaseAuth.currentUser?.email ?: "Guest"}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}. (Normal if google-services.json not yet attached).")
            isFirebaseAvailable = false
        }
    }

    fun isConfigured(): Boolean = isFirebaseAvailable

    fun getGoogleSignInClient(context: Context, defaultWebClientId: String = ""): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (defaultWebClientId.isNotBlank()) {
            gsoBuilder.requestIdToken(defaultWebClientId)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun signInWithGoogleIdToken(idToken: String, onResult: (Boolean, String?) -> Unit) {
        if (!isFirebaseAvailable) {
            onResult(false, "Firebase belum dikonfigurasi dengan google-services.json")
            return
        }

        _isAuthLoading.value = true
        _authErrorMessage.value = null

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isAuthLoading.value = false
                if (task.isSuccessful) {
                    _currentUser.value = task.result?.user
                    onResult(true, null)
                } else {
                    val err = task.exception?.localizedMessage ?: "Gagal masuk dengan Akun Google"
                    _authErrorMessage.value = err
                    onResult(false, err)
                }
            }
    }

    fun signInAnonymously(onResult: (Boolean, String?) -> Unit) {
        if (!isFirebaseAvailable) {
            onResult(false, "Firebase SDK belum terhubung")
            return
        }

        _isAuthLoading.value = true
        _authErrorMessage.value = null

        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                _isAuthLoading.value = false
                if (task.isSuccessful) {
                    _currentUser.value = task.result?.user
                    onResult(true, null)
                } else {
                    val err = task.exception?.localizedMessage ?: "Gagal masuk sebagai Tamu"
                    _authErrorMessage.value = err
                    onResult(false, err)
                }
            }
    }

    fun signOut() {
        if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out", e)
            }
        }
        _currentUser.value = null
    }

    fun clearError() {
        _authErrorMessage.value = null
    }
}
