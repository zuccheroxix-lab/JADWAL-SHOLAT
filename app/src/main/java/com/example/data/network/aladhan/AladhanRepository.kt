package com.example.data.network.aladhan

import android.util.Log
import com.example.data.model.PrayerTimes
import com.example.utils.PrayerTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class PrayerFetchResult {
    data class Success(val prayerTimes: PrayerTimes, val isFromApi: Boolean, val sourceDescription: String) : PrayerFetchResult()
    data class Error(val message: String, val fallbackTimes: PrayerTimes) : PrayerFetchResult()
}

class AladhanRepository(
    private val apiService: AladhanApiService = AladhanApiClient.apiService
) {
    private val TAG = "AladhanRepository"

    /**
     * Map app calculation method to Aladhan API method ID.
     */
    fun getMethodId(methodName: String): Int {
        return when (methodName) {
            "Kemenag RI" -> 20
            "Umm Al-Qura" -> 4
            "Muslim World League" -> 3
            "ISNA" -> 2
            "Egypt" -> 5
            else -> 20
        }
    }

    /**
     * Clean prayer time string (e.g. "04:20 (WIB)" -> "04:20")
     */
    private fun sanitizeTime(time: String?): String {
        if (time.isNullOrBlank()) return "--:--"
        return time.trim().substringBefore(" ").take(5)
    }

    /**
     * Fetch prayer times from Aladhan API based on latitude, longitude, and calculation method.
     * Automatically falls back to offline calculation if offline or in case of error.
     */
    suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        calculationMethod: String,
        calendar: Calendar = Calendar.getInstance()
    ): PrayerFetchResult = withContext(Dispatchers.IO) {
        val dateApiFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(calendar.time)
        val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(calendar.time)
        val methodId = getMethodId(calculationMethod)

        try {
            Log.d(TAG, "Requesting Aladhan API for lat=$latitude, lng=$longitude, date=$dateApiFormat, method=$methodId")
            val response = apiService.getTimingsByDate(
                date = dateApiFormat,
                latitude = latitude,
                longitude = longitude,
                method = methodId
            )

            if (response.isSuccessful && response.body()?.data?.timings != null) {
                val data = response.body()!!.data!!
                val timings = data.timings!!

                val imsak = sanitizeTime(timings.imsak)
                val subuh = sanitizeTime(timings.fajr)
                val terbit = sanitizeTime(timings.sunrise)
                val dzuhur = sanitizeTime(timings.dhuhr)
                val ashar = sanitizeTime(timings.asr)
                val maghrib = sanitizeTime(timings.maghrib)
                val isya = sanitizeTime(timings.isha)

                // Parse Hijri date from Aladhan response
                val hijri = data.date?.hijri
                val hijriDateStr = if (hijri != null) {
                    val day = hijri.day ?: ""
                    val monthName = hijri.month?.en ?: ""
                    val year = hijri.year ?: ""
                    val desig = hijri.designation?.abbreviated ?: "H"
                    "$day $monthName $year $desig".trim()
                } else {
                    displayDateFormat
                }

                val prayerTimes = PrayerTimes(
                    date = displayDateFormat,
                    hijriDate = hijriDateStr,
                    imsak = imsak,
                    subuh = subuh,
                    terbit = terbit,
                    dzuhur = dzuhur,
                    ashar = ashar,
                    maghrib = maghrib,
                    isya = isya
                )

                val enriched = PrayerTimeCalculator.enrichWithCountdown(prayerTimes)
                Log.d(TAG, "Aladhan API fetch successful: Subuh=$subuh, Dzuhur=$dzuhur, Maghrib=$maghrib")
                return@withContext PrayerFetchResult.Success(
                    prayerTimes = enriched,
                    isFromApi = true,
                    sourceDescription = "Aladhan API (Online)"
                )
            } else {
                val errCode = response.code()
                val errMsg = response.errorBody()?.string() ?: "Empty body"
                Log.w(TAG, "Aladhan API returned error code $errCode: $errMsg. Falling back to offline astronomical engine.")
                val fallback = PrayerTimeCalculator.enrichWithCountdown(
                    PrayerTimeCalculator.calculateTimes(latitude, longitude, calculationMethod, calendar)
                )
                return@withContext PrayerFetchResult.Error(
                    message = "Gagal memuat dari Aladhan API (Error $errCode). Menggunakan kalkulasi astronomis offline.",
                    fallbackTimes = fallback
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception contacting Aladhan API: ${e.message}. Using offline calculation.", e)
            val fallback = PrayerTimeCalculator.enrichWithCountdown(
                PrayerTimeCalculator.calculateTimes(latitude, longitude, calculationMethod, calendar)
            )
            return@withContext PrayerFetchResult.Error(
                message = "Mode offline: ${e.localizedMessage ?: "Tidak ada koneksi internet"}. Menggunakan kalkulasi lokal akurat.",
                fallbackTimes = fallback
            )
        }
    }
}
