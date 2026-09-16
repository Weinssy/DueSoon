package com.duesoon.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.duesoon.app.MainActivity
import com.duesoon.app.R
import com.duesoon.app.data.repository.TaskRepository
import com.duesoon.app.data.local.AppDatabase
import com.duesoon.app.data.local.toDomainModel
import com.duesoon.app.domain.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        if (intent.action == ACTION_SNOOZE) {
            val durationMs = intent.getLongExtra(EXTRA_SNOOZE_DURATION_MS, 0L)
            handleSnoozeAction(context, taskId, durationMs)
            return
        }

        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val deadline = intent.getLongExtra(EXTRA_TASK_DEADLINE, -1L)

        // For snooze alarms firing, we must check if it's stale. But here we can't easily wait for DB result in a receiver without goAsync or Coroutine.
        // Actually, BroadcastReceiver can use goAsync. Let's use CoroutineScope.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val taskDao = db.taskDao()
                val entity = taskDao.getTask(taskId)
                
                // If task no longer exists or is completed, do nothing
                if (entity == null || entity.completed) {
                    return@launch
                }
                
                // If this is a snooze alarm firing, check if the snooze was canceled/changed
                if (isSnooze) {
                    val intentSnoozedUntil = intent.getLongExtra(EXTRA_SNOOZED_UNTIL, -1L)
                    if (entity.snoozedUntil == null || entity.snoozedUntil != intentSnoozedUntil) {
                        return@launch
                    }
                }

                // If snooze fires, clear the snooze state as it's authoritative
                if (isSnooze) {
                    val repo = TaskRepository(taskDao, AndroidNotificationScheduler(context), com.duesoon.app.data.repository.UserPreferencesRepository(context), context)
                    repo.clearSnooze(taskId)
                }

                showNotification(context, taskId, title, deadline)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnoozeAction(context: Context, taskId: Long, durationMs: Long) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val taskDao = db.taskDao()
                val entity = taskDao.getTask(taskId)
                if (entity == null || entity.completed) {
                    return@launch
                }
                
                // Clear the current notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId.toInt())
                

                val snoozedUntil = if (durationMs == SNOOZE_TOMORROW) {
                    DateTimeUtils.getTomorrowSnoozeTime()
                } else {
                    System.currentTimeMillis() + durationMs
                }
                
                val updatedEntity = entity.copy(snoozedUntil = snoozedUntil, updatedAt = System.currentTimeMillis())
                taskDao.update(updatedEntity)
                
                val scheduler = AndroidNotificationScheduler(context)
                scheduler.scheduleSnooze(updatedEntity.toDomainModel(), snoozedUntil)
                
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String, deadline: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notif_channel_description) }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.toInt(), activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Snooze Intents
        val snooze10Intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_SNOOZE_DURATION_MS, 10L * 60 * 1000)
        }
        val pSnooze10 = PendingIntent.getBroadcast(context, taskId.toInt() * 10 + 1, snooze10Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snooze1hIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_SNOOZE_DURATION_MS, 60L * 60 * 1000)
        }
        val pSnooze1h = PendingIntent.getBroadcast(context, taskId.toInt() * 10 + 2, snooze1hIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeTomorrowIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_SNOOZE_DURATION_MS, SNOOZE_TOMORROW)
        }
        val pSnoozeTomorrow = PendingIntent.getBroadcast(context, taskId.toInt() * 10 + 3, snoozeTomorrowIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val deadlineStr = if (deadline != -1L) {
            DateTimeUtils.formatDeadline(deadline) ?: ""
        } else ""

        val contentText = if (deadlineStr.isNotEmpty()) {
            context.getString(R.string.notif_body_with_deadline, deadlineStr)
        } else {
            context.getString(R.string.notif_body_due_soon)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.notif_title_format, title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.snooze_10m), pSnooze10)
            .addAction(0, context.getString(R.string.snooze_1h), pSnooze1h)
            .addAction(0, context.getString(R.string.snooze_tomorrow), pSnoozeTomorrow)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DEADLINE = "extra_task_deadline"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val EXTRA_SNOOZED_UNTIL = "extra_snoozed_until"
        
        const val ACTION_SNOOZE = "com.duesoon.app.ACTION_SNOOZE"
        const val EXTRA_SNOOZE_DURATION_MS = "extra_snooze_duration_ms"
        
        const val SNOOZE_TOMORROW = -2L
        
        const val CHANNEL_ID = "duesoon_reminders"
    }
}


