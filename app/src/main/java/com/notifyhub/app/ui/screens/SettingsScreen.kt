package com.notifyhub.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.notifyhub.app.NotifyHubApp
import com.notifyhub.app.data.ConnectionSettings
import com.notifyhub.app.service.PollingService

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as NotifyHubApp
    val persisted by app.settingsStore.settings.collectAsState()

    var intervalSeconds by remember { mutableStateOf(persisted.pollIntervalSeconds.toFloat()) }
    var serviceEnabled by remember { mutableStateOf(persisted.serviceEnabled) }

    LaunchedEffect(persisted) {
        intervalSeconds = persisted.pollIntervalSeconds.toFloat()
        serviceEnabled = persisted.serviceEnabled
    }

    fun persist(newServiceEnabled: Boolean = serviceEnabled) {
        app.settingsStore.update(
            ConnectionSettings(
                pollIntervalSeconds = intervalSeconds.toInt(),
                serviceEnabled = newServiceEnabled
            )
        )
        if (newServiceEnabled) PollingService.start(context) else PollingService.stop(context)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)
        ) {
            Text("Poll interval: ${intervalSeconds.toInt()}s", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = intervalSeconds,
                onValueChange = { intervalSeconds = it },
                valueRange = 5f..120f,
                steps = 22
            )

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Background sync", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Runs a foreground service that polls each project's server and posts notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = serviceEnabled,
                        onCheckedChange = {
                            serviceEnabled = it
                            persist(newServiceEnabled = it)
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { persist() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save settings")
            }
        }
    }
}
