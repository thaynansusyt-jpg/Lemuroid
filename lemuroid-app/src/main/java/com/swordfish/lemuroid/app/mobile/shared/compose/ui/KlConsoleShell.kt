package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
private fun pointerPreference(): Pair<Boolean, (Boolean) -> Unit> {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("kl_console_ui", Context.MODE_PRIVATE) }
    var enabled by remember(preferences) { mutableStateOf(preferences.getBoolean("touch_pointer", false)) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "touch_pointer") enabled = prefs.getBoolean(key, false)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled to { value: Boolean ->
        enabled = value
        preferences.edit().putBoolean("touch_pointer", value).apply()
    }
}

@Composable
fun KlPointerToggle() {
    val (enabled, update) = pointerPreference()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ponteiro ao tocar")
        Switch(checked = enabled, onCheckedChange = update)
    }
}

@Composable
fun KlConsoleShell(content: @Composable () -> Unit) {
    val (enabled, _) = pointerPreference()
    var position by remember { mutableStateOf(Offset.Zero) }
    var visible by remember { mutableStateOf(false) }
    var eventCount by remember { mutableStateOf(0) }
    LaunchedEffect(eventCount, enabled) {
        if (!enabled) visible = false
        else { delay(900); visible = false }
    }
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(enabled) {
            if (enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val touch = event.changes.firstOrNull { it.type != PointerType.Mouse }
                        if (touch != null) {
                            position = touch.position
                            visible = true
                            eventCount++
                        }
                        // Observe only: do not consume clicks, long presses or scroll gestures.
                    }
                }
            }
        },
    ) {
        content()
        if (enabled && visible) {
            Canvas(Modifier.fillMaxSize()) {
                val unit = 1.dp.toPx()
                translate(position.x + 6 * unit, position.y + 6 * unit) {
                    val hand = Path().apply {
                        moveTo(6 * unit, 23 * unit)
                        lineTo(6 * unit, 4 * unit)
                        cubicTo(6 * unit, -2 * unit, 13 * unit, -2 * unit, 13 * unit, 4 * unit)
                        lineTo(13 * unit, 15 * unit)
                        lineTo(25 * unit, 18 * unit)
                        quadraticBezierTo(30 * unit, 19 * unit, 29 * unit, 26 * unit)
                        lineTo(26 * unit, 38 * unit)
                        lineTo(12 * unit, 38 * unit)
                        lineTo(1 * unit, 27 * unit)
                        quadraticBezierTo(-2 * unit, 20 * unit, 6 * unit, 23 * unit)
                        close()
                    }
                    drawPath(hand, Color.White)
                    drawPath(hand, Color(0xFF007AAE), style = Stroke(width = 2 * unit))
                }
            }
        }
    }
}
