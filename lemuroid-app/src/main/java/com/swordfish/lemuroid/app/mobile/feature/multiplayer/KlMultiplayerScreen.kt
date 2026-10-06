package com.swordfish.lemuroid.app.mobile.feature.multiplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.multiplayer.KlRoomService
import com.swordfish.lemuroid.app.shared.multiplayer.KlWifiRoom

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KlMultiplayerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state by KlRoomService.room.state.collectAsState()
    val prefs = remember { context.getSharedPreferences("kl_room_ui", 0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "Jogador").orEmpty()) }
    var ip by rememberSaveable { mutableStateOf(prefs.getString("ip", "").orEmpty()) }
    var cable by rememberSaveable { mutableStateOf(prefs.getBoolean("cable", false)) }
    var mode by rememberSaveable { mutableStateOf(prefs.getString("gba_mode", "MULTIPAK") ?: "MULTIPAK") }
    var error by remember { mutableStateOf<String?>(null) }
    val connected = state.phase == KlWifiRoom.Phase.HOSTING || state.phase == KlWifiRoom.Phase.JOINED
    val connecting = state.phase == KlWifiRoom.Phase.CONNECTING
    fun start(host: Boolean) {
        error = null
        if (name.isBlank()) { error = "Escolha um nome para entrar na sala."; return }
        if (!host && !KlWifiRoom.validAddress(ip)) { error = "Digite o IP do anfitrião."; return }
        prefs.edit().putString("name", name).putString("ip", ip).apply()
        runCatching { KlRoomService.start(context, host, name, ip) }
            .onFailure { error = "Não foi possível manter a sala aberta: ${it.message}" }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("KL Play • Conexão", style = MaterialTheme.typography.headlineMedium)
        Text("Uma área só para jogar junto. Conecte os celulares à mesma rede Wi-Fi ou ao ponto de acesso de um deles.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Salas Wi-Fi • até 3 pessoas", style = MaterialTheme.typography.titleLarge)
                Text("Cabo GB, GBC e GBA • testes com 2 jogadores • Android de 64 bits. A sala ainda comporta 3 pessoas, mas o cabo funciona com apenas 2.")
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Usar cabo Wi-Fi no GB, GBC e GBA", modifier = Modifier.weight(1f))
                    Switch(checked = cable, onCheckedChange = {
                        cable = it
                        prefs.edit().putBoolean("cable", it).apply()
                    })
                }
                Text("1. Ative o cabo e escolha o mesmo modo nos dois aparelhos.\n2. Crie a sala e conecte o segundo celular.\n3. Na biblioteca, abram a mesma versão do jogo.\n4. Esperem a confirmação de conexão e escolham a opção multiplayer dentro do jogo.")
                Text("Conexão do GBA", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MULTIPAK" to "Multi-Pak", "POKEMON" to "Pokémon • cabo", "WIRELESS" to "Adaptador sem fio").forEach { (id, label) ->
                        FilterChip(selected = mode == id, enabled = !connected && !connecting, onClick = {
                            mode = id; prefs.edit().putString("gba_mode", id).apply()
                        }, label = { Text(label) })
                    }
                }
                Text("Os dois aparelhos devem escolher o mesmo modo antes de criar a sala. Pokémon de GBA: teste cabo; Quetzal: teste adaptador sem fio. Compatibilidade com hacks ainda precisa de teste.")
                Text("GB/GBC usam o cabo do Gambatte. DS e 3DS ainda não usam esta sala.")
                Text("O primeiro teste é de cabo Multi-Pak: Mario Kart: Super Circuit é um bom ponto de partida. Os modos de Pokémon e adaptador sem fio precisam de testes de troca e batalha. DS e 3DS continuam singleplayer nesta candidata.")
                Text("Mantenha os dois jogos abertos; abrir menus ou apagar a tela pode interromper o cabo. Uma cópia do save anterior é guardada automaticamente. Estados automáticos, cheats e fast forward ficam desativados nesta sessão. Os saves feitos dentro do jogo continuam separados por aparelho.")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (connected || connecting) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.message, style = MaterialTheme.typography.titleMedium)
                    if (state.phase == KlWifiRoom.Phase.HOSTING) {
                        if (state.addresses.isEmpty()) Text("Nenhum IP local encontrado. Confira o Wi-Fi ou o ponto de acesso.")
                        state.addresses.forEach { address ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(address, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(address)) }) { Text("Copiar IP") }
                            }
                        }
                    }
                    if (state.phase == KlWifiRoom.Phase.JOINED) Text("Anfitrião: ${state.addresses.firstOrNull().orEmpty()}")
                    state.members.forEach { Text("${if (it.id == 0) "Anfitrião" else "Visitante ${it.id}"} • ${it.name}") }
                    Text("A sala continua aberta ao trocar de aba. Use Sair para encerrar a conexão.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { KlRoomService.leave(context) }) { Text(if (connecting) "Cancelar conexão" else "Sair da sala") }
                }
            }
        } else {
            if (state.phase == KlWifiRoom.Phase.ERROR) Text(state.message, color = MaterialTheme.colorScheme.error)
            OutlinedTextField(value = name, onValueChange = { name = it.take(32) }, label = { Text("Seu nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { start(true) }, modifier = Modifier.fillMaxWidth()) { Text("Criar sala • Anfitrião") }
            OutlinedTextField(value = ip, onValueChange = { ip = it.take(15) }, label = { Text("IP do anfitrião") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { start(false) }, modifier = Modifier.fillMaxWidth()) { Text("Entrar na sala • Visitante") }
        }
        Text("Se não conectar, confira se a rede permite comunicação entre aparelhos. Redes de convidados podem bloquear isso.", style = MaterialTheme.typography.bodySmall)
    }
}
