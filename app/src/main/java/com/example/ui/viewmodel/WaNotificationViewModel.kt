package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.WaNotification
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WaNotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val waDao = db.waNotificationDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlyImportant = MutableStateFlow(false)
    val filterOnlyImportant: StateFlow<Boolean> = _filterOnlyImportant.asStateFlow()

    // High performance reactive stream filtering database contents based on query and filters
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilterImportant() {
        _filterOnlyImportant.value = !_filterOnlyImportant.value
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            waDao.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            waDao.clearAllNotifications()
        }
    }
}
