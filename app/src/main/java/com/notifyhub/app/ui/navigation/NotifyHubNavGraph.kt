package com.notifyhub.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notifyhub.app.ui.screens.AddEditProjectScreen
import com.notifyhub.app.ui.screens.NotificationsScreen
import com.notifyhub.app.ui.screens.ProjectsScreen
import com.notifyhub.app.ui.screens.SettingsScreen

private object Routes {
    const val NOTIFICATIONS = "notifications"
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"
    const val ADD_EDIT_PROJECT = "projects/edit"
    const val PROJECT_ID_ARG = "projectId"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.NOTIFICATIONS, "Notifications", Icons.Filled.Notifications),
    TopLevelDestination(Routes.PROJECTS, "Projects", Icons.Filled.List),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun NotifyHubNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                topLevelDestinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.NOTIFICATIONS,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen()
            }
            composable(Routes.PROJECTS) {
                ProjectsScreen(
                    onAddProject = { navController.navigate("${Routes.ADD_EDIT_PROJECT}?${Routes.PROJECT_ID_ARG}=") },
                    onEditProject = { id -> navController.navigate("${Routes.ADD_EDIT_PROJECT}?${Routes.PROJECT_ID_ARG}=$id") }
                )
            }
            composable(
                route = "${Routes.ADD_EDIT_PROJECT}?${Routes.PROJECT_ID_ARG}={${Routes.PROJECT_ID_ARG}}",
                arguments = listOf(
                    androidx.navigation.navArgument(Routes.PROJECT_ID_ARG) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString(Routes.PROJECT_ID_ARG)?.takeIf { it.isNotBlank() }
                AddEditProjectScreen(
                    projectId = projectId,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
