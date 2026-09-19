package com.duesoon.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duesoon.app.DueSoonApplication
import com.duesoon.app.ui.calendar.CalendarViewModel
import com.duesoon.app.ui.home.HomeViewModel
import com.duesoon.app.ui.task.CreateTaskViewModel
import com.duesoon.app.ui.task.EditTaskViewModel
import com.duesoon.app.ui.task.TaskDetailViewModel
import com.duesoon.app.ui.tasks.TasksViewModel
import com.duesoon.app.ui.settings.SettingsViewModel
import com.duesoon.app.ui.archive.ArchiveViewModel

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
            TasksViewModel(dueSoonApplication().container.taskRepository)
        }
        initializer {
            CalendarViewModel(dueSoonApplication().container.taskRepository)
        }
        initializer {
            SettingsViewModel(
                dueSoonApplication().container.userPreferencesRepository,
                dueSoonApplication().container.taskRepository,
                dueSoonApplication().container.notificationScheduler,
                dueSoonApplication().container.backupRestoreCoordinator
            )
        }
        initializer {
            ArchiveViewModel(
                dueSoonApplication().container.taskRepository,
                dueSoonApplication().container.restoreTaskUseCase
            )
        }
    }
}

fun CreationExtras.dueSoonApplication(): DueSoonApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as DueSoonApplication)
