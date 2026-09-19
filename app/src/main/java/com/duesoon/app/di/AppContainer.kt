package com.duesoon.app.di

import android.content.Context
import com.duesoon.app.data.backup.BackupRestoreCoordinator
import com.duesoon.app.data.backup.BackupStorage
import com.duesoon.app.data.backup.SafBackupStorage
import com.duesoon.app.data.local.AppDatabase
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.notification.AndroidNotificationScheduler
import com.duesoon.app.notification.NotificationScheduler
import com.duesoon.app.BuildConfig
import com.duesoon.app.domain.usecase.CompleteTaskUseCase

interface AppContainer {
    val taskRepository: TaskRepository
    val notificationScheduler: NotificationScheduler
    val userPreferencesRepository: UserPreferencesRepository
    val backupStorage: BackupStorage
    val backupRestoreCoordinator: BackupRestoreCoordinator
    val completeTaskUseCase: com.duesoon.app.domain.usecase.CompleteTaskUseCase
    val restoreTaskUseCase: com.duesoon.app.domain.usecase.RestoreTaskUseCase
    val secureStorage: com.duesoon.app.core.crypto.SecureStorage
    val cryptoManager: com.duesoon.app.core.crypto.CryptoManager
    val syncScheduler: com.duesoon.app.core.sync.SyncScheduler
    val taskDao: com.duesoon.app.data.local.TaskDao
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val notificationScheduler: NotificationScheduler by lazy {
        AndroidNotificationScheduler(context)
    }
    
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getDatabase(context).taskDao(), notificationScheduler, userPreferencesRepository, context)
    }

    override val backupStorage: BackupStorage by lazy {
        SafBackupStorage(context)
    }

    override val backupRestoreCoordinator: BackupRestoreCoordinator by lazy {
        BackupRestoreCoordinator(
            taskRepository = taskRepository,
            backupStorage = backupStorage,
            appVersion = BuildConfig.VERSION_NAME,
            notificationScheduler = notificationScheduler,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    override val completeTaskUseCase: CompleteTaskUseCase by lazy {
        CompleteTaskUseCase(taskRepository)
    }

    override val restoreTaskUseCase: com.duesoon.app.domain.usecase.RestoreTaskUseCase by lazy {
        com.duesoon.app.domain.usecase.RestoreTaskUseCase(taskRepository, notificationScheduler, userPreferencesRepository, context)
    }

    override val secureStorage: com.duesoon.app.core.crypto.SecureStorage by lazy {
        com.duesoon.app.core.crypto.SecureStorage(context)
    }

    override val cryptoManager: com.duesoon.app.core.crypto.CryptoManager by lazy {
        com.duesoon.app.core.crypto.CryptoManager()
    }

    override val syncScheduler: com.duesoon.app.core.sync.SyncScheduler by lazy {
        com.duesoon.app.core.sync.SyncScheduler(context)
    }

    override val taskDao: com.duesoon.app.data.local.TaskDao by lazy {
        AppDatabase.getDatabase(context).taskDao()
    }
}
