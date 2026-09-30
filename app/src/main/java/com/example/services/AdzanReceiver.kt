package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.database.PrayerNotificationLog
import com.example.data.preferences.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdzanReceiver : BroadcastReceiver() {
    private val TAG = "AdzanReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Sholat"
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)

        Log.d(TAG, "Adzan alarm received for: $prayerName, IsPreReminder: $isPreReminder")

        val settings = SettingsManager(context)

        // Validate state
        if (!settings.isAdzanNotifEnabled) {
            Log.d(TAG, "Adzan notifications are disabled in settings. Skipping notification.")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "zucchero_prayer_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pengingat Waktu Sholat"
            val descriptionText = "Menampilkan notifikasi jadwal sholat dan adzan"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        if (isPreReminder) {
            title = "10 Menit Menuju $prayerName"
            text = "Bersiaplah, sebentar lagi waktu sholat $prayerName makan segera tiba."
        } else {
            title = "Waktu Sholat $prayerName Telah Tiba!"
            text = "Mari tegakkan sholat tepat waktu. Suara adzan: ${settings.adzanSound}."
        }

        // Setup sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))

        notificationManager.notify(prayerName.hashCode(), builder.build())

        // Save to local PrayerNotificationLog Room Database if triggered from background alarm
        val fromViewModel = intent.getBooleanExtra("EXTRA_FROM_VIEWMODEL", false)
        if (!fromViewModel) {
            val currentTimeFormatted = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = com.example.data.database.AppDatabase.getDatabase(context)
                    db.prayerNotificationLogDao().insertLog(
                        com.example.data.database.PrayerNotificationLog(
                            prayerName = prayerName,
                            title = title,
                            message = text,
                            prayerTime = currentTimeFormatted,
                            timestamp = System.currentTimeMillis(),
                            isPreReminder = isPreReminder,
                            locationName = settings.cityName
                        )
                    )
                    Log.d(TAG, "Successfully logged prayer notification to Room DB: $prayerName")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log prayer notification to Room DB", e)
                }
            }
        }
    }
}
