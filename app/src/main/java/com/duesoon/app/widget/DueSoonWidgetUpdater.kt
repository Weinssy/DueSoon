package com.duesoon.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object DueSoonWidgetUpdater {
    suspend fun update(context: Context?) {
        if (context == null) return
        try {
            DueSoonWidget().updateAll(context)
        } catch (_: Exception) {
            // Widget might not be placed or Glance not initialized
        }
    }
}
