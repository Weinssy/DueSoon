package com.duesoon.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.duesoon.app.DueSoonApplication
import com.duesoon.app.MainActivity
import com.duesoon.app.R
import com.duesoon.app.domain.model.DeadlineState
import com.duesoon.app.domain.model.Task
import com.duesoon.app.domain.util.DateTimeUtils
import com.duesoon.app.domain.util.DeadlineStateCalculator
import kotlinx.coroutines.flow.first

class DueSoonWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DueSoonApplication
        val allTasks = app.container.taskRepository.observeTasks().first()
        val upcomingTasks = allTasks
            .filter { !it.completed && it.deadline != null }
            .sortedBy { it.deadline }
            .take(3)

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E2E))
                    .cornerRadius(16.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DueSoon",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF6366F1)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = context.getString(R.string.widget_deadline_count, upcomingTasks.size),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF94A3B8)),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (upcomingTasks.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = context.getString(R.string.widget_empty_message),
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF94A3B8)),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    } else {
                        upcomingTasks.forEachIndexed { index, task ->
                            if (index > 0) {
                                Spacer(modifier = GlanceModifier.height(6.dp))
                            }
                            WidgetItem(task = task)
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetItem(task: Task) {
        val state = DeadlineStateCalculator.calculate(task)
        val deadlineColor = when (state) {
            DeadlineState.OVERDUE -> Color(0xFFEF4444)
            DeadlineState.DUE_TODAY, DeadlineState.DUE_SOON -> Color(0xFFF59E0B)
            else -> Color(0xFF94A3B8)
        }

        val deadlineText = task.deadline?.let {
            DateTimeUtils.formatDeadline(it)
        } ?: ""

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF282A36))
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = task.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = deadlineText,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(deadlineColor),
                        fontSize = 11.sp
                    )
                )
            }

            if (!task.category.isNullOrBlank()) {
                Text(
                    text = task.category,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFA5B4FC)),
                        fontSize = 10.sp
                    ),
                    modifier = GlanceModifier.padding(start = 4.dp)
                )
            }
        }
    }
}
