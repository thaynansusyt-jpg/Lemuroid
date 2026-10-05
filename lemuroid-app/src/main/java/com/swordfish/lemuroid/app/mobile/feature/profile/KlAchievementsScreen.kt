package com.swordfish.lemuroid.app.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.swordfish.lemuroid.app.shared.profile.KlAchievements
import com.swordfish.lemuroid.app.shared.profile.KlProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun KlAchievementsScreen(profile: KlProfileStore.Profile, onClose: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var onlyUnlocked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row { Text("Conquistas KL", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge); TextButton(onClick = onClose) { Text("✕") } }
                Text("${profile.earned.size} / ${KlAchievements.medals.size} • cada conquista libera uma medalha e uma cor para o Sii")
                LinearProgressIndicator(progress = profile.earned.size.toFloat() / KlAchievements.medals.size, modifier = Modifier.fillMaxWidth())
                Text("Conquistas do uso do KL. Não detectam tarefas dentro dos jogos e não são RetroAchievements.", style = MaterialTheme.typography.bodySmall)
                Row { FilterChip(selected = !onlyUnlocked, onClick = { onlyUnlocked = false }, label = { Text("Todas") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = onlyUnlocked, onClick = { onlyUnlocked = true }, label = { Text("Concluídas") }) }
                message?.let { Text(it) }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(KlAchievements.medals.filter { !onlyUnlocked || it.id in profile.earned }, key = { it.id }) { medal ->
                        val owned = medal.id in profile.earned
                        val progress = KlAchievements.progress(medal, profile.facts, profile.earned)
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${if (owned) "✓" else "○"} #${medal.id} · ${medal.title}", style = MaterialTheme.typography.titleMedium)
                                Text(medal.requirement)
                                Text(if (owned) "Conquista concluída" else "$progress / ${medal.target}", style = MaterialTheme.typography.bodySmall)
                                Text("● ${medal.reward}", color = if (owned) Color(android.graphics.Color.parseColor(medal.color)) else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (owned) TextButton(enabled = !saving, onClick = {
                                    saving = true
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val current = KlProfileStore.snapshot(context)
                                                require(current.key == profile.key) { "O perfil mudou. Abra as conquistas novamente." }
                                                val costume = when (medal.id) { 1 -> "sonic"; 100 -> "super_sonic"; else -> current.sii.clothes }
                                                KlProfileStore.update(context, current.name, current.sii.copy(badge = medal.id, shirt = medal.color, clothes = costume))
                                            }
                                            message = "Recompensa #${medal.id} aplicada ao Sii."; onRefresh()
                                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                        catch (e: Exception) { message = e.message ?: "Não foi possível salvar a recompensa." }
                                        finally { saving = false }
                                    }
                                }) { Text("Usar recompensa no Sii") }
                            }
                        }
                    }
                }
            }
        }
    }
}
