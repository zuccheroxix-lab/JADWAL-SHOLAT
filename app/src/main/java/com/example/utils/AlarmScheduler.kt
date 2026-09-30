package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.preferences.SettingsManager
import com.example.services.AdzanReceiver
import java.text.SimpleDateFormat
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAlarms(context: Context) {
        val settings = SettingsManager(context)
        if (!settings.isAdzanNotifEnabled) {
            cancelAllAlarms(context)
            Log.d(TAG, "Alarms scheduled canceled because adzan notification is turned off.")
            return
        }

        // Calculate prayer times for today and tomorrow
        val calendar = Calendar.getInstance()
        val timesToday = PrayerTimeCalculator.calculateTimes(
            settings.latitude.toDouble(),
            settings.longitude.toDouble(),
            settings.calculationMethod,
            calendar
        )

        val nextCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val timesTomorrow = PrayerTimeCalculator.calculateTimes(
            settings.latitude.toDouble(),
            settings.longitude.toDouble(),
            settings.calculationMethod,
            nextCalendar
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        fun schedulePrayerAlarm(prayerName: String, timeStr: String, isTomorrow: Boolean) {
            try {
                val parser = SimpleDateFormat("HH:mm", Locale.US)
                val timeDate = parser.parse(timeStr) ?: return

                val targetCal = Calendar.getInstance().apply {
                    if (isTomorrow) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    set(Calendar.HOUR_OF_DAY, timeDate.hours)
                    set(Calendar.MINUTE, timeDate.minutes)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If scheduled time is in the past, skip it
                if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                    return
                }

                val intent = Intent(context, AdzanReceiver::class.java).apply {
                    putExtra("PRAYER_NAME", prayerName)
                    putExtra("IS_PRE_REMINDER", false)
                }

                val requestCode = (prayerName + (if (isTomorrow) "_t" else "")).hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetCal.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            targetCal.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                }

                Log.d(TAG, "Scheduled alarm for $prayerName in millis: ${targetCal.timeInMillis} ($timeStr)")

                // Schedule Pre-Reminder 10 minutes before
                if (settings.isPreReminderEnabled) {
                    val preReminderCal = (targetCal.clone() as Calendar).apply {
                        add(Calendar.MINUTE, -10)
                    }
                    if (preReminderCal.timeInMillis > System.currentTimeMillis()) {
                        val preIntent = Intent(context, AdzanReceiver::class.java).apply {
                            putExtra("PRAYER_NAME", prayerName)
                            putExtra("IS_PRE_REMINDER", true)
                        }
                        val prePendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode + 1000,
                            preIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    preReminderCal.timeInMillis,
                                    prePendingIntent
                                )
                            } else {
                                alarmManager.set(
                                    AlarmManager.RTC_WAKEUP,
                                    preReminderCal.timeInMillis,
                                    prePendingIntent
                                )
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    preReminderCal.timeInMillis,
                                    prePendingIntent
                            )
                        }
                        Log.d(TAG, "Scheduled 10-minutes pre-reminder for $prayerName at: ${preReminderCal.timeInMillis}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm for $prayerName: ", e)
            }
        }

        // Schedule all prayer times for today and tomorrow
        val prayers = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")

        for (name in prayers) {
            val timeStr = when (name) {
                "Subuh" -> timesToday.subuh
                "Dzuhur" -> timesToday.dzuhur
                "Ashar" -> timesToday.ashar
                "Maghrib" -> timesToday.maghrib
                else -> timesToday.isya
            }
            schedulePrayerAlarm(name, timeStr, false)

            val timeStrTomorrow = when (name) {
                "Subuh" -> timesTomorrow.subuh
                "Dzuhur" -> timesTomorrow.dzuhur
                "Ashar" -> timesTomorrow.ashar
                "Maghrib" -> timesTomorrow.maghrib
                else -> timesTomorrow.isya
            }
            schedulePrayerAlarm(name, timeStrTomorrow, true)
        }
    }

    private fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prayers = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")

        for (prayerName in prayers) {
            fun cancelAlarm(requestCode: Int) {
                val intent = Intent(context, AdzanReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            }

            cancelAlarm(prayerName.hashCode())
            cancelAlarm((prayerName + "_t").hashCode())
            cancelAlarm(prayerName.hashCode() + 1000)
            cancelAlarm((prayerName + "_t").hashCode() + 1000)
        }
        Log.d(TAG, "All active alarms cancelled successfully.")
    }
}
