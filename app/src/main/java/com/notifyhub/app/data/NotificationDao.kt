package com.notifyhub.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE cleared = 0 ORDER BY timestampMillis DESC")
    fun observeActive(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<NotificationItem>>

    @Query("SELECT MAX(timestampMillis) FROM notifications")
    suspend fun getLatestTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<NotificationItem>): List<Long>

    @Query("UPDATE notifications SET cleared = 1 WHERE remoteId = :remoteId")
    suspend fun clearOne(remoteId: String)

    @Query("UPDATE notifications SET cleared = 1")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE insertedAtMillis < :olderThanMillis")
    suspend fun pruneOlderThan(olderThanMillis: Long)
}
