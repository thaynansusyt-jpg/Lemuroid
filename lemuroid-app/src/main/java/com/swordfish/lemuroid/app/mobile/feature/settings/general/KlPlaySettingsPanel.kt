package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.saveable.rememberSaveable
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KlSettingsSection("Aparência e sons", "Tema do app e som dos menus", true) {
            com.swordfish.lemuroid.app.mobile.shared.compose.ui.KlEditionChoice()
            KlPlayToggle(KlPlaySettings.CLICK_SOUND, "Som dos menus", "Usa o som de clique do Android e respeita os sons do sistema.")
        }
        KlSettingsSection("Telas e controles", "Tela cheia e disposição das duas telas") {
            com.swordfish.lemuroid.app.mobile.shared.compose.ui.KlScreenOptions()
            Text("Para criar uma skin, abra um jogo e entre no menu dos controles. Cada console guarda um ajuste para vertical e outro para horizontal.", style = MaterialTheme.typography.bodySmall)
        }
        KlSettingsSection("Desempenho", "Avanço rápido, filtros e resolução") {
            KlFastForwardControl()
            Text("A velocidade atingida depende do jogo e do celular.", style = MaterialTheme.typography.bodySmall)
            KlPlayToggle(KlPlaySettings.SIMPLE_FILTER, "Filtro leve", "Usa o filtro simples e desliga o efeito HD durante o jogo.")
            KlPlayToggle(KlPlaySettings.NATIVE_RESOLUTION, "Resolução nativa", "Usa resolução 1x em 3DS e PSP, reduzindo a carga gráfica.")
            KlPlayToggle(KlPlaySettings.SHADER_CACHE, "Cache de shaders 3DS", "Reutiliza shaders. O primeiro carregamento ainda pode ter pausas.")
        }
        KlSettingsSection("CPU emulada do 3DS", "Ajuste avançado de clock") {
            KlCpuClockControl()
        }
    }
}

@Composable
private fun KlSettingsSection(title: String, subtitle: String, initiallyOpen: Boolean = false, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    val clickSound = com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberKlMenuClick()
    LemuroidCardSettingsGroup(title = { Text(title) }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(subtitle, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { clickSound(); expanded = !expanded }) { Text(if (expanded) "Fechar" else "Abrir") }
            }
            if (expanded) { HorizontalDivider(); content() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KlCpuClockControl() {
    val context = LocalContext.current
    val prefs = remember { KlPlaySettings.preferences(context) }
    var clock by remember { mutableStateOf(prefs.getInt(KlPlaySettings.CPU_3DS, 0)) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 50, 75, 100, 125, 150, 200).forEach { value ->
            FilterChip(selected = clock == value, onClick = {
                clock = value; prefs.edit().putInt(KlPlaySettings.CPU_3DS, value).apply()
            }, label = { Text(if (value == 0) "Configuração do núcleo" else "${value}%") })
        }
    }
    Text("Ajusta a CPU virtual do 3DS. Abaixo de 100% pode aliviar a carga, mas mudar a velocidade de partes do jogo. Acima de 100% pode reduzir lentidão do console e exigir mais do celular. Não aumenta o clock do aparelho.", style = MaterialTheme.typography.bodySmall)
    Text("Feche e abra o jogo para aplicar. Escolha configuração do núcleo para voltar aos ajustes anteriores.", style = MaterialTheme.typography.bodySmall)
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
    var checked by remember { mutableStateOf(if (key == KlPlaySettings.CLICK_SOUND) KlPlaySettings.preferences(context).getBoolean(key, true) else KlPlaySettings.enabled(context, key)) }
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
