package com.duesoon.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferences
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateTheme(theme)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateNotificationsEnabled(enabled)
            
            val tasks = taskRepository.observeTasks().first()
            tasks.forEach { task ->
                if (!task.completed && task.deadline != null) {
                    if (enabled) {
                        notificationScheduler.schedule(task)
                    } else {
                        notificationScheduler.cancelAll(task)
                    }
                }
            }
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val tasks = taskRepository.observeTasks().first()
            val completedTasks = tasks.filter { it.completed }
            completedTasks.forEach { task ->
                taskRepository.deleteTask(task)
            }
        }
    }
}
