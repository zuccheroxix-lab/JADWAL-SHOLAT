package com.example.data.network.aladhan

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AladhanResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: AladhanData?
)

@JsonClass(generateAdapter = true)
data class AladhanData(
    @Json(name = "timings") val timings: AladhanTimings,
    @Json(name = "date") val date: AladhanDate,
    @Json(name = "meta") val meta: AladhanMeta? = null
)

@JsonClass(generateAdapter = true)
data class AladhanTimings(
    @Json(name = "Fajr") val fajr: String,
    @Json(name = "Sunrise") val sunrise: String,
    @Json(name = "Dhuhr") val dhuhr: String,
    @Json(name = "Asr") val asr: String,
    @Json(name = "Sunset") val sunset: String? = null,
    @Json(name = "Maghrib") val maghrib: String,
    @Json(name = "Isha") val isha: String,
    @Json(name = "Imsak") val imsak: String? = null,
    @Json(name = "Midnight") val midnight: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanDate(
    @Json(name = "readable") val readable: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "hijri") val hijri: AladhanHijri? = null,
    @Json(name = "gregorian") val gregorian: AladhanGregorian? = null
)

@JsonClass(generateAdapter = true)
data class AladhanHijri(
    @Json(name = "date") val date: String? = null,
    @Json(name = "day") val day: String? = null,
    @Json(name = "weekday") val weekday: AladhanWeekday? = null,
    @Json(name = "month") val month: AladhanMonth? = null,
    @Json(name = "year") val year: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanWeekday(
    @Json(name = "en") val en: String? = null,
    @Json(name = "ar") val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanMonth(
    @Json(name = "number") val number: Int? = null,
    @Json(name = "en") val en: String? = null,
    @Json(name = "ar") val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanGregorian(
    @Json(name = "date") val date: String? = null,
    @Json(name = "day") val day: String? = null,
    @Json(name = "weekday") val weekday: AladhanWeekday? = null,
    @Json(name = "month") val month: AladhanMonth? = null,
    @Json(name = "year") val year: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanMeta(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timezone") val timezone: String? = null
)
