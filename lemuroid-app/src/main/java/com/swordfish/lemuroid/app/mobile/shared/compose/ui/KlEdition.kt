package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings
import com.swordfish.lemuroid.app.shared.profile.KlProfileStore

@Composable
fun rememberKlSonic(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("kl_console_ui", Context.MODE_PRIVATE) }
    var sonic by remember(prefs) { mutableStateOf(prefs.getString("ui_mode", "wiiu") == "sonic") }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key -> if (key == "ui_mode") sonic = p.getString(key, "wiiu") == "sonic" }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return sonic
}

@Composable
fun KlEditionChoice() {
    val context = LocalContext.current
    val sonic = rememberKlSonic()
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tema / UI do KL", style = MaterialTheme.typography.titleMedium)
        listOf("wiiu" to "Wii U • padrão KL", "sonic" to "Sonic Edition • SEGA Edition").forEach { (mode, label) ->
            FilterChip(selected = sonic == (mode == "sonic"), onClick = {
                context.getSharedPreferences("kl_console_ui", Context.MODE_PRIVATE).edit().putString("ui_mode", mode).apply()
                KlProfileStore.eventAsync(context, if (mode == "sonic") "sonic_theme" else "wiiu_theme")
            }, label = { Text(label) })
        }
        Text("O tema muda cores, tipografia e detalhes da interface. A skin azul Sonic é escolhida separadamente no menu dos controles.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun rememberKlFullScreen(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { KlPlaySettings.preferences(context) }
    var full by remember(prefs) { mutableStateOf(KlPlaySettings.enabled(context, KlPlaySettings.FULL_SCREEN)) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key -> if (key == KlPlaySettings.FULL_SCREEN) full = p.getBoolean(key, false) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return full
}

@Composable
fun KlScreenOptions(core: String? = null) {
    val context = LocalContext.current
    val full = rememberKlFullScreen()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tela cheia KL", style = MaterialTheme.typography.titleMedium)
        Row {
            Text("Usar a área dos controles para o jogo", Modifier.weight(1f))
            Switch(checked = full, onCheckedChange = {
                KlPlaySettings.preferences(context).edit().putBoolean(KlPlaySettings.FULL_SCREEN, it).apply()
                if (it) KlProfileStore.eventAsync(context, "fullscreen")
            })
        }
        Text("Controles ficam sobre a imagem. A proporção do jogo e a proteção do recorte da câmera são mantidas.", style = MaterialTheme.typography.bodySmall)
        val systems = if (core == null) listOf("NDS" to KlPlaySettings.NDS_LAYOUT, "3DS" to KlPlaySettings.THREEDS_LAYOUT)
            else when (core) { "citra" -> listOf("3DS" to KlPlaySettings.THREEDS_LAYOUT); "melonds", "desmume" -> listOf("NDS" to KlPlaySettings.NDS_LAYOUT); else -> emptyList() }
        systems.forEach { (name, key) ->
            var value by remember(key) { mutableStateOf(KlPlaySettings.preferences(context).getString(key, "DEFAULT") ?: "DEFAULT") }
            Text("Telas de $name", style = MaterialTheme.typography.titleSmall)
            KlPlaySettings.layouts.forEach { (id, label) ->
                FilterChip(selected = value == id, onClick = {
                    value = id
                    KlPlaySettings.preferences(context).edit().putString(key, id).apply()
                    KlProfileStore.eventAsync(context, if (name == "NDS") "nds_layout" else "3ds_layout")
                }, label = { Text(label) })
            }
            if (name == "NDS") {
                var ratio by remember { mutableStateOf(KlPlaySettings.preferences(context).getInt(KlPlaySettings.NDS_RATIO, 2)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(2, 3).forEach { n ->
                    FilterChip(selected = ratio == n, onClick = { ratio = n; KlPlaySettings.preferences(context).edit().putInt(KlPlaySettings.NDS_RATIO, n).apply() }, label = { Text("Tela grande ${n}:1") })
                } }
            }
        }
        if (systems.isNotEmpty()) Text("Para aplicar o layout, saia do jogo e abra novamente. A opção de tela única esconde a outra tela; volte a duas telas para usar menus e toque quando precisar.", style = MaterialTheme.typography.bodySmall)
    }
}
