package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.KlDsOptions
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KlDsSettings() {
    val context = LocalContext.current
    val prefs = remember { KlPlaySettings.preferences(context) }
    var profile by remember { mutableStateOf(prefs.getString(KlPlaySettings.NDS_PERFORMANCE, "CORE")) }
    Text("Nintendo DS", style = MaterialTheme.typography.titleLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KlDsOptions.profiles.forEach { (key, label) ->
            FilterChip(selected = profile == key, onClick = {
                profile = key; prefs.edit().putString(KlPlaySettings.NDS_PERFORMANCE, key).apply()
            }, label = { Text(label) })
        }
    }
    Text(when (profile) {
        "BALANCED" -> "JIT e renderização em outra thread. Primeiro perfil para testar."
        "FAST" -> "JIT com blocos de 32 instruções e renderização em outra thread. Teste por jogo."
        "COMPATIBLE" -> "Desativa JIT e a renderização em outra thread para diagnosticar falhas. Pode ficar mais lento."
        else -> "Preserva os ajustes individuais do núcleo."
    })
    Text("Feche e reabra o jogo para aplicar. Perfis para melonDS; o ganho depende do jogo e do aparelho.", style = MaterialTheme.typography.bodySmall)
}
