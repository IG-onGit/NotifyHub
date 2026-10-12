package com.notifyhub.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single notification fetched from the server and stored locally.
 * [remoteId] is the id the server assigned, used both to de-duplicate on
 * repeated polls and, optionally, to acknowledge/delete it server-side.
 */
@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey
    val remoteId: String,
    val projectKey: String,
    val title: String,
    val message: String,
    val timestampMillis: Long,
    val cleared: Boolean = false,
    val insertedAtMillis: Long = System.currentTimeMillis()
)
