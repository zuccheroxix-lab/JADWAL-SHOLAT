package com.example.data.database

import android.content.Context
import kotlinx.coroutines.flow.Flow

class PrayerLogRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).prayerNotificationLogDao()

    val allLogs: Flow<List<PrayerNotificationLog>> = dao.getAllLogs()

    suspend fun logNotification(
        prayerName: String,
        title: String,
        message: String,
        prayerTime: String,
        isPreReminder: Boolean = false,
        locationName: String = ""
    ) {
        val entry = PrayerNotificationLog(
            prayerName = prayerName,
            title = title,
            message = message,
            prayerTime = prayerTime,
            timestamp = System.currentTimeMillis(),
            isPreReminder = isPreReminder,
            locationName = locationName
        )
        dao.insertLog(entry)
    }

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)

    suspend fun clearAllLogs() = dao.clearAllLogs()
}
