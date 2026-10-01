package com.example.data.network.aladhan

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApiService {

    /**
     * Fetch prayer times by date and GPS coordinates
     * Endpoint: https://api.aladhan.com/v1/timings/{date}?latitude={lat}&longitude={lng}&method={method}
     * @param date Date formatted as dd-MM-yyyy (e.g. 01-10-2026) or timestamp
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param method Calculation method ID (e.g. 20 for Kemenag RI, 4 for Umm Al-Qura)
     */
    @GET("v1/timings/{date}")
    suspend fun getTimingsByDate(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int? = 20
    ): Response<AladhanResponse>

    /**
     * Fetch prayer times with current timestamp
     */
    @GET("v1/timings/{timestamp}")
    suspend fun getTimingsByTimestamp(
        @Path("timestamp") timestamp: Long,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int? = 20
    ): Response<AladhanResponse>
}
