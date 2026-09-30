package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "zucchero_prayer_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CALC_METHOD = "calc_method"
        private const val KEY_ADZAN_SOUND = "adzan_sound"
        private const val KEY_NOTIF_ADZAN = "notif_adzan"
        private const val KEY_NOTIF_PRE_REMINDER = "notif_pre_reminder"
        private const val KEY_NOTIF_FASTING_SUNNAH = "notif_fasting_sunnah"
        private const val KEY_USE_GPS = "use_gps"
        private const val KEY_CITY_NAME = "city_name"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_LANGUAGE = "language"
    }

    var darkMode: String
        get() = prefs.getString(KEY_DARK_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value).apply()

    var calculationMethod: String
        get() = prefs.getString(KEY_CALC_METHOD, "Kemenag RI") ?: "Kemenag RI"
        set(value) = prefs.edit().putString(KEY_CALC_METHOD, value).apply()

    var adzanSound: String
        get() = prefs.getString(KEY_ADZAN_SOUND, "Mekkah") ?: "Mekkah"
        set(value) = prefs.edit().putString(KEY_ADZAN_SOUND, value).apply()

    var isAdzanNotifEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ADZAN, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ADZAN, value).apply()

    var isPreReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_PRE_REMINDER, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_PRE_REMINDER, value).apply()

    var isFastingSunnahNotifEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_FASTING_SUNNAH, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_FASTING_SUNNAH, value).apply()

    var useGps: Boolean
        get() = prefs.getBoolean(KEY_USE_GPS, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_GPS, value).apply()

    var cityName: String
        get() = prefs.getString(KEY_CITY_NAME, "Jakarta") ?: "Jakarta"
        set(value) = prefs.edit().putString(KEY_CITY_NAME, value).apply()

    var latitude: Float
        get() = prefs.getFloat(KEY_LATITUDE, -6.2000f)
        set(value) = prefs.edit().putFloat(KEY_LATITUDE, value).apply()

    var longitude: Float
        get() = prefs.getFloat(KEY_LONGITUDE, 106.8166f)
        set(value) = prefs.edit().putFloat(KEY_LONGITUDE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "id") ?: "id"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()
}
