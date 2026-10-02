package com.example.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Data model for GPS Coordinates and resolved address/city name
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val cityName: String = "Lokasi Saya (GPS)"
)

/**
 * LocationService uses Google Play Services Location API (FusedLocationProviderClient)
 * to retrieve the user's real-time GPS coordinates for accurate prayer time calculations.
 */
class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationService"
        
        // Default coordinates: Jakarta Pusat (Indonesia) as graceful fallback
        const val DEFAULT_LATITUDE = -6.1754
        const val DEFAULT_LONGITUDE = 106.8272
        const val DEFAULT_CITY_NAME = "Jakarta Pusat"
    }

    /**
     * Check if the app has been granted location permissions
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Fetch the user's current GPS location with high accuracy.
     * Falls back to lastKnownLocation if immediate GPS fix is null.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(): UserLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted. Cannot retrieve GPS coordinates.")
            return@withContext null
        }

        try {
            // First attempt: Request fresh high accuracy location fix
            val freshLocation = getFreshLocation()
            if (freshLocation != null) {
                val resolvedCity = resolveCityName(freshLocation.latitude, freshLocation.longitude)
                return@withContext UserLocation(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    accuracy = freshLocation.accuracy,
                    cityName = resolvedCity
                )
            }

            // Second attempt: Fallback to last known location cached by Play Services
            val lastLoc = getLastKnownLocation()
            if (lastLoc != null) {
                val resolvedCity = resolveCityName(lastLoc.latitude, lastLoc.longitude)
                return@withContext UserLocation(
                    latitude = lastLoc.latitude,
                    longitude = lastLoc.longitude,
                    accuracy = lastLoc.accuracy,
                    cityName = resolvedCity
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring location from Google Play Services Location API", e)
        }

        return@withContext null
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (continuation.isActive) {
                continuation.resume(location)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "getCurrentLocation failed: ${exception.message}", exception)
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "lastLocation failed: ${exception.message}", exception)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
    }

    /**
     * Resolves human-readable city or administrative area from coordinates using Android Geocoder.
     */
    fun resolveCityName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val locality = address.subAdminArea ?: address.locality ?: address.adminArea
                locality ?: "Lokasi GPS (${String.format(Locale.US, "%.2f, %.2f", latitude, longitude)})"
            } else {
                "Lokasi GPS (${String.format(Locale.US, "%.2f, %.2f", latitude, longitude)})"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reverse geocoding failed or offline: ${e.message}")
            "Lokasi GPS (${String.format(Locale.US, "%.2f, %.2f", latitude, longitude)})"
        }
    }
}
