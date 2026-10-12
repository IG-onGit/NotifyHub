package com.notifyhub.app.data

import com.notifyhub.app.network.ApiService
import com.notifyhub.app.network.NetworkModule
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val projectDao: ProjectDao,
    private val notificationDao: NotificationDao
) {
    fun observeProjects(): Flow<List<Project>> = projectDao.observeAll()
    fun observeActiveNotifications(): Flow<List<NotificationItem>> = notificationDao.observeActive()
    fun observeAllNotifications(): Flow<List<NotificationItem>> = notificationDao.observeAll()

    suspend fun upsertProject(project: Project) = projectDao.upsert(project)
    suspend fun deleteProject(project: Project) = projectDao.delete(project)
    suspend fun getProjectByKey(projectKey: String): Project? = projectDao.getByKey(projectKey)
    suspend fun getAllProjects(): List<Project> = projectDao.getAll()

    suspend fun clearNotification(remoteId: String) = notificationDao.clearOne(remoteId)
    suspend fun clearAllNotifications() = notificationDao.clearAll()

    /**
     * Polls the server once for notifications newer than the latest one we
     * already have, and inserts any new rows. Returns the list of genuinely
     * new notifications (for posting as Android system notifications).
     */
    suspend fun pollOnce(baseUrl: String, apiToken: String): List<NotificationItem> {
        if (baseUrl.isBlank()) return emptyList()

        val api: ApiService = NetworkModule.buildApiService(baseUrl, apiToken)
        val since = notificationDao.getLatestTimestamp() ?: 0L
        val response = api.getNotifications(since)

        val items = response.notifications.map {
            NotificationItem(
                remoteId = it.id,
                projectKey = it.projectKey,
                title = it.title,
                message = it.message,
                timestampMillis = it.timestamp
            )
        }
        if (items.isEmpty()) return emptyList()

        // insertAll returns -1 for rows that were IGNORED due to an existing
        // primary key (already-seen remoteId); filter those out so we only
        // post system notifications for genuinely new ones.
        val insertResults = notificationDao.insertAll(items)
        return items.filterIndexed { index, _ -> insertResults.getOrNull(index) != -1L }
    }

    suspend fun pruneOlderThanDays(days: Int) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        notificationDao.pruneOlderThan(cutoff)
    }
}
