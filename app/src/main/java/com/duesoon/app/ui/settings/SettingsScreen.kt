package com.duesoon.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
import com.duesoon.app.BuildConfig
import com.duesoon.app.ui.AppViewModelProvider
import androidx.compose.ui.unit.dp
import com.duesoon.app.domain.model.AccentPalette
import com.duesoon.app.domain.model.VisualDensity

import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.duesoon.app.data.update.ApkInstallLaunchResult
import com.duesoon.app.data.update.ApkInstaller
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val prefs by viewModel.userPreferences.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showPaletteMenu by remember { mutableStateOf(false) }
    var showDensityMenu by remember { mutableStateOf(false) }
    var showUnknownSourcesDialog by remember { mutableStateOf(false) }

    val backupState by viewModel.backupUiState.collectAsState()
    val updateState by viewModel.updateUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetBackupState()
            }
            is BackupUiState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.resetBackupState()
            }
            else -> {}
        }
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateUiState.UpToDate -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.update_up_to_date_message, state.currentVersion)
                )
                viewModel.resetUpdateState()
            }
            is UpdateUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    if (showUnknownSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showUnknownSourcesDialog = false },
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnknownSourcesDialog = false
                    context.startActivity(ApkInstaller.createUnknownSourcesIntent(context))
                }) {
                    Text(stringResource(R.string.update_action_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnknownSourcesDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    when (val state = updateState) {
        is UpdateUiState.UpdateAvailable -> {
            val release = state.release
            val sizeLabel = release.apkSizeBytes?.let { formatBytes(it) }
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                title = { Text(stringResource(R.string.update_available_title, release.versionName)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = buildString {
                                append("Versi saat ini: v${BuildConfig.VERSION_NAME} → v${release.versionName}")
                                if (sizeLabel != null) append(" ($sizeLabel)")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val notes = release.releaseNotes.ifBlank {
                            stringResource(R.string.update_no_changelog)
                        }
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    if (!release.apkDownloadUrl.isNullOrBlank()) {
                        TextButton(onClick = { viewModel.startUpdateDownload() }) {
                            Text(stringResource(R.string.update_action_download_install))
                        }
                    } else {
                        TextButton(onClick = {
                            viewModel.resetUpdateState()
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                            context.startActivity(browserIntent)
                        }) {
                            Text(stringResource(R.string.update_action_open_release))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetUpdateState() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
        is UpdateUiState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* keep visible while downloading */ },
                title = { Text(stringResource(R.string.update_downloading_title, state.release.versionName)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val pct = state.progressPercent
                        if (pct != null) {
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            val detail = if (state.totalBytes > 0L) {
                                "$pct% (${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)})"
                            } else {
                                "$pct%"
                            }
                            Text(text = detail, style = MaterialTheme.typography.bodySmall)
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = formatBytes(state.bytesDownloaded),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelUpdateDownload() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
        is UpdateUiState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                title = { Text(stringResource(R.string.update_ready_title)) },
                text = { Text(stringResource(R.string.update_ready_message, state.release.versionName)) },
                confirmButton = {
                    TextButton(onClick = {
                        when (val result = ApkInstaller.launchInstall(context, state.apkFilePath)) {
                            is ApkInstallLaunchResult.Launched -> {
                                viewModel.resetUpdateState()
                            }
                            is ApkInstallLaunchResult.NeedsUnknownSourcesPermission -> {
                                showUnknownSourcesDialog = true
                            }
                            is ApkInstallLaunchResult.Error -> {
                                viewModel.resetUpdateState()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(result.message)
                                }
                            }
                        }
                    }) {
                        Text(stringResource(R.string.update_action_install_now))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetUpdateState() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
        else -> {}
    }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(scrollState)) {
            
            Text(
                text = stringResource(R.string.settings_section_appearance),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

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
                headlineContent = { Text(stringResource(R.string.settings_accent_palette_title)) },
                supportingContent = { 
                    val paletteName = when (prefs.accentPalette) {
                        AccentPalette.INDIGO -> stringResource(R.string.accent_indigo)
                        AccentPalette.EMERALD -> stringResource(R.string.accent_emerald)
                        AccentPalette.AMBER -> stringResource(R.string.accent_amber)
                        AccentPalette.ROSE -> stringResource(R.string.accent_rose)
                        AccentPalette.CAMPUS_PAPER -> stringResource(R.string.accent_campus_paper)
                    }
                    Text(paletteName)
                },
                trailingContent = {
                    Box {
                        Button(onClick = { showPaletteMenu = true }) {
                            Text(stringResource(R.string.action_change))
                        }
                        DropdownMenu(
                            expanded = showPaletteMenu,
                            onDismissRequest = { showPaletteMenu = false }
                        ) {
                            AccentPalette.values().forEach { palette ->
                                DropdownMenuItem(
                                    text = { 
                                        val pName = when (palette) {
                                            AccentPalette.INDIGO -> stringResource(R.string.accent_indigo)
                                            AccentPalette.EMERALD -> stringResource(R.string.accent_emerald)
                                            AccentPalette.AMBER -> stringResource(R.string.accent_amber)
                                            AccentPalette.ROSE -> stringResource(R.string.accent_rose)
                                            AccentPalette.CAMPUS_PAPER -> stringResource(R.string.accent_campus_paper)
                                        }
                                        Text(pName) 
                                    },
                                    onClick = { viewModel.updateAccentPalette(palette); showPaletteMenu = false }
                                )
                            }
                        }
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_visual_density_title)) },
                supportingContent = { 
                    val densityName = when (prefs.visualDensity) {
                        VisualDensity.COMFORTABLE -> stringResource(R.string.density_comfortable)
                        VisualDensity.COMPACT -> stringResource(R.string.density_compact)
                    }
                    Text(densityName)
                },
                trailingContent = {
                    Box {
                        Button(onClick = { showDensityMenu = true }) {
                            Text(stringResource(R.string.action_change))
                        }
                        DropdownMenu(
                            expanded = showDensityMenu,
                            onDismissRequest = { showDensityMenu = false }
                        ) {
                            VisualDensity.values().forEach { density ->
                                DropdownMenuItem(
                                    text = { 
                                        val dName = when (density) {
                                            VisualDensity.COMFORTABLE -> stringResource(R.string.density_comfortable)
                                            VisualDensity.COMPACT -> stringResource(R.string.density_compact)
                                        }
                                        Text(dName) 
                                    },
                                    onClick = { viewModel.updateVisualDensity(density); showDensityMenu = false }
                                )
                            }
                        }
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_haptics_title)) },
                supportingContent = { Text(stringResource(R.string.settings_haptics_subtitle)) },
                trailingContent = {
                    Switch(
                        checked = prefs.hapticsEnabled,
                        onCheckedChange = { viewModel.updateHapticsEnabled(it) }
                    )
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
                headlineContent = { Text("Alarm Pengingat") },
                supportingContent = { Text("Putar alarm berulang saat waktu pengingat tiba.") },
                trailingContent = {
                    Switch(
                        checked = prefs.alarmReminderEnabled,
                        onCheckedChange = { enabled -> 
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                if (!notificationManager.canUseFullScreenIntent()) {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            viewModel.updateAlarmReminderEnabled(enabled)
                        }
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
                supportingContent = { Text(stringResource(R.string.settings_about_subtitle, BuildConfig.VERSION_NAME)) },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_check_update_title)) },
                supportingContent = {
                    Text(
                        if (updateState is UpdateUiState.Checking) {
                            stringResource(R.string.update_checking_subtitle)
                        } else {
                            stringResource(R.string.settings_check_update_subtitle, BuildConfig.VERSION_NAME)
                        }
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_system_update),
                        contentDescription = null
                    )
                },
                trailingContent = {
                    if (updateState is UpdateUiState.Checking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        OutlinedButton(onClick = { viewModel.checkForUpdates() }) {
                            Text(stringResource(R.string.update_action_check))
                        }
                    }
                },
                modifier = Modifier.clickable(
                    enabled = updateState !is UpdateUiState.Checking && updateState !is UpdateUiState.Downloading,
                    onClick = { viewModel.checkForUpdates() }
                )
            )
            HorizontalDivider()

            // --- Data & Backup ---
            
            val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    viewModel.exportBackup(uri)
                }
            }

            val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    viewModel.prepareImport(uri)
                }
            }

            val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    viewModel.prepareRestore(uri)
                }
            }

            val fokusKuliahImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    viewModel.prepareFokusKuliahImport(uri)
                }
            }

            if (backupState is BackupUiState.AwaitingFokusKuliahImportConfirmation) {
                val state = backupState as BackupUiState.AwaitingFokusKuliahImportConfirmation
                AlertDialog(
                    onDismissRequest = { viewModel.cancelConfirmation() },
                    title = { Text(stringResource(R.string.dialog_import_fk_title)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.dialog_import_fk_message,
                                state.parsed.tasks.size,
                                state.parsed.focusSessions.size
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.executeFokusKuliahImport() }) {
                            Text(stringResource(R.string.action_import))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelConfirmation() }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            if (backupState is BackupUiState.AwaitingImportConfirmation) {
                val state = backupState as BackupUiState.AwaitingImportConfirmation
                AlertDialog(
                    onDismissRequest = { viewModel.cancelConfirmation() },
                    title = { Text(stringResource(R.string.dialog_import_title)) },
                    text = {
                        val msg = if (state.backup.focusSessions.isNotEmpty()) {
                            stringResource(
                                R.string.dialog_import_with_focus_message,
                                state.backup.tasks.size,
                                state.backup.focusSessions.size
                            )
                        } else {
                            stringResource(R.string.dialog_import_message, state.backup.tasks.size)
                        }
                        Text(msg)
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.executeImport() }) {
                            Text(stringResource(R.string.action_import))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelConfirmation() }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            if (backupState is BackupUiState.AwaitingRestoreConfirmation) {
                val state = backupState as BackupUiState.AwaitingRestoreConfirmation
                AlertDialog(
                    onDismissRequest = { viewModel.cancelConfirmation() },
                    title = { Text(stringResource(R.string.dialog_restore_title)) },
                    text = {
                        val msg = if (state.backup.focusSessions.isNotEmpty()) {
                            stringResource(
                                R.string.dialog_restore_with_focus_message,
                                state.backup.tasks.size,
                                state.backup.focusSessions.size
                            )
                        } else {
                            stringResource(R.string.dialog_restore_message, state.backup.tasks.size)
                        }
                        Text(msg)
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.executeRestore() }) {
                            Text(stringResource(R.string.action_restore), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelConfirmation() }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
            
            Text(
                text = stringResource(R.string.settings_section_data),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export_title)) },
                supportingContent = { Text(stringResource(R.string.settings_export_subtitle)) },
                modifier = Modifier.clickable(
                    enabled = backupState !is BackupUiState.Loading,
                    onClick = {
                        val formatter = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                        val fileName = "DueSoon_Backup_${formatter.format(java.util.Date())}.json"
                        exportLauncher.launch(fileName)
                    }
                )
            )
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import_title)) },
                supportingContent = { Text(stringResource(R.string.settings_import_subtitle)) },
                modifier = Modifier.clickable(
                    enabled = backupState !is BackupUiState.Loading,
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_restore_title)) },
                supportingContent = { Text(stringResource(R.string.settings_restore_subtitle)) },
                modifier = Modifier.clickable(
                    enabled = backupState !is BackupUiState.Loading,
                    onClick = { restoreLauncher.launch(arrayOf("application/json")) }
                )
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import_fk_title)) },
                supportingContent = { Text(stringResource(R.string.settings_import_fk_subtitle)) },
                modifier = Modifier.clickable(
                    enabled = backupState !is BackupUiState.Loading,
                    onClick = { fokusKuliahImportLauncher.launch(arrayOf("application/json")) }
                )
            )
            HorizontalDivider()

            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                if (backupState is BackupUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
