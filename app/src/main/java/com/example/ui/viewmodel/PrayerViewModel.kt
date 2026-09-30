package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.data.model.PrayerTimes
import com.example.data.preferences.SettingsManager
import com.example.services.AdzanReceiver
import com.example.utils.AlarmScheduler
import com.example.utils.PrayerTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _prayerTimes = MutableStateFlow<PrayerTimes?>(null)
    val prayerTimes: StateFlow<PrayerTimes?> = _prayerTimes.asStateFlow()

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentLocationName = MutableStateFlow("")
    val currentLocationName: StateFlow<String> = _currentLocationName.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _activeBrowserNotification = MutableStateFlow<BrowserNotificationAlert?>(null)
    val activeBrowserNotification: StateFlow<BrowserNotificationAlert?> = _activeBrowserNotification.asStateFlow()

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
            message = "Bersiaplah, sebentar lagi waktu sholat $prayerName makan segera tiba."
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

        // Fire background Broadcast to also register native Android notification
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, AdzanReceiver::class.java).apply {
                putExtra("PRAYER_NAME", prayerName)
                putExtra("IS_PRE_REMINDER", isPreReminder)
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

    fun loadPrayerTimes() {
        viewModelScope.launch(Dispatchers.Default) {
            val cal = Calendar.getInstance()
            val baseTimes = PrayerTimeCalculator.calculateTimes(
                settings.latitude.toDouble(),
                settings.longitude.toDouble(),
                settings.calculationMethod,
                cal
            )
            val enriched = PrayerTimeCalculator.enrichWithCountdown(baseTimes)
            _prayerTimes.value = enriched

            // Reschedule device alarms for fresh times
            AlarmScheduler.scheduleAlarms(getApplication())
        }
    }

    @SuppressLint("MissingPermission")
    fun requestGpsLocation(onLocationPermissionNeeded: () -> Unit) {
        if (!settings.useGps) return

        _isRefreshing.value = true
        val cancellationTokenSource = CancellationTokenSource()

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    settings.latitude = location.latitude.toFloat()
                    settings.longitude = location.longitude.toFloat()
                    settings.cityName = "Lokasi Saya (GPS)"
                    _currentLocationName.value = settings.cityName

                    Log.d(TAG, "GPS location parsed: Lat: ${location.latitude}, Lng: ${location.longitude}")
                    loadPrayerTimes()
                } else {
                    Log.d(TAG, "GPS location returned null, falling back or trying last location.")
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            settings.latitude = lastLoc.latitude.toFloat()
                            settings.longitude = lastLoc.longitude.toFloat()
                            settings.cityName = "Lokasi Saya (GPS)"
                            _currentLocationName.value = settings.cityName
                            loadPrayerTimes()
                        }
                    }
                }
                _isRefreshing.value = false
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get current GPS location.", e)
                _isRefreshing.value = false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing or disabled.", e)
            onLocationPermissionNeeded()
            _isRefreshing.value = false
        }
    }

    fun selectManualLocation(cityName: String, lat: Double, lng: Double) {
        settings.useGps = false
        settings.cityName = cityName
        settings.latitude = lat.toFloat()
        settings.longitude = lng.toFloat()
        _currentLocationName.value = cityName
        loadPrayerTimes()
    }
}
