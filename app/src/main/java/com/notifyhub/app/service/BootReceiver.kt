package com.notifyhub.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifyhub.app.NotifyHubApp

/** Restarts the polling service after a device reboot, if it was previously enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as NotifyHubApp
        if (app.settingsStore.current().serviceEnabled) {
            PollingService.start(context)
        }
    }
}
