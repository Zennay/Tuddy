package com.livelingo.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.SizeF
import android.widget.RemoteViews

/**
 * Optional private Android 12+ launcher surface. Android selects the compact
 * or regular RemoteViews without waking the app merely to redraw a resize.
 * Both layouts expose the same three explicit, user-initiated destinations.
 *
 * No captions, clipboard, spoken audio, translation or network state is read,
 * cached, rendered or sent by this widget.
 */
internal class TuddyQuickActionsWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val compact = actions(
                context, widgetId, R.layout.tuddy_quick_actions_widget_compact
            )
            val regular = actions(
                context, widgetId, R.layout.tuddy_quick_actions_widget
            )
            // Android 12+ chooses the best fitting RemoteViews when the user
            // rotates or resizes. Do not query models or refresh in background.
            val layouts = mapOf(
                SizeF(125f, 100f) to compact,
                SizeF(220f, 140f) to regular
            )
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(layouts))
        }
    }

    private fun actions(context: Context, widgetId: Int, layoutId: Int): RemoteViews =
        RemoteViews(context.packageName, layoutId).apply {
            bindAction(
                context, this, widgetId, R.id.tuddy_widget_type,
                TuddyLauncherDestination.TYPE_TEXT
            )
            bindAction(
                context, this, widgetId, R.id.tuddy_widget_check,
                TuddyLauncherDestination.OFFLINE_STARTCHECK
            )
            bindAction(
                context, this, widgetId, R.id.tuddy_widget_sentence,
                TuddyLauncherDestination.ONE_SENTENCE
            )
        }

    private fun bindAction(
        context: Context,
        views: RemoteViews,
        widgetId: Int,
        viewId: Int,
        destination: TuddyLauncherDestination
    ) {
        // PendingIntent equality ignores extras. Separate each installed
        // widget/action and never allow mutable/externally supplied payloads.
        val requestCode = widgetId * TuddyLauncherDestination.entries.size +
            destination.ordinal
        val pending = PendingIntent.getActivity(
            context,
            requestCode,
            TuddyLauncherShortcuts.intentFor(context, destination),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pending)
    }
}
