package com.example.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.WaNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "WaListenerService"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        // Check if the source application is WhatsApp
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            try {
                val notification = sbn.notification
                val extras = notification.extras ?: return

                val senderObj = extras.get(Notification.EXTRA_TITLE)
                val messageObj = extras.get(Notification.EXTRA_TEXT)

                val sender = senderObj?.toString() ?: "Seseorang"
                val message = messageObj?.toString() ?: ""

                // Filter out empty or duplicate messages e.g. "Calling...", "New messages"
                if (message.isEmpty() || message.contains("pesan baru") || message.contains("Checking for new messages")) {
                    return
                }

                // Custom filters for important messages
                val importantKeywords = listOf(
                    "sholat", "shalat", "adzan", "urgent", "darurat", "penting", 
                    "cepat", "tolong", "bantuan", "info", "masjid", "ibadah", 
                    "doa", "pengingat", "penting!", "segera", "rapat", "kerja"
                )
                val isImportant = importantKeywords.any { keyword ->
                    message.lowercase().contains(keyword) || sender.lowercase().contains(keyword)
                }

                val timestamp = sbn.postTime
                val waNotification = WaNotification(
                    sender = sender,
                    message = message,
                    timestamp = timestamp,
                    isImportant = isImportant
                )

                // Save to Room DB
                scope.launch {
                    val db = AppDatabase.getDatabase(applicationContext)
                    db.waNotificationDao().insertNotification(waNotification)
                    Log.d(TAG, "Successfully captured WhatsApp notification: Sender: $sender, Message: $message, IsImportant: $isImportant")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing status bar notification: ", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
