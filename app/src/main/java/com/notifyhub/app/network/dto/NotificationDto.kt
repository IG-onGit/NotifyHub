package com.notifyhub.app.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire format for GET /api/notifications. See the project README for the
 * full contract your own server must implement.
 */
@JsonClass(generateAdapter = true)
data class NotificationsResponse(
    val notifications: List<NotificationDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    @Json(name = "projectKey") val projectKey: String,
    val title: String,
    val message: String,
    /** Epoch milliseconds. */
    val timestamp: Long
)
