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
}
