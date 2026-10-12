package com.notifyhub.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notifyhub.app.NotifyHubApp
import com.notifyhub.app.data.NotificationItem
import com.notifyhub.app.data.Project
import com.notifyhub.app.notification.IconCatalog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as NotifyHubApp
    val scope = rememberCoroutineScope()

    val notifications by app.repository.observeActiveNotifications().collectAsState(initial = emptyList())
    val projects by app.repository.observeProjects().collectAsState(initial = emptyList())
    val projectsByKey = projects.associateBy { it.projectKey }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { scope.launch { app.repository.clearAllNotifications() } }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear all")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No notifications yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.remoteId }) { item ->
                    NotificationRow(
                        item = item,
                        project = projectsByKey[item.projectKey],
                        onClear = { scope.launch { app.repository.clearNotification(item.remoteId) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, project: Project?, onClear: () -> Unit) {
    val icon = IconCatalog.byKey(project?.iconKey ?: IconCatalog.default.key)
    val color = try {
        Color(android.graphics.Color.parseColor(project?.colorHex ?: "#6750A4"))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val timeFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon.drawableRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        timeFormatter.format(Date(item.timestampMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (project != null) {
                    Text(project.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(4.dp))
                Text(item.message, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = "Clear")
            }
        }
    }
}
