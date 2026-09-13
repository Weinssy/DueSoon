package com.duesoon.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val prefs by viewModel.userPreferences.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear completed tasks?") },
            text = { Text("This action cannot be undone. All completed tasks will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearCompletedTasks()
                }) { Text("Clear All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            ListItem(
                headlineContent = { Text("App Theme") },
                supportingContent = { 
                    val themeName = when (prefs.theme) {
                        "LIGHT" -> "Light"
                        "DARK" -> "Dark"
                        else -> "System Default"
                    }
                    Text(themeName)
                },
                trailingContent = {
                    Box {
                        Button(onClick = { showThemeMenu = true }) {
                            Text("Change")
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Default") },
                                onClick = { viewModel.updateTheme("SYSTEM"); showThemeMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Light") },
                                onClick = { viewModel.updateTheme("LIGHT"); showThemeMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Dark") },
                                onClick = { viewModel.updateTheme("DARK"); showThemeMenu = false }
                            )
                        }
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text("Enable or disable all reminders") },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                    )
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Clear Completed Tasks") },
                supportingContent = { Text("Permanently delete all completed tasks") },
                leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { showClearDialog = true }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("About DueSoon") },
                supportingContent = { Text("MVP v1.0") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) }
            )
        }
    }
}
