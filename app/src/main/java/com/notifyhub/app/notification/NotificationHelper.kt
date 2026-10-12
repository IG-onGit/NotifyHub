package com.notifyhub.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.notifyhub.app.MainActivity
import com.notifyhub.app.R
import com.notifyhub.app.data.NotificationItem
import com.notifyhub.app.data.Project

object NotificationHelper {

    const val SYNC_CHANNEL_ID = "sync_status"
    const val PROJECT_CHANNEL_ID = "project_notifications"
    const val SYNC_FOREGROUND_ID = 1

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val syncChannel = NotificationChannel(
            SYNC_CHANNEL_ID,
            context.getString(R.string.sync_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.sync_channel_desc)
            setShowBadge(false)
        }

        val projectChannel = NotificationChannel(
            PROJECT_CHANNEL_ID,
            "Project notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications pushed by your registered software"
        }

        manager.createNotificationChannel(syncChannel)
        manager.createNotificationChannel(projectChannel)
    }

    fun buildSyncForegroundNotification(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cat_bell)
            .setContentTitle(context.getString(R.string.sync_notification_title))
            .setContentText(context.getString(R.string.sync_notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /** Posts one real Android notification for a project-tagged item, using that project's icon + color. */
    fun postProjectNotification(context: Context, project: Project?, item: NotificationItem) {
        val icon = IconCatalog.byKey(project?.iconKey ?: IconCatalog.default.key)
        val color = try {
            Color.parseColor(project?.colorHex ?: "#6750A4")
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#6750A4")
        }

        val openAppIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            item.remoteId.hashCode(),
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, PROJECT_CHANNEL_ID)
            .setSmallIcon(icon.drawableRes)
            .setColor(color)
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        project?.let { builder.setSubText(it.name) }

        val manager = androidx.core.app.NotificationManagerCompat.from(context)
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            manager.notify(item.remoteId.hashCode(), builder.build())
        }
    }
}
