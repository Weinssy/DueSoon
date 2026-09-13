package com.duesoon.app.di

import android.content.Context
import com.duesoon.app.data.local.AppDatabase
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import com.duesoon.app.notification.AndroidNotificationScheduler
import com.duesoon.app.notification.NotificationScheduler

interface AppContainer {
    val taskRepository: TaskRepository
    val notificationScheduler: NotificationScheduler
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val notificationScheduler: NotificationScheduler by lazy {
        AndroidNotificationScheduler(context)
    }
    
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getDatabase(context).taskDao(), notificationScheduler, userPreferencesRepository)
    }
}
