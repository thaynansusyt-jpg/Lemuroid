package com.swordfish.lemuroid.app.mobile.feature.multiplayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.multiplayer.KlLinkSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun KlLinkStatus(modifier: Modifier = Modifier) {
    if (!KlLinkSession.active) return
    var message by remember { mutableStateOf("Preparando cabo GBA…") }
    LaunchedEffect(Unit) {
        while (isActive) {
            val status = withContext(Dispatchers.IO) {
                runCatching { KlLinkSession.statusFile?.readText()?.take(256) }.getOrNull()
            }
            if (!status.isNullOrBlank()) message = status
            delay(500)
        }
    }
    Surface(modifier, tonalElevation = 4.dp) {
        Text(message, modifier = Modifier.padding(8.dp))
    }
}
