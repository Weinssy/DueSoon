package com.duesoon.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.size
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
        val currentTime = System.currentTimeMillis()
        val upcomingTasks = allTasks
            .filter { !it.completed }
            .sortedBy { it.deadline }
            .take(3)

        provideContent {
            val localContext = androidx.glance.LocalContext.current

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E2E))
                    .cornerRadius(16.dp)
                    .padding(12.dp)
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
                        
                        // Header Add Button
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .background(Color(0xFF313244))
                                .cornerRadius(16.dp)
                                .clickable(
                                    actionStartActivity<MainActivity>(
                                        actionParametersOf(
                                            androidx.glance.action.ActionParameters.Key<Boolean>("quick_add") to true
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (upcomingTasks.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = localContext.getString(R.string.widget_empty_message),
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
        val currentTime = System.currentTimeMillis()
        val tier = com.duesoon.app.domain.util.AttentionRankingEngine.calculateTier(task, currentTime)
        val deadlineColor = when (tier) {
            com.duesoon.app.domain.model.AttentionTier.OVERDUE, 
            com.duesoon.app.domain.model.AttentionTier.CRITICAL -> Color(0xFFEF4444)
            com.duesoon.app.domain.model.AttentionTier.HIGH, 
            com.duesoon.app.domain.model.AttentionTier.ELEVATED -> Color(0xFFF59E0B)
            else -> Color(0xFF94A3B8)
        }

        val deadlineText = task.deadline?.let {
            DateTimeUtils.formatDeadline(it)
        } ?: ""

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF282A36))
                .cornerRadius(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Area (Min 48dp Touch Target)
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .clickable(
                        actionRunCallback<CompleteTaskActionCallback>(
                            actionParametersOf(taskIdKey to task.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(18.dp)
                        .background(Color(0xFF1E1E2E))
                        .cornerRadius(9.dp) // Border radius to simulate a circle
                ) {}
            }

            // Task Body (Clickable to open app)
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(end = 8.dp, top = 6.dp, bottom = 6.dp)
                    .clickable(actionStartActivity<MainActivity>()),
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
}
