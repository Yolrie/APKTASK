package com.example.apktask.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.apktask.R
import com.example.apktask.data.LocalDataSource
import com.example.apktask.model.TaskStatus
import com.example.apktask.ui.MainActivity
import com.example.apktask.util.DateUtils

/**
 * Home screen widget — shows today's task progress (X/Y + progress bar + streak).
 *
 * Data is read from [LocalDataSource] (SQLCipher-encrypted Room DB).
 * onUpdate is called by the system at most every 30 min (updatePeriodMillis).
 * WorkManager triggers [update] on each task save for real-time accuracy.
 *
 * Security:
 *  - FLAG_SECURE applies to the Activity, not RemoteViews. The widget
 *    intentionally shows only aggregate progress (count), never task titles.
 *  - Tap launches MainActivity (back stack preserved via FLAG_UPDATE_CURRENT).
 */
class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            update(context, appWidgetManager, widgetId)
        }
    }

    companion object {

        /**
         * Builds and pushes a [RemoteViews] update for a single widget instance.
         * Called from [onUpdate] and from WorkManager after each task save.
         */
        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val local = LocalDataSource.getInstance(context)
            val today = DateUtils.today()
            val tasks = local.loadTasks(today)
            val streak = local.loadStreak()

            val total = tasks.size
            val done = tasks.count { it.status == TaskStatus.COMPLETED }
            val percent = if (total > 0) (done * 100) / total else 0

            val views = RemoteViews(context.packageName, R.layout.widget_tasks)

            // Progress fraction
            val progressText = if (total > 0) "$done / $total" else context.getString(R.string.widget_no_tasks)
            views.setTextViewText(R.id.tvWidgetProgress, progressText)
            views.setProgressBar(R.id.progressWidget, 100, percent, false)

            // Streak
            if (streak.count > 0) {
                views.setViewVisibility(R.id.tvWidgetStreak, View.VISIBLE)
                views.setTextViewText(
                    R.id.tvWidgetStreak,
                    context.getString(R.string.widget_streak_format, streak.count)
                )
            } else {
                views.setViewVisibility(R.id.tvWidgetStreak, View.GONE)
            }

            // Tap to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tvWidgetProgress, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }

        /**
         * Triggers a widget refresh for all active instances of this provider.
         * Call this from any place that modifies task data (WorkManager, ViewModel).
         */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, TaskWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val provider = TaskWidgetProvider()
                provider.onUpdate(context, manager, ids)
            }
        }
    }
}
