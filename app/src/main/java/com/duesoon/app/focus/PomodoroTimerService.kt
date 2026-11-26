package com.duesoon.app.focus

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.duesoon.app.DueSoonApplication
import com.duesoon.app.MainActivity
import com.duesoon.app.R
import com.duesoon.app.domain.model.PomodoroMode

class AndroidPomodoroServiceBridge(
    private val context: Context
) : PomodoroServiceBridge {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun startForegroundTimer() {
        PomodoroTimerService.ensureChannels(context)
        val intent = Intent(context, PomodoroTimerService::class.java).apply {
            action = PomodoroTimerService.ACTION_START_OR_SYNC
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            // Fallback to updating notification directly if background FGS restriction applies
            updateNotification()
        }
    }

    override fun updateNotification() {
        val app = context.applicationContext as? DueSoonApplication ?: return
        val snapshot = app.container.pomodoroTimerCoordinator.state.value
        if (!snapshot.isRunning && !snapshot.isPaused) return
        PomodoroTimerService.ensureChannels(context)
        notificationManager.notify(
            PomodoroTimerService.ONGOING_NOTIFICATION_ID,
            PomodoroTimerService.buildOngoingNotification(context, snapshot)
        )
    }

    override fun stopForegroundTimer(keepPausedNotification: Boolean) {
        val app = context.applicationContext as? DueSoonApplication ?: return
        val snapshot = app.container.pomodoroTimerCoordinator.state.value
        val stopIntent = Intent(context, PomodoroTimerService::class.java).apply {
            action = if (keepPausedNotification) {
                PomodoroTimerService.ACTION_PAUSE_NOTIFICATION
            } else {
                PomodoroTimerService.ACTION_STOP_SERVICE
            }
        }
        try {
            context.startService(stopIntent)
        } catch (_: Exception) {
            // Service might not be running
        }

        if (keepPausedNotification && snapshot.isPaused) {
            PomodoroTimerService.ensureChannels(context)
            notificationManager.notify(
                PomodoroTimerService.ONGOING_NOTIFICATION_ID,
                PomodoroTimerService.buildOngoingNotification(context, snapshot)
            )
        } else {
            notificationManager.cancel(PomodoroTimerService.ONGOING_NOTIFICATION_ID)
        }
    }

    override fun notifyPhaseCompleted(
        completedMode: PomodoroMode,
        nextMode: PomodoroMode,
        soundEnabled: Boolean
    ) {
        if (isAppInForeground(context)) {
            return
        }
        PomodoroTimerService.ensureChannels(context)
        val notification = PomodoroTimerService.buildCompletionNotification(
            context = context,
            completedMode = completedMode,
            nextMode = nextMode,
            soundEnabled = soundEnabled
        )
        notificationManager.notify(PomodoroTimerService.COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun isAppInForeground(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val processes = am.runningAppProcesses ?: return false
        return processes.any {
            it.processName == context.packageName &&
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }
}

class PomodoroTimerService : Service() {

    private val coordinator: PomodoroTimerCoordinator
        get() = (application as DueSoonApplication).container.pomodoroTimerCoordinator

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannels(this)
        val action = intent?.action

        when (action) {
            ACTION_TOGGLE -> {
                coordinator.toggleTimer()
            }
            ACTION_SKIP -> {
                coordinator.skipSession()
            }
            ACTION_STOP -> {
                coordinator.resetTimer()
                stopForegroundAndSelf(removeNotification = true)
                return START_NOT_STICKY
            }
            ACTION_PAUSE_NOTIFICATION -> {
                stopForegroundAndSelf(removeNotification = false)
                val snapshot = coordinator.state.value
                if (snapshot.isPaused) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(ONGOING_NOTIFICATION_ID, buildOngoingNotification(this, snapshot))
                }
                return START_NOT_STICKY
            }
            ACTION_STOP_SERVICE -> {
                stopForegroundAndSelf(removeNotification = true)
                return START_NOT_STICKY
            }
        }

        val snapshot = coordinator.state.value
        if (snapshot.isRunning) {
            val notification = buildOngoingNotification(this, snapshot)
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, ONGOING_NOTIFICATION_ID, notification, fgsType)
            return START_STICKY
        }

        stopForegroundAndSelf(removeNotification = !snapshot.isPaused)
        return START_NOT_STICKY
    }

    private fun stopForegroundAndSelf(removeNotification: Boolean) {
        val flags = if (removeNotification) {
            ServiceCompat.STOP_FOREGROUND_REMOVE
        } else {
            ServiceCompat.STOP_FOREGROUND_DETACH
        }
        ServiceCompat.stopForeground(this, flags)
        if (removeNotification) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(ONGOING_NOTIFICATION_ID)
        }
        stopSelf()
    }

    companion object {
        const val ONGOING_NOTIFICATION_ID = 4201
        const val COMPLETION_NOTIFICATION_ID = 4202
        const val ONGOING_CHANNEL_ID = "duesoon_pomodoro_ongoing"
        const val COMPLETION_CHANNEL_ID = "duesoon_pomodoro_complete"
        const val COMPLETION_SILENT_CHANNEL_ID = "duesoon_pomodoro_complete_silent"

        const val ACTION_START_OR_SYNC = "com.duesoon.app.focus.ACTION_START_OR_SYNC"
        const val ACTION_TOGGLE = "com.duesoon.app.focus.ACTION_TOGGLE"
        const val ACTION_SKIP = "com.duesoon.app.focus.ACTION_SKIP"
        const val ACTION_STOP = "com.duesoon.app.focus.ACTION_STOP"
        const val ACTION_PAUSE_NOTIFICATION = "com.duesoon.app.focus.ACTION_PAUSE_NOTIFICATION"
        const val ACTION_STOP_SERVICE = "com.duesoon.app.focus.ACTION_STOP_SERVICE"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val ongoing = NotificationChannel(
                ONGOING_CHANNEL_ID,
                context.getString(R.string.focus_notif_channel_ongoing_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.focus_notif_channel_ongoing_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(ongoing)

            val complete = NotificationChannel(
                COMPLETION_CHANNEL_ID,
                context.getString(R.string.focus_notif_channel_complete_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.focus_notif_channel_complete_desc)
                enableVibration(true)
            }
            nm.createNotificationChannel(complete)

            val completeSilent = NotificationChannel(
                COMPLETION_SILENT_CHANNEL_ID,
                context.getString(R.string.focus_notif_channel_complete_silent_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.focus_notif_channel_complete_desc)
                setSound(null, null)
                enableVibration(true)
            }
            nm.createNotificationChannel(completeSilent)
        }

        fun buildOngoingNotification(
            context: Context,
            snapshot: PomodoroTimerSnapshot
        ): android.app.Notification {
            val formattedTime = formatRemaining(snapshot.remainingSeconds)
            val modeLabel = when (snapshot.mode) {
                PomodoroMode.FOCUS -> context.getString(R.string.focus_mode_focus_label)
                PomodoroMode.SHORT_BREAK -> context.getString(R.string.focus_mode_short_break_label)
                PomodoroMode.LONG_BREAK -> context.getString(R.string.focus_mode_long_break_label)
            }
            val statusText = if (snapshot.isRunning) formattedTime else "${context.getString(R.string.focus_notif_paused)} · $formattedTime"
            val title = "$modeLabel · $statusText"
            val contentText = snapshot.linkedTaskTitle?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.focus_notif_running_subtitle)

            val openFocusIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_FOCUS
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                4200,
                openFocusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val toggleIntent = Intent(context, PomodoroTimerService::class.java).apply {
                action = ACTION_TOGGLE
            }
            val togglePendingIntent = PendingIntent.getService(
                context,
                4201,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val skipIntent = Intent(context, PomodoroTimerService::class.java).apply {
                action = ACTION_SKIP
            }
            val skipPendingIntent = PendingIntent.getService(
                context,
                4202,
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(context, PomodoroTimerService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPendingIntent = PendingIntent.getService(
                context,
                4203,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val toggleLabel = if (snapshot.isRunning) {
                context.getString(R.string.action_pause)
            } else {
                context.getString(R.string.action_resume)
            }

            return NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_focus)
                .setContentTitle(title)
                .setContentText(contentText)
                .setOngoing(snapshot.isRunning)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(openPendingIntent)
                .addAction(0, toggleLabel, togglePendingIntent)
                .addAction(0, context.getString(R.string.focus_notif_action_skip), skipPendingIntent)
                .addAction(0, context.getString(R.string.action_reset), stopPendingIntent)
                .build()
        }

        fun buildCompletionNotification(
            context: Context,
            completedMode: PomodoroMode,
            nextMode: PomodoroMode,
            soundEnabled: Boolean
        ): android.app.Notification {
            val channelId = if (soundEnabled) COMPLETION_CHANNEL_ID else COMPLETION_SILENT_CHANNEL_ID
            val title = if (completedMode == PomodoroMode.FOCUS) {
                context.getString(R.string.focus_completed_title)
            } else {
                context.getString(R.string.focus_break_completed_title)
            }
            val nextLabel = when (nextMode) {
                PomodoroMode.FOCUS -> context.getString(R.string.focus_mode_focus_label)
                PomodoroMode.SHORT_BREAK -> context.getString(R.string.focus_mode_short_break_label)
                PomodoroMode.LONG_BREAK -> context.getString(R.string.focus_mode_long_break_label)
            }
            val body = context.getString(R.string.focus_notif_next_phase, nextLabel)

            val openFocusIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_FOCUS
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                4204,
                openFocusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_nav_focus)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .build()
        }

        private fun formatRemaining(remainingSeconds: Long): String {
            val safe = remainingSeconds.coerceAtLeast(0L)
            val m = safe / 60
            val s = safe % 60
            return "%02d:%02d".format(m, s)
        }
    }
}
