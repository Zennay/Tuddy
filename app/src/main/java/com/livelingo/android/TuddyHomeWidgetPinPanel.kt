package com.livelingo.android

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Local launcher capabilities only. Never reads captions, speech, clipboard,
 * app settings or network state. Counts are Android-managed widget instances.
 */
internal data class TuddyHomeWidgetStatus(
    val pinSupported: Boolean,
    val installedCount: Int,
    val available: Boolean
)

internal object TuddyHomeWidgetPin {
    private fun provider(context: Context): ComponentName =
        ComponentName(context, TuddyQuickActionsWidget::class.java)

    fun snapshot(context: Context): TuddyHomeWidgetStatus = runCatching {
        val manager = AppWidgetManager.getInstance(context)
        TuddyHomeWidgetStatus(
            pinSupported = manager.isRequestPinAppWidgetSupported,
            installedCount = manager.getAppWidgetIds(provider(context)).size,
            available = true
        )
    }.getOrDefault(TuddyHomeWidgetStatus(false, 0, false))

    /**
     * Invoke only from a visible, explicit button tap. A true return means
     * Android accepted the REQUEST, not that the user placed the widget.
     * Never attempt to bind widgets or bypass launcher confirmation.
     */
    fun request(context: Context): Boolean = runCatching {
        AppWidgetManager.getInstance(context)
            .requestPinAppWidget(provider(context), null, null)
    }.getOrDefault(false)
}

@Composable
internal fun TuddyHomeWidgetPinPanel(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(context) {
        mutableStateOf(TuddyHomeWidgetPin.snapshot(context))
    }
    var requestNotice by remember { mutableStateOf("") }

    // The launcher may finish a pin dialog while Tuddy is in the background.
    // Android's installed-widget count is re-read when the screen resumes.
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = TuddyHomeWidgetPin.snapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Tuddy-widget · vier veilige startknoppen",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Typ lokaal, bekijk de offline check, zet één zin klaar of open " +
                "afbeeldingstekst. De widget toont geen privé-inhoud, " +
                "opent geen camera en luistert nooit vanzelf.",
            style = MaterialTheme.typography.bodySmall
        )
        if (status.available && status.installedCount > 0) {
            Text(
                "Op dit toestel: ${status.installedCount} Tuddy-widget" +
                    (if (status.installedCount == 1) " geplaatst." else "s geplaatst."),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (status.available && status.pinSupported) {
            OutlinedButton(
                enabled = enabled,
                onClick = {
                    val accepted = TuddyHomeWidgetPin.request(context)
                    status = TuddyHomeWidgetPin.snapshot(context)
                    requestNotice = if (accepted)
                        "Android heeft het widgetverzoek ontvangen. Bevestig de " +
                            "plaatsing in je launcher; dit is nog geen " +
                            "bevestiging dat de widget is toegevoegd."
                    else
                        "De launcher heeft het widgetverzoek geweigerd. " +
                            "Gebruik eventueel het Widgets-menu van je startscherm."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Widget op startscherm aanvragen")
            }
        } else {
            Text(
                if (status.available)
                    "Deze launcher ondersteunt geen directe widgetaanvraag vanuit de app."
                else
                    "De Android-widgetstatus is tijdelijk niet beschikbaar.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "Handmatig: houd het startscherm ingedrukt → Widgets → " +
                "Tuddy · snelle acties. Android bepaalt de plaatsing.",
            style = MaterialTheme.typography.bodySmall
        )
        if (requestNotice.isNotBlank()) {
            Text(requestNotice, style = MaterialTheme.typography.bodySmall)
        }
    }
    TuddyQuickSettingsTilePinPanel(enabled = enabled)
}
