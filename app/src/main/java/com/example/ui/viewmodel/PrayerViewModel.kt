package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.PrayerNotificationLog
import com.example.data.database.PrayerLogRepository
import com.example.data.model.PrayerTimes
import com.example.data.preferences.SettingsManager
import com.example.services.AdzanReceiver
import com.example.services.LocationService
import com.example.utils.AlarmScheduler
import com.example.utils.PrayerTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BrowserNotificationAlert(
    val id: String,
    val prayerName: String,
    val title: String,
    val message: String,
    val time: String,
    val iconType: String = "PRAYER" // "PRAYER", "PRE_REMINDER", "INFO"
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "PrayerViewModel"
    private val settings = SettingsManager(application)
    private val locationService = LocationService(application)
    private val prayerLogRepo = PrayerLogRepository(application)

    private val _prayerTimes = MutableStateFlow<PrayerTimes?>(null)
    val prayerTimes: StateFlow<PrayerTimes?> = _prayerTimes.asStateFlow()

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentLocationName = MutableStateFlow("")
    val currentLocationName: StateFlow<String> = _currentLocationName.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _dataSourceInfo = MutableStateFlow("GPS & Kalender Astronomis")
    val dataSourceInfo: StateFlow<String> = _dataSourceInfo.asStateFlow()

    private val _isFromApi = MutableStateFlow(true)
    val isFromApi: StateFlow<Boolean> = _isFromApi.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _activeBrowserNotification = MutableStateFlow<BrowserNotificationAlert?>(null)
    val activeBrowserNotification: StateFlow<BrowserNotificationAlert?> = _activeBrowserNotification.asStateFlow()

    val prayerLogs: StateFlow<List<PrayerNotificationLog>> = prayerLogRepo.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        _currentLocationName.value = settings.cityName
        startClockAndCountdown()
        loadPrayerTimes()
    }

    private fun startClockAndCountdown() {
        viewModelScope.launch {
            while (true) {
                val now = Calendar.getInstance()
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
                val timeWithSecs = sdf.format(now.time)
                _currentTime.value = timeWithSecs

                // Tick the countdown
                _prayerTimes.value?.let { currentTimes ->
                    _prayerTimes.value = PrayerTimeCalculator.enrichWithCountdown(currentTimes)

                    // Trigger real-time browser alerts when match is found at start of a minute
                    if (now.get(Calendar.SECOND) == 0) {
                        val currentMinuteStr = String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))

                        val matchingPrayer = when (currentMinuteStr) {
                            currentTimes.subuh -> "Subuh"
                            currentTimes.dzuhur -> "Dzuhur"
                            currentTimes.ashar -> "Ashar"
                            currentTimes.maghrib -> "Maghrib"
                            currentTimes.isya -> "Isya"
                            currentTimes.imsak -> "Imsak"
                            else -> null
                        }

                        if (matchingPrayer != null) {
                            if (settings.isAdzanNotifEnabled) {
                                triggerBrowserNotification(matchingPrayer, currentMinuteStr, isPreReminder = false)
                            }
                        } else if (settings.isPreReminderEnabled) {
                            // Support pre-reminder exactly 10 minutes before
                            val preReminderMatchingPrayer = when (currentMinuteStr) {
                                getMinutesOffset(currentTimes.subuh, -10) -> "Subuh"
                                getMinutesOffset(currentTimes.dzuhur, -10) -> "Dzuhur"
                                getMinutesOffset(currentTimes.ashar, -10) -> "Ashar"
                                getMinutesOffset(currentTimes.maghrib, -10) -> "Maghrib"
                                getMinutesOffset(currentTimes.isya, -10) -> "Isya"
                                else -> null
                            }
                            if (preReminderMatchingPrayer != null) {
                                triggerBrowserNotification(preReminderMatchingPrayer, currentMinuteStr, isPreReminder = true)
                            }
                        }
                    }
                }

                delay(1000)
            }
        }
    }

    private fun getMinutesOffset(timeStr: String, offsetMinutes: Int): String {
        return try {
            val parser = SimpleDateFormat("HH:mm", Locale.US)
            val date = parser.parse(timeStr) ?: return ""
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MINUTE, offsetMinutes)
            }
            parser.format(cal.time)
        } catch (e: Exception) {
            ""
        }
    }

    fun triggerBrowserNotification(prayerName: String, timeStr: String, isPreReminder: Boolean = false) {
        val title: String
        val message: String
        if (isPreReminder) {
            title = "10 Menit Menuju $prayerName"
            message = "Bersiaplah, sebentar lagi waktu sholat $prayerName akan segera tiba."
        } else {
            title = "Waktu Sholat $prayerName Telah Tiba!"
            message = "Mari tegakkan sholat tepat waktu. Suara adzan: ${settings.adzanSound}."
        }

        _activeBrowserNotification.value = BrowserNotificationAlert(
            id = UUID.randomUUID().toString(),
            prayerName = prayerName,
            title = title,
            message = message,
            time = timeStr,
            iconType = if (isPreReminder) "PRE_REMINDER" else "PRAYER"
        )

        // Directly log to local Room database
        viewModelScope.launch(Dispatchers.IO) {
            prayerLogRepo.logNotification(
                prayerName = prayerName,
                title = title,
                message = message,
                prayerTime = timeStr,
                isPreReminder = isPreReminder,
                locationName = _currentLocationName.value
            )
        }

        // Fire background Broadcast to also register native Android notification
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, AdzanReceiver::class.java).apply {
                putExtra("PRAYER_NAME", prayerName)
                putExtra("IS_PRE_REMINDER", isPreReminder)
                putExtra("EXTRA_FROM_VIEWMODEL", true)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch Broadcast to AdzanReceiver", e)
        }
    }

    fun triggerTestBrowserNotification(prayerName: String = "Dzuhur") {
        val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        triggerBrowserNotification(prayerName, timeStr, isPreReminder = false)
    }

    fun dismissBrowserNotification() {
        _activeBrowserNotification.value = null
    }

    fun deletePrayerLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            prayerLogRepo.deleteLog(id)
        }
    }

    fun clearPrayerLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            prayerLogRepo.clearAllLogs()
        }
    }

    /**
     * Primary loader that calculates prayer times based on saved coordinates.
     */
    fun loadPrayerTimes() {
        fetchPrayerTimes(
            latitude = settings.latitude.toDouble(),
            longitude = settings.longitude.toDouble(),
            locationLabel = settings.cityName
        )
    }

    /**
     * Calculates prayer times with high astronomical precision using current coordinates and method
     */
    private fun fetchPrayerTimes(latitude: Double, longitude: Double, locationLabel: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val nowTimeFormat = SimpleDateFormat("HH:mm", Locale.US).format(Date())

            try {
                val cal = Calendar.getInstance()
                val calculatedTimes = PrayerTimeCalculator.calculateTimes(
                    latitude = latitude,
                    longitude = longitude,
                    method = settings.calculationMethod,
                    calendar = cal
                )
                val enrichedTimes = PrayerTimeCalculator.enrichWithCountdown(calculatedTimes)

                _prayerTimes.value = enrichedTimes
                _isFromApi.value = true
                _dataSourceInfo.value = if (settings.useGps) "GPS Akurat (${settings.calculationMethod})" else "Koordinat: $locationLabel"
                _lastSyncTime.value = nowTimeFormat
                Log.d(TAG, "Prayer times calculated successfully for $locationLabel")

                // Reschedule device alarms for freshly synced times
                try {
                    AlarmScheduler.scheduleAlarms(getApplication())
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling alarms", e)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating prayer times", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Request live GPS location from Google Play Services Location Provider
     * using the LocationService class to calculate accurate prayer times.
     */
    fun requestGpsLocation(onLocationPermissionNeeded: () -> Unit) {
        if (!locationService.hasLocationPermission()) {
            Log.w(TAG, "Location permission missing, requesting permission from user.")
            onLocationPermissionNeeded()
            return
        }

        settings.useGps = true
        _isRefreshing.value = true

        viewModelScope.launch {
            val userLocation = locationService.getCurrentGpsLocation()
            if (userLocation != null) {
                settings.latitude = userLocation.latitude.toFloat()
                settings.longitude = userLocation.longitude.toFloat()
                val locLabel = userLocation.cityName
                settings.cityName = locLabel
                _currentLocationName.value = locLabel

                Log.d(TAG, "GPS location acquired via LocationService: Lat=${userLocation.latitude}, Lng=${userLocation.longitude}, City=$locLabel")
                fetchPrayerTimes(userLocation.latitude, userLocation.longitude, locLabel)
            } else {
                Log.w(TAG, "LocationService returned null, fallback to stored coordinates.")
                fetchPrayerTimes(settings.latitude.toDouble(), settings.longitude.toDouble(), settings.cityName)
            }
            _isRefreshing.value = false
        }
    }

    fun selectManualLocation(cityName: String, lat: Double, lng: Double) {
        settings.useGps = false
        settings.cityName = cityName
        settings.latitude = lat.toFloat()
        settings.longitude = lng.toFloat()
        _currentLocationName.value = cityName
        fetchPrayerTimes(lat, lng, cityName)
    }

    fun forceRefreshAladhanApi() {
        if (settings.useGps) {
            requestGpsLocation { }
        } else {
            loadPrayerTimes()
        }
    }
}
