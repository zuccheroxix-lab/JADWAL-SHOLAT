package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.PrayerNotificationLog
import com.example.data.database.WaNotification
import com.example.data.preferences.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaNotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val waDao = db.waNotificationDao()
    private val prayerLogDao = db.prayerNotificationLogDao()
    private val settings = SettingsManager(application)

    private val _selectedTab = MutableStateFlow(0) // 0 = Pesan WA Masuk, 1 = Log Notifikasi Sholat
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlyImportant = MutableStateFlow(false)
    val filterOnlyImportant: StateFlow<Boolean> = _filterOnlyImportant.asStateFlow()

    private val _prayerLogFilter = MutableStateFlow("Semua") // "Semua", "Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya", "Pengingat"
    val prayerLogFilter: StateFlow<String> = _prayerLogFilter.asStateFlow()

    // High performance reactive stream filtering WhatsApp notification listener contents
    val notifications: StateFlow<List<WaNotification>> = combine(
        waDao.getAllNotifications(),
        _searchQuery,
        _filterOnlyImportant
    ) { list, query, showOnlyImportant ->
        list.filter { item ->
            val matchesQuery = item.sender.contains(query, ignoreCase = true) || 
                               item.message.contains(query, ignoreCase = true)
            val matchesImportant = !showOnlyImportant || item.isImportant
            matchesQuery && matchesImportant
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive stream for prayer notification logs
    val prayerLogs: StateFlow<List<PrayerNotificationLog>> = combine(
        prayerLogDao.getAllLogs(),
        _prayerLogFilter
    ) { logs, filter ->
        if (filter == "Semua") {
            logs
        } else if (filter == "Pengingat") {
            logs.filter { it.isPreReminder }
        } else {
            logs.filter { it.prayerName.equals(filter, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setPrayerLogFilter(filter: String) {
        _prayerLogFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilterImportant() {
        _filterOnlyImportant.value = !_filterOnlyImportant.value
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            waDao.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            waDao.clearAllNotifications()
        }
    }

    fun deletePrayerLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            prayerLogDao.deleteLog(id)
        }
    }

    fun clearAllPrayerLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            prayerLogDao.clearAllLogs()
        }
    }

    fun addSamplePrayerLog(prayerName: String = "Dzuhur", isPreReminder: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
            val currentTime = timeFormat.format(Date())
            val title = if (isPreReminder) "10 Menit Menuju $prayerName" else "Waktu Sholat $prayerName Telah Tiba!"
            val msg = if (isPreReminder) {
                "Bersiaplah, sebentar lagi waktu sholat $prayerName akan segera tiba."
            } else {
                "Mari tegakkan sholat tepat waktu. Suara adzan: ${settings.adzanSound}."
            }

            prayerLogDao.insertLog(
                PrayerNotificationLog(
                    prayerName = prayerName,
                    title = title,
                    message = msg,
                    prayerTime = currentTime,
                    timestamp = System.currentTimeMillis(),
                    isPreReminder = isPreReminder,
                    locationName = settings.cityName
                )
            )
        }
    }
}
