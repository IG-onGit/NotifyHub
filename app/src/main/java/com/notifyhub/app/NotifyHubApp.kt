package com.notifyhub.app

import android.app.Application
import com.notifyhub.app.data.AppDatabase
import com.notifyhub.app.data.NotificationRepository
import com.notifyhub.app.data.SettingsStore

class NotifyHubApp : Application() {

    val settingsStore: SettingsStore by lazy { SettingsStore.getInstance(this) }

    val repository: NotificationRepository by lazy {
        val db = AppDatabase.getInstance(this)
        NotificationRepository(db.projectDao(), db.notificationDao())
    }
}
