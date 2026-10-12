package com.notifyhub.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.notifyhub.app.NotifyHubApp
import com.notifyhub.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A foreground service that repeatedly polls the user's server for new
 * notifications and posts them as real Android notifications, using each
 * item's registered project for icon/color/name. Runs as a foreground
 * service (with a persistent low-priority "sync active" notification) so
 * Android's background execution limits and Doze don't kill the polling loop.
 */
class PollingService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NotificationHelper.SYNC_FOREGROUND_ID, NotificationHelper.buildSyncForegroundNotification(this))
        startPollingLoop()
        return START_STICKY
    }

    private fun startPollingLoop() {
        if (loopJob?.isActive == true) return
        val app = application as NotifyHubApp

        loopJob = scope.launch {
            while (true) {
                val settings = app.settingsStore.current()
                if (!settings.serviceEnabled) {
                    stopSelf()
                    return@launch
                }
                try {
                    for (project in app.repository.getAllProjects()) {
                        if (project.baseUrl.isBlank()) continue
                        try {
                            val newItems = app.repository.pollOnce(project)
                            for (item in newItems) {
                                NotificationHelper.postProjectNotification(applicationContext, project, item)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Poll failed for ${project.projectKey}: ${e.message}")
                        }
                    }
                    // Keep local history from growing unbounded.
                    app.repository.pruneOlderThanDays(30)
                } catch (e: Exception) {
                    Log.w(TAG, "Poll failed: ${e.message}")
                }

                val intervalSeconds = settings.pollIntervalSeconds.coerceIn(5, 3600)
                delay(intervalSeconds * 1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PollingService"

        fun start(context: android.content.Context) {
            val intent = Intent(context, PollingService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, PollingService::class.java))
        }
    }
}
