package com.livelingo.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * An opt-in Android launcher surface for existing, user-initiated workflows.
 *
 * No transcript, clipboard, speech state, device identifier or translation is
 * read or rendered here. A tap opens MainActivity with the same validated
 * route used by dynamic shortcuts; it never records or downloads a model.
 */
internal class TuddyQuickActionsWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.tuddy_quick_actions_widget)
            bindAction(
                context, views, widgetId, R.id.tuddy_widget_type,
                TuddyLauncherDestination.TYPE_TEXT
            )
            bindAction(
                context, views, widgetId, R.id.tuddy_widget_check,
                TuddyLauncherDestination.OFFLINE_STARTCHECK
            )
            bindAction(
                context, views, widgetId, R.id.tuddy_widget_sentence,
                TuddyLauncherDestination.ONE_SENTENCE
            )
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun bindAction(
        context: Context,
        views: RemoteViews,
        widgetId: Int,
        viewId: Int,
        destination: TuddyLauncherDestination
    ) {
        // PendingIntent equality ignores extras. Allocate a distinct request
        // code for each widget instance and each destination.
        val requestCode = widgetId * TuddyLauncherDestination.entries.size +
            destination.ordinal
        val open = TuddyLauncherShortcuts.intentFor(context, destination)
        val pending = PendingIntent.getActivity(
            context,
            requestCode,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pending)
    }
}
