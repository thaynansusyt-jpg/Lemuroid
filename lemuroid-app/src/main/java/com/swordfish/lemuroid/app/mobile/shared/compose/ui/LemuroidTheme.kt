package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
private fun klDarkPreference(): Pair<Boolean, (Boolean) -> Unit> {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) { context.getSharedPreferences("kl_console_ui", Context.MODE_PRIVATE) }
    var dark by remember(prefs) { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "dark_mode") dark = p.getBoolean(key, false)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return dark to { value: Boolean -> dark = value; prefs.edit().putBoolean("dark_mode", value).apply() }
}

@Composable
fun KlDarkModeToggle() {
    val (dark, update) = klDarkPreference()
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Modo escuro", style = MaterialTheme.typography.titleMedium)
            Text("Tema KL noturno para biblioteca, menus e configurações.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = dark, onCheckedChange = update)
    }
}

@Composable
fun AppTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val (savedDark, _) = klDarkPreference()
    val sonic = rememberKlSonic()
    val colors = if (sonic && (darkTheme ?: savedDark)) darkColorScheme(primary = Color(0xFF69A4FF), onPrimary = Color(0xFF001E58), secondary = Color(0xFFFFD342), primaryContainer = Color(0xFF063D9C), onPrimaryContainer = Color.White, secondaryContainer = Color(0xFF5A4200), onSecondaryContainer = Color(0xFFFFE58B), background = Color(0xFF070F26), surface = Color(0xFF111F3D), surfaceVariant = Color(0xFF203455))
    else if (sonic) lightColorScheme(primary = Color(0xFF0758D8), onPrimary = Color.White, secondary = Color(0xFF846400), primaryContainer = Color(0xFFD9E7FF), onPrimaryContainer = Color(0xFF002B73), secondaryContainer = Color(0xFFFFE58B), onSecondaryContainer = Color(0xFF443000), background = Color(0xFFF0F5FF), surface = Color.White, surfaceVariant = Color(0xFFE1EAFA))
    else if (darkTheme ?: savedDark) darkColorScheme(
        primary = Color(0xFF74D3FF), onPrimary = Color(0xFF00364F),
        primaryContainer = Color(0xFF124563), onPrimaryContainer = Color(0xFFD6F2FF),
        secondary = Color(0xFFAFD1E2), secondaryContainer = Color(0xFF263F50),
        onSecondaryContainer = Color(0xFFDCEFFF),
        background = Color(0xFF0D1720), onBackground = Color(0xFFE3EFF5),
        surface = Color(0xFF14212C), onSurface = Color(0xFFE3EFF5),
        surfaceVariant = Color(0xFF253543), onSurfaceVariant = Color(0xFFB7CDD9),
        outline = Color(0xFF7D99AA), outlineVariant = Color(0xFF3B5160),
    ) else lightColorScheme(
        primary = Color(0xFF007AAE), onPrimary = Color.White,
        primaryContainer = Color(0xFFD6F2FF), onPrimaryContainer = Color(0xFF163A50),
        secondary = Color(0xFF477B94), secondaryContainer = Color(0xFFE0F5FF),
        onSecondaryContainer = Color(0xFF163A50),
        background = Color(0xFFF0F7FA), onBackground = Color(0xFF163A50),
        surface = Color(0xFFFCFEFF), onSurface = Color(0xFF163A50),
        surfaceVariant = Color(0xFFE5EFF4), onSurfaceVariant = Color(0xFF476579),
        outline = Color(0xFF7496A8), outlineVariant = Color(0xFFC9DEE8),
    )
    val typography = if (sonic) Typography(headlineLarge = Typography().headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), titleLarge = Typography().titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) else Typography()
    MaterialTheme(colorScheme = colors, typography = typography) { content() }
}
