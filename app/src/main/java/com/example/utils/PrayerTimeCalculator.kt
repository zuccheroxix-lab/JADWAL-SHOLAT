package com.example.utils

import com.example.data.model.PrayerTimes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

object PrayerTimeCalculator {

    fun calculateTimes(latitude: Double, longitude: Double, method: String, calendar: Calendar): PrayerTimes {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val dateString = sdf.format(calendar.time)

        // Generate dynamic Hijri date estimate
        val hijriDate = convertToHijri(calendar)

        // Standard astronomical algorithm for Equation of Time and Declination
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val timezone = calendar.timeZone.rawOffset / (1000 * 60 * 60).toDouble()

        // Mean anomaly of the Sun (g)
        val g = 357.5291 + 0.98560028 * dayOfYear
        val gRad = Math.toRadians(g)

        // Mean longitude of the Sun (q)
        val q = 280.4597 + 0.98564736 * dayOfYear
        val qRad = Math.toRadians(q)

        // Ecliptic longitude (L)
        val L = q + 1.915 * sin(gRad) + 0.020 * sin(2 * gRad)
        val LRad = Math.toRadians(L)

        // Obliquity of the ecliptic (e)
        val e = 23.439 - 0.00000036 * dayOfYear
        val eRad = Math.toRadians(e)

        // Declination (d)
        val dRad = asin(sin(eRad) * sin(LRad))
        val d = Math.toDegrees(dRad)

        // Right ascension (RA)
        var raRad = atan2(cos(eRad) * sin(LRad), cos(LRad))
        if (raRad < 0) raRad += 2 * PI
        var ra = Math.toDegrees(raRad) / 15.0

        // Equation of Time (EqT) in hours
        val q15 = q / 15.0
        var eqt = q15 - ra
        if (eqt > 20) eqt -= 24.0
        if (eqt < -20) eqt += 24.0

        // Mid Day (Dhuhr)
        // dhuhr = 12 + timezone - longitude/15 - eqt
        val meriton = 15.0 * timezone
        val dhuhrUtc = 12.0 - eqt + (meriton - longitude) / 15.0
        val dhuhrLocalValue = dhuhrUtc

        // Convert the fractional hours to HH:mm format
        fun getFormattedTime(hourFraction: Double): String {
            var h = hourFraction.toInt()
            var m = round((hourFraction - h) * 60).toInt()
            if (m >= 60) {
                h += 1
                m -= 60
            }
            if (h >= 24) h -= 24
            if (h < 0) h += 24
            return String.format(Locale.US, "%02d:%02d", h, m)
        }

        // Angles for Subuh and Isya based on target calculation method
        val subuhAngle = when (method) {
            "Kemenag RI" -> 20.0
            "Umm Al-Qura" -> 18.5
            "Muslim World League" -> 18.0
            "ISNA" -> 15.0
            "Egypt" -> 19.5
            else -> 20.0
        }

        val isyaAngle = when (method) {
            "Kemenag RI" -> 18.0
            "Umm Al-Qura" -> 18.0 // Handles as 90 min after Maghrib elsewhere or uses 18.0 fallback
            "Muslim World League" -> 17.0
            "ISNA" -> 15.0
            "Egypt" -> 17.5
            else -> 18.0
        }

        // Standard sunset/sunrise equation:
        // cos(H) = (-sin(Angle) - sin(Lat)*sin(Dec)) / (cos(Lat)*cos(Dec))
        fun getHourAngle(angle: Double, isSunrise: Boolean): Double {
            val latRad = Math.toRadians(latitude)
            val angleRad = Math.toRadians(if (isSunrise) -angle else angle)
            val cosH = (sin(angleRad) - sin(latRad) * sin(dRad)) / (cos(latRad) * cos(dRad))
            if (cosH > 1.0 || cosH < -1.0) {
                // Out of range (polar regions). Fallback to standard offset durations
                return if (isSunrise) 6.0 else 6.0
            }
            val hRad = acos(cosH)
            return Math.toDegrees(hRad) / 15.0
        }

        // Calculate hours
        val dhuhr = dhuhrLocalValue
        val sunriseH = getHourAngle(0.833, true)
        val sunsetH = getHourAngle(0.833, false)

        val subuhH = getHourAngle(subuhAngle, true)
        val isyaH = getHourAngle(isyaAngle, false)

        // Ashar Angle parameter (Standard / Syafii / Hanbali / Maliki: ratio is 1)
        // cot(A) = 1 + tan(abs(lat - dec))
        val latDecDiff = abs(latitude - d)
        val cotA = 1.0 + tan(Math.toRadians(latDecDiff))
        val asharAngleRad = atan(1.0 / cotA)
        val asharAngle = Math.toDegrees(asharAngleRad)
        val asharH = getHourAngle(asharAngle, false)

        // Combine to local hour positions
        val sunriseHour = dhuhr - sunriseH
        val sunsetHour = dhuhr + sunsetH
        val subuhHour = dhuhr - subuhH
        var isyaHour = dhuhr + isyaH

        // Specific override for Umm Al-Qura which uses fixed 90 min after Maghrib (Sunset)
        if (method == "Umm Al-Qura") {
            isyaHour = sunsetHour + 1.5
        }

        val subuhStr = getFormattedTime(subuhHour)
        val sunriseStr = getFormattedTime(sunriseHour)
        val dhuhrStr = getFormattedTime(dhuhr)
        val asharStr = getFormattedTime(dhuhr + asharH)
        val maghribStr = getFormattedTime(sunsetHour)
        val isyaStr = getFormattedTime(isyaHour)

        // Imsak is usually 10 minutes before Subuh
        val imsakHour = subuhHour - (10.0 / 60.0)
        val imsakStr = getFormattedTime(imsakHour)

        return PrayerTimes(
            date = dateString,
            hijriDate = hijriDate,
            imsak = imsakStr,
            subuh = subuhStr,
            terbit = sunriseStr,
            dzuhur = dhuhrStr,
            ashar = asharStr,
            maghrib = maghribStr,
            isya = isyaStr
        )
    }

    private fun convertToHijri(calendar: Calendar): String {
        // Simple approximate dry conversion for Hijri calendar estimation
        // For real-time updates and full accuracy
        val gDate = calendar.time
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        var jd = if (gMonth <= 2) {
            val y = gYear - 1
            val m = gMonth + 12
            (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + gDay - 1524.5
        } else {
            val y = gYear
            val m = gMonth
            (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + gDay - 1524.5
        }

        // Gregorian calendar correction
        if (jd > 2299160) {
            val a = (gYear / 100)
            val b = a / 4
            jd += 2 - a + b
        }

        // Julian Day to Hijri Epoch
        val l = jd.toInt() - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val r = l - 10631 * n
        val j = ((10985 - r) / 30).toInt()
        val c = ((r - 30 * j + 15) / 30).toInt()
        val hYear = 30 * n + j - 30
        val hMonth = ((10985 - r) / 325).toInt() + 1
        val hDay = r - (325 * (hMonth - 1) / 10).toInt() + c - 2 // calibration offset

        val hijriMonths = arrayOf(
            "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Akhir",
            "Jumadil Awwal", "Jumadil Akhir", "Rajab", "Sya'ban",
            "Ramadhan", "Syawwal", "Dzulqa'dah", "Dzulhijjah"
        )

        // Safety caps
        val finalDay = maxOf(1, minOf(30, hDay))
        val finalMonth = maxOf(1, minOf(12, hMonth))

        return "$finalDay ${hijriMonths[finalMonth - 1]} ${hYear} H"
    }

    fun enrichWithCountdown(times: PrayerTimes): PrayerTimes {
        val parser = SimpleDateFormat("HH:mm", Locale.US)
        val now = Calendar.getInstance()
        val currentStr = String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        val nowTime = parser.parse(currentStr)!!

        val schedule = listOf(
            "Imsak" to parser.parse(times.imsak)!!,
            "Subuh" to parser.parse(times.subuh)!!,
            "Terbit" to parser.parse(times.terbit)!!,
            "Dzuhur" to parser.parse(times.dzuhur)!!,
            "Ashar" to parser.parse(times.ashar)!!,
            "Maghrib" to parser.parse(times.maghrib)!!,
            "Isya" to parser.parse(times.isya)!!
        )

        // Find next prayer
        var nextName = "Subuh"
        var nextTarget = schedule[1].second // defaulting to Subuh
        var found = false

        for (item in schedule) {
            if (item.second.after(nowTime)) {
                nextName = item.first
                nextTarget = item.second
                found = true
                break
            }
        }

        if (!found) {
            // Next prayer is Imsak/Subuh tomorrow
            nextName = "Imsak"
            nextTarget = schedule[0].second
        }

        // Calculate countdown seconds
        val diffMs = if (nextTarget.time >= nowTime.time) {
            nextTarget.time - nowTime.time
        } else {
            // crosses midnight
            (24 * 60 * 60 * 1000) - (nowTime.time - nextTarget.time)
        }

        val countdownSecs = diffMs / 1000

        // calculate progress
        val totalSecsIn24h = 24.0 * 60 * 60
        val elapsedTime = totalSecsIn24h - countdownSecs
        val progressVal = (elapsedTime / totalSecsIn24h).toFloat().coerceIn(0f, 1f)

        return times.copy(
            nextPrayerName = nextName,
            nextPrayerTime = SimpleDateFormat("HH:mm", Locale.US).format(nextTarget),
            countdownSeconds = countdownSecs,
            progress = progressVal
        )
    }
}
