package com.gauto.app.core

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gauto.app.R
import com.gauto.app.ui.MainActivity

/** Небольшой виджет: следующий подъём + три быстрых действия. */
class GautoWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateOne(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val store = GautoStore(context)
        when (intent.action) {
            ACTION_OPEN -> context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ACTION_FOCUS -> QuickActions.startFocus(context, store)
            ACTION_SLEEP -> QuickActions.sleep(context, store)
            ACTION_QUIET -> QuickActions.quiet(context, store)
        }
        AppWidgetManager.getInstance(context).updateAppWidget(
            ComponentName(context, GautoWidget::class.java),
            RemoteViews(context.packageName, R.layout.gauto_widget)
        )
    }

    companion object {
        const val ACTION_OPEN = "com.gauto.app.widget.OPEN"
        const val ACTION_FOCUS = "com.gauto.app.widget.FOCUS"
        const val ACTION_SLEEP = "com.gauto.app.widget.SLEEP"
        const val ACTION_QUIET = "com.gauto.app.widget.QUIET"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GautoWidget::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val store = GautoStore(context)
            val views = RemoteViews(context.packageName, R.layout.gauto_widget)
            views.setTextViewText(R.id.widget_status, QuickActions.shortStatus(store))
            views.setTextViewText(R.id.widget_alarm, QuickActions.nextAlarmLabel(store))
            views.setOnClickPendingIntent(R.id.widget_title, pending(context, ACTION_OPEN, 0))
            views.setOnClickPendingIntent(R.id.widget_alarm, pending(context, ACTION_OPEN, 1))
            views.setOnClickPendingIntent(R.id.widget_focus, pending(context, ACTION_FOCUS, 2))
            views.setOnClickPendingIntent(R.id.widget_sleep, pending(context, ACTION_SLEEP, 3))
            views.setOnClickPendingIntent(R.id.widget_quiet, pending(context, ACTION_QUIET, 4))
            manager.updateAppWidget(id, views)
        }

        private fun pending(context: Context, action: String, request: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, request,
                Intent(context, GautoWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
