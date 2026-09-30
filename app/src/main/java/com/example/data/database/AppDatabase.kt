package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "wa_notifications")
data class WaNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isImportant: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long
)

@Dao
interface WaNotificationDao {
    @Query("SELECT * FROM wa_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<WaNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: WaNotification)

    @Query("DELETE FROM wa_notifications")
    suspend fun clearAllNotifications()

    @Query("DELETE FROM wa_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Database(entities = [WaNotification::class, ChatMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waNotificationDao(): WaNotificationDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zucchero_prayer_ai_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
