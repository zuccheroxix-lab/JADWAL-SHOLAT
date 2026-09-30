package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.preferences.SettingsManager
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsManager(application)

    private val _darkMode = MutableStateFlow(settings.darkMode)
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()

    private val _calcMethod = MutableStateFlow(settings.calculationMethod)
    val calcMethod: StateFlow<String> = _calcMethod.asStateFlow()

    private val _adzanSound = MutableStateFlow(settings.adzanSound)
    val adzanSound: StateFlow<String> = _adzanSound.asStateFlow()

    private val _isAdzanNotifEnabled = MutableStateFlow(settings.isAdzanNotifEnabled)
    val isAdzanNotifEnabled: StateFlow<Boolean> = _isAdzanNotifEnabled.asStateFlow()

    private val _isPreReminderEnabled = MutableStateFlow(settings.isPreReminderEnabled)
    val isPreReminderEnabled: StateFlow<Boolean> = _isPreReminderEnabled.asStateFlow()

    private val _isFastingSunnahEnabled = MutableStateFlow(settings.isFastingSunnahNotifEnabled)
    val isFastingSunnahEnabled: StateFlow<Boolean> = _isFastingSunnahEnabled.asStateFlow()

    private val _language = MutableStateFlow(settings.language)
    val language: StateFlow<String> = _language.asStateFlow()

    fun updateDarkMode(mode: String) {
        settings.darkMode = mode
        _darkMode.value = mode
    }

    fun updateCalculationMethod(method: String) {
        settings.calculationMethod = method
        _calcMethod.value = method
        AlarmScheduler.scheduleAlarms(getApplication())
    }

    fun updateAdzanSound(sound: String) {
        settings.adzanSound = sound
        _adzanSound.value = sound
    }

    fun toggleAdzanNotif() {
        val next = !settings.isAdzanNotifEnabled
        settings.isAdzanNotifEnabled = next
        _isAdzanNotifEnabled.value = next
        AlarmScheduler.scheduleAlarms(getApplication())
    }

    fun togglePreReminder() {
        val next = !settings.isPreReminderEnabled
        settings.isPreReminderEnabled = next
        _isPreReminderEnabled.value = next
        AlarmScheduler.scheduleAlarms(getApplication())
    }

    fun toggleFastingSunnah() {
        val next = !settings.isFastingSunnahNotifEnabled
        settings.isFastingSunnahNotifEnabled = next
        _isFastingSunnahEnabled.value = next
    }

    fun updateLanguage(lang: String) {
        settings.language = lang
        _language.value = lang
    }
}
