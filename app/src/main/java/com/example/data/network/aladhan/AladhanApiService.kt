package com.example.data.network.aladhan

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit Service Interface for Aladhan Islamic Prayer Times API
 */
interface AladhanApiService {

    /**
     * Get prayer times for a given date formatted as DD-MM-YYYY and exact GPS coordinates
     */
    @GET("v1/timings/{date}")
    suspend fun getTimingsByDate(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int? = 20
    ): AladhanResponse

    /**
     * Get current day prayer times for given GPS coordinates
     */
    @GET("v1/timings")
    suspend fun getTimings(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int? = 20
    ): AladhanResponse
}
