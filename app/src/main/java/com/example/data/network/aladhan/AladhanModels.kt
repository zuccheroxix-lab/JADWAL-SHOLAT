package com.example.data.network.aladhan

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AladhanResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: AladhanData? = null
)

@JsonClass(generateAdapter = true)
data class AladhanData(
    @Json(name = "timings") val timings: AladhanTimings? = null,
    @Json(name = "date") val date: AladhanDate? = null,
    @Json(name = "meta") val meta: AladhanMeta? = null
)

@JsonClass(generateAdapter = true)
data class AladhanTimings(
    @Json(name = "Fajr") val fajr: String? = null,
    @Json(name = "Sunrise") val sunrise: String? = null,
    @Json(name = "Dhuhr") val dhuhr: String? = null,
    @Json(name = "Asr") val asr: String? = null,
    @Json(name = "Sunset") val sunset: String? = null,
    @Json(name = "Maghrib") val maghrib: String? = null,
    @Json(name = "Isha") val isha: String? = null,
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
    @Json(name = "month") val month: AladhanHijriMonth? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "designation") val designation: AladhanDesignation? = null
)

@JsonClass(generateAdapter = true)
data class AladhanHijriMonth(
    @Json(name = "number") val number: Int? = null,
    @Json(name = "en") val en: String? = null,
    @Json(name = "ar") val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanDesignation(
    @Json(name = "abbreviated") val abbreviated: String? = null,
    @Json(name = "expanded") val expanded: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanGregorian(
    @Json(name = "date") val date: String? = null,
    @Json(name = "day") val day: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanMeta(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "method") val method: AladhanMethodMeta? = null
)

@JsonClass(generateAdapter = true)
data class AladhanMethodMeta(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null
)
