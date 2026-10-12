package com.notifyhub.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.notifyhub.app.notification.NotificationHelper
import com.notifyhub.app.service.PollingService
import com.notifyhub.app.ui.navigation.NotifyHubNavGraph
import com.notifyhub.app.ui.theme.NotifyHubTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) maybeStartServiceIfEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannels(this)
        ensureNotificationPermission()

        setContent {
            NotifyHubTheme {
                NotifyHubNavGraph()
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        maybeStartServiceIfEnabled()
    }

    private fun maybeStartServiceIfEnabled() {
        val app = application as NotifyHubApp
        if (app.settingsStore.current().serviceEnabled) {
            PollingService.start(this)
        }
    }
}
