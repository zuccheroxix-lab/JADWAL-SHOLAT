package com.example.data.model

data class PrayerTimes(
    val date: String,
    val hijriDate: String,
    val imsak: String,
    val subuh: String,
    val terbit: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String,
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val countdownSeconds: Long = 0,
    val progress: Float = 0f
)
