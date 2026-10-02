package com.example.data.firebase

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val lastActive: Long = 0L,
    val cityName: String = "",
    val calculationMethod: String = "",
    val isAnonymous: Boolean = false
)

data class FirestorePrayerRecord(
    val id: String = "",
    val prayerName: String = "",
    val date: String = "",
    val isDone: Boolean = false,
    val timestamp: Long = 0L,
    val location: String = ""
)

data class FirestoreSavedDua(
    val id: String = "",
    val title: String = "",
    val arabicText: String = "",
    val translation: String = "",
    val timestamp: Long = 0L
)
