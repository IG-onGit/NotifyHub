package com.notifyhub.app.network

import com.notifyhub.app.network.dto.NotificationsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    /**
     * Fetch notifications created after [since] (epoch millis, exclusive).
     * Pass 0 to fetch everything the server still has.
     */
    @GET("api/notifications")
    suspend fun getNotifications(@Query("since") since: Long): NotificationsResponse
}
