package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.profile.KlSegaEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun KlSegaEventBanner() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Evento SEGA Edition", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(when (KlSegaEvent.phase(now)) {
            KlSegaEvent.Phase.UPCOMING -> "Começa em 05/10, às 21h11 (Brasília)."
            KlSegaEvent.Phase.ACTIVE -> "Tempo restante: ${KlSegaEvent.countdown(now)}"
            KlSegaEvent.Phase.ENDED -> "Evento encerrado. Sua coleção continua!"
        }, style = MaterialTheme.typography.bodyMedium)
        Text("Até 15/10/2026, às 21h11 (Brasília). As 128 conquistas e os conjuntos Sonic/Super Sonic continuam disponíveis depois do evento.",
            style = MaterialTheme.typography.bodySmall)
    }
}
