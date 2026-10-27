package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.swordfish.lemuroid.app.shared.game.KlGameLaunch

@Composable
fun KlGameLaunchOverlay() {
    val coordinator = KlGameLaunch.coordinator
    val status by coordinator.status.collectAsState()
    val clipboard = LocalClipboardManager.current
    status?.let { current ->
        AlertDialog(
            onDismissRequest = { if (current.error) coordinator.dismissError() },
            properties = DialogProperties(dismissOnBackPress = current.error, dismissOnClickOutside = current.error),
            title = { Text(if (current.error) "Não foi possível abrir" else current.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!current.error) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(current.message)
                }
            },
            confirmButton = {
                if (current.error) TextButton(onClick = coordinator::dismissError) { Text("Entendi") }
            },
            dismissButton = {
                if (current.error) TextButton(onClick = {
                    clipboard.setText(AnnotatedString("KL Play 1.0.0-rc.5.1 | ${current.title} | ${current.message}"))
                }) { Text("Copiar erro") }
            },
        )
    }
}
