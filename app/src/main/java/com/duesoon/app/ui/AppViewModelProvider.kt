package com.duesoon.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duesoon.app.BuildConfig
import com.duesoon.app.DueSoonApplication
import com.duesoon.app.ui.calendar.CalendarViewModel
import com.duesoon.app.ui.focus.FocusViewModel
import com.duesoon.app.ui.home.HomeViewModel
import com.duesoon.app.ui.settings.SettingsViewModel
import com.duesoon.app.ui.task.CreateTaskViewModel
import com.duesoon.app.ui.task.EditTaskViewModel
import com.duesoon.app.ui.task.TaskDetailViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(dueSoonApplication().container.taskRepository)
        }
        initializer {
            CreateTaskViewModel(dueSoonApplication().container.taskRepository)
        }
        initializer {
            TaskDetailViewModel(
                this.createSavedStateHandle(),
                dueSoonApplication().container.taskRepository
            )
        }
        initializer {
            EditTaskViewModel(
                this.createSavedStateHandle(),
                dueSoonApplication().container.taskRepository
            )
        }
        initializer {
            CalendarViewModel(dueSoonApplication().container.taskRepository)
        }
        initializer {
            val container = dueSoonApplication().container
            SettingsViewModel(
                userPreferencesRepository = container.userPreferencesRepository,
                taskRepository = container.taskRepository,
                notificationScheduler = container.notificationScheduler,
                backupRestoreCoordinator = container.backupRestoreCoordinator,
                fokusKuliahImportCoordinator = container.fokusKuliahImportCoordinator,
                appUpdateRepository = container.appUpdateRepository,
                currentVersionName = BuildConfig.VERSION_NAME
            )
        }
        initializer {
            val container = dueSoonApplication().container
            FocusViewModel(
                savedStateHandle = this.createSavedStateHandle(),
                repository = container.taskRepository,
                focusRepository = container.focusRepository,
                userPreferencesRepository = container.userPreferencesRepository,
                sharedCoordinator = container.pomodoroTimerCoordinator
            )
        }
    }
}

fun CreationExtras.dueSoonApplication(): DueSoonApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as DueSoonApplication)
