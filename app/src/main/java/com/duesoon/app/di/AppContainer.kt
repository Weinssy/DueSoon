package com.duesoon.app.di

import android.content.Context
import androidx.room.withTransaction
import com.duesoon.app.BuildConfig
import com.duesoon.app.data.backup.BackupRestoreCoordinator
import com.duesoon.app.data.backup.BackupStorage
import com.duesoon.app.data.backup.FokusKuliahImportCoordinator
import com.duesoon.app.data.backup.SafBackupStorage
import com.duesoon.app.data.local.AppDatabase
import com.duesoon.app.data.local.TaskDao
import com.duesoon.app.data.repository.FocusRepository
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.data.update.GitHubAppUpdateRepository
import com.duesoon.app.domain.update.AppUpdateRepository
import com.duesoon.app.domain.usecase.CompleteTaskUseCase
import com.duesoon.app.focus.AndroidPomodoroServiceBridge
import com.duesoon.app.focus.PomodoroTimerCoordinator
import com.duesoon.app.notification.AndroidNotificationScheduler
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val taskRepository: TaskRepository
    val focusRepository: FocusRepository
    val notificationScheduler: NotificationScheduler
    val userPreferencesRepository: UserPreferencesRepository
    val backupStorage: BackupStorage
    val backupRestoreCoordinator: BackupRestoreCoordinator
    val fokusKuliahImportCoordinator: FokusKuliahImportCoordinator
    val appUpdateRepository: AppUpdateRepository
    val pomodoroTimerCoordinator: PomodoroTimerCoordinator
    val completeTaskUseCase: CompleteTaskUseCase
    val taskDao: TaskDao
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val notificationScheduler: NotificationScheduler by lazy {
        AndroidNotificationScheduler(context)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepository(
            database.taskDao(),
            notificationScheduler,
            userPreferencesRepository,
            context
        )
    }

    override val focusRepository: FocusRepository by lazy {
        FocusRepository(database.focusSessionDao())
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
            userPreferencesRepository = userPreferencesRepository,
            focusRepository = focusRepository,
            runInTransaction = { block -> database.withTransaction { block() } }
        )
    }

    override val fokusKuliahImportCoordinator: FokusKuliahImportCoordinator by lazy {
        FokusKuliahImportCoordinator(
            taskRepository = taskRepository,
            focusRepository = focusRepository,
            backupStorage = backupStorage,
            notificationScheduler = notificationScheduler,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    override val appUpdateRepository: AppUpdateRepository by lazy {
        GitHubAppUpdateRepository(
            context = context,
            appVersion = BuildConfig.VERSION_NAME
        )
    }

    override val pomodoroTimerCoordinator: PomodoroTimerCoordinator by lazy {
        PomodoroTimerCoordinator(
            scope = appScope,
            taskRepository = taskRepository,
            focusRepository = focusRepository,
            userPreferencesRepository = userPreferencesRepository,
            serviceBridge = AndroidPomodoroServiceBridge(context)
        )
    }

    override val completeTaskUseCase: CompleteTaskUseCase by lazy {
        CompleteTaskUseCase(taskRepository)
    }

    override val taskDao: TaskDao by lazy {
        database.taskDao()
    }
}
