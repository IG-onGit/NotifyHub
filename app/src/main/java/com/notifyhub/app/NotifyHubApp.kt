package com.notifyhub.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.notifyhub.app.data.AppDatabase
import com.notifyhub.app.data.NotificationRepository
import com.notifyhub.app.data.SettingsStore

class NotifyHubApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch { repository.migrateLegacyConnection() }
    }

    val settingsStore: SettingsStore by lazy { SettingsStore.getInstance(this) }

    val repository: NotificationRepository by lazy {
        val db = AppDatabase.getInstance(this)
        NotificationRepository(db.projectDao(), db.notificationDao(), settingsStore)
    }
}
