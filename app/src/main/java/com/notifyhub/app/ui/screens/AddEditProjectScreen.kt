package com.notifyhub.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.notifyhub.app.NotifyHubApp
import com.notifyhub.app.data.Project
import com.notifyhub.app.notification.IconCatalog
import kotlinx.coroutines.launch
import java.util.UUID

private val presetColors = listOf(
    "#6750A4", "#1976D2", "#0097A7", "#2E7D32", "#F9A825",
    "#EF6C00", "#D84315", "#C2185B", "#8E24AA", "#5D4037",
    "#455A64", "#37474F"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProjectScreen(projectId: String?, onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NotifyHubApp
    val scope = rememberCoroutineScope()

    var existing by remember { mutableStateOf<Project?>(null) }
    var name by remember { mutableStateOf("") }
    var projectKey by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf(IconCatalog.default.key) }
    var selectedColor by remember { mutableStateOf(presetColors.first()) }
    var keyManuallyEdited by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        if (projectId != null) {
            val project = app.repository.getAllProjects().firstOrNull { it.id == projectId }
            if (project != null) {
                existing = project
                name = project.name
                projectKey = project.projectKey
                selectedIconKey = project.iconKey
                selectedColor = project.colorHex
                keyManuallyEdited = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (existing == null) "Register project" else "Edit project") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (!keyManuallyEdited) {
                        projectKey = slugify(it)
                    }
                },
                label = { Text("Project / software name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = projectKey,
                onValueChange = {
                    projectKey = it
                    keyManuallyEdited = true
                },
                label = { Text("Project key (sent by your server)") },
                supportingText = { Text("Your backend must tag each notification with this exact key") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text("Icon", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(IconCatalog.icons) { catIcon ->
                    val isSelected = catIcon.key == selectedIconKey
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(android.graphics.Color.parseColor(selectedColor)) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickableSimple { selectedIconKey = catIcon.key },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(catIcon.drawableRes),
                            contentDescription = catIcon.label,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Notification color", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetColors) { hex ->
                    val isSelected = hex == selectedColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickableSimple { selectedColor = hex }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    val toSave = Project(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        projectKey = projectKey.trim(),
                        name = name.trim(),
                        iconKey = selectedIconKey,
                        colorHex = selectedColor,
                        createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                    )
                    scope.launch {
                        app.repository.upsertProject(toSave)
                        onDone()
                    }
                },
                enabled = name.isNotBlank() && projectKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existing == null) "Register project" else "Save changes")
            }
        }
    }
}

private fun slugify(input: String): String =
    input.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

private fun Modifier.clickableSimple(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
