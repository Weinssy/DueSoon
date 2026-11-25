package com.duesoon.app.ui.focus

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duesoon.app.R
import com.duesoon.app.domain.focus.CourseFocusStat
import com.duesoon.app.domain.focus.DailyFocusBar
import com.duesoon.app.domain.focus.FocusStatsSummary
import com.duesoon.app.domain.model.PomodoroMode
import com.duesoon.app.domain.model.PomodoroSettings
import com.duesoon.app.domain.model.Task
import com.duesoon.app.ui.AppViewModelProvider
import com.duesoon.app.ui.components.CategoryChip
import com.duesoon.app.ui.components.DeadlineLabel
import com.duesoon.app.ui.components.PriorityIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    navigateBack: () -> Unit = {},
    onTaskCompleted: () -> Unit = {},
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showTaskPickerSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.completionEventId) {
        if (uiState.completionEventId > 0L) {
            triggerCompletionVibration(context)
            if (uiState.pomodoroSettings.soundEnabled) {
                playCompletionTone()
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "FocusProgress"
    )

    val modeAccentColor = when (uiState.mode) {
        PomodoroMode.FOCUS -> MaterialTheme.colorScheme.primary
        PomodoroMode.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
        PomodoroMode.LONG_BREAK -> MaterialTheme.colorScheme.secondary
    }

    if (showSettingsDialog) {
        PomodoroSettingsDialog(
            currentSettings = uiState.pomodoroSettings,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                viewModel.updatePomodoroSettings(updated)
                showSettingsDialog = false
            }
        )
    }

    if (showTaskPickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTaskPickerSheet = false },
            sheetState = sheetState
        ) {
            TaskPickerSheetContent(
                activeTasks = uiState.activeTasks,
                selectedTaskId = uiState.task?.id,
                taskTotals = uiState.stats.taskTotals,
                onSelectTask = { taskId ->
                    viewModel.selectTask(taskId)
                    showTaskPickerSheet = false
                }
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showBackButton) stringResource(R.string.title_focus_session)
                        else stringResource(R.string.nav_focus)
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tune),
                            contentDescription = stringResource(R.string.focus_settings_title)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Mode Switcher (Focus / Short Break / Long Break)
            val modes = listOf(
                PomodoroMode.FOCUS to stringResource(R.string.focus_mode_focus),
                PomodoroMode.SHORT_BREAK to stringResource(R.string.focus_mode_short_break),
                PomodoroMode.LONG_BREAK to stringResource(R.string.focus_mode_long_break)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = uiState.mode == mode,
                        onClick = { viewModel.switchMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }

            // 2. Cycle Progress & Daily Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.focus_cycle_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (i in 0 until uiState.cycleTarget) {
                        val filled = i < uiState.cycleCount
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) modeAccentColor
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.focus_today_counter,
                        uiState.stats.todaySessions,
                        uiState.stats.todayMinutes
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Linked Task Target Card
            LinkedTaskCard(
                task = uiState.task,
                taskSummary = uiState.task?.let { uiState.stats.taskTotals[it.id] },
                canChangeTask = !uiState.isTaskScopedMode,
                onPickTask = { showTaskPickerSheet = true },
                onClearTask = { viewModel.selectTask(null) },
                onCompleteTask = { viewModel.completeTask(onTaskCompleted) }
            )

            // 4. Circular Timer Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = modeAccentColor,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.formattedTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (uiState.mode) {
                            PomodoroMode.FOCUS -> stringResource(R.string.focus_mode_focus_label)
                            PomodoroMode.SHORT_BREAK -> stringResource(R.string.focus_mode_short_break_label)
                            PomodoroMode.LONG_BREAK -> stringResource(R.string.focus_mode_long_break_label)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Phase Completion Banner
            AnimatedVisibility(visible = uiState.isFinished && uiState.lastCompletedMode != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (uiState.lastCompletedMode == PomodoroMode.FOCUS) {
                                stringResource(R.string.focus_completed_title)
                            } else {
                                stringResource(R.string.focus_break_completed_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.lastCompletedMode == PomodoroMode.FOCUS) {
                                stringResource(R.string.focus_completed_message)
                            } else {
                                stringResource(R.string.focus_break_completed_message)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 6. Quick Focus Duration Presets (shown when idle in FOCUS mode)
            if (!uiState.isRunning && uiState.mode == PomodoroMode.FOCUS) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        15L to R.string.focus_preset_15,
                        25L to R.string.focus_preset_25,
                        50L to R.string.focus_preset_50
                    ).forEach { (minutes, labelRes) ->
                        FilterChip(
                            selected = uiState.totalSeconds == minutes * 60L,
                            onClick = { viewModel.setDuration(minutes) },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }

            // 7. Timer Controls (Reset | Play/Pause | Skip | +5m)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.action_reset)
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = modeAccentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (uiState.isRunning) {
                        Text(
                            text = "II",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.action_resume),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                OutlinedIconButton(
                    onClick = { viewModel.skipSession() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = stringResource(R.string.focus_action_skip)
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.addMinutes(5) }
            ) {
                Text(stringResource(R.string.action_add_5_minutes))
            }

            // 8. Focus Statistics Card (Weekly, Streak, Avg, 7-day chart, Course breakdown)
            FocusStatisticsSection(stats = uiState.stats)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LinkedTaskCard(
    task: Task?,
    taskSummary: com.duesoon.app.domain.focus.TaskFocusSummary?,
    canChangeTask: Boolean,
    onPickTask: () -> Unit,
    onClearTask: () -> Unit,
    onCompleteTask: () -> Unit
) {
    if (task == null) {
        if (canChangeTask) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickTask)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.focus_target_empty_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.focus_target_empty_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onPickTask) {
                        Text(stringResource(R.string.focus_action_pick_task))
                    }
                }
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.focus_target_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canChangeTask) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onPickTask) {
                            Text(stringResource(R.string.action_change))
                        }
                        IconButton(
                            onClick = onClearTask,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.focus_action_unlink_task),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityIndicator(priority = task.priority)
                task.category?.let { CategoryChip(category = it) }
                if (task.deadline != null) {
                    DeadlineLabel(task = task)
                }
            }

            if (taskSummary != null && taskSummary.sessions > 0) {
                Text(
                    text = stringResource(
                        R.string.focus_task_logged_summary,
                        taskSummary.sessions,
                        taskSummary.minutes
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!task.completed) {
                Button(
                    onClick = onCompleteTask,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_complete_task))
                }
            }
        }
    }
}

@Composable
private fun FocusStatisticsSection(stats: FocusStatsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.focus_stats_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // 3 KPI tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatKpiTile(
                    label = stringResource(R.string.focus_stat_week),
                    value = "${stats.weekMinutes} mnt",
                    subLabel = "${stats.weekSessions} sesi",
                    modifier = Modifier.weight(1f)
                )
                StatKpiTile(
                    label = stringResource(R.string.focus_stat_streak),
                    value = "${stats.streakDays} hr",
                    subLabel = stringResource(R.string.focus_stat_streak_sub),
                    modifier = Modifier.weight(1f)
                )
                StatKpiTile(
                    label = stringResource(R.string.focus_stat_avg),
                    value = "${stats.averageDailyMinutes} mnt",
                    subLabel = stringResource(R.string.focus_stat_avg_sub),
                    modifier = Modifier.weight(1f)
                )
            }

            if (!stats.hasAnyHistory) {
                Text(
                    text = stringResource(R.string.focus_stats_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 7-day bar chart
            if (stats.last7Days.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.focus_chart_7days_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SevenDayBarChart(days = stats.last7Days)
            }

            // Per-course / category breakdown
            if (stats.courseStats.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.focus_course_stats_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.courseStats.forEach { course ->
                        CourseStatRow(course = course)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatKpiTile(
    label: String,
    value: String,
    subLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SevenDayBarChart(days: List<DailyFocusBar>) {
    val maxMinutes = days.maxOfOrNull { it.minutes } ?: 0
    val chartDesc = "Grafik fokus 7 hari terakhir"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .semantics { contentDescription = chartDesc },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val fraction = if (day.minutes > 0 && maxMinutes > 0) {
                (day.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.08f, 1f)
            } else {
                0f
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (day.minutes > 0) "${day.minutes}" else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.65f)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (day.isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CourseStatRow(course: CourseFocusStat) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${course.minutes} mnt (${course.sessions} sesi)",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { course.shareFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun TaskPickerSheetContent(
    activeTasks: List<Task>,
    selectedTaskId: Long?,
    taskTotals: Map<Long, com.duesoon.app.domain.focus.TaskFocusSummary>,
    onSelectTask: (Long?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.focus_picker_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (activeTasks.isEmpty()) {
            Text(
                text = stringResource(R.string.focus_picker_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                if (selectedTaskId != null) {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.focus_action_unlink_task)) },
                            modifier = Modifier.clickable { onSelectTask(null) }
                        )
                        HorizontalDivider()
                    }
                }
                items(activeTasks, key = { it.id }) { task ->
                    val summary = taskTotals[task.id]
                    ListItem(
                        headlineContent = {
                            Text(
                                text = task.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (task.id == selectedTaskId) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        supportingContent = {
                            val parts = buildList {
                                task.category?.takeIf { it.isNotBlank() }?.let { add(it) }
                                if (summary != null && summary.sessions > 0) {
                                    add("${summary.sessions} sesi · ${summary.minutes} mnt")
                                }
                            }
                            if (parts.isNotEmpty()) {
                                Text(parts.joinToString(" • "))
                            }
                        },
                        trailingContent = {
                            if (task.id == selectedTaskId) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onSelectTask(task.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PomodoroSettingsDialog(
    currentSettings: PomodoroSettings,
    onDismiss: () -> Unit,
    onSave: (PomodoroSettings) -> Unit
) {
    var focusText by remember { mutableStateOf(currentSettings.focusMinutes.toString()) }
    var shortBreakText by remember { mutableStateOf(currentSettings.shortBreakMinutes.toString()) }
    var longBreakText by remember { mutableStateOf(currentSettings.longBreakMinutes.toString()) }
    var autoStart by remember { mutableStateOf(currentSettings.autoStartNext) }
    var soundEnabled by remember { mutableStateOf(currentSettings.soundEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = focusText,
                    onValueChange = { focusText = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.focus_settings_focus_min)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortBreakText,
                    onValueChange = { shortBreakText = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.focus_settings_short_break_min)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = longBreakText,
                    onValueChange = { longBreakText = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.focus_settings_long_break_min)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.focus_settings_auto_start),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = autoStart, onCheckedChange = { autoStart = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.focus_settings_sound),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val focus = PomodoroSettings.sanitizeFocusMinutes(
                        focusText.toIntOrNull() ?: currentSettings.focusMinutes
                    )
                    val shortBreak = PomodoroSettings.sanitizeBreakMinutes(
                        shortBreakText.toIntOrNull() ?: currentSettings.shortBreakMinutes
                    )
                    val longBreak = PomodoroSettings.sanitizeBreakMinutes(
                        longBreakText.toIntOrNull() ?: currentSettings.longBreakMinutes
                    )
                    onSave(
                        PomodoroSettings(
                            focusMinutes = focus,
                            shortBreakMinutes = shortBreak,
                            longBreakMinutes = longBreak,
                            autoStartNext = autoStart,
                            soundEnabled = soundEnabled
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun triggerCompletionVibration(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(450L, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {
        // Ignore if device has no vibrator
    }
}

private fun playCompletionTone() {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
    } catch (_: Exception) {
        // Ignore if audio service unavailable
    }
}
