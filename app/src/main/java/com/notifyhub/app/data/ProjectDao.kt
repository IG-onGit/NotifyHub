package com.notifyhub.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<Project>

    @Query("SELECT * FROM projects WHERE projectKey = :projectKey LIMIT 1")
    suspend fun getByKey(projectKey: String): Project?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: Project)

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
