package com.duesoon.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.duesoon.app.DueSoonApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val taskIdKey = ActionParameters.Key<Long>("taskId")

class CompleteTaskActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return
        val app = context.applicationContext as DueSoonApplication
        val useCase = app.container.completeTaskUseCase

        withContext(Dispatchers.IO) {
            useCase(taskId)
        }
    }
}
