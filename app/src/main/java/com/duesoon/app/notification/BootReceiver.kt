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
                val now = System.currentTimeMillis()
                tasks.forEach { task ->
                    if (!task.completed) {
                        if (task.deadline != null) {
                            scheduler.schedule(task)
                        }
                        if (task.snoozedUntil != null) {
                            if (task.snoozedUntil > now) {
                                scheduler.scheduleSnooze(task, task.snoozedUntil)
                            } else {
                                repository.clearSnooze(task.id)
                                // Past snooze - fire immediately
                                val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                                    putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
                                    putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.title)
                                    putExtra(ReminderReceiver.EXTRA_TASK_DEADLINE, task.deadline ?: -1L)
                                    // Don't set EXTRA_IS_SNOOZE because we already cleared it and want to force show
                                }
                                context.sendBroadcast(snoozeIntent)
                            }
                        }
                    }
                }
            }
        }
    }
}
