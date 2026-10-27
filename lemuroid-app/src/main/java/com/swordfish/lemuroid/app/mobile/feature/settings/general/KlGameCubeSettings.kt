package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings

@Composable
fun KlGameCubeSettings() {
    val context = LocalContext.current
    val prefs = remember { KlPlaySettings.preferences(context) }
    var scale by remember { mutableStateOf(prefs.getInt("kl_gc_scale", 1)) }
    var dual by remember { mutableStateOf(prefs.getBoolean("kl_gc_dual_core", false)) }
    Text("GameCube • Dolphin", style = MaterialTheme.typography.titleLarge)
    Text("Comece em 1x no Galaxy A15. A velocidade depende do jogo. Feche e reabra o jogo para aplicar.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1, 2).forEach { value ->
            FilterChip(selected = scale == value, onClick = { scale = value; prefs.edit().putInt("kl_gc_scale", value).apply() }, label = { Text("${value}x") })
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Dual Core experimental", Modifier.weight(1f))
        Switch(checked = dual, onCheckedChange = { dual = it; prefs.edit().putBoolean("kl_gc_dual_core", it).apply() })
    }
    Text("Dual Core pode melhorar a velocidade e causar falhas em alguns jogos. Saves são feitos no cartão de memória virtual do próprio jogo; estados rápidos estão desativados.", style = MaterialTheme.typography.bodySmall)
    Text("Formatos: GCM, RVZ, GCZ e ISO de GameCube. O KL reconhece ISO pelo cabeçalho, sem confundir com PSP/PS1. Nenhum jogo está incluído.", style = MaterialTheme.typography.bodySmall)
}
