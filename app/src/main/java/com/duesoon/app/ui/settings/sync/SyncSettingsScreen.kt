package com.duesoon.app.ui.settings.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    viewModel: SyncSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var serverUrl by remember { mutableStateOf(uiState.serverUrl) }
    var passphrase by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isSyncEnabled) {
                // Sync Status
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Sync is Active", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Server: ${uiState.serverUrl}")
                            Text(text = "Unsynced Changes: ${uiState.dirtyTaskCount}")
                            if (uiState.lastSyncTimestamp != null) {
                                Text(text = "Last Synced: ${java.util.Date(uiState.lastSyncTimestamp!!)}")
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.triggerManualSync() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSyncing
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Sync Now")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = { viewModel.showDisconnectDialog(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect & Remove Keys", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                // Setup Form
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Enable End-to-End Encrypted Sync", style = MaterialTheme.typography.titleLarge)
                    Text("Your tasks are encrypted locally. The server never sees your data.")

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Sync Passphrase") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Info else Icons.Filled.Lock
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility")
                            }
                        }
                    )

                    if (uiState.syncErrorMessage != null) {
                        Text(text = uiState.syncErrorMessage!!, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = { viewModel.onSetupSubmit(serverUrl, passphrase.toCharArray()) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = serverUrl.isNotBlank() && passphrase.isNotBlank() && !uiState.isConfiguring
                    ) {
                        if (uiState.isConfiguring) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Enable Sync")
                        }
                    }
                }
            }

            if (uiState.showDisconnectDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showDisconnectDialog(false) },
                    title = { Text("Disconnect Sync?") },
                    text = { Text("Your local tasks will NOT be deleted. However, your encryption keys will be removed from this device.") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onDisconnect() }) {
                            Text("Disconnect", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showDisconnectDialog(false) }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
