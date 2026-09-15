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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
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
            title = { Text(stringResource(R.string.dialog_clear_completed_title)) },
            text = { Text(stringResource(R.string.dialog_clear_completed_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearCompletedTasks()
                }) { Text(stringResource(R.string.action_clear_all), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme_title)) },
                supportingContent = { 
                    val themeName = when (prefs.theme) {
                        "LIGHT" -> stringResource(R.string.theme_light)
                        "DARK" -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    }
                    Text(themeName)
                },
                trailingContent = {
                    Box {
                        Button(onClick = { showThemeMenu = true }) {
                            Text(stringResource(R.string.settings_theme_change))
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_system)) },
                                onClick = { viewModel.updateTheme("SYSTEM"); showThemeMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_light)) },
                                onClick = { viewModel.updateTheme("LIGHT"); showThemeMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_dark)) },
                                onClick = { viewModel.updateTheme("DARK"); showThemeMenu = false }
                            )
                        }
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications_title)) },
                supportingContent = { Text(stringResource(R.string.settings_notifications_subtitle)) },
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
                headlineContent = { Text(stringResource(R.string.settings_clear_completed_title)) },
                supportingContent = { Text(stringResource(R.string.settings_clear_completed_subtitle)) },
                leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { showClearDialog = true }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about_title)) },
                supportingContent = { Text(stringResource(R.string.settings_about_subtitle)) },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) }
            )
        }
    }
}
