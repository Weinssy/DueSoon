package com.duesoon.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.duesoon.app.data.local.AppDatabase
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = AndroidNotificationScheduler(context)
            val repository = TaskRepository(
                AppDatabase.getDatabase(context).taskDao(),
                scheduler,
                UserPreferencesRepository(context)
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                val tasks = repository.observeTasks().first()
                tasks.forEach { task ->
                    if (!task.completed && task.deadline != null) {
                        scheduler.schedule(task)
                    }
                }
            }
        }
    }
}
