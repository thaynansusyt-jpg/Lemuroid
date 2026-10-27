package com.swordfish.lemuroid.app.mobile.feature.multiplayer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.multiplayer.KlDsWfc
import com.swordfish.lemuroid.app.mobile.feature.settings.general.KlDsSettings

@Composable
fun KlMultiplayerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kl_play_settings", 0) }
    val available = remember { KlDsWfc.available(context) }
    var online by remember { mutableStateOf(KlDsWfc.enabled(context)) }
    var legacy by rememberSaveable { mutableStateOf(false) }
    if (legacy) {
        Column(modifier) {
            TextButton(onClick = { legacy = false }) { Text("Voltar ao Nintendo DS") }
            KlLegacyCableScreen(Modifier.weight(1f))
        }
        return
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("JOGAR JUNTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Nintendo DS Online", style = MaterialTheme.typography.headlineLarge)
        Text("Encontre seus amigos de volta ao mundo do DS.", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Kaeru WFC", style = MaterialTheme.typography.titleLarge)
                Text(if (available) "Núcleo melonDS DS incluído • integração experimental" else "Núcleo online ausente nesta compilação")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Usar DS Online nos jogos DS", Modifier.weight(1f))
                    Switch(checked = online, enabled = available || online, onCheckedChange = {
                        online = it; prefs.edit().putBoolean(KlDsWfc.ENABLED, it).apply()
                    })
                }
                Text("Feche o jogo antes de mudar. Ao ativar, os jogos DS abrem no núcleo online, desde a tela inicial, usando seu save de bateria. Estados rápidos ficam desativados nesta sessão.")
            }
        }
        Text("Como conectar", style = MaterialTheme.typography.titleLarge)
        Text("1. Ative DS Online e abra um jogo com Nintendo WFC.\n2. Nas configurações Nintendo WFC do jogo, procure a rede melonAP e salve a conexão.\n3. Entre no modo Nintendo WFC do próprio jogo e troque Friend Codes quando necessário.")
        Text("O Kaeru WFC atende funções online compatíveis com os servidores comunitários. Salas locais, Download Play e todas as funções de todos os jogos não estão garantidos. A configuração da conexão não é um teste de troca ou batalha.")
        OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://kaeru.world/projects/wfc"))) }) { Text("Sobre o Kaeru WFC") }
        HorizontalDivider()
        KlDsSettings()
        HorizontalDivider()
        Text("GBA", style = MaterialTheme.typography.titleMedium)
        Text("O multiplayer experimental de GBA foi aposentado. Seus jogos e saves continuam disponíveis no modo individual.")
        TextButton(onClick = { legacy = true }) { Text("Ferramentas antigas de cabo GB / GBC") }
    }
}
