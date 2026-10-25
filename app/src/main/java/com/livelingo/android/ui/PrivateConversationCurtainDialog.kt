package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Opt-in neutral cover for a shared device. Contains no transcript, language,
 * translation, user identification, files or session details, and must never
 * start a microphone or TTS engine.
 *
 * Deliberate two-tap reveal prevents an incidental touch from exposing the
 * conversation. This is NOT authentication, encryption or screenshot security:
 * anyone with the unlocked device can deliberately reveal it.
 */
@Composable
internal fun PrivateConversationCurtainDialog(
    onReveal: () -> Unit
) {
    var confirmReveal by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = {}, // Back/outside must not accidentally reveal private text.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Gesprek afgedekt",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    "De microfoon en lokale voorlezing zijn gestopt. " +
                        "Je tijdelijke gesprek blijft alleen in het geheugen. " +
                        "Er wordt niets gedeeld of opgeslagen.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Dit scherm verbergt de inhoud alleen binnen Tuddy. " +
                        "Het vergrendelt de telefoon niet, wist geen eerder gekopieerde " +
                        "tekst van Androids klembord en is geen wachtwoordbeveiliging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (confirmReveal) {
                    Text(
                        "Weer tonen? Iedereen met toegang tot deze ontgrendelde " +
                            "telefoon kan het tijdelijke gesprek zien.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                        Text("Ja, toon het gesprek")
                    }
                    OutlinedButton(
                        onClick = { confirmReveal = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Afgedekt houden") }
                } else {
                    Button(
                        onClick = { confirmReveal = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Gesprek weer tonen…") }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
