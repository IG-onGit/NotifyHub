package com.notifyhub.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A "registered software" connection as described in the app's Projects screen.
 * [projectKey] is the stable slug that the user's own backend scripts must send
 * back in each pushed notification so the app knows which project it belongs to,
 * and therefore which icon/color/name to render it with.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectKey: String,
    val name: String,
    val iconKey: String,
    val colorHex: String, // e.g. "#6750A4"
    val createdAtMillis: Long = System.currentTimeMillis()
)
