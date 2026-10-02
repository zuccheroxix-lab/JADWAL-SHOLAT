package com.example.data.network.aladhan

import android.util.Log
import com.example.data.model.PrayerTimes
import com.example.utils.PrayerTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

sealed class PrayerFetchResult {
    data class Success(val prayerTimes: PrayerTimes, val isFromApi: Boolean = true) : PrayerFetchResult()
    data class Error(val message: String, val fallbackTimes: PrayerTimes) : PrayerFetchResult()
}

/**
 * Repository to manage prayer times fetching from Aladhan Retrofit API
 * with robust offline fallback to astronomical calculations.
 */
class AladhanRepository(
    private val apiService: AladhanApiService = AladhanApiClient.apiService
) {
    private val TAG = "AladhanRepository"

    /**
     * Map calculation method label to Aladhan API method ID
     */
    private fun mapMethodToId(methodName: String): Int {
        return when (methodName) {
            "Kemenag RI" -> 20 // Kementerian Agama Republik Indonesia
            "Muslim World League" -> 3
            "ISNA" -> 2
            "Egypt" -> 5
            "Umm Al-Qura" -> 4
            else -> 20
        }
    }

    /**
     * Strip potential timezone suffix like "(WIB)" or extra whitespace from API response
     */
    private fun cleanTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "00:00"
        return raw.split(" ")[0].trim()
    }

    suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        calculationMethod: String,
        calendar: Calendar = Calendar.getInstance()
    ): PrayerFetchResult = withContext(Dispatchers.IO) {
        val methodId = mapMethodToId(calculationMethod)
        val apiDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(calendar.time)

        try {
            val response = apiService.getTimingsByDate(
                date = apiDateFormat,
                latitude = latitude,
                longitude = longitude,
                method = methodId
            )

            if (response.code == 200 && response.data != null) {
                val data = response.data
                val timings = data.timings
                val hijri = data.date.hijri

                val sdfMasehi = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                val masehiDateStr = sdfMasehi.format(calendar.time)

                val hijriDateStr = if (hijri != null && !hijri.day.isNullOrEmpty() && hijri.month != null && !hijri.year.isNullOrEmpty()) {
                    val monthName = hijri.month.ar ?: hijri.month.en ?: "Hijriah"
                    "${hijri.day} $monthName ${hijri.year} H"
                } else {
                    "Hijriah Terkini"
                }

                val prayerTimes = PrayerTimes(
                    date = masehiDateStr,
                    hijriDate = hijriDateStr,
                    imsak = cleanTime(timings.imsak),
                    subuh = cleanTime(timings.fajr),
                    terbit = cleanTime(timings.sunrise),
                    dzuhur = cleanTime(timings.dhuhr),
                    ashar = cleanTime(timings.asr),
                    maghrib = cleanTime(timings.maghrib),
                    isya = cleanTime(timings.isha)
                )

                val enriched = PrayerTimeCalculator.enrichWithCountdown(prayerTimes)
                Log.d(TAG, "Successfully fetched accurate prayer times from Aladhan API for ($latitude, $longitude)")
                return@withContext PrayerFetchResult.Success(enriched, isFromApi = true)
            } else {
                Log.w(TAG, "Aladhan API returned non-200 code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Aladhan API: ${e.message}", e)
        }

        // Offline Astronomical Calculation Fallback
        val fallbackCalculated = PrayerTimeCalculator.calculateTimes(
            latitude = latitude,
            longitude = longitude,
            method = calculationMethod,
            calendar = calendar
        )
        val enrichedFallback = PrayerTimeCalculator.enrichWithCountdown(fallbackCalculated)
        return@withContext PrayerFetchResult.Error(
            message = "Menggunakan kalkulasi offline astronomis",
            fallbackTimes = enrichedFallback
        )
    }
}
