package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import kotlin.math.roundToInt

@Composable
fun KlPlaySettingsPanel() {
    LemuroidCardSettingsGroup(title = { Text("Velocidade e desempenho KL") }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KlFastForwardControl()
            Text("2x a 8x são velocidades solicitadas. A velocidade atingida depende do jogo e do celular.", style = MaterialTheme.typography.bodySmall)
            KlPlayToggle(KlPlaySettings.SIMPLE_FILTER, "Filtro leve", "Usa o filtro simples e desliga o efeito HD durante o jogo.")
            KlPlayToggle(KlPlaySettings.NATIVE_RESOLUTION, "Resolução nativa", "Usa resolução 1x em 3DS e PSP, reduzindo a carga gráfica.")
            KlPlayToggle(KlPlaySettings.SHADER_CACHE, "Cache de shaders 3DS", "Reutiliza shaders. O primeiro carregamento ainda pode ter pequenas pausas.")
            Text("Os ajustes de desempenho valem ao sair do jogo e abri-lo novamente. Suas configurações anteriores ficam guardadas.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun KlFastForwardControl() {
    val context = LocalContext.current
    val prefs = remember { KlPlaySettings.preferences(context) }
    var speed by remember { mutableStateOf(KlPlaySettings.speed(context).toFloat()) }
    fun save(value: Int) {
        speed = value.coerceIn(2, 8).toFloat()
        prefs.edit().putInt(KlPlaySettings.SPEED, speed.toInt()).apply()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Avanço rápido: ${speed.roundToInt()}x", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(2, 4, 8).forEach { value ->
                FilterChip(selected = speed.roundToInt() == value, onClick = { save(value) }, label = { Text("${value}x") })
            }
        }
        Slider(value = speed, onValueChange = { speed = it }, onValueChangeFinished = { save(speed.roundToInt()) }, valueRange = 2f..8f, steps = 5)
        Text("Arraste para escolher 2, 3, 4, 5, 6, 7 ou 8x.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun KlPlayToggle(key: String, title: String, description: String) {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(KlPlaySettings.enabled(context, key)) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = {
            checked = it
            KlPlaySettings.preferences(context).edit().putBoolean(key, it).apply()
        })
    }
}
